package com.wallet.controller;

import com.wallet.dto.PortfolioResponse;
import com.wallet.dto.PortfolioListRequest;
import com.wallet.dto.PortfolioListResponse;
import com.wallet.service.PortfolioService;
import com.wallet.security.JwtTokenUtil;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WalletBalanceRepository;
import com.wallet.entity.Wallet;
import com.wallet.entity.WalletBalance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Portfolio REST Controller
 * 
 * HTTP REST API endpoints cho portfolio data
 * Bổ sung cho WebSocket endpoints trong PortfolioWebSocketController
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PortfolioController {

    @Qualifier("portfolioServiceV2")
    private final PortfolioService portfolioService;
    
    private final JwtTokenUtil jwtTokenUtil;
    
    private final WalletRepository walletRepository;
    
    private final WalletBalanceRepository walletBalanceRepository;

    /**
     * Get user portfolio via HTTP REST
     * 
     * GET /api/portfolio/{userId}
     * 
     * @param userId User ID
     * @param refresh Whether to refresh cache
     * @param httpRequest HTTP request for JWT extraction
     * @return Portfolio data
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PortfolioResponse> getUserPortfolio(
            @PathVariable @NotNull String userId,
            @RequestParam(value = "refresh", defaultValue = "false") Boolean refresh,
            HttpServletRequest httpRequest) {
        
        String authenticatedUserId = extractUserIdFromRequest(httpRequest);
        log.info("Get portfolio request for userId: {}, refresh: {}, authenticated: {}", 
                userId, refresh, authenticatedUserId);
        
        // Validate user can access this portfolio
        if (!userId.equals(authenticatedUserId)) {
            log.warn("Access denied: user {} attempted to access portfolio for user {}", 
                    authenticatedUserId, userId);
            return ResponseEntity.status(403).build();
        }
        
        try {
            PortfolioResponse portfolio = portfolioService.getUserPortfolio(userId, refresh);
            
            if (portfolio == null) {
                log.warn("Portfolio not found for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(portfolio);
            
        } catch (Exception e) {
            log.error("Error getting portfolio for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 🆕 NEW: Get Portfolio List via REST API
     * 
     * POST /portfolio/list
     * 
     * @param request Portfolio list request
     * @param httpRequest HTTP request for JWT extraction
     * @return Portfolio list response
     */
    @PostMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PortfolioListResponse> getPortfolioList(
            @Valid @RequestBody PortfolioListRequest request,
            HttpServletRequest httpRequest) {
        
        String authenticatedUserId = extractUserIdFromRequest(httpRequest);
        log.info("Portfolio list REST request: {}, authenticated: {}", request, authenticatedUserId);
        
        // Validate user can access this portfolio
        if (!request.getUserId().equals(authenticatedUserId)) {
            log.warn("Access denied: user {} attempted to access portfolio for user {}", 
                    authenticatedUserId, request.getUserId());
            return ResponseEntity.status(403).build();
        }
        
        try {
            // Create pageable object
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
            
            // Get enriched portfolio data với ĐẦY ĐỦ thông tin từ database
            PortfolioListResponse response = getEnrichedPortfolioList(
                request.getUserId(), request, pageable);
            
            if (response != null && !response.getPortfolios().isEmpty()) {
                log.info("Portfolio list sent successfully for userId: {} with {} wallets", 
                    request.getUserId(), response.getPortfolios().size());
                return ResponseEntity.ok(response);
                
            } else {
                log.warn("No portfolios found for userId: {}", request.getUserId());
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting portfolio list for userId: {}", request.getUserId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Subscribe to portfolio updates (redirect to WebSocket info)
     * 
     * GET /api/portfolio/subscribe/{userId}
     * 
     * @param userId User ID
     * @return WebSocket connection info
     */
    @GetMapping("/subscribe/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PortfolioWebSocketInfo> getPortfolioSubscriptionInfo(
            @PathVariable @NotNull String userId,
            HttpServletRequest httpRequest) {
        
        String authenticatedUserId = extractUserIdFromRequest(httpRequest);
        log.info("Portfolio subscription info request for userId: {}, authenticated: {}", 
                userId, authenticatedUserId);
        
        // Validate user can access this portfolio
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(403).build();
        }
        
        PortfolioWebSocketInfo info = PortfolioWebSocketInfo.builder()
                .message("Portfolio subscriptions are available via WebSocket")
                .websocketEndpoint("/ws")
                .subscriptionDestination("/app/portfolio/subscribe/" + userId)
                .responseDestination("/user/queue/portfolio")
                .instructions("Connect to WebSocket endpoint and subscribe to destination")
                .example("stompClient.subscribe('/user/queue/portfolio', callback)")
                .jwtRequired(true)
                .build();
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * 🆕 NEW: Get enriched portfolio data với đầy đủ thông tin
     */
    private PortfolioListResponse getEnrichedPortfolioList(
            String userId, PortfolioListRequest request, Pageable pageable) {
        
        // Get wallets for user
        List<Wallet> userWallets = walletRepository.findByUserId(userId);
        log.info("🔍 Found {} wallets for userId: {}", userWallets.size(), userId);
        
        if (!userWallets.isEmpty()) {
            userWallets.forEach(wallet -> 
                log.info("📝 Wallet: ID={}, Address={}, Type={}, Status={}", 
                    wallet.getId(), wallet.getWalletAddress(), 
                    wallet.getWalletType(), wallet.getStatus()));
        }
        
        // Apply pagination manually (vì chúng ta cần enrich data)
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), userWallets.size());
        List<Wallet> paginatedWallets = userWallets.subList(start, end);
        
        // Create portfolio items với đầy đủ data từ entities
        List<PortfolioListResponse.PortfolioItem> portfolioItems = 
            paginatedWallets.stream().map(wallet -> {
                
                // Get all balances for this wallet từ WalletBalance junction table
                List<WalletBalance> walletBalances = walletBalanceRepository.findByWalletIdAndStatus(
                    wallet.getId(), WalletBalance.WalletBalanceStatus.ACTIVE);
                
                log.info("💰 Found {} balances for walletId: {} ({})", 
                    walletBalances.size(), wallet.getId(), wallet.getWalletAddress());
                
                if (!walletBalances.isEmpty()) {
                    walletBalances.forEach(wb -> 
                        log.info("🪙 Balance: ID={}, Token={}, Amount={}, USD={}", 
                            wb.getBalanceId(), wb.getTokenSymbol(), 
                            wb.getCurrentAmount(), wb.getUsdValue()));
                }
                
                // Convert balances với ĐẦY ĐỦ thông tin từ WalletBalance entity
                List<PortfolioListResponse.BalanceItem> balanceItems = 
                    walletBalances.stream().map(walletBalance -> 
                        PortfolioListResponse.BalanceItem.builder()
                            .balanceId(walletBalance.getBalanceId()) // ✅ Real Balance ID
                            .tokenSymbol(walletBalance.getTokenSymbol()) // ✅ Real symbol
                            .tokenName(walletBalance.getTokenSymbol()) // ✅ Using symbol as name
                            .tokenContract(walletBalance.getTokenContract()) // ✅ Real contract
                            .balance(walletBalance.getCurrentAmount()) // ✅ Real current balance
                            .availableBalance(walletBalance.getCurrentAmount()) // ✅ Current as available
                            .lockedBalance(BigDecimal.ZERO) // Not tracked in WalletBalance
                            .stakedBalance(BigDecimal.ZERO) // Not tracked in WalletBalance
                            .usdValue(walletBalance.getUsdValue()) // ✅ Real USD value
                            .tokenPriceUsd(BigDecimal.ZERO) // Not tracked in WalletBalance
                            .priceChange24h(walletBalance.hasBalanceChanged() ? 
                                walletBalance.getChangePercentage() : BigDecimal.ZERO) // ✅ Real change
                            .isNativeToken(walletBalance.getIsPrimaryBalance()) // ✅ Primary as native
                            .decimalPlaces(4) // Default decimal places
                            .lastUpdated(walletBalance.getLastSyncedAt()) // ✅ Real last sync
                            .build()
                    ).collect(Collectors.toList());
                
                // Calculate total USD value cho wallet này
                BigDecimal totalWalletUsd = walletBalances.stream()
                    .map(WalletBalance::getUsdValue)
                    .filter(usd -> usd != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return PortfolioListResponse.PortfolioItem.builder()
                    .walletId(wallet.getId()) // ✅ Real wallet ID
                    .walletAddress(wallet.getWalletAddress()) // ✅ Real address
                    .walletName(wallet.getWalletName()) // ✅ Real name
                    .walletType(wallet.getWalletType().name()) // ✅ Real type
                    .isPrimary(wallet.getIsPrimary()) // ✅ Real primary flag
                    .status(wallet.getStatus().name()) // ✅ Real status
                    .balances(request.isIncludeBalances() ? balanceItems : null)
                    .totalBalanceUsd(totalWalletUsd) // ✅ Calculated total USD
                    .totalTokens(walletBalances.size()) // ✅ Real token count
                    .lastUpdated(wallet.getLastSyncAt()) // ✅ Real last sync
                    .createdAt(wallet.getCreatedAt()) // ✅ Real created time
                    .build();
                    
            }).collect(Collectors.toList());
        
        // Calculate pagination info
        PortfolioListResponse.PaginationInfo pagination = 
            PortfolioListResponse.PaginationInfo.builder()
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) userWallets.size() / pageable.getPageSize()))
                .totalElements(userWallets.size())
                .hasNext(pageable.getPageNumber() < (userWallets.size() / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .isFirst(pageable.getPageNumber() == 0)
                .isLast(pageable.getPageNumber() >= (userWallets.size() / pageable.getPageSize()) - 1)
                .build();
        
        // Calculate summary với REAL data
        BigDecimal totalPortfolioUsd = portfolioItems.stream()
            .map(PortfolioListResponse.PortfolioItem::getTotalBalanceUsd)
            .filter(usd -> usd != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        int totalTokens = portfolioItems.stream()
            .mapToInt(PortfolioListResponse.PortfolioItem::getTotalTokens)
            .sum();
            
        // Find primary wallet address
        String primaryWalletAddress = userWallets.stream()
            .filter(Wallet::getIsPrimary)
            .map(Wallet::getWalletAddress)
            .findFirst()
            .orElse("");
        
        PortfolioListResponse.PortfolioSummary summary = 
            PortfolioListResponse.PortfolioSummary.builder()
                .totalWallets(userWallets.size()) // ✅ Real total wallets
                .activeWallets((int) userWallets.stream()
                    .filter(w -> w.getStatus().name().equals("ACTIVE"))
                    .count()) // ✅ Real active wallets
                .totalBalanceUsd(totalPortfolioUsd) // ✅ Real total USD
                .totalTokens(totalTokens) // ✅ Real total tokens
                .primaryWalletAddress(primaryWalletAddress) // ✅ Real primary wallet
                .lastSyncTime(userWallets.stream()
                    .map(Wallet::getLastSyncAt)
                    .filter(sync -> sync != null)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now())) // ✅ Real last sync
                .nextSyncTime(LocalDateTime.now().plusMinutes(5)) // Next sync in 5 minutes
                .build();
        
        // Get user info từ first wallet
        String username = userWallets.isEmpty() ? "" : userId; // Could be improved với UserRepository
        
        return PortfolioListResponse.builder()
            .userId(userId)
            .username(username)
            .portfolios(portfolioItems)
            .pagination(pagination)
            .summary(summary)
            .timestamp(LocalDateTime.now())
            .cacheHit(false) // Set based on actual cache usage
            .build();
    }

    /**
     * Helper method để extract userId từ JWT token
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtTokenUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("No valid JWT token found");
    }
    
    /**
     * Response DTO cho WebSocket subscription info
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PortfolioWebSocketInfo {
        private String message;
        private String websocketEndpoint;
        private String subscriptionDestination;
        private String responseDestination;
        private String instructions;
        private String example;
        private Boolean jwtRequired;
    }
}