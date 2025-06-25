package com.eoswallet.controller;

import com.eoswallet.dto.*;
import com.eoswallet.facade.UserFacade;
import com.wallet.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserFacade userFacade;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("User registration request for email: {}", request.getEmail());
        
        try {
            UserRegistrationResponse response = userFacade.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> loginUser(
            @Valid @RequestBody UserLoginRequest request) {
        log.info("User login request for email: {}", request.getEmail());
        
        try {
            UserLoginResponse response = userFacade.authenticateUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(HttpServletRequest request) {
        String userId = extractUserIdFromRequest(request);
        log.debug("Get user profile request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.getUserProfile(Long.parseLong(userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Update user profile request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.updateUserProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        log.info("Deactivate user request for userId: {}", userId);
        
        userFacade.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long userId) {
        log.info("Reactivate user request for userId: {}", userId);
        
        userFacade.reactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(Pageable pageable) {
        log.debug("Get all users request with pagination");
        
        Page<UserProfileResponse> response = userFacade.getAllUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> promoteToAdmin(@PathVariable Long userId) {
        log.info("Promote user to admin request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.promoteToAdmin(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> demoteFromAdmin(@PathVariable Long userId) {
        log.info("Demote user from admin request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.demoteFromAdmin(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method để extract userId từ JWT token trong request
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtTokenUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("No valid JWT token found");
    }
    
}