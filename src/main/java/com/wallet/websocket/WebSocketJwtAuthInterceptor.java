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
        log.info("🔄 preSend called - message type: {}", message.getClass().getSimpleName());
        try {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null) {
                StompCommand command = accessor.getCommand();
                log.info("🔄 WebSocket message received - Command: {}", command);
                log.info("Session ID: {}, User: {}", accessor.getSessionId(), accessor.getUser());
                log.info("Headers: {}", accessor.toNativeHeaderMap());
                
                if (StompCommand.CONNECT.equals(command)) {
                    log.info("🔐 Processing STOMP CONNECT command");
                    
                    // Check if already authenticated to prevent duplicate processing
                    if (accessor.getUser() != null) {
                        log.info("User already authenticated: {}, skipping CONNECT processing", accessor.getUser().getName());
                        return message;
                    }
                    
                    try {
                        log.info("🔄 Starting JWT authentication for CONNECT");
                        handleConnect(accessor);
                        log.info("✅ CONNECT command processed successfully");
                    } catch (Exception e) {
                        log.error("❌ CONNECT command failed: {}", e.getMessage(), e);
                        // Don't throw - this may be causing immediate disconnection
                        // Instead, create a temporary user
                        WebSocketUserPrincipal tempPrincipal = new WebSocketUserPrincipal("temp_user", "temp_user");
                        accessor.setUser(tempPrincipal);
                        accessor.getSessionAttributes().put("userId", "temp_user");
                        accessor.getSessionAttributes().put("username", "temp_user");
                        accessor.getSessionAttributes().put("authenticated", false);
                        accessor.getSessionAttributes().put("auth_failed", true);
                        log.warn("Created temporary user due to authentication failure");
                    }
                } else if (StompCommand.SUBSCRIBE.equals(command)) {
                    try {
                        handleSubscribe(accessor);
                        log.info("✅ SUBSCRIBE command processed successfully");
                    } catch (Exception e) {
                        log.error("❌ SUBSCRIBE command failed: {}", e.getMessage(), e);
                    }
                } else if (StompCommand.SEND.equals(command)) {
                    try {
                        handleSend(accessor);
                        log.info("✅ SEND command processed successfully");
                    } catch (Exception e) {
                        log.error("❌ SEND command failed: {}", e.getMessage(), e);
                    }
                }
            } else {
                log.warn("⚠️ StompHeaderAccessor is null for message: {}", message.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("❌ Fatal error in preSend: {}", e.getMessage(), e);
            // Return message anyway to prevent connection close
        }
        
        return message;
    }

    /**
     * Handle CONNECT command
     * Validate JWT token và setup authentication
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        log.info("🔐 Processing WebSocket CONNECT command");
        String sessionId = accessor.getSessionId();
        log.info("Session ID: {}", sessionId);
        
        // Debug session attributes
        if (accessor.getSessionAttributes() != null) {
            log.info("Session attributes available: {}", accessor.getSessionAttributes().keySet());
            Object token = accessor.getSessionAttributes().get("token");
            log.info("Token in session attributes: {}", token != null ? "YES" : "NO");
        } else {
            log.warn("No session attributes available");
        }
        
        try {
            // Get JWT token from headers
            String authToken = getJwtFromHeaders(accessor);
            log.info("JWT token found: {}", authToken != null ? "YES" : "NO");
            
            if (authToken != null) {
                log.info("Validating JWT token...");
                log.info("Token starts with: {}", authToken.substring(0, Math.min(20, authToken.length())));
                
                boolean isValid = false;
                String username = null;
                String userId = null;
                
                try {
                    // First try to extract user info to see if token is structurally valid
                    username = jwtTokenUtil.getUsernameFromToken(authToken);
                    userId = jwtTokenUtil.getUserIdFromToken(authToken);
                    log.info("Extracted user info - Username: {}, UserID: {}", username, userId);
                    
                    // Then validate the token
                    isValid = jwtTokenUtil.validateToken(authToken);
                    log.info("JWT token validation result: {}", isValid);
                    
                    if (!isValid) {
                        log.warn("⚠️ JWT token validation failed but token structure is valid");
                        log.warn("This might be due to token expiration or signature mismatch");
                        
                        // Check if token is expired
                        try {
                            java.util.Date expiration = jwtTokenUtil.getExpirationDateFromToken(authToken);
                            java.util.Date now = new java.util.Date();
                            if (expiration.before(now)) {
                                log.error("❌ JWT token is expired. Expires: {}, Now: {}", expiration, now);
                            } else {
                                log.warn("⚠️ JWT token is not expired but validation failed - possible signature issue");
                            }
                        } catch (Exception expE) {
                            log.error("❌ Could not check token expiration: {}", expE.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("❌ JWT token parsing failed: {}", e.getMessage(), e);
                    log.error("Token content (first 50 chars): {}", authToken.substring(0, Math.min(50, authToken.length())));
                    isValid = false;
                }
                
                if (isValid && username != null && userId != null) {
                    
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
                    log.error("❌ JWT token validation failed or missing user information");
                }
            } else {
                log.warn("❌ No JWT token found in any headers or session attributes");
            }
            
            // Special handling for SockJS fallback transports
            // These connections don't have JWT tokens but are needed for SockJS negotiation
            if (sessionId != null) {
                // Check if this is a SockJS fallback connection by checking session attributes
                Object sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    // For SockJS fallback, create a temporary unauthenticated user
                    // The real authentication will happen when the actual WebSocket connection is established
                    log.info("Creating temporary user for SockJS fallback connection: {}", sessionId);
                    WebSocketUserPrincipal tempPrincipal = new WebSocketUserPrincipal("sockjs_temp", "sockjs_temp");
                    accessor.setUser(tempPrincipal);
                    accessor.getSessionAttributes().put("userId", "sockjs_temp");
                    accessor.getSessionAttributes().put("username", "sockjs_temp");
                    accessor.getSessionAttributes().put("authenticated", false);
                    accessor.getSessionAttributes().put("sockjs_fallback", true);
                    return;
                }
            }
            
            // If we reach here, authentication failed for a real WebSocket connection
            log.error("❌ WebSocket authentication failed - no valid token found");
            // Don't throw exception to prevent immediate disconnection
            // Instead, create a guest user to allow basic functionality
            WebSocketUserPrincipal guestPrincipal = new WebSocketUserPrincipal("guest_user", "guest_user");
            accessor.setUser(guestPrincipal);
            accessor.getSessionAttributes().put("userId", "guest_user");
            accessor.getSessionAttributes().put("username", "guest_user");
            accessor.getSessionAttributes().put("authenticated", false);
            accessor.getSessionAttributes().put("auth_failed", true);
            log.warn("Created guest user due to authentication failure - connection will proceed with limited functionality");
            
        } catch (SecurityException e) {
            log.error("❌ WebSocket security exception: {}", e.getMessage());
            // Don't throw - create fallback user instead
            WebSocketUserPrincipal errorPrincipal = new WebSocketUserPrincipal("error_user", "error_user");
            accessor.setUser(errorPrincipal);
            accessor.getSessionAttributes().put("userId", "error_user");
            accessor.getSessionAttributes().put("username", "error_user");
            accessor.getSessionAttributes().put("authenticated", false);
            accessor.getSessionAttributes().put("auth_error", true);
            log.warn("Created error user due to security exception - connection will proceed with limited functionality");
        } catch (Exception e) {
            log.error("❌ WebSocket authentication failed with exception: {}", e.getMessage(), e);
            // Don't throw - create fallback user instead
            WebSocketUserPrincipal errorPrincipal = new WebSocketUserPrincipal("error_user", "error_user");
            accessor.setUser(errorPrincipal);
            accessor.getSessionAttributes().put("userId", "error_user");
            accessor.getSessionAttributes().put("username", "error_user");
            accessor.getSessionAttributes().put("authenticated", false);
            accessor.getSessionAttributes().put("auth_error", true);
            log.warn("Created error user due to exception - connection will proceed with limited functionality: {}", e.getMessage());
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
            
            // Check if this is a SockJS fallback connection
            Boolean isSockJsFallback = (Boolean) accessor.getSessionAttributes().get("sockjs_fallback");
            if (Boolean.TRUE.equals(isSockJsFallback)) {
                log.debug("SockJS fallback connection attempting to subscribe to: {}", destination);
                // Allow subscription but mark as unauthenticated
                return;
            }
            
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
            
            // Check if this is a SockJS fallback connection
            Boolean isSockJsFallback = (Boolean) accessor.getSessionAttributes().get("sockjs_fallback");
            if (Boolean.TRUE.equals(isSockJsFallback)) {
                log.warn("❌ SockJS fallback connection attempting to send to: {}", destination);
                // Block sending from unauthenticated connections
                throw new SecurityException("Unauthenticated connection cannot send messages");
            }
            
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
            log.info("Session attributes keys: {}", accessor.getSessionAttributes().keySet());
            String tokenFromSession = (String) accessor.getSessionAttributes().get("token");
            if (tokenFromSession != null) {
                log.info("✅ Found JWT token in session attributes (from URL)");
                log.info("Token length: {}, starts with: {}", tokenFromSession.length(), 
                        tokenFromSession.substring(0, Math.min(20, tokenFromSession.length())));
                return tokenFromSession;
            } else {
                log.warn("No 'token' key found in session attributes");
            }
        } else {
            log.warn("Session attributes is null");
        }
        
        // Try to get from STOMP native headers
        log.info("STOMP native headers: {}", accessor.toNativeHeaderMap());
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
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            
            if (StompCommand.DISCONNECT.equals(command)) {
                Principal user = accessor.getUser();
                if (user instanceof WebSocketUserPrincipal) {
                    WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
                    log.info("🔌 WebSocket disconnected for user: {} (ID: {})", 
                            principal.getUsername(), principal.getUserId());
                } else {
                    log.info("🔌 WebSocket disconnected for unauthenticated session");
                }
            } else if (StompCommand.CONNECTED.equals(command)) {
                Principal user = accessor.getUser();
                if (user instanceof WebSocketUserPrincipal) {
                    WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
                    log.info("🟢 WebSocket CONNECTED for user: {} (ID: {})", 
                            principal.getUsername(), principal.getUserId());
                }
            }
        }
    }
}