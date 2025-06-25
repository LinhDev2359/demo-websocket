package com.wallet.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

/**
 * WebSocket Event Listener
 * 
 * Chức năng:
 * 1. Monitor WebSocket connection events
 * 2. Log user activities cho debugging và analytics
 * 3. Handle cleanup khi user disconnect
 * 4. Track active WebSocket sessions
 * 
 * Events được handle:
 * - CONNECTED: User successfully connected
 * - DISCONNECTED: User disconnected
 * - SUBSCRIBED: User subscribed to topic
 * - UNSUBSCRIBED: User unsubscribed from topic
 */
@Component
@Slf4j
public class WebSocketEventListener {

    /**
     * Handle WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            log.info("🟢 WebSocket CONNECTED: User {} (ID: {}) - Session: {}", 
                    principal.getUsername(), 
                    principal.getUserId(),
                    headerAccessor.getSessionId());
        } else {
            log.info("🟢 WebSocket CONNECTED: Anonymous user - Session: {}", 
                    headerAccessor.getSessionId());
        }
    }

    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            log.info("🔴 WebSocket DISCONNECTED: User {} (ID: {}) - Session: {}", 
                    principal.getUsername(), 
                    principal.getUserId(),
                    headerAccessor.getSessionId());
                    
            // TODO: Cleanup user-specific data if needed
            // TODO: Notify other services about user disconnection
            
        } else {
            log.info("🔴 WebSocket DISCONNECTED: Session: {}", 
                    headerAccessor.getSessionId());
        }
    }

    /**
     * Handle topic subscription
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String destination = headerAccessor.getDestination();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            log.info("📋 WebSocket SUBSCRIBED: User {} (ID: {}) to destination: {}", 
                    principal.getUsername(), 
                    principal.getUserId(),
                    destination);
        } else {
            log.info("📋 WebSocket SUBSCRIBED: Anonymous user to destination: {}", destination);
        }
    }

    /**
     * Handle topic unsubscription
     */
    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal principal = (WebSocketUserPrincipal) user;
            log.info("📤 WebSocket UNSUBSCRIBED: User {} (ID: {}) - Session: {}", 
                    principal.getUsername(), 
                    principal.getUserId(),
                    headerAccessor.getSessionId());
        }
    }
}