package com.wallet.service.impl;

import com.wallet.dto.BalanceResponse;
import com.wallet.entity.Balance;
import com.wallet.entity.TokenType;
import com.wallet.entity.Wallet;
import com.wallet.entity.WalletStatus;
import com.wallet.repository.BalanceRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.service.BalanceService;
import com.wallet.service.BalanceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Balance Service Implementation
 * Implements business logic for EOS token balance operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceServiceImpl implements BalanceService {

    private final BalanceRepository balanceRepository;
    private final WalletRepository walletRepository;
    private final BalanceSyncService balanceSyncService;

    @Override
    public Page<BalanceResponse> getUserBalances(String userId, Pageable pageable, 
                                               BigDecimal minBalance, TokenType tokenType) {
        log.info("Getting balances for user: {} with filters - minBalance: {}, tokenType: {}", 
                userId, minBalance, tokenType);

        try {
            // Get user's active wallets
            List<Wallet> userWallets = walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE);
            
            if (userWallets.isEmpty()) {
                log.info("No active wallets found for user: {}", userId);
                return Page.empty(pageable);
            }

            List<Long> walletIds = userWallets.stream()
                    .map(Wallet::getId)
                    .collect(Collectors.toList());

            // Get balances for user's wallets
            List<Balance> balances = balanceRepository.findByWalletIdIn(walletIds);

            // Apply filters
            List<Balance> filteredBalances = balances.stream()
                    .filter(balance -> {
                        // Filter by minimum balance
                        if (minBalance != null && balance.getBalance().compareTo(minBalance) < 0) {
                            return false;
                        }
                        // Filter by token type
                        if (tokenType != null && !balance.getTokenType().equals(tokenType)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Convert to DTOs
            List<BalanceResponse> balanceResponses = filteredBalances.stream()
                    .map(this::convertToBalanceResponse)
                    .collect(Collectors.toList());

            // Apply pagination manually (for demo - in production use repository pagination)
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), balanceResponses.size());
            
            if (start > balanceResponses.size()) {
                return Page.empty(pageable);
            }

            List<BalanceResponse> pageContent = balanceResponses.subList(start, end);
            
            log.info("Retrieved {} balances for user: {} (page {}/{})", 
                    pageContent.size(), userId, pageable.getPageNumber(), 
                    (balanceResponses.size() + pageable.getPageSize() - 1) / pageable.getPageSize());

            return new PageImpl<>(pageContent, pageable, balanceResponses.size());

        } catch (Exception e) {
            log.error("Error retrieving balances for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve user balances", e);
        }
    }

    @Override
    public List<BalanceResponse> getWalletBalances(String walletAddress, String userId) {
        log.info("Getting balances for wallet: {} by user: {}", walletAddress, userId);

        try {
            // Verify wallet exists and belongs to user
            Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));

            if (!wallet.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied: wallet does not belong to user");
            }

            if (!wallet.getStatus().equals(WalletStatus.ACTIVE)) {
                throw new RuntimeException("Wallet is not active: " + walletAddress);
            }

            // Get balances for this wallet
            List<Balance> balances = balanceRepository.findByWalletId(wallet.getId());

            List<BalanceResponse> balanceResponses = balances.stream()
                    .map(this::convertToBalanceResponse)
                    .collect(Collectors.toList());

            log.info("Retrieved {} balances for wallet: {}", balanceResponses.size(), walletAddress);
            return balanceResponses;

        } catch (Exception e) {
            log.error("Error retrieving balances for wallet: {}", walletAddress, e);
            throw new RuntimeException("Failed to retrieve wallet balances", e);
        }
    }

    @Override
    public Map<String, BigDecimal> getBalanceSummary(String userId) {
        log.info("Getting balance summary for user: {}", userId);

        try {
            // Get portfolio breakdown from repository
            List<Object[]> portfolioData = balanceRepository.getPortfolioBreakdown(userId);

            Map<String, BigDecimal> summary = new HashMap<>();

            for (Object[] row : portfolioData) {
                Integer tokenTypeIndex = (Integer) row[0];
                BigDecimal totalBalance = (BigDecimal) row[1];
                BigDecimal totalUsdValue = (BigDecimal) row[2];

                String tokenTypeName = getTokenTypeNameByIndex(tokenTypeIndex);
                summary.put(tokenTypeName + "_balance", totalBalance);
                summary.put(tokenTypeName + "_usd_value", totalUsdValue);
            }

            log.info("Retrieved balance summary for user: {} with {} entries", userId, summary.size());
            return summary;

        } catch (Exception e) {
            log.error("Error retrieving balance summary for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve balance summary", e);
        }
    }

    @Override
    public BigDecimal getTotalPortfolioValue(String userId) {
        log.info("Getting total portfolio value for user: {}", userId);

        try {
            BigDecimal totalValue = balanceRepository.getTotalPortfolioValue(userId);
            
            log.info("Total portfolio value for user: {} = ${}", userId, totalValue);
            return totalValue != null ? totalValue : BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("Error retrieving portfolio value for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve portfolio value", e);
        }
    }

    @Override
    public Optional<BalanceResponse> getWalletTokenBalance(String walletAddress, TokenType tokenType, String userId) {
        log.info("Getting {} balance for wallet: {} by user: {}", tokenType, walletAddress, userId);

        try {
            // Verify wallet exists and belongs to user
            Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));

            if (!wallet.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied: wallet does not belong to user");
            }

            // Get specific balance
            Optional<Balance> balance = balanceRepository.findByWalletIdAndTokenType(wallet.getId(), tokenType);

            if (balance.isPresent()) {
                BalanceResponse response = convertToBalanceResponse(balance.get());
                log.info("Found {} balance for wallet: {} = {}", tokenType, walletAddress, response.getBalance());
                return Optional.of(response);
            } else {
                log.info("No {} balance found for wallet: {}", tokenType, walletAddress);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error retrieving {} balance for wallet: {}", tokenType, walletAddress, e);
            throw new RuntimeException("Failed to retrieve wallet token balance", e);
        }
    }

    @Override
    public Map<String, Object> getBalanceStatistics(String userId) {
        log.info("Getting balance statistics for user: {}", userId);

        try {
            // Get user's wallets
            List<Wallet> userWallets = walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE);
            List<Long> walletIds = userWallets.stream().map(Wallet::getId).collect(Collectors.toList());

            if (walletIds.isEmpty()) {
                return Map.of(
                    "totalWallets", 0,
                    "totalBalances", 0,
                    "nonZeroBalances", 0,
                    "totalUsdValue", BigDecimal.ZERO
                );
            }

            // Get balances
            List<Balance> balances = balanceRepository.findByWalletIdIn(walletIds);

            // Calculate statistics
            long totalBalances = balances.size();
            long nonZeroBalances = balances.stream()
                    .mapToLong(b -> b.getBalance().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                    .sum();
            
            BigDecimal totalUsdValue = balances.stream()
                    .map(b -> b.getUsdValue() != null ? b.getUsdValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> statistics = Map.of(
                "userId", userId,
                "totalWallets", userWallets.size(),
                "totalBalances", totalBalances,
                "nonZeroBalances", nonZeroBalances,
                "totalUsdValue", totalUsdValue,
                "averageBalancePerWallet", 
                    userWallets.size() > 0 ? totalUsdValue.divide(BigDecimal.valueOf(userWallets.size()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                "lastCalculated", LocalDateTime.now()
            );

            log.info("Calculated balance statistics for user: {}", userId);
            return statistics;

        } catch (Exception e) {
            log.error("Error calculating balance statistics for user: {}", userId, e);
            throw new RuntimeException("Failed to calculate balance statistics", e);
        }
    }

    @Override
    @Transactional
    public boolean refreshWalletBalance(String walletAddress, String userId) {
        log.info("Refreshing balance for wallet: {} by user: {}", walletAddress, userId);

        try {
            // Verify wallet exists and belongs to user
            Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));

            if (!wallet.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied: wallet does not belong to user");
            }

            // Trigger balance sync
            balanceSyncService.syncWalletBalance(walletAddress).join(); // Wait for completion

            log.info("Successfully refreshed balance for wallet: {}", walletAddress);
            return true;

        } catch (Exception e) {
            log.error("Error refreshing balance for wallet: {}", walletAddress, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getBalanceHistory(String walletAddress, String userId, int days) {
        log.info("Getting balance history for wallet: {} by user: {} ({} days)", walletAddress, userId, days);

        try {
            // Verify wallet access
            Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));

            if (!wallet.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied: wallet does not belong to user");
            }

            // For now, return current balance as a single history point
            // In a real implementation, you would have a balance_history table
            List<Balance> currentBalances = balanceRepository.findByWalletId(wallet.getId());

            List<Map<String, Object>> history = currentBalances.stream()
                    .map(balance -> Map.<String, Object>of(
                        "date", balance.getLastUpdated(),
                        "tokenType", balance.getTokenType().name(),
                        "balance", balance.getBalance(),
                        "usdValue", balance.getUsdValue() != null ? balance.getUsdValue() : BigDecimal.ZERO
                    ))
                    .collect(Collectors.toList());

            log.info("Retrieved {} balance history entries for wallet: {}", history.size(), walletAddress);
            return history;

        } catch (Exception e) {
            log.error("Error retrieving balance history for wallet: {}", walletAddress, e);
            throw new RuntimeException("Failed to retrieve balance history", e);
        }
    }

    /**
     * Convert Balance entity to BalanceResponse DTO
     */
    private BalanceResponse convertToBalanceResponse(Balance balance) {
        return BalanceResponse.builder()
                .id(balance.getId())
                .walletId(balance.getWalletId())
                .tokenType(balance.getTokenType().name())
                .tokenSymbol(balance.getTokenSymbol())
                .tokenContract(balance.getTokenContract())
                .tokenName(balance.getTokenName())
                .balance(balance.getBalance())
                .availableBalance(balance.getAvailableBalance())
                .lockedBalance(balance.getLockedBalance())
                .stakedBalance(balance.getStakedBalance())
                .usdValue(balance.getUsdValue())
                .tokenPriceUsd(balance.getTokenPriceUsd())
                .priceChange24h(balance.getPriceChange24h())
                .lastUpdated(balance.getLastUpdated())
                .lastTransactionAt(balance.getLastTransactionAt())
                .status(balance.getStatus().name())
                .decimalPlaces(balance.getDecimalPlaces())
                .build();
    }

    /**
     * Get token type name by database index
     */
    private String getTokenTypeNameByIndex(Integer index) {
        if (index == null) return "UNKNOWN";
        
        try {
            TokenType[] values = TokenType.values();
            if (index >= 0 && index < values.length) {
                return values[index].name();
            }
        } catch (Exception e) {
            log.warn("Invalid token type index: {}", index);
        }
        
        return "UNKNOWN";
    }
}