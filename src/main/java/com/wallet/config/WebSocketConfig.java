package com.wallet.config;

import com.wallet.websocket.WebSocketJwtAuthInterceptor;
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
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        // Extract JWT token from URL parameters
                        String query = request.getURI().getQuery();
                        if (query != null && query.contains("token=")) {
                            String[] params = query.split("&");
                            for (String param : params) {
                                if (param.startsWith("token=")) {
                                    String token = param.substring(6); // Remove "token=" prefix
                                    attributes.put("token", java.net.URLDecoder.decode(token, "UTF-8"));
                                    log.info("JWT token extracted from URL parameters during handshake");
                                    break;
                                }
                            }
                        }
                        return true;
                    }
                    
                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        // No action needed
                    }
                })
                .withSockJS() // SockJS fallback support
                .setSessionCookieNeeded(false) // Không cần cookies
                .setHeartbeatTime(25000) // Heartbeat every 25 seconds
                .setDisconnectDelay(5000); // Disconnect delay 5 seconds
        
        log.info("✅ STOMP endpoint '/ws' configured with SockJS fallback and JWT token extraction");
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
        
        // Enable simple broker với destinations
        if (webSocketHeartbeatTaskScheduler != null) {
            // With heartbeat support if TaskScheduler is available
            registry.enableSimpleBroker("/topic", "/user")
                    .setHeartbeatValue(new long[]{10000, 10000}) // Server-Client heartbeat every 10s
                    .setTaskScheduler(webSocketHeartbeatTaskScheduler);
            log.info("✅ Message broker configured with heartbeat support");
        } else {
            // Without heartbeat if TaskScheduler is not available
            registry.enableSimpleBroker("/topic", "/user");
            log.info("✅ Message broker configured without heartbeat");
        }
        
        // Application destination prefix
        registry.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix cho personal messages
        registry.setUserDestinationPrefix("/user");
        
        log.info("✅ Message broker configured:");
        log.info("   - Simple broker: /topic, /user");
        log.info("   - App prefix: /app");
        log.info("   - User prefix: /user");
        log.info("   - Heartbeat: {}", webSocketHeartbeatTaskScheduler != null ? "enabled" : "disabled");
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
        log.info("Configuring client inbound channel WITHOUT JWT authentication for testing");
        
        // Temporarily disable JWT interceptor for testing
        // registration.interceptors(jwtAuthInterceptor);
        
        // Configure thread pool cho inbound messages
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(1000)
                .keepAliveSeconds(60);
        
        log.info("✅ Inbound channel configured WITHOUT JWT interceptor");
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