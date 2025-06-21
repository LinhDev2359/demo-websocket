package com.eoswallet.controller;

import com.eoswallet.dto.*;
import com.eoswallet.facade.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserFacade userFacade;

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

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        log.debug("Get user profile request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        log.info("Update user profile request for userId: {}", userId);
        
        UserProfileResponse response = userFacade.updateUserProfile(userId, request);
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
}