package com.wallet.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Principal;

/**
 * Custom Principal cho WebSocket connections
 * 
 * Chức năng:
 * 1. Store user information trong WebSocket session
 * 2. Provide user ID và username cho message routing
 * 3. Enable user-specific message delivery
 * 
 * Được sử dụng bởi:
 * - WebSocketJwtAuthInterceptor để set user info
 * - Message routing để determine destination
 * - User-specific subscriptions và notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketUserPrincipal implements Principal {
    
    private String userId;
    private String username;
    
    /**
     * Get principal name (required by Principal interface)
     * Returns userId để uniquely identify user
     */
    @Override
    public String getName() {
        return userId;
    }
    
    /**
     * Get user ID
     * Sử dụng cho user-specific message routing
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Get username
     * Sử dụng cho logging và display purposes
     */
    public String getUsername() {
        return username;
    }
    
    @Override
    public String toString() {
        return String.format("WebSocketUserPrincipal{userId='%s', username='%s'}", userId, username);
    }
}