package com.wallet.controller;

import com.wallet.dto.PortfolioResponse;
import com.wallet.dto.PortfolioUpdateRequest;
import com.wallet.dto.PortfolioListRequest;
import com.wallet.dto.PortfolioListResponse;
import com.wallet.dto.WebSocketErrorResponse;
import com.wallet.service.PortfolioService;
import com.wallet.websocket.WebSocketUserPrincipal;
import com.wallet.repository.BalanceRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WalletBalanceRepository;
import com.wallet.entity.Balance;
import com.wallet.entity.Wallet;
import com.wallet.entity.WalletBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Portfolio WebSocket Controller
 * Handles real-time portfolio data streaming via WebSocket
 * 
 * WebSocket Message Destinations:
 * - /app/portfolio/list - Get portfolio list với pagination
 * - /app/portfolio/subscribe/{userId} - Subscribe to portfolio updates
 * - /app/portfolio/update - Request portfolio update
 * - /app/portfolio/refresh - Force refresh portfolio
 * - /app/portfolio/unsubscribe/{userId} - Unsubscribe from updates
 * 
 * Client Subscription Destinations:
 * - /user/queue/portfolio - User-specific portfolio updates
 * - /user/queue/portfolio-list - Portfolio list responses
 * - /user/queue/errors - Error messages
 * - /topic/portfolio/{userId} - User portfolio broadcast (if enabled)
 */
