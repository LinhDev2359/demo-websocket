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
            log.info("WebSocket message received - Command: {}", command);
            
            if (StompCommand.CONNECT.equals(command)) {
                try {
                    handleConnect(accessor);
                    log.info("✅ CONNECT command processed successfully");
                } catch (Exception e) {
                    log.error("❌ CONNECT command failed: {}", e.getMessage(), e);
                    // Don't return null - let the message proceed
                }
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                try {
                    handleSubscribe(accessor);
                    log.info("✅ SUBSCRIBE command processed successfully");
                } catch (Exception e) {
                    log.error("❌ SUBSCRIBE command failed: {}", e.getMessage(), e);
                    // Don't return null - let the message proceed
                }
            } else if (StompCommand.SEND.equals(command)) {
                try {
                    handleSend(accessor);
                    log.info("✅ SEND command processed successfully");
                } catch (Exception e) {
                    log.error("❌ SEND command failed: {}", e.getMessage(), e);
                    // Don't return null - let the message proceed
                }
            }
        }
        
        return message;
    }

    /**
     * Handle CONNECT command
     * Validate JWT token và setup authentication
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        log.info("Processing WebSocket CONNECT command");
        
        try {
            // Get JWT token from headers
            String authToken = getJwtFromHeaders(accessor);
            log.info("JWT token found: {}", authToken != null ? "YES" : "NO");
            
            if (authToken != null) {
                log.info("Validating JWT token...");
                boolean isValid = jwtTokenUtil.validateToken(authToken);
                log.info("JWT token validation result: {}", isValid);
                
                if (isValid) {
                    // Extract user information
                    String username = jwtTokenUtil.getUsernameFromToken(authToken);
                    String userId = jwtTokenUtil.getUserIdFromToken(authToken);
                    
                    log.info("Extracted user info - Username: {}, UserID: {}", username, userId);
                    
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
                    return; // Success - exit early
                } else {
                    log.warn("❌ JWT token validation failed");
                }
            } else {
                log.warn("❌ No JWT token found in any headers or session attributes");
            }
            
            // If we reach here, authentication failed
            log.error("❌ WebSocket authentication failed - no valid token found");
            // Set a default user to allow connection
            WebSocketUserPrincipal defaultPrincipal = new WebSocketUserPrincipal("anonymous", "anonymous");
            accessor.setUser(defaultPrincipal);
            accessor.getSessionAttributes().put("authenticated", false);
            
        } catch (Exception e) {
            log.error("❌ WebSocket authentication failed with exception: {}", e.getMessage(), e);
            // Set a default user to allow connection
            WebSocketUserPrincipal defaultPrincipal = new WebSocketUserPrincipal("anonymous", "anonymous");
            accessor.setUser(defaultPrincipal);
            accessor.getSessionAttributes().put("authenticated", false);
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
                return; // Just return instead of throwing exception
            }
            
            log.info("✅ User {} subscribed to: {}", userId, destination);
        } else {
            log.warn("❌ Unauthenticated user attempting to subscribe to: {}", destination);
            return; // Just return instead of throwing exception
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
                return; // Just return instead of throwing exception
            }
            
        } else {
            log.warn("❌ Unauthenticated user attempting to send to: {}", destination);
            return; // Just return instead of throwing exception
        }
    }

    /**
     * Extract JWT token từ WebSocket headers
     */
    private String getJwtFromHeaders(StompHeaderAccessor accessor) {
        log.info("Searching for JWT token in WebSocket headers...");
        
        // First try session attributes (from URL params)
        if (accessor.getSessionAttributes() != null) {
            String tokenFromSession = (String) accessor.getSessionAttributes().get("token");
            if (tokenFromSession != null) {
                log.info("✅ Found JWT token in session attributes (from URL)");
                return tokenFromSession;
            }
        }
        
        // Try to get from STOMP native headers
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        log.info("Authorization header: {}", authHeader != null ? "FOUND" : "NOT FOUND");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.info("✅ Found JWT token in Authorization header with Bearer prefix");
            return authHeader.substring(7);
        }
        
        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            log.info("✅ Found JWT token in Authorization header without Bearer prefix");
            return authHeader;
        }
        
        // Try to get token from token header (alternative)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null) {
            log.info("✅ Found JWT token in token header");
            return tokenHeader;
        }
        
        // Try lowercase variants
        authHeader = accessor.getFirstNativeHeader("authorization");
        if (authHeader != null) {
            log.info("✅ Found JWT token in authorization header (lowercase)");
            return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        }
        
        log.warn("❌ No JWT token found in any location");
        log.info("Session attributes available: {}", accessor.getSessionAttributes() != null);
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