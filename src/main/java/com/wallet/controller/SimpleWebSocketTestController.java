package com.wallet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * Simple WebSocket Test Controller
 * 
 * Chức năng:
 * 1. Test basic WebSocket functionality
 * 2. No authentication required
 * 3. Simple echo and broadcast messages
 */
@Controller
@Slf4j
public class SimpleWebSocketTestController {

    /**
     * Simple echo test - returns message back to sender
     */
    @MessageMapping("/test/echo")
    @SendTo("/topic/echo")
    public String echo(String message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Echo request from session {}: {}", sessionId, message);
        
        return "Echo from server: " + message + " (Session: " + sessionId + ")";
    }

    /**
     * Simple ping test - returns pong
     */
    @MessageMapping("/test/ping")
    @SendTo("/topic/ping")
    public String ping(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Ping from session: {}", sessionId);
        
        return "Pong from server at " + System.currentTimeMillis() + " (Session: " + sessionId + ")";
    }

    /**
     * Connection test - just log that we received a message
     */
    @MessageMapping("/test/hello")
    @SendTo("/topic/hello")
    public String hello(String name, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Hello from session {}: {}", sessionId, name);
        
        return "Hello " + name + "! Server time: " + System.currentTimeMillis();
    }
}