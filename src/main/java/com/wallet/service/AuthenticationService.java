package com.wallet.service;

import com.wallet.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Service
 * Xử lý authentication logic: login, register, refresh token
 * 
 * Chức năng:
 * 1. User login với username/password
 * 2. User registration
 * 3. JWT token refresh
 * 4. Password validation và encryption
 * 5. Security logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user với username/password
     * 
     * @param username username hoặc email
     * @param password plain text password
     * @return authentication response với tokens
     * @throws BadCredentialsException nếu credentials không đúng
     */
    @Transactional(readOnly = true)
    public Map<String, Object> authenticateUser(String username, String password) {
        log.info("Authentication attempt for user: {}", username);
        
        try {
            // Authenticate với Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Get authenticated user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Generate tokens
            String userId = extractUserIdFromUserDetails(userDetails);
            String email = extractEmailFromUserDetails(userDetails);
            
            String accessToken = jwtTokenUtil.generateAccessToken(userDetails, userId, email);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails, userId);

            log.info("Authentication successful for user: {}", username);

            return createAuthenticationResponse(userDetails, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user: {} - Invalid credentials", username);
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            log.error("Authentication error for user: {} - {}", username, e.getMessage());
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * Refresh JWT access token using refresh token
     * 
     * @param refreshToken JWT refresh token
     * @return new authentication response với new tokens
     * @throws BadCredentialsException nếu refresh token không valid
     */
    @Transactional(readOnly = true)
    public Map<String, Object> refreshToken(String refreshToken) {
        log.debug("Token refresh attempt");
        
        try {
            // Validate refresh token
            if (!jwtTokenUtil.validateToken(refreshToken) || !jwtTokenUtil.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            // Extract username từ refresh token
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Validate token with user details
            if (!jwtTokenUtil.validateToken(refreshToken, userDetails)) {
                throw new BadCredentialsException("Refresh token validation failed");
            }

            // Generate new tokens
            String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
            String email = extractEmailFromUserDetails(userDetails);
            
            String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails, userId, email);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails, userId);

            log.info("Token refresh successful for user: {}", username);

            return createAuthenticationResponse(userDetails, newAccessToken, newRefreshToken);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    /**
     * Register new user
     * TODO: Implement khi có UserRepository
     * 
     * @param username username
     * @param email email
     * @param password plain text password
     * @return registration response
     */
    @Transactional
    public Map<String, Object> registerUser(String username, String email, String password) {
        log.info("User registration attempt for username: {}, email: {}", username, email);
        
        // Validate input
        validateRegistrationInput(username, email, password);
        
        try {
            // TODO: Check if user already exists
            // if (userRepository.existsByUsernameOrEmail(username, email)) {
            //     throw new RuntimeException("Username or email already exists");
            // }

            // Hash password
            String hashedPassword = passwordEncoder.encode(password);
            
            // TODO: Create và save user entity
            // User user = new User();
            // user.setUsername(username);
            // user.setEmail(email);
            // user.setPasswordHash(hashedPassword);
            // user.setStatus(User.UserStatus.ACTIVE);
            // user = userRepository.save(user);

            log.info("User registration successful for username: {}", username);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("username", username);
            
            return response;

        } catch (Exception e) {
            log.error("User registration failed for username: {} - {}", username, e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Validate registration input
     * 
     * @param username username
     * @param email email  
     * @param password password
     */
    private void validateRegistrationInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    /**
     * Create authentication response object
     * 
     * @param userDetails user details
     * @param accessToken access token
     * @param refreshToken refresh token
     * @return authentication response
     */
    private Map<String, Object> createAuthenticationResponse(UserDetails userDetails, 
                                                           String accessToken, 
                                                           String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        
        // Token information
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400); // 24 hours in seconds
        
        // User information
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", userDetails.getUsername());
        userInfo.put("authorities", userDetails.getAuthorities());
        response.put("user", userInfo);
        
        return response;
    }

    /**
     * Extract user ID từ UserDetails
     * TODO: Implement khi có proper UserDetails implementation
     * 
     * @param userDetails user details
     * @return user ID
     */
    private String extractUserIdFromUserDetails(UserDetails userDetails) {
        // Temporary implementation
        return "user_000001";
    }

    /**
     * Extract email từ UserDetails
     * TODO: Implement khi có proper UserDetails implementation
     * 
     * @param userDetails user details
     * @return email
     */
    private String extractEmailFromUserDetails(UserDetails userDetails) {
        // Temporary implementation
        return "user@example.com";
    }
}