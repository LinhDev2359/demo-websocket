package com.wallet.websocket;

import com.wallet.controller.PortfolioWebSocketController;
import com.wallet.dto.PortfolioResponse;
import com.wallet.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Portfolio WebSocket Event Listener
 * Handles WebSocket connection events and manages portfolio subscriptions
 */
@Component
public class PortfolioWebSocketEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioWebSocketEventListener.class);
    
    // Track active portfolio subscriptions
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> subscriptionTimes = new ConcurrentHashMap<>();
    
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    
    @Autowired
    @Qualifier("portfolioServiceV2")
    private PortfolioService portfolioService;
    
    @Autowired
    private PortfolioWebSocketController portfolioController;
    
    // Scheduled executor for periodic portfolio updates
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Handle WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal principal = headerAccessor.getUser();
        
        logger.info("WebSocket connection established - SessionId: {}, User: {}", 
                   sessionId, principal != null ? principal.getName() : "anonymous");
        
        // Extract user ID from principal
        if (principal != null) {
            String userId = extractUserIdFromPrincipal(principal);
            if (userId != null) {
                sessionUserMap.put(sessionId, userId);
                logger.debug("Mapped session {} to user {}", sessionId, userId);
            }
        }
    }
    
    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("WebSocket connection closed - SessionId: {}", sessionId);
        
        // Clean up session tracking
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            subscriptionTimes.remove(sessionId);
            logger.debug("Cleaned up session {} for user {}", sessionId, userId);
        }
    }
    
    /**
     * Handle WebSocket subscription
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        Principal principal = headerAccessor.getUser();
        
        logger.info("WebSocket subscription - SessionId: {}, Destination: {}, User: {}", 
                   sessionId, destination, principal != null ? principal.getName() : "anonymous");
        
        // Track portfolio subscriptions
        if (destination != null && destination.contains("/portfolio/subscribe/")) {
            String userId = extractUserIdFromDestination(destination);
            if (userId != null) {
                sessionUserMap.put(sessionId, userId);
                subscriptionTimes.put(sessionId, LocalDateTime.now());
                
                // Schedule periodic portfolio updates for this user
                schedulePortfolioUpdates(userId, sessionId);
                
                logger.info("Started portfolio subscription tracking for user: {} (session: {})", userId, sessionId);
            }
        }
    }
    
    /**
     * Handle WebSocket unsubscription
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();
        
        logger.info("WebSocket unsubscription - SessionId: {}, SubscriptionId: {}", sessionId, subscriptionId);
        
        // Clean up subscription tracking
        String userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            subscriptionTimes.remove(sessionId);
            logger.info("Stopped portfolio subscription tracking for user: {} (session: {})", userId, sessionId);
        }
    }
    
    /**
     * Schedule periodic portfolio updates for a user
     * Updates every 30 seconds for active subscriptions
     */
    private void schedulePortfolioUpdates(String userId, String sessionId) {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // Check if session is still active
                if (!sessionUserMap.containsKey(sessionId)) {
                    logger.debug("Session {} no longer active, stopping portfolio updates", sessionId);
                    return;
                }
                
                // Check if subscription is still recent (within last 10 minutes)
                LocalDateTime subscriptionTime = subscriptionTimes.get(sessionId);
                if (subscriptionTime != null && subscriptionTime.isBefore(LocalDateTime.now().minusMinutes(10))) {
                    logger.debug("Subscription for session {} is old, stopping updates", sessionId);
                    sessionUserMap.remove(sessionId);
                    subscriptionTimes.remove(sessionId);
                    return;
                }
                
                // Get fresh portfolio data
                PortfolioResponse portfolio = portfolioService.getUserPortfolio(userId, true);
                if (portfolio != null) {
                    // Broadcast portfolio update
                    portfolioController.broadcastPortfolioUpdate(userId, portfolio);
                    logger.debug("Sent periodic portfolio update for user: {}", userId);
                }
                
            } catch (Exception e) {
                logger.error("Error sending periodic portfolio update for user: {}", userId, e);
            }
        }, 30, 30, TimeUnit.SECONDS); // Initial delay: 30s, Period: 30s
    }
    
    /**
     * Extract user ID from WebSocket principal
     */
    private String extractUserIdFromPrincipal(Principal principal) {
        if (principal instanceof WebSocketUserPrincipal) {
            return ((WebSocketUserPrincipal) principal).getUserId();
        }
        return principal.getName();
    }
    
    /**
     * Extract user ID from WebSocket destination
     * Example: /app/portfolio/subscribe/user123 -> user123
     */
    private String extractUserIdFromDestination(String destination) {
        if (destination == null) return null;
        
        String[] parts = destination.split("/");
        if (parts.length >= 4 && "portfolio".equals(parts[2]) && "subscribe".equals(parts[3])) {
            return parts.length > 4 ? parts[4] : null;
        }
        
        return null;
    }
    
    /**
     * Get active portfolio subscriptions count
     */
    public int getActiveSubscriptionsCount() {
        return sessionUserMap.size();
    }
    
    /**
     * Get active subscriptions for a user
     */
    public long getActiveSubscriptionsForUser(String userId) {
        return sessionUserMap.values().stream()
            .filter(u -> u.equals(userId))
            .count();
    }
    
    /**
     * Manually trigger portfolio update for all active subscriptions
     */
    public void triggerPortfolioUpdateForAllSubscriptions() {
        logger.info("Triggering portfolio updates for {} active subscriptions", sessionUserMap.size());
        
        sessionUserMap.values().stream()
            .distinct()
            .forEach(userId -> {
                try {
                    PortfolioResponse portfolio = portfolioService.getUserPortfolio(userId, true);
                    if (portfolio != null) {
                        portfolioController.broadcastPortfolioUpdate(userId, portfolio);
                    }
                } catch (Exception e) {
                    logger.error("Error triggering portfolio update for user: {}", userId, e);
                }
            });
    }
    
    /**
     * Cleanup method called during shutdown
     */
    public void shutdown() {
        logger.info("Shutting down portfolio WebSocket event listener");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        sessionUserMap.clear();
        subscriptionTimes.clear();
    }
}