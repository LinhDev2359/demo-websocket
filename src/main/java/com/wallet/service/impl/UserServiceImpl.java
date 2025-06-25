package com.wallet.service.impl;

import com.wallet.entity.User;
import com.wallet.entity.UserStatus;
import com.wallet.repository.UserRepository;
import com.wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service Implementation
 * Implementation của UserService interface
 * 
 * Chức năng:
 * 1. Business logic cho user operations
 * 2. Validation và error handling
 * 3. Transaction management
 * 4. Security integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public User createUser(String username, String email, String password, String firstName, String lastName) {
        return createUserWithId(generateUserId(), username, email, password, firstName, lastName);
    }
    
    @Override
    public User createUserWithId(String userId, String username, String email, String password, String firstName, String lastName) {
        log.info("Creating new user with userId: {}, username: {} and email: {}", userId, username, email);
        
        // Validate input
        validateUserInput(username, email, password);
        
        // Check if userId already exists
        if (getUserById(userId).isPresent()) {
            throw new RuntimeException("UserId already exists: " + userId);
        }
        
        // Check if username or email already exists
        if (!isUsernameAvailable(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        if (!isEmailAvailable(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        // Create new user
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        
        User savedUser = userRepository.save(user);
        
        log.info("Successfully created user with ID: {}", savedUser.getUserId());
        return savedUser;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(String userId) {
        return userRepository.findByUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
    
    @Override
    public User updateUserProfile(String userId, String firstName, String lastName) {
        log.info("Updating profile for user: {}", userId);
        
        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        User updatedUser = userRepository.save(user);
        
        log.info("Successfully updated profile for user: {}", userId);
        return updatedUser;
    }
    
    @Override
    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);
        
        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password
        validatePassword(newPassword);
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Successfully changed password for user: {}", userId);
        return true;
    }
    
    @Override
    public User updateUserStatus(String userId, UserStatus status) {
        log.info("Updating status to {} for user: {}", status, userId);
        
        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        
        log.info("Successfully updated status for user: {}", userId);
        return updatedUser;
    }
    
    @Override
    public User verifyUserEmail(String userId) {
        log.info("Verifying email for user: {}", userId);
        
        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setEmailVerified(true);
        User updatedUser = userRepository.save(user);
        
        log.info("Successfully verified email for user: {}", userId);
        return updatedUser;
    }
    
    @Override
    public void updateLastLoginTime(String userId, LocalDateTime lastLoginAt) {
        userRepository.updateLastLoginTime(userId, lastLoginAt);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<User> getActiveUsersWithRecentLogin(int days, Pageable pageable) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        return userRepository.findActiveUsersWithRecentLogin(sinceDate, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        
        // Count verified users
        long verifiedUsers = userRepository.findByEmailVerified(true, Pageable.unpaged()).getTotalElements();
        
        return new UserService.UserStatistics(totalUsers, activeUsers, inactiveUsers, suspendedUsers, verifiedUsers);
    }
    
    /**
     * Generate unique user ID
     */
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * Validate user input
     */
    private void validateUserInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        
        validatePassword(password);
    }
    
    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password must not exceed 100 characters");
        }
        
        // Add more password strength validation if needed
        // e.g., check for special characters, numbers, etc.
    }
}