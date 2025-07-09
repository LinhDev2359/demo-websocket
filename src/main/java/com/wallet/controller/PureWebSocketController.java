package com.wallet.controller;

import com.wallet.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure WebSocket Handler - NO STOMP
 * 
 * This handler bypasses STOMP completely and uses pure WebSocket for Portfolio Operations
 * It handles JWT authentication at the WebSocket level and provides direct communication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PureWebSocketController extends TextWebSocketHandler {

    private final JwtTokenUtil jwtTokenUtil;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔄 Pure WebSocket connection established - Session: {}", session.getId());
        
        // Get JWT token from session attributes (set by handshake interceptor)
        String token = (String) session.getAttributes().get("token");
        
        if (token != null) {
            try {
                // Validate JWT token
                boolean isValid = jwtTokenUtil.validateToken(token);
                
                if (isValid) {
                    String username = jwtTokenUtil.getUsernameFromToken(token);
                    String userId = jwtTokenUtil.getUserIdFromToken(token);
                    
                    // Store session and user mapping
                    sessions.put(session.getId(), session);
                    sessionUsers.put(session.getId(), userId);
                    
                    log.info("✅ Pure WebSocket authenticated - User: {} (ID: {})", username, userId);
                    
                    // Send welcome message
                    session.sendMessage(new TextMessage("{\"type\":\"welcome\",\"message\":\"Connected successfully\",\"userId\":\"" + userId + "\"}"));
                    
                } else {
                    log.error("❌ JWT token validation failed for pure WebSocket");
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                }
            } catch (Exception e) {
                log.error("❌ Error validating JWT token in pure WebSocket: {}", e.getMessage());
                session.close(CloseStatus.NOT_ACCEPTABLE);
            }
        } else {
            log.error("❌ No JWT token found in pure WebSocket session");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("📥 Pure WebSocket message received from session {}: {}", session.getId(), message.getPayload());
        
        String userId = sessionUsers.get(session.getId());
        if (userId == null) {
            log.warn("❌ No user found for session: {}", session.getId());
            return;
        }
        
        try {
            // Parse the message (assuming JSON format)
            String payload = message.getPayload();
            
            // Handle different message types
            if (payload.contains("portfolio/subscribe")) {
                handlePortfolioSubscribe(session, userId);
            } else if (payload.contains("portfolio/list")) {
                handlePortfolioList(session, userId);
            } else if (payload.contains("portfolio/update")) {
                handlePortfolioUpdate(session, userId);
            } else if (payload.contains("portfolio/unsubscribe")) {
                handlePortfolioUnsubscribe(session, userId);
            } else {
                log.info("📤 Echo message back to client: {}", payload);
                session.sendMessage(new TextMessage("{\"type\":\"echo\",\"data\":\"" + payload + "\"}"));
            }
            
        } catch (Exception e) {
            log.error("❌ Error handling pure WebSocket message: {}", e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Error processing message\"}"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ Pure WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("🔴 Pure WebSocket connection closed - Session: {}, Status: {}", session.getId(), status);
        cleanupSession(session);
    }

    private void cleanupSession(WebSocketSession session) {
        sessions.remove(session.getId());
        sessionUsers.remove(session.getId());
        log.info("🧹 Cleaned up session: {}", session.getId());
    }

    private void handlePortfolioSubscribe(WebSocketSession session, String userId) throws Exception {
        log.info("📊 Portfolio subscribe request for user: {}", userId);
        
        // Send mock portfolio data
        String portfolioData = "{\"type\":\"portfolio\",\"data\":{\"userId\":\"" + userId + "\",\"totalBalance\":\"1000.00\",\"wallets\":[{\"name\":\"Main Wallet\",\"balance\":\"500.00\"},{\"name\":\"Savings\",\"balance\":\"500.00\"}]}}";
        
        session.sendMessage(new TextMessage(portfolioData));
        log.info("✅ Portfolio data sent to user: {}", userId);
    }

    private void handlePortfolioList(WebSocketSession session, String userId) throws Exception {
        log.info("📋 Portfolio list request for user: {}", userId);
        
        // Send mock portfolio list
        String portfolioList = "{\"type\":\"portfolio-list\",\"data\":{\"portfolios\":[{\"id\":1,\"name\":\"Portfolio 1\",\"value\":\"750.00\"},{\"id\":2,\"name\":\"Portfolio 2\",\"value\":\"250.00\"}],\"total\":\"1000.00\"}}";
        
        session.sendMessage(new TextMessage(portfolioList));
        log.info("✅ Portfolio list sent to user: {}", userId);
    }

    private void handlePortfolioUpdate(WebSocketSession session, String userId) throws Exception {
        log.info("🔄 Portfolio update request for user: {}", userId);
        
        // Send updated portfolio data
        String updatedData = "{\"type\":\"portfolio\",\"data\":{\"userId\":\"" + userId + "\",\"totalBalance\":\"1050.00\",\"lastUpdated\":\"" + System.currentTimeMillis() + "\"}}";
        
        session.sendMessage(new TextMessage(updatedData));
        log.info("✅ Portfolio update sent to user: {}", userId);
    }

    private void handlePortfolioUnsubscribe(WebSocketSession session, String userId) throws Exception {
        log.info("🔇 Portfolio unsubscribe request for user: {}", userId);
        
        // Send confirmation
        String confirmation = "{\"type\":\"unsubscribe\",\"message\":\"Unsubscribed from portfolio updates\"}";
        
        session.sendMessage(new TextMessage(confirmation));
        log.info("✅ Portfolio unsubscribe confirmed for user: {}", userId);
    }
}