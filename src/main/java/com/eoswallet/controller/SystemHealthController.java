package com.eoswallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "EOS Wallet System",
            "architecture", "Clean Architecture with Facade Pattern",
            "message", "All systems operational"
        ));
    }

    @GetMapping("/facade")
    public ResponseEntity<Map<String, String>> facadeHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "layer", "Facade Layer",
            "message", "UserFacade and WalletFacade are ready"
        ));
    }
}