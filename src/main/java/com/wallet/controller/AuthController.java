package com.wallet.controller;

import com.wallet.dto.LoginRequest;
import com.wallet.dto.RefreshTokenRequest;
import com.wallet.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 * Expose REST endpoints cho authentication operations
 * 
 * Endpoints:
 * - POST /api/auth/login - User login
 * - POST /api/auth/refresh - Token refresh
 * - POST /api/auth/register - User registration (future)
 * - POST /api/auth/logout - User logout (future)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * User login endpoint
     * 
     * @param loginRequest login credentials
     * @return authentication response với JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request for username: {}", loginRequest.getUsername());
        
        try {
            Map<String, Object> response = authenticationService.authenticateUser(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Login failed for username: {} - {}", loginRequest.getUsername(), e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Authentication failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Token refresh endpoint
     * 
     * @param refreshRequest refresh token request
     * @return new authentication response
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        log.debug("Token refresh request");
        
        try {
            Map<String, Object> response = authenticationService.refreshToken(
                refreshRequest.getRefreshToken()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Token refresh failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Test endpoint để verify authentication
     * Chỉ users đã authenticate mới access được
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // TODO: Implement get current user from SecurityContext
        return ResponseEntity.ok(Map.of(
            "message", "Authentication successful",
            "user", "Current authenticated user info here"
        ));
    }

    /**
     * Health check endpoint cho authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Authentication Service"
        ));
    }
}