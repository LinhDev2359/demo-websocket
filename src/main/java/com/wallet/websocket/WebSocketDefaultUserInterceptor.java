package com.wallet.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Default User Interceptor for WebSocket
 * Sets a default user for all connections (for testing without authentication)
 */
@Component
@Slf4j
public class WebSocketDefaultUserInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            log.info("🔍 Interceptor received message: {}", message);
            log.info("🔍 Message headers: {}", message.getHeaders());
            
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null) {
                StompCommand command = accessor.getCommand();
                String sessionId = accessor.getSessionId();
                log.info("WebSocket interceptor - Command: {}, SessionId: {}", command, sessionId);
                
                if (StompCommand.CONNECT.equals(command)) {
                    log.info("🔄 Processing STOMP CONNECT command for session: {}", sessionId);
                    
                    // Log all available headers for debugging
                    log.info("Available STOMP headers: {}", accessor.toNativeHeaderMap());
                    
                    // Get userId from headers or use default
                    String userId = accessor.getFirstNativeHeader("userId");
                    if (userId == null || userId.isEmpty()) {
                        userId = "user123"; // Default user ID
                    }
                    
                    String username = accessor.getFirstNativeHeader("username");
                    if (username == null || username.isEmpty()) {
                        username = "testuser"; // Default username
                    }
                    
                    // Create a default principal
                    WebSocketUserPrincipal principal = new WebSocketUserPrincipal(userId, username);
                    accessor.setUser(principal);
                    
                    // Store user info in session attributes
                    if (accessor.getSessionAttributes() != null) {
                        accessor.getSessionAttributes().put("userId", userId);
                        accessor.getSessionAttributes().put("username", username);
                        accessor.getSessionAttributes().put("authenticated", true);
                    }
                    
                    log.info("✅ WebSocket STOMP connection established for user: {} (ID: {}), session: {}", 
                            username, userId, sessionId);
                } else if (StompCommand.DISCONNECT.equals(command)) {
                    log.info("🔌 Processing STOMP DISCONNECT command for session: {}", sessionId);
                    log.info("Disconnect reason: {}", accessor.getFirstNativeHeader("receipt"));
                } else if (command == null) {
                    log.warn("⚠️ STOMP command is null for session: {}", sessionId);
                    log.warn("⚠️ Raw message: {}", message);
                } else {
                    log.info("📨 Other STOMP command: {} for session: {}", command, sessionId);
                }
            } else {
                log.warn("⚠️ StompHeaderAccessor is null for message: {}", message);
            }
        } catch (Exception e) {
            log.error("❌ Error in WebSocketDefaultUserInterceptor: ", e);
            // Don't throw - let connection proceed
        }
        
        return message;
    }
    
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            if (command != null) {
                log.debug("WebSocket message sent - Command: {}, Sent: {}, SessionId: {}", 
                         command, sent, accessor.getSessionId());
            }
        }
    }
}