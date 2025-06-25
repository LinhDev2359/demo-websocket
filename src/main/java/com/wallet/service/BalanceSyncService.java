package com.wallet.service;

import com.wallet.dto.BalanceUpdateMessage;
import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import com.wallet.dto.SyncStatistics;
import com.wallet.entity.*;
import com.wallet.repository.BalanceRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WalletBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service để tự động sync balance từ EOS blockchain
 * Chạy scheduled job mỗi 5 phút và update database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSyncService {
    
    private final WalletRepository walletRepository;
    private final BalanceRepository balanceRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final EOSCircuitBreakerService eosCircuitBreakerService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Executor cho parallel processing - sẽ được quản lý bởi Spring
    // private final Executor syncExecutor = Executors.newFixedThreadPool(10);
    
    /**
     * Scheduled job chạy mỗi 5 phút để sync tất cả balance
     * Cron: 0 * /5 * * * * = mỗi 5 phút
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Async
    public void syncAllBalances() {
        log.info("Starting scheduled balance sync for all wallets");
        
        LocalDateTime startTime = LocalDateTime.now();
        int totalWallets = 0;
        int successCount = 0;
        int errorCount = 0;
        
        try {
            // Lấy tất cả wallets theo batch để tránh memory issues
            int pageSize = 100;
            int pageNumber = 0;
            
            Page<Wallet> walletPage;
            do {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                walletPage = walletRepository.findAllActiveWallets(pageable);
                
                if (!walletPage.hasContent()) {
                    break;
                }
                
                List<Wallet> wallets = walletPage.getContent();
                totalWallets += wallets.size();
                
                log.info("Processing batch {}: {} wallets", pageNumber + 1, wallets.size());
                
                // Sync balance cho batch này
                SyncResult batchResult = syncWalletBalancesBatch(wallets);
                successCount += batchResult.successCount;
                errorCount += batchResult.errorCount;
                
                pageNumber++;
                
            } while (walletPage.hasNext());
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            log.info("Completed balance sync: {} total wallets, {} success, {} errors, duration: {}ms",
                    totalWallets, successCount, errorCount, durationMs);
                    
        } catch (Exception e) {
            log.error("Error during scheduled balance sync", e);
        }
    }
    
    /**
     * Sync balance cho một batch wallets
     */
    private SyncResult syncWalletBalancesBatch(List<Wallet> wallets) {
        int successCount = 0;
        int errorCount = 0;
        
        // Tạo EOS API requests
        List<EOSBalanceRequest> requests = wallets.stream()
                .map(wallet -> EOSBalanceRequest.forEOSToken(wallet.getWalletAddress()))
                .toList();
        
        try {
            // Gọi EOS API parallel cho batch này
            List<CompletableFuture<EOSBalanceResponse>> futures = requests.stream()
                    .map(eosCircuitBreakerService::getWalletBalanceWithCircuitBreaker)
                    .toList();
            
            // Chờ tất cả complete và process results
            for (int i = 0; i < futures.size(); i++) {
                try {
                    EOSBalanceResponse response = futures.get(i).join();
                    Wallet wallet = wallets.get(i);
                    
                    if (response.isSuccess()) {
                        updateWalletBalance(wallet, response);
                        successCount++;
                        
                        // Send WebSocket update cho user
                        sendBalanceUpdateToUser(wallet, response);
                        
                    } else {
                        log.warn("Failed to sync balance for wallet: {}, error: {}", 
                                wallet.getWalletAddress(), response.getErrorMessage());
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing balance sync for wallet: {}", 
                            wallets.get(i).getWalletAddress(), e);
                    errorCount++;
                }
            }
            
        } catch (Exception e) {
            log.error("Error during batch balance sync", e);
            errorCount = wallets.size(); // Tất cả fail
        }
        
        return new SyncResult(successCount, errorCount);
    }
    
    /**
     * Sync balance cho một wallet cụ thể
     */
    @Async
    public CompletableFuture<Boolean> syncWalletBalance(String walletAddress) {
        log.info("Syncing balance for individual wallet: {}", walletAddress);
        
        try {
            // Tìm wallet
            Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletAddress));
            
            // Tạo EOS request
            EOSBalanceRequest request = EOSBalanceRequest.forEOSToken(walletAddress);
            
            // Gọi EOS API
            EOSBalanceResponse response = eosCircuitBreakerService
                    .getWalletBalanceWithCircuitBreaker(request)
                    .join();
            
            if (response.isSuccess()) {
                updateWalletBalance(wallet, response);
                sendBalanceUpdateToUser(wallet, response);
                
                log.info("Successfully synced balance for wallet: {}, balance: {}", 
                        walletAddress, response.getRawBalance());
                return CompletableFuture.completedFuture(true);
            } else {
                log.warn("Failed to sync balance for wallet: {}, error: {}", 
                        walletAddress, response.getErrorMessage());
                return CompletableFuture.completedFuture(false);
            }
            
        } catch (Exception e) {
            log.error("Error syncing balance for wallet: {}", walletAddress, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Update balance trong database
     */
    @Transactional
    public void updateWalletBalance(Wallet wallet, EOSBalanceResponse response) {
        try {
            // Tìm existing balance hoặc tạo mới
            Balance balance = balanceRepository
                    .findByWalletIdAndTokenType(wallet.getId(), TokenType.A)
                    .orElse(createNewBalance(wallet));
            
            // Check nếu balance thay đổi
            BigDecimal newBalance = response.getBalance();
            if (newBalance != null && balance.getBalance().compareTo(newBalance) != 0) {
                
                BigDecimal oldBalance = balance.getBalance();
                balance.setBalance(newBalance);
                balance.setAvailableBalance(newBalance);
                balance.setLastUpdated(LocalDateTime.now());
                
                // Save to database
                balanceRepository.save(balance);
                
                // ✅ NEW: Update WalletBalance junction table
                updateWalletBalanceRecord(wallet, balance, oldBalance, newBalance);
                
                log.info("Updated balance for wallet: {}, old: {}, new: {}", 
                        wallet.getWalletAddress(), oldBalance, newBalance);
            } else {
                log.debug("No balance change for wallet: {}, current: {}", 
                        wallet.getWalletAddress(), balance.getBalance());
            }
            
        } catch (Exception e) {
            log.error("Error updating balance in database for wallet: {}", 
                    wallet.getWalletAddress(), e);
            throw e;
        }
    }
    
    /**
     * Send WebSocket update cho user khi balance thay đổi
     */
    private void sendBalanceUpdateToUser(Wallet wallet, EOSBalanceResponse response) {
        try {
            // Validate input
            if (wallet == null || wallet.getUserId() == null || response == null) {
                log.warn("Invalid wallet or response for WebSocket update");
                return;
            }
            
            // Tạo balance update message
            BalanceUpdateMessage message = BalanceUpdateMessage.builder()
                    .walletAddress(wallet.getWalletAddress())
                    .tokenType(TokenType.A.name())
                    .balance(response.getBalance())
                    .rawBalance(response.getRawBalance())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Send to user's private channel
            String destination = "/topic/balance/" + wallet.getUserId();
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent balance update to user: {}, wallet: {}", 
                    wallet.getUserId(), wallet.getWalletAddress());
                    
        } catch (Exception e) {
            log.warn("Failed to send WebSocket balance update for wallet: {}", 
                    wallet.getWalletAddress(), e);
        }
    }
    
    /**
     * Get sync statistics
     */
    public SyncStatistics getSyncStatistics() {
        try {
            // Get total wallets count
            long totalWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);
            
            // Get total balances count (successful syncs)
            long successfulSyncs = balanceRepository.count();
            
            // TODO: Implement failed syncs tracking in future
            // This would require a separate sync_log table
            long failedSyncs = 0L;
            
            return SyncStatistics.builder()
                    .totalWallets(totalWallets)
                    .lastSyncTime(LocalDateTime.now())
                    .successfulSyncs(successfulSyncs)
                    .failedSyncs(failedSyncs)
                    .averageResponseTime(0.0) // TODO: Calculate from sync logs
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting sync statistics", e);
            
            // Return default stats on error
            return SyncStatistics.builder()
                    .totalWallets(0L)
                    .lastSyncTime(LocalDateTime.now())
                    .successfulSyncs(0L)
                    .failedSyncs(0L)
                    .averageResponseTime(0.0)
                    .build();
        }
    }
    
    /**
     * Create new Balance entity
     */
    private Balance createNewBalance(Wallet wallet) {
        Balance balance = new Balance();
        balance.setWalletId(wallet.getId());
        balance.setTokenType(TokenType.A);
        balance.setTokenSymbol("EOS");
        balance.setTokenContract("eosio.token");
        balance.setTokenName("EOS Native Token");
        balance.setBalance(BigDecimal.ZERO);
        balance.setAvailableBalance(BigDecimal.ZERO);
        balance.setLockedBalance(BigDecimal.ZERO);
        balance.setStakedBalance(BigDecimal.ZERO);
        balance.setUsdValue(BigDecimal.ZERO);
        balance.setTokenPriceUsd(BigDecimal.ZERO);
        balance.setPriceChange24h(BigDecimal.ZERO);
        balance.setStatus(BalanceStatus.ACTIVE);
        balance.setLastUpdated(LocalDateTime.now());
        return balance;
    }
    
    /**
     * Cleanup resources on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("BalanceSyncService is shutting down");
        // No cleanup needed as we use Spring's async executor
    }
    
    /**
     * Result class cho batch sync
     */
    private static class SyncResult {
        final int successCount;
        final int errorCount;
        
        SyncResult(int successCount, int errorCount) {
            this.successCount = successCount;
            this.errorCount = errorCount;
        }
    }
    
    /**
     * ✅ NEW: Update WalletBalance junction table khi balance thay đổi
     * 
     * Giải thích:
     * - WalletBalance table track history và changes
     * - Lưu previous amount để calculate percentage change
     * - Update sync status và timestamp
     */
    private void updateWalletBalanceRecord(Wallet wallet, Balance balance, 
                                         BigDecimal oldBalance, BigDecimal newBalance) {
        try {
            // Tìm existing WalletBalance record
            Optional<WalletBalance> existingRecord = walletBalanceRepository
                .findByWalletIdAndBalanceId(wallet.getId(), balance.getId());
            
            WalletBalance walletBalance;
            
            if (existingRecord.isPresent()) {
                // Update existing record
                walletBalance = existingRecord.get();
                walletBalance.setPreviousAmount(walletBalance.getCurrentAmount()); // Store old value
                walletBalance.setCurrentAmount(newBalance);
                walletBalance.setLastSyncedAt(LocalDateTime.now());
                walletBalance.setSyncStatus(WalletBalance.SyncStatus.SYNCED);
                
                log.debug("Updated existing WalletBalance record for wallet: {} balance: {}", 
                         wallet.getWalletAddress(), balance.getTokenSymbol());
                
            } else {
                // Create new record
                walletBalance = new WalletBalance();
                walletBalance.setWalletId(wallet.getId());
                walletBalance.setBalanceId(balance.getId());
                walletBalance.setTokenType(balance.getTokenType());
                walletBalance.setTokenSymbol(balance.getTokenSymbol());
                walletBalance.setTokenContract(balance.getTokenContract());
                walletBalance.setPreviousAmount(oldBalance);
                walletBalance.setCurrentAmount(newBalance);
                walletBalance.setUsdValue(balance.getUsdValue());
                walletBalance.setStatus(WalletBalance.WalletBalanceStatus.ACTIVE);
                walletBalance.setFirstDetectedAt(LocalDateTime.now());
                walletBalance.setLastSyncedAt(LocalDateTime.now());
                walletBalance.setSyncStatus(WalletBalance.SyncStatus.SYNCED);
                walletBalance.setIsPrimaryBalance(balance.getTokenSymbol().equals("EOS")); // EOS is primary
                walletBalance.setDisplayThreshold(BigDecimal.ZERO);
                
                log.debug("Created new WalletBalance record for wallet: {} balance: {}", 
                         wallet.getWalletAddress(), balance.getTokenSymbol());
            }
            
            // Save WalletBalance record
            walletBalanceRepository.save(walletBalance);
            
            log.debug("WalletBalance record saved successfully for wallet: {} token: {}", 
                     wallet.getWalletAddress(), balance.getTokenSymbol());
                     
        } catch (Exception e) {
            log.error("Error updating WalletBalance record for wallet: {} balance: {}", 
                     wallet.getWalletAddress(), balance.getTokenSymbol(), e);
            // Don't throw exception để không break main sync flow
        }
    }
}