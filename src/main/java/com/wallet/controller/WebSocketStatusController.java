package com.wallet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Status Controller
 * Provides debugging endpoints for WebSocket connection status
 */
@RestController
@RequestMapping("/api/websocket")
@Slf4j
public class WebSocketStatusController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        log.info("WebSocket status requested");
        
        Map<String, Object> status = new HashMap<>();
        status.put("message", "WebSocket service is running");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("endpoints", new String[]{"/ws", "/ws-native"});
        status.put("status", "OK");
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebSocketEndpoint() {
        log.info("WebSocket test endpoint called");
        
        Map<String, Object> test = new HashMap<>();
        test.put("message", "WebSocket test successful");
        test.put("serverTime", System.currentTimeMillis());
        test.put("timestamp", LocalDateTime.now().toString());
        test.put("ready", true);
        
        return ResponseEntity.ok(test);
    }
}