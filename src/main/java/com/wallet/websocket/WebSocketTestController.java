package com.wallet.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Test Controller
 * 
 * Chức năng:
 * 1. Test WebSocket configuration và JWT authentication
 * 2. Provide endpoints để test real-time messaging
 * 3. Demonstrate user-specific và broadcast messaging
 * 4. Validate WebSocket security configuration
 * 
 * Test Endpoints:
 * - /app/test.message: Broadcast message to all users
 * - /app/test.private: Send private message to specific user
 * - /app/test.ping: Health check endpoint
 * 
 * Note: Đây là test controller, có thể remove trong production
 */
@Controller
@Slf4j
public class WebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Test broadcast message
     * Client gửi message đến /app/test.message
     * Server broadcast đến tất cả users subscribed /topic/test
     */
    @MessageMapping("/test.message")
    @SendTo("/topic/test")
    public Map<String, Object> handleTestMessage(Map<String, Object> message, Principal principal) {
        log.info("📨 Received test message from user: {}", getPrincipalInfo(principal));
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "broadcast");
        response.put("message", message.get("message"));
        response.put("sender", getPrincipalInfo(principal));
        response.put("timestamp", LocalDateTime.now());
        response.put("recipients", "all");
        
        return response;
    }

    /**
     * Test private message
     * Client gửi message đến /app/test.private
     * Server gửi private message đến user đó
     */
    @MessageMapping("/test.private")
    @SendToUser("/topic/private")
    public Map<String, Object> handlePrivateMessage(Map<String, Object> message, Principal principal) {
        log.info("🔒 Received private message from user: {}", getPrincipalInfo(principal));
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "private");
        response.put("message", message.get("message"));
        response.put("recipient", getPrincipalInfo(principal));
        response.put("timestamp", LocalDateTime.now());
        
        return response;
    }

    /**
     * Test ping/pong cho health check
     * Client gửi ping, server respond với pong
     */
    @MessageMapping("/test.ping")
    @SendToUser("/topic/pong")
    public Map<String, Object> handlePing(Map<String, Object> message, Principal principal) {
        log.debug("🏓 Ping received from user: {}", getPrincipalInfo(principal));
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "pong");
        response.put("message", "pong");
        response.put("user", getPrincipalInfo(principal));
        response.put("timestamp", LocalDateTime.now());
        response.put("originalMessage", message.get("message"));
        
        return response;
    }

    /**
     * Test server-initiated message
     * Server có thể gửi message đến specific user bất cứ lúc nào
     */
    public void sendNotificationToUser(String userId, Map<String, Object> notification) {
        log.info("📢 Sending server notification to user: {}", userId);
        
        String destination = "/user/" + userId + "/topic/notifications";
        messagingTemplate.convertAndSend(destination, notification);
    }

    /**
     * Test broadcast notification
     * Server broadcast message đến tất cả connected users
     */
    public void broadcastNotification(Map<String, Object> notification) {
        log.info("📢 Broadcasting notification to all users");
        
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    /**
     * Helper method để get principal information
     */
    private Map<String, Object> getPrincipalInfo(Principal principal) {
        Map<String, Object> info = new HashMap<>();
        
        if (principal instanceof WebSocketUserPrincipal) {
            WebSocketUserPrincipal userPrincipal = (WebSocketUserPrincipal) principal;
            info.put("userId", userPrincipal.getUserId());
            info.put("username", userPrincipal.getUsername());
            info.put("authenticated", true);
        } else if (principal != null) {
            info.put("name", principal.getName());
            info.put("authenticated", true);
        } else {
            info.put("authenticated", false);
        }
        
        return info;
    }
}