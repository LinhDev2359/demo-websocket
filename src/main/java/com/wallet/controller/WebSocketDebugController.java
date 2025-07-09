package com.wallet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket Debug Controller
 * Test controller for debugging WebSocket connections
 */
@Controller
@Slf4j
public class WebSocketDebugController {

    @MessageMapping("/debug/ping")
    @SendTo("/topic/debug/pong")
    public String ping(SimpMessageHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        
        log.info("Ping received from session: {}, user: {}", 
                sessionId, 
                user != null ? user.getName() : "anonymous");
        
        return "Pong from server - Session: " + sessionId;
    }

    @MessageMapping("/debug/echo")
    @SendToUser("/queue/debug/echo")
    public String echo(String message, SimpMessageHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        log.info("Echo received from user: {} - Message: {}", 
                user != null ? user.getName() : "anonymous", 
                message);
        
        return "Echo: " + message;
    }

    @MessageMapping("/debug/test")
    @SendTo("/topic/debug/test")
    public String test(SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        boolean authenticated = accessor.getSessionAttributes().get("authenticated") != null 
                && (boolean) accessor.getSessionAttributes().get("authenticated");
        
        String response = String.format(
            "Test response - Session: %s, User: %s, Authenticated: %s",
            sessionId,
            user != null ? user.getName() : "anonymous",
            authenticated
        );
        
        log.info(response);
        return response;
    }
}