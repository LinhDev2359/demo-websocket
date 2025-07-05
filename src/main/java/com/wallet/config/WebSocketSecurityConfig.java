package com.wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * Simple WebSocket Security Configuration
 * 
 * Chức năng:
 * 1. Enable WebSocket security integration
 * 2. Configure basic authorization rules
 * 3. Work với JWT authentication từ interceptor
 * 
 * Note: Phiên bản đơn giản để tránh dependency conflicts
 * JWT authentication được handle trong WebSocketJwtAuthInterceptor
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    /**
     * Configure authorization manager cho WebSocket messages
     * 
     * Giải thích:
     * - Cho phép tất cả messages vì authentication đã được handle trong interceptor
     * - WebSocketJwtAuthInterceptor sẽ validate JWT và set authentication
     * - Simplified approach để tránh Spring Security messaging dependency issues
     */
    @Bean
    public AuthorizationManager<org.springframework.messaging.Message<?>> messageAuthorizationManager() {
        MessageMatcherDelegatingAuthorizationManager.Builder messages = 
            MessageMatcherDelegatingAuthorizationManager.builder();

        // Allow all messages - authentication handled by JWT interceptor
        messages
            .simpTypeMatchers(SimpMessageType.CONNECT, 
                             SimpMessageType.HEARTBEAT, 
                             SimpMessageType.UNSUBSCRIBE, 
                             SimpMessageType.DISCONNECT).permitAll()
            .simpDestMatchers("/app/**").permitAll()
            .simpSubscribeDestMatchers("/topic/**", "/user/**").permitAll()
            .anyMessage().permitAll();

        return messages.build();
    }
}