@Controller
@Validated
public class PortfolioWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioWebSocketController.class);
    
    @Autowired
    @Qualifier("portfolioServiceV2")
    private PortfolioService portfolioService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private BalanceRepository balanceRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private WalletBalanceRepository walletBalanceRepository;
    
    /**
     * Subscribe to portfolio updates for a specific user
     * Client subscribes to: /app/portfolio/subscribe/{userId}
     * 
     * @param userId User ID to subscribe to
     * @param principal Authenticated user principal
     * @param headerAccessor Message header accessor
     * @return Initial portfolio data
     */
    @SubscribeMapping("/portfolio/subscribe/{userId}")
    public PortfolioResponse subscribeToPortfolio(
            @DestinationVariable String userId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("Portfolio subscription request for userId: {} from principal: {}", 
                   userId, principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate user authorization
            validateUserAccess(userId, principal);
            
            // Get initial portfolio data
            PortfolioResponse portfolio = portfolioService.getUserPortfolio(userId, false);
            
            if (portfolio == null) {
                logger.warn("Portfolio not found for userId: {}", userId);
                sendErrorToUser(userId, WebSocketErrorResponse.portfolioNotFound(userId));
                return null;
            }
            
            // Add user to session for tracking
            if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("subscribedUserId", userId);
                headerAccessor.getSessionAttributes().put("subscriptionTime", LocalDateTime.now());
            }
            
            logger.info("Successfully subscribed user {} to portfolio updates", userId);
            return portfolio;
            
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for portfolio subscription: userId={}, principal={}", 
                       userId, principal != null ? principal.getName() : "anonymous");
            sendErrorToUser(userId, WebSocketErrorResponse.accessDenied(userId, "portfolio"));
            return null;
            
        } catch (Exception e) {
            logger.error("Error during portfolio subscription for userId: {}", userId, e);
            sendErrorToUser(userId, WebSocketErrorResponse.serviceUnavailable(userId));
            return null;
        }
    }
    
    /**
     * Handle portfolio update requests
     * Client sends to: /app/portfolio/update
     * Response sent to: /user/queue/portfolio
     * 
     * @param request Portfolio update request
     * @param principal Authenticated user principal
     * @param message Original message
     */
    @MessageMapping("/portfolio/update")
    @SendToUser("/queue/portfolio")
    public void handlePortfolioUpdate(
            @Valid @Payload PortfolioUpdateRequest request,
            Principal principal,
            Message<?> message) {
        
        logger.info("Portfolio update request: {} from principal: {}", 
                   request, principal != null ? principal.getName() : "anonymous");
        
        // Process portfolio update asynchronously to avoid blocking
        CompletableFuture.supplyAsync(() -> {
            try {
                // Validate user authorization
                validateUserAccess(request.getUserId(), principal);
                
                // Get updated portfolio data
                PortfolioResponse portfolio = portfolioService.getUserPortfolio(
                    request.getUserId(), 
                    request.getRefreshCache()
                );
                
                if (portfolio != null) {
                    // Send portfolio update to specific user
                    sendPortfolioUpdateToUser(request.getUserId(), portfolio);
                    logger.info("Portfolio update sent successfully for userId: {}", request.getUserId());
                } else {
                    logger.warn("Portfolio not found during update for userId: {}", request.getUserId());
                    sendErrorToUser(request.getUserId(), WebSocketErrorResponse.portfolioNotFound(request.getUserId()));
                }
                
                return portfolio;
                
            } catch (AccessDeniedException e) {
                logger.warn("Access denied for portfolio update: userId={}, principal={}", 
                           request.getUserId(), principal != null ? principal.getName() : "anonymous");
                sendErrorToUser(request.getUserId(), WebSocketErrorResponse.accessDenied(request.getUserId(), "portfolio"));
                return null;
                
            } catch (Exception e) {
                logger.error("Error processing portfolio update for userId: {}", request.getUserId(), e);
                sendErrorToUser(request.getUserId(), WebSocketErrorResponse.serviceUnavailable(request.getUserId()));
                return null;
            }
        });
    }
    
    /**
     * Handle portfolio refresh requests (force cache refresh)
     * Client sends to: /app/portfolio/refresh
     * 
     * @param userId User ID to refresh
     * @param principal Authenticated user principal
     */
    @MessageMapping("/portfolio/refresh")
    public void handlePortfolioRefresh(
            @Payload String userId,
            Principal principal) {
        
        logger.info("Portfolio refresh request for userId: {} from principal: {}", 
                   userId, principal != null ? principal.getName() : "anonymous");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Validate user authorization
                validateUserAccess(userId, principal);
                
                // Force refresh portfolio data
                PortfolioResponse portfolio = portfolioService.getUserPortfolio(userId, true);
                
                if (portfolio != null) {
                    sendPortfolioUpdateToUser(userId, portfolio);
                    logger.info("Portfolio refresh completed for userId: {}", userId);
                } else {
                    sendErrorToUser(userId, WebSocketErrorResponse.portfolioNotFound(userId));
                }
                
            } catch (Exception e) {
                logger.error("Error during portfolio refresh for userId: {}", userId, e);
                sendErrorToUser(userId, WebSocketErrorResponse.serviceUnavailable(userId));
            }
        });
    }
    
    /**
     * Handle unsubscribe requests
     * Client sends to: /app/portfolio/unsubscribe/{userId}
     * 
     * @param userId User ID to unsubscribe
     * @param principal Authenticated user principal
     * @param headerAccessor Message header accessor
     */
    @MessageMapping("/portfolio/unsubscribe/{userId}")
    public void unsubscribeFromPortfolio(
            @DestinationVariable String userId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("Portfolio unsubscribe request for userId: {} from principal: {}", 
                   userId, principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate user authorization
            validateUserAccess(userId, principal);
            
            // Remove user from session tracking
            if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().remove("subscribedUserId");
                headerAccessor.getSessionAttributes().remove("subscriptionTime");
            }
            
            logger.info("Successfully unsubscribed user {} from portfolio updates", userId);
            
        } catch (Exception e) {
            logger.error("Error during portfolio unsubscribe for userId: {}", userId, e);
        }
    }
    
    /**
     * Broadcast portfolio update to specific user
     * Called internally when portfolio data changes
     * 
     * @param userId User ID
     * @param portfolio Updated portfolio data
     */
    public void broadcastPortfolioUpdate(String userId, PortfolioResponse portfolio) {
        try {
            sendPortfolioUpdateToUser(userId, portfolio);
            logger.debug("Portfolio update broadcasted to userId: {}", userId);
        } catch (Exception e) {
            logger.error("Error broadcasting portfolio update to userId: {}", userId, e);
        }
    }
    
    /**
     * Send portfolio update to specific user
     * 
     * @param userId User ID
     * @param portfolio Portfolio data
     */
    private void sendPortfolioUpdateToUser(String userId, PortfolioResponse portfolio) {
        // Send to user-specific queue
        messagingTemplate.convertAndSendToUser(
            userId, 
            "/queue/portfolio", 
            portfolio
        );
    }
    
    /**
     * Send error message to specific user
     * 
     * @param userId User ID
     * @param error Error response
     */
    private void sendErrorToUser(String userId, WebSocketErrorResponse error) {
        messagingTemplate.convertAndSendToUser(
            userId, 
            "/queue/errors", 
            error
        );
    }
    
    /**
     * Validate user access authorization
     * 
     * @param userId Requested user ID
     * @param principal Authenticated principal
     * @throws AccessDeniedException if access is denied
     */
    private void validateUserAccess(String userId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        // Extract user ID from principal
        String authenticatedUserId = null;
        if (principal instanceof WebSocketUserPrincipal) {
            authenticatedUserId = ((WebSocketUserPrincipal) principal).getUserId();
        } else if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            authenticatedUserId = auth.getName();
        } else {
            authenticatedUserId = principal.getName();
        }
        
        // Check if user is authorized to access this portfolio
        if (!userId.equals(authenticatedUserId)) {
            logger.warn("Access denied: authenticated user {} attempted to access portfolio for user {}", 
                       authenticatedUserId, userId);
            throw new AccessDeniedException("Access denied to user portfolio: " + userId);
        }
    }
    
    /**
     * 🆕 NEW: Get Portfolio List with Pagination
     * Client sends to: /app/portfolio/list
     * Response sent to: /user/queue/portfolio-list
     * 
     * @param request Portfolio list request với pagination
     * @param principal Authenticated user principal
     */
    @MessageMapping("/portfolio/list")
    public void getPortfolioList(
            @Valid @Payload PortfolioListRequest request,
            Principal principal) {
        
        logger.info("Portfolio list request: {} from principal: {}", 
                   request, principal != null ? principal.getName() : "anonymous");
        
        // Process portfolio list request asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Validate user authorization
                validateUserAccess(request.getUserId(), principal);
                
                // Create pageable object
                Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
                
                // Get enriched portfolio data với ĐẦY ĐỦ thông tin từ database
                PortfolioListResponse response = getEnrichedPortfolioList(
                    request.getUserId(), request, pageable);
                
                if (response != null && !response.getPortfolios().isEmpty()) {
                    // Send portfolio list to user
                    sendPortfolioListToUser(request.getUserId(), response);
                    logger.info("Portfolio list sent successfully for userId: {} with {} wallets", 
                        request.getUserId(), response.getPortfolios().size());
                    
                } else {
                    logger.warn("No portfolios found for userId: {}", request.getUserId());
                    sendErrorToUser(request.getUserId(), 
                        WebSocketErrorResponse.portfolioNotFound(request.getUserId()));
                }
                
            } catch (AccessDeniedException e) {
                logger.warn("Access denied for portfolio list: userId={}, principal={}", 
                           request.getUserId(), principal != null ? principal.getName() : "anonymous");
                sendErrorToUser(request.getUserId(), 
                    WebSocketErrorResponse.accessDenied(request.getUserId(), "portfolio-list"));
                
            } catch (Exception e) {
                logger.error("Error getting portfolio list for userId: {}", request.getUserId(), e);
                sendErrorToUser(request.getUserId(), 
                    WebSocketErrorResponse.serviceUnavailable(request.getUserId()));
            }
        });
    }
    
    /**
     * 🆕 NEW: Get enriched portfolio data với đầy đủ thông tin
     */
    private PortfolioListResponse getEnrichedPortfolioList(
            String userId, PortfolioListRequest request, Pageable pageable) {
        
        // Get wallets for user
        List<Wallet> userWallets = walletRepository.findByUserId(userId);
        
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
     * 🆕 NEW: Send portfolio list to specific user
     */
    private void sendPortfolioListToUser(String userId, PortfolioListResponse response) {
        messagingTemplate.convertAndSendToUser(
            userId, 
            "/queue/portfolio-list", 
            response
        );
    }
}