package com.wallet.controller;

import com.wallet.dto.BalanceResponse;
import com.wallet.entity.TokenType;
import com.wallet.security.CustomUserPrincipal;
import com.wallet.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Balance Controller
 * API endpoints for managing and retrieving EOS token balances
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
@Tag(name = "Balance Management", description = "APIs for EOS token balance operations")
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * Get all balances for authenticated user's wallets
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get user's token balances",
        description = "Retrieve all token balances for the authenticated user's wallets"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balances")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<Page<BalanceResponse>> getUserBalances(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by minimum balance")
            @RequestParam(required = false) BigDecimal minBalance,
            @Parameter(description = "Filter by token type")
            @RequestParam(required = false) TokenType tokenType) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting balances for user: {}", userId);

            // Create pagination and sorting
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get balances using service
            Page<BalanceResponse> balances = balanceService.getUserBalances(userId, pageable, minBalance, tokenType);

            log.info("Retrieved {} balances for user: {}", balances.getContent().size(), userId);
            return ResponseEntity.ok(balances);

        } catch (Exception e) {
            log.error("Error retrieving balances for user: {}", userPrincipal.getUserId(), e);
            throw new RuntimeException("Failed to retrieve balances", e);
        }
    }

    /**
     * Get balance for specific wallet address
     */
    @GetMapping("/wallet/{walletAddress}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get balances for specific wallet",
        description = "Retrieve token balances for a specific wallet address"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved wallet balances")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<List<BalanceResponse>> getWalletBalances(
            @PathVariable String walletAddress,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting balances for wallet: {} by user: {}", walletAddress, userId);

            List<BalanceResponse> balances = balanceService.getWalletBalances(walletAddress, userId);

            log.info("Retrieved {} balances for wallet: {}", balances.size(), walletAddress);
            return ResponseEntity.ok(balances);

        } catch (Exception e) {
            log.error("Error retrieving balances for wallet: {}", walletAddress, e);
            throw new RuntimeException("Failed to retrieve wallet balances", e);
        }
    }

    /**
     * Get balance summary by token type
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get balance summary by token type",
        description = "Retrieve aggregated balance summary grouped by token type"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balance summary")
    public ResponseEntity<Map<String, BigDecimal>> getBalanceSummary(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting balance summary for user: {}", userId);

            Map<String, BigDecimal> summary = balanceService.getBalanceSummary(userId);

            log.info("Retrieved balance summary for user: {} with {} entries", userId, summary.size());
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error retrieving balance summary for user: {}", userPrincipal.getUserId(), e);
            throw new RuntimeException("Failed to retrieve balance summary", e);
        }
    }

    /**
     * Get total portfolio value in USD
     */
    @GetMapping("/portfolio-value")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get total portfolio value",
        description = "Retrieve total portfolio value in USD for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved portfolio value")
    public ResponseEntity<Map<String, Object>> getPortfolioValue(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting portfolio value for user: {}", userId);

            BigDecimal totalValue = balanceService.getTotalPortfolioValue(userId);
            
            Map<String, Object> response = Map.of(
                "userId", userId,
                "totalValueUsd", totalValue,
                "currency", "USD",
                "timestamp", java.time.LocalDateTime.now()
            );

            log.info("Retrieved portfolio value for user: {} = ${}", userId, totalValue);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving portfolio value for user: {}", userPrincipal.getUserId(), e);
            throw new RuntimeException("Failed to retrieve portfolio value", e);
        }
    }

    /**
     * Get balance statistics for user
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get balance statistics",
        description = "Retrieve balance statistics for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balance statistics")
    public ResponseEntity<Map<String, Object>> getBalanceStatistics(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting balance statistics for user: {}", userId);

            Map<String, Object> statistics = balanceService.getBalanceStatistics(userId);

            log.info("Retrieved balance statistics for user: {}", userId);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error retrieving balance statistics for user: {}", userPrincipal.getUserId(), e);
            throw new RuntimeException("Failed to retrieve balance statistics", e);
        }
    }

    /**
     * Refresh balance for specific wallet
     */
    @PostMapping("/wallet/{walletAddress}/refresh")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Refresh wallet balance",
        description = "Trigger a balance refresh for a specific wallet"
    )
    @ApiResponse(responseCode = "200", description = "Balance refresh triggered successfully")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<Map<String, Object>> refreshWalletBalance(
            @PathVariable String walletAddress,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Refreshing balance for wallet: {} by user: {}", walletAddress, userId);

            boolean success = balanceService.refreshWalletBalance(walletAddress, userId);

            Map<String, Object> response = Map.of(
                "walletAddress", walletAddress,
                "refreshTriggered", success,
                "timestamp", java.time.LocalDateTime.now()
            );

            log.info("Balance refresh for wallet: {} = {}", walletAddress, success);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error refreshing balance for wallet: {}", walletAddress, e);
            throw new RuntimeException("Failed to refresh wallet balance", e);
        }
    }

    /**
     * Get specific token balance for wallet
     */
    @GetMapping("/wallet/{walletAddress}/token/{tokenType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get specific token balance",
        description = "Retrieve balance for a specific token type in a wallet"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved token balance")
    @ApiResponse(responseCode = "404", description = "Balance not found")
    public ResponseEntity<BalanceResponse> getWalletTokenBalance(
            @PathVariable String walletAddress,
            @PathVariable TokenType tokenType,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            String userId = userPrincipal.getUserId();
            log.info("Getting {} balance for wallet: {} by user: {}", tokenType, walletAddress, userId);

            Optional<BalanceResponse> balance = balanceService.getWalletTokenBalance(walletAddress, tokenType, userId);

            if (balance.isPresent()) {
                log.info("Found {} balance for wallet: {}", tokenType, walletAddress);
                return ResponseEntity.ok(balance.get());
            } else {
                log.info("No {} balance found for wallet: {}", tokenType, walletAddress);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving {} balance for wallet: {}", tokenType, walletAddress, e);
            throw new RuntimeException("Failed to retrieve token balance", e);
        }
    }
}