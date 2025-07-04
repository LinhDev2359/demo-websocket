package com.wallet.controller;

import com.wallet.dto.LoginRequest;
import com.wallet.dto.RefreshTokenRequest;
import com.wallet.entity.User;
import com.wallet.service.AuthenticationService;
import com.wallet.service.UserService;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final UserService userService;

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
        try {
            // Get current authentication from SecurityContext
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "No authentication found",
                    "message", "User is not authenticated"
                ));
            }
            
            // Check if authenticated
            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Not authenticated",
                    "message", "User authentication failed"
                ));
            }
            
            // Get user details
            Object principal = authentication.getPrincipal();
            Map<String, Object> userInfo = new HashMap<>();
            
            if (principal instanceof com.wallet.security.CustomUserPrincipal) {
                var customPrincipal = (com.wallet.security.CustomUserPrincipal) principal;
                userInfo.put("userId", customPrincipal.getUserId());
                userInfo.put("username", customPrincipal.getUsername());
                userInfo.put("email", customPrincipal.getEmail());
                userInfo.put("enabled", customPrincipal.isEnabled());
                userInfo.put("accountNonLocked", customPrincipal.isAccountNonLocked());
                userInfo.put("authorities", customPrincipal.getAuthorities().toString());
            } else {
                userInfo.put("username", authentication.getName());
                userInfo.put("principalType", principal.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Authentication successful",
                "authenticated", true,
                "user", userInfo,
                "authorities", authentication.getAuthorities().toString()
            ));
            
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to list all users in database
     */
    @GetMapping("/debug/users")
    public ResponseEntity<Map<String, Object>> listAllUsers() {
        try {
            var users = userService.getAllUsers();
            log.info("Found {} users in database", users.size());
            
            return ResponseEntity.ok(Map.of(
                "total_users", users.size(),
                "users", users.stream().map(user -> Map.of(
                    "id", user.getId(),
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "status", user.getStatus(),
                    "emailVerified", user.getEmailVerified(),
                    "hasPassword", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()
                )).toList()
            ));
        } catch (Exception e) {
            log.error("Failed to list users: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to list users",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to test password encoding
     */
    @PostMapping("/debug/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Username and password are required"
                ));
            }
            
            // Find user
            Optional<User> userOpt = userService.getUserByUsernameOrEmail(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "username", username
                ));
            }
            
            com.wallet.entity.User user = userOpt.get();
            
            // Simple response with user info
            return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "status", user.getStatus(),
                "emailVerified", user.getEmailVerified(),
                "passwordHashExists", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty(),
                "passwordHashLength", user.getPasswordHash() != null ? user.getPasswordHash().length() : 0,
                "note", "Use this to verify user exists before testing login"
            ));
            
        } catch (Exception e) {
            log.error("Password test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Password test failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Simple test endpoint for authenticated users
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testAuth() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Map.of(
            "message", "Access granted",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "user", authentication != null ? authentication.getName() : "unknown",
            "authenticated", authentication != null && authentication.isAuthenticated()
        ));
    }

    /**
     * Temporary endpoint to grant admin role to current user (for testing)
     */
    @PostMapping("/grant-admin")
    public ResponseEntity<Map<String, Object>> grantAdminRole() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Not authenticated"
                ));
            }
            
            if (authentication.getPrincipal() instanceof com.wallet.security.CustomUserPrincipal) {
                var customPrincipal = (com.wallet.security.CustomUserPrincipal) authentication.getPrincipal();
                String userId = customPrincipal.getUserId();
                
                // For testing purposes - add admin role logic here
                // This is a temporary solution for development
                
                return ResponseEntity.ok(Map.of(
                    "message", "Admin role feature needs to be implemented",
                    "userId", userId,
                    "note", "Currently users are created with ROLE_USER only"
                ));
            }
            
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid user principal type"
            ));
            
        } catch (Exception e) {
            log.error("Error granting admin role: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
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