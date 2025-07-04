package com.wallet.websocket;

import com.wallet.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Interceptor cho WebSocket
 * 
 * Chức năng:
 * 1. Validate JWT token từ WebSocket headers
 * 2. Extract user information từ JWT
 * 3. Set authentication context cho WebSocket session
 * 4. Handle connection/disconnection events với proper logging
 * 
 * Flow:
 * 1. Client gửi JWT token trong Authorization header
 * 2. Interceptor validate token và extract user info
 * 3. Set Principal cho WebSocket session
 * 4. Allow/deny connection based on token validity
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * Pre-send interceptor
     * Chạy trước khi message được send
     * 
     * Giải thích:
     * - CONNECT: Validate JWT và setup authentication
     * - SUBSCRIBE: Check user permissions cho specific topics
     * - SEND: Validate user có quyền send message không
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            
            if (StompCommand.CONNECT.equals(command)) {
                handleConnect(accessor);
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                handleSubscribe(accessor);
            } else if (StompCommand.SEND.equals(command)) {
                handleSend(accessor);
            }
        }
        
        return message;
    }

    /**
     * Handle CONNECT command
     * Validate JWT token và setup authentication
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        log.debug("Processing WebSocket CONNECT command");
        
        try {
            // Get JWT token from headers
            String authToken = getJwtFromHeaders(accessor);
            
            if (authToken != null && jwtTokenUtil.validateToken(authToken)) {
                // Extract user information
                String username = jwtTokenUtil.getUsernameFromToken(authToken);
                String userId = jwtTokenUtil.getUserIdFromToken(authToken);
                
                // Create authentication object
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
                );
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities
                );
                
                // Set authentication trong security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Create custom principal với user information
                WebSocketUserPrincipal principal = new WebSocketUserPrincipal(userId, username);
                accessor.setUser(principal);
                
                // Store additional user info trong session attributes
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("username", username);
                accessor.getSessionAttributes().put("authenticated", true);
                
                log.info("✅ WebSocket connection authenticated for user: {} (ID: {})", username, userId);
                
            } else {
                log.warn("❌ Invalid or missing JWT token in WebSocket connection");
                throw new SecurityException("Invalid or missing JWT token");
            }
            
        } catch (Exception e) {
            log.error("❌ WebSocket authentication failed: {}", e.getMessage());
            throw new SecurityException("WebSocket authentication failed: " + e.getMessage());
        }
    }

    /**
     * Handle SUBSCRIBE command
     * Check user có quyền subscribe topic không
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            String userId = principal.getUserId();
            
            log.debug("User {} subscribing to destination: {}", userId, destination);
            
            // Validate user có quyền subscribe destination này không
            if (!isAuthorizedForDestination(userId, destination)) {
                log.warn("❌ User {} not authorized for destination: {}", userId, destination);
                throw new SecurityException("Not authorized for destination: " + destination);
            }
            
            log.info("✅ User {} subscribed to: {}", userId, destination);
        } else {
            log.warn("❌ Unauthenticated user attempting to subscribe to: {}", destination);
            throw new SecurityException("Authentication required for subscription");
        }
    }

    /**
     * Handle SEND command
     * Validate user có quyền send message không
     */
    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            String userId = principal.getUserId();
            
            log.debug("User {} sending message to: {}", userId, destination);
            
            // Validate user có quyền send đến destination này không
            if (!isAuthorizedForSending(userId, destination)) {
                log.warn("❌ User {} not authorized to send to: {}", userId, destination);
                throw new SecurityException("Not authorized to send to destination: " + destination);
            }
            
        } else {
            log.warn("❌ Unauthenticated user attempting to send to: {}", destination);
            throw new SecurityException("Authentication required for sending messages");
        }
    }

    /**
     * Extract JWT token từ WebSocket headers
     */
    private String getJwtFromHeaders(StompHeaderAccessor accessor) {
        // Try to get token from Authorization header
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try to get token from token header (alternative)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null) {
            return tokenHeader;
        }
        
        return null;
    }

    /**
     * Check user authorization cho specific destination
     */
    private boolean isAuthorizedForDestination(String userId, String destination) {
        if (destination == null) {
            return false;
        }
        
        // Allow all users to subscribe to general topics
        if (destination.startsWith("/topic/")) {
            return true;
        }
        
        // For user-specific destinations, check user ID
        if (destination.startsWith("/user/")) {
            // Extract user ID from destination: /user/{userId}/...
            String[] parts = destination.split("/");
            if (parts.length >= 3) {
                String destinationUserId = parts[2];
                return userId.equals(destinationUserId);
            }
        }
        
        return false;
    }

    /**
     * Check user authorization cho sending messages
     */
    private boolean isAuthorizedForSending(String userId, String destination) {
        if (destination == null) {
            return false;
        }
        
        // Allow authenticated users to send to app destinations
        if (destination.startsWith("/app/")) {
            return true;
        }
        
        return false;
    }

    /**
     * Post-send interceptor
     * Log successful message delivery
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user instanceof WebSocketUserPrincipal) {
                WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
                log.info("🔌 WebSocket disconnected for user: {} (ID: {})", 
                        principal.getUsername(), principal.getUserId());
            }
        }
    }
}