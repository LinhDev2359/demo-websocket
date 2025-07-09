package com.wallet.config;

import com.wallet.websocket.WebSocketJwtAuthInterceptor;
import com.wallet.websocket.WebSocketDefaultUserInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Configuration với STOMP
 * 
 * Chức năng chính:
 * 1. Cấu hình STOMP endpoints cho WebSocket connections
 * 2. Setup message broker cho real-time communication
 * 3. Cấu hình JWT authentication cho WebSocket
 * 4. CORS configuration cho cross-origin requests
 * 
 * Architecture:
 * - Client connects qua /ws endpoint
 * - Messages được route qua /app prefix
 * - Subscriptions qua /topic và /user prefixes
 * - JWT authentication trên mọi WebSocket messages
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtAuthInterceptor jwtAuthInterceptor;
    private final WebSocketDefaultUserInterceptor defaultUserInterceptor;
    
    @Autowired(required = false)
    private TaskScheduler webSocketHeartbeatTaskScheduler;

    /**
     * Configure STOMP endpoints
     * 
     * Giải thích:
     * - /ws: WebSocket connection endpoint
     * - SockJS fallback cho browsers không support WebSocket
     * - CORS configuration cho cross-origin connections
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Configuring STOMP endpoints for WebSocket connections");
        
        // Add native WebSocket endpoint (without SockJS)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        log.info("WebSocket native handshake starting - URI: {}", request.getURI());
                        
                        // Extract JWT token from URL parameters
                        String query = request.getURI().getQuery();
                        if (query != null && query.contains("token=")) {
                            String[] params = query.split("&");
                            for (String param : params) {
                                if (param.startsWith("token=")) {
                                    String token = param.substring(6); // Remove "token=" prefix
                                    String decodedToken = java.net.URLDecoder.decode(token, "UTF-8");
                                    attributes.put("token", decodedToken);
                                    log.info("JWT token extracted from URL parameters during native handshake");
                                    log.debug("Token length: {}, starts with: {}", 
                                            decodedToken.length(), 
                                            decodedToken.substring(0, Math.min(20, decodedToken.length())));
                                    return true; // Allow handshake
                                }
                            }
                        }
                        
                        log.warn("No token parameter found in native WebSocket handshake URL");
                        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                        return false; // Reject handshake
                    }
                    
                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            log.error("WebSocket native handshake failed: ", exception);
                        } else {
                            log.info("WebSocket native handshake completed successfully");
                        }
                    }
                });
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        log.info("WebSocket handshake starting - URI: {}", request.getURI());
                        
                        String uri = request.getURI().toString();
                        
                        // Allow SockJS fallback transports (xhr, eventsource, etc.) to pass through
                        // These are used for connection negotiation and don't carry the token
                        if (uri.contains("/xhr") || uri.contains("/eventsource") || 
                            uri.contains("/jsonp") || uri.contains("/htmlfile") ||
                            uri.contains("t=")) { // SockJS timestamp parameter indicates transport negotiation
                            log.debug("SockJS transport negotiation - allowing handshake: {}", uri);
                            return true;
                        }
                        
                        // For actual WebSocket connections, require JWT token
                        if (uri.contains("/websocket")) {
                            String query = request.getURI().getQuery();
                            if (query != null && query.contains("token=")) {
                                String[] params = query.split("&");
                                for (String param : params) {
                                    if (param.startsWith("token=")) {
                                        String token = param.substring(6); // Remove "token=" prefix
                                        String decodedToken = java.net.URLDecoder.decode(token, "UTF-8");
                                        attributes.put("token", decodedToken);
                                        log.info("JWT token extracted from URL parameters during handshake");
                                        log.debug("Token length: {}, starts with: {}", 
                                                decodedToken.length(), 
                                                decodedToken.substring(0, Math.min(20, decodedToken.length())));
                                        return true; // Allow handshake with token
                                    }
                                }
                            }
                            
                            log.warn("No token parameter found in WebSocket handshake URL");
                            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            return false; // Reject WebSocket handshake without token
                        }
                        
                        // Allow other SockJS-related requests
                        log.debug("SockJS related request - allowing handshake: {}", uri);
                        return true;
                    }
                    
                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            log.error("WebSocket handshake failed: ", exception);
                        } else {
                            log.info("WebSocket handshake completed successfully");
                        }
                    }
                })
                .withSockJS() // SockJS fallback support
                .setSessionCookieNeeded(false) // Không cần cookies
                .setHeartbeatTime(25000) // Heartbeat every 25 seconds
                .setDisconnectDelay(30000) // Increase disconnect delay to 30 seconds
                .setWebSocketEnabled(true) // Enable WebSocket transport
                .setHttpMessageCacheSize(1000) // Cache size for HTTP messages
                .setStreamBytesLimit(524288); // 512KB limit for streaming
        
        log.info("✅ STOMP endpoint '/ws' configured with SockJS fallback and JWT authentication");
        log.info("✅ STOMP endpoint '/ws-native' configured for native WebSocket connections with JWT authentication");
    }

    /**
     * Configure Message Broker
     * 
     * Giải thích:
     * - /app: Prefix cho messages gửi đến server
     * - /topic: Prefix cho broadcast messages (portfolio updates)
     * - /user: Prefix cho user-specific messages
     * - Simple broker: In-memory message broker (production có thể dùng external broker)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        log.info("Configuring message broker for real-time communication");
        
        // Enable simple broker với destinations - disable heartbeat to prevent disconnect issues
        registry.enableSimpleBroker("/topic", "/user", "/queue")
                .setHeartbeatValue(new long[]{0, 0}); // Disable heartbeat to prevent disconnection issues
        
        // Application destination prefix
        registry.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix cho personal messages
        registry.setUserDestinationPrefix("/user");
        
        log.info("✅ Message broker configured:");
        log.info("   - Simple broker: /topic, /user, /queue");
        log.info("   - App prefix: /app");
        log.info("   - User prefix: /user");
        log.info("   - Heartbeat: disabled (prevents disconnection issues)");
    }

    /**
     * Configure Client Inbound Channel
     * 
     * Giải thích:
     * - Thêm JWT authentication interceptor
     * - Validate JWT token cho mọi WebSocket messages
     * - Extract user information từ JWT
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Configuring client inbound channel with JWT authentication");
        
        // Enable JWT authentication
        registration.interceptors(jwtAuthInterceptor);
        
        // For testing without authentication, use default user interceptor:
        // registration.interceptors(defaultUserInterceptor);
        
        // Configure thread pool cho inbound messages
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(1000)
                .keepAliveSeconds(60);
        
        log.info("✅ Inbound channel configured with JWT authentication interceptor");
    }

    /**
     * Configure Client Outbound Channel
     * 
     * Giải thích:
     * - Configure thread pool cho outbound messages
     * - Optimize performance cho high-throughput scenarios
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        log.info("Configuring client outbound channel for message delivery");
        
        // Configure thread pool cho outbound messages
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(1000)
                .keepAliveSeconds(60);
        
        log.info("✅ Outbound channel configured for optimal performance");
    }
}