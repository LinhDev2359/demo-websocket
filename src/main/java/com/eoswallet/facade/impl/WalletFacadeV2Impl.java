package com.eoswallet.facade.impl;

import com.eoswallet.dto.*;
import com.wallet.entity.*;
import com.wallet.repository.*;
import com.eoswallet.facade.WalletFacade;
import com.wallet.service.WalletService;
import com.wallet.service.UserService;
import com.wallet.service.HyperionService;
import com.wallet.dto.TokenInfo;
import com.wallet.dto.WalletTokensResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wallet Facade Implementation V2
 * 
 * ✅ REFACTORED: Sử dụng Junction Tables approach
 * 
 * Key Changes:
 * 1. ❌ NO MORE direct entity relationship navigation
 * 2. ✅ Sử dụng UserWalletRepository và WalletBalanceRepository
 * 3. ✅ Better performance với explicit queries
 * 4. ✅ Cleaner error handling và validation
 */
@Service
@Primary
@Slf4j
@Transactional
public class WalletFacadeV2Impl implements WalletFacade {

    @Autowired
    @Qualifier("walletServiceV2") // Use the V2 implementation
    private WalletService walletService;
    
    @Autowired
    private UserService userService;
    
    // ✅ NEW: Junction Table Repositories
    @Autowired
    private UserWalletRepository userWalletRepository;
    
    @Autowired
    private WalletBalanceRepository walletBalanceRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private HyperionService hyperionService;

    @Override
    public WalletCreateResponse createWallet(String userId, WalletCreateRequest request) {
        // ✅ IMPROVED: Auto-create user if not found
        log.info("Looking for user with userId: {}", userId);
        var userOpt = userService.getUserById(userId);
        User user;
        
        if (userOpt.isEmpty()) {
            log.warn("User not found with userId: {}. Auto-creating user...", userId);
            
            // Auto-create user with the exact userId from JWT token
            String username = "user_" + userId.substring(userId.lastIndexOf("_") + 1);
            String email = username + "@auto-generated.local";
            String defaultPassword = "TempPassword123!"; // This should be changed by user
            
            user = userService.createUserWithId(userId, username, email, defaultPassword, "Auto", "Generated");
            log.info("Auto-created user: {} with userId: {}", user.getUsername(), user.getUserId());
        } else {
            user = userOpt.get();
            log.info("Found existing user: {} with email: {}", user.getUserId(), user.getEmail());
        }
        
        // ✅ Use V2 wallet service với Junction Tables
        Wallet wallet = walletService.createWallet(userId, request.getEosAddress(), 
                                                 request.getName());
        
        // ✅ Get isPrimary from Junction Table
        Optional<UserWallet> userWalletOpt = userWalletRepository
            .findByUserIdAndWalletIdAndStatus(
                userId, 
                wallet.getId(), 
                UserWallet.UserWalletStatus.ACTIVE
            );
        
        boolean isPrimary = userWalletOpt.map(UserWallet::getIsPrimary).orElse(false);
        
        return WalletCreateResponse.builder()
                .walletId(wallet.getId())
                .name(wallet.getWalletName())
                .eosAddress(wallet.getWalletAddress())
                .isPrimary(isPrimary)
                .message("Wallet created successfully using Junction Tables")
                .build();
    }

    @Override
    public WalletResponse getWallet(Long walletId, String userId) {
        // ✅ IMPROVED: Validate user exists
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ Get wallet by ID (not address)
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found: " + walletId);
        }
        
        Wallet wallet = walletOpt.get();
        
