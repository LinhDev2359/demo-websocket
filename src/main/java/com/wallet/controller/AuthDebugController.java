package com.wallet.controller;

import com.wallet.entity.User;
import com.wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Debug Controller
 * Provides debugging endpoints for authentication issues
 */
@RestController
@RequestMapping("/api/debug/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthDebugController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/user/{usernameOrEmail}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String usernameOrEmail) {
        log.info("Debug: Getting user info for: {}", usernameOrEmail);
        
        Optional<User> userOptional = userService.getUserByUsernameOrEmail(usernameOrEmail);
        
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            response.put("found", true);
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("status", user.getStatus());
            response.put("emailVerified", user.getEmailVerified());
            response.put("passwordHashLength", user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
            response.put("passwordHashPrefix", user.getPasswordHash() != null ? 
                    user.getPasswordHash().substring(0, Math.min(10, user.getPasswordHash().length())) : "null");
        } else {
            response.put("found", false);
        }
        
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
        String usernameOrEmail = request.get("username");
        String password = request.get("password");
        
        log.info("Debug: Testing password for user: {}", usernameOrEmail);
        
        Optional<User> userOptional = userService.getUserByUsernameOrEmail(usernameOrEmail);
        
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String storedHash = user.getPasswordHash();
            
            // Test password matching
            boolean matches = passwordEncoder.matches(password, storedHash);
            
            response.put("userFound", true);
            response.put("passwordMatches", matches);
            response.put("passwordLength", password.length());
            response.put("hashLength", storedHash.length());
            response.put("hashPrefix", storedHash.substring(0, Math.min(10, storedHash.length())));
            
            log.info("Password test result - User: {}, Password matches: {}", usernameOrEmail, matches);
        } else {
            response.put("userFound", false);
            response.put("passwordMatches", false);
        }
        
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/hash-password")
    public ResponseEntity<Map<String, Object>> hashPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        
        log.info("Debug: Hashing password");
        
        String hashedPassword = passwordEncoder.encode(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalPassword", password);
        response.put("hashedPassword", hashedPassword);
        response.put("hashLength", hashedPassword.length());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}