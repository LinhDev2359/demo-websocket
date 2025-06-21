package com.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "EOS Wallet System",
            "version", "1.0.0-SNAPSHOT"
        );
    }
    
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "message", "EOS Wallet System API",
            "status", "Running"
        );
    }
}