        // ✅ NEW APPROACH: Verify ownership through Junction Table
        boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
            userId, walletId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied: Wallet does not belong to user");
        }
        
        return mapToWalletResponseV2(wallet, userId);
    }

    @Override
    public Page<WalletResponse> getUserWallets(String userId, Pageable pageable) {
        // ✅ IMPROVED: Validate user exists
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ NEW APPROACH: Get wallets through Junction Table với pagination
        Page<UserWallet> userWalletPage = userWalletRepository.findByUserIdAndStatus(
            userId, UserWallet.UserWalletStatus.ACTIVE, pageable);
        
        // Get wallet IDs
        List<Long> walletIds = userWalletPage.getContent().stream()
            .map(UserWallet::getWalletId)
            .collect(Collectors.toList());
        
        if (walletIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Get actual wallets
        List<Wallet> wallets = walletRepository.findAllById(walletIds);
        
        // Map to responses với Junction Table data
        List<WalletResponse> walletResponses = wallets.stream()
            .map(wallet -> mapToWalletResponseV2(wallet, userId))
            .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
            walletResponses, pageable, userWalletPage.getTotalElements());
    }

    @Override
    public WalletResponse updateWallet(Long walletId, String userId, WalletUpdateRequest request) {
        // ✅ IMPROVED: Better validation
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ Get wallet by ID
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found: " + walletId);
        }
        
        Wallet existingWallet = walletOpt.get();
        
        // ✅ Use V2 wallet service
        Wallet wallet = walletService.updateWalletName(userId, 
                                                      existingWallet.getWalletAddress(), 
                                                      request.getName());
        return mapToWalletResponseV2(wallet, userId);
    }

    @Override
    public void deleteWallet(Long walletId, String userId) {
        // ✅ IMPROVED: Better validation
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ Get wallet by ID
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found: " + walletId);
        }
        
        Wallet wallet = walletOpt.get();
        
        // ✅ Use V2 wallet service
        walletService.deleteWallet(userId, wallet.getWalletAddress());
    }

    @Override
    public WalletResponse setPrimaryWallet(Long walletId, String userId) {
        // ✅ IMPROVED: Better validation
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ Get wallet by ID
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found: " + walletId);
        }
        
        Wallet existingWallet = walletOpt.get();
        
        // ✅ Use V2 wallet service
        Wallet wallet = walletService.setPrimaryWallet(userId, 
                                                      existingWallet.getWalletAddress());
        return mapToWalletResponseV2(wallet, userId);
    }

    @Override
    public WalletResponse getPrimaryWallet(String userId) {
        // ✅ IMPROVED: Better validation
        var userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // ✅ Use V2 wallet service
        var walletOpt = walletService.getPrimaryWallet(userId);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Primary wallet not found for user: " + userId);
        }
        
        return mapToWalletResponseV2(walletOpt.get(), userId);
    }

    @Override
    public BalanceResponse getWalletBalance(Long walletId, String userId) {
        // ✅ NEW APPROACH: Get wallet balance through Junction Tables
        
        // Validate access
        boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
            userId, walletId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied: Wallet does not belong to user");
        }
        
        // Get wallet balances through Junction Table
        List<WalletBalance> walletBalances = walletBalanceRepository.findByWalletIdAndStatus(
            walletId, WalletBalance.WalletBalanceStatus.ACTIVE);
        
        if (walletBalances.isEmpty()) {
            return BalanceResponse.builder()
                    .balanceId(0L)
                    .walletId(walletId)
                    .amount(BigDecimal.ZERO)
                    .currency("EOS")
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();
        }
        
        // Get primary balance or first one
        WalletBalance primaryBalance = walletBalances.stream()
            .filter(WalletBalance::getIsPrimaryBalance)
            .findFirst()
            .orElse(walletBalances.get(0));
        
        return BalanceResponse.builder()
                .balanceId(primaryBalance.getBalanceId())
                .walletId(walletId)
                .amount(primaryBalance.getCurrentAmount())
                .currency(primaryBalance.getTokenSymbol())
                .lastUpdated(primaryBalance.getLastSyncedAt())
                .build();
    }

    @Override
    public List<BalanceResponse> getAllUserBalances(String userId) {
        // ✅ NEW APPROACH: Get all user balances through Junction Tables
        
        // Get user's wallet IDs
        List<Long> walletIds = userWalletRepository.findWalletIdsByUserId(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (walletIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get all balances for these wallets
        List<WalletBalance> allBalances = walletBalanceRepository.findByWalletIdInAndStatus(
            walletIds, WalletBalance.WalletBalanceStatus.ACTIVE);
        
        // Map to BalanceResponse
        return allBalances.stream()
            .map(wb -> BalanceResponse.builder()
                .balanceId(wb.getBalanceId())
                .walletId(wb.getWalletId())
                .amount(wb.getCurrentAmount())
                .currency(wb.getTokenSymbol())
                .lastUpdated(wb.getLastSyncedAt())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public BalanceResponse updateBalance(Long walletId, BigDecimal amount, String operation) {
        // TODO: Implement balance update using Junction Tables
        // This would involve updating WalletBalance table
        throw new RuntimeException("Balance operations will be implemented with Junction Tables approach");
    }

    @Override
    public TransferResponse transferBetweenWallets(Long fromWalletId, Long toWalletId, 
                                                 BigDecimal amount, String userId) {
        // TODO: Implement transfer using Junction Tables
        // This would involve:
        // 1. Verify user has access to both wallets through UserWallet table
        // 2. Update balances through WalletBalance table
        throw new RuntimeException("Transfer functionality will be implemented with Junction Tables approach");
    }

    @Override
    public void syncWalletWithBlockchain(Long walletId, String userId) {
        // TODO: Implement blockchain sync using Junction Tables
        // This would update WalletBalance table với blockchain data
        throw new RuntimeException("Blockchain sync will be implemented with Junction Tables approach");
    }

    @Override
    public void syncAllUserWalletsWithBlockchain(String userId) {
        // ✅ NEW APPROACH: Sync all user wallets using Junction Tables
        
        // Get user's wallet IDs
        List<Long> walletIds = userWalletRepository.findWalletIdsByUserId(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        
        // TODO: Implement bulk sync logic
        for (Long walletId : walletIds) {
            try {
                syncWalletWithBlockchain(walletId, userId);
            } catch (Exception e) {
                // Log error but continue với other wallets
                System.err.println("Failed to sync wallet " + walletId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public boolean validateEosAddress(String eosAddress) {
        return walletService.isValidEOSAddress(eosAddress);
    }

    @Override
    public WalletResponse importExistingWallet(String userId, String privateKey, String eosAddress) {
        // TODO: Implement wallet import with Junction Tables
        throw new RuntimeException("Wallet import will be implemented with Junction Tables approach");
    }

    @Override
    public String exportWalletPrivateKey(Long walletId, String userId, String password) {
        // TODO: Implement wallet export with security checks via Junction Tables
        throw new RuntimeException("Wallet export will be implemented with Junction Tables approach");
    }

    @Override
    public void backupWallet(Long walletId, String userId) {
        // TODO: Implement wallet backup
        throw new RuntimeException("Wallet backup will be implemented with Junction Tables approach");
    }

    @Override
    public WalletResponse restoreWallet(String userId, String backupData, String password) {
        // TODO: Implement wallet restore
        throw new RuntimeException("Wallet restore will be implemented with Junction Tables approach");
    }

    /**
     * ✅ NEW METHOD: Map Wallet to WalletResponse using Junction Table data
     */
    private WalletResponse mapToWalletResponseV2(Wallet wallet, String userId) {
        // Get isPrimary from Junction Table
        Optional<UserWallet> userWalletOpt = userWalletRepository
            .findByUserIdAndWalletIdAndStatus(
                userId, 
                wallet.getId(), 
                UserWallet.UserWalletStatus.ACTIVE
            );
        
        boolean isPrimary = userWalletOpt.map(UserWallet::getIsPrimary).orElse(false);
        
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .name(wallet.getWalletName())
                .eosAddress(wallet.getWalletAddress())
                .isPrimary(isPrimary) // ✅ From Junction Table, not entity
                .isActive(wallet.getStatus() == WalletStatus.ACTIVE)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
    
    @Override
    public WalletTokensResponse getWalletTokens(Long walletId, String userId) {
        log.info("Getting tokens for wallet: {}, user: {}", walletId, userId);
        
        try {
            // ✅ STEP 1: Validate user exists
            var userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return WalletTokensResponse.error(walletId, null, "User not found: " + userId);
            }
            
            // ✅ STEP 2: Get wallet by ID
            Optional<Wallet> walletOpt = walletRepository.findById(walletId);
            if (walletOpt.isEmpty()) {
                return WalletTokensResponse.error(walletId, null, "Wallet not found: " + walletId);
            }
            
            Wallet wallet = walletOpt.get();
            
            // ✅ STEP 3: Verify ownership through Junction Table
            boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
                userId, walletId, UserWallet.UserWalletStatus.ACTIVE);
            
            if (!hasAccess) {
                return WalletTokensResponse.error(walletId, wallet.getWalletAddress(), 
                    "Access denied: Wallet does not belong to user");
            }
            
            // ✅ STEP 4: Validate EOS address format
            String walletAddress = wallet.getWalletAddress();
            if (!hyperionService.isValidEosAccount(walletAddress)) {
                return WalletTokensResponse.error(walletId, walletAddress, 
                    "Invalid EOS address format: " + walletAddress);
            }
            
            // ✅ STEP 5: Call Hyperion API to get tokens
            List<TokenInfo> tokens = hyperionService.getAccountTokens(walletAddress);
            
            // ✅ STEP 6: Return successful response
            WalletTokensResponse response = WalletTokensResponse.success(
                walletId, walletAddress, wallet.getWalletName(), tokens);
            
            log.info("Successfully retrieved {} tokens for wallet: {}", tokens.size(), walletId);
            return response;
            
        } catch (Exception e) {
            log.error("Error getting tokens for wallet: {}, user: {}", walletId, userId, e);
            return WalletTokensResponse.error(walletId, null, 
                "Failed to get tokens: " + e.getMessage());
        }
    }
}