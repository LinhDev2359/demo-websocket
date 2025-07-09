package com.wallet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Test Controller
 * Provides test endpoints for WebSocket functionality
 * 
 * Test endpoints:
 * - /app/test/ping -> /topic/ping
 * - /app/test/echo -> /topic/echo  
 * - /app/test/hello -> /topic/hello
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Simple ping test endpoint
     * Client sends to: /app/test/ping
     * Response sent to: /topic/ping
     */
    @MessageMapping("/test/ping")
    @SendTo("/topic/ping")
    public Map<String, Object> ping() {
        log.info("Received ping request");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "OK");
        
        return response;
    }
    
    /**
     * Echo test endpoint - returns the same message back
     * Client sends to: /app/test/echo
     * Response sent to: /topic/echo
     */
    @MessageMapping("/test/echo")
    @SendTo("/topic/echo")
    public Map<String, Object> echo(@Payload String message) {
        log.info("Received echo request with message: {}", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalMessage", message);
        response.put("echoedMessage", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "ECHOED");
        
        return response;
    }
    
    /**
     * Hello test endpoint - greets the sender
     * Client sends to: /app/test/hello
     * Response sent to: /topic/hello
     */
    @MessageMapping("/test/hello")
    @SendTo("/topic/hello")
    public Map<String, Object> hello(@Payload String name) {
        log.info("Received hello request from: {}", name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("greeting", "Hello, " + (name != null && !name.isEmpty() ? name : "Anonymous") + "!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("serverInfo", "WebSocket Server v1.0");
        response.put("status", "GREETED");
        
        return response;
    }
    
    /**
     * Connection test endpoint - verifies WebSocket connection
     * Client sends to: /app/test/connection
     * Response sent to: /topic/connection
     */
    @MessageMapping("/test/connection")
    @SendTo("/topic/connection")
    public Map<String, Object> testConnection() {
        log.info("Testing WebSocket connection");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "WebSocket connection is working properly");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("serverTime", System.currentTimeMillis());
        response.put("status", "CONNECTED");
        
        return response;
    }
    
    /**
     * Keep-alive endpoint - sends periodic heartbeat
     * Client sends to: /app/test/keepalive
     * Response sent to: /topic/keepalive
     */
    @MessageMapping("/test/keepalive")
    @SendTo("/topic/keepalive")
    public Map<String, Object> keepAlive() {
        log.debug("Keep-alive ping received");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "keep-alive");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("serverTime", System.currentTimeMillis());
        response.put("status", "ALIVE");
        
        return response;
    }
    
    /**
     * Broadcast test - sends message to all connected clients
     * Client sends to: /app/test/broadcast
     * Response sent to: /topic/broadcast
     */
    @MessageMapping("/test/broadcast")
    public void broadcast(@Payload Map<String, String> payload) {
        log.info("Broadcasting message: {}", payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "BROADCAST");
        response.put("message", payload.get("message"));
        response.put("sender", payload.get("sender"));
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Broadcast to all subscribers
        messagingTemplate.convertAndSend("/topic/broadcast", response);
    }
}