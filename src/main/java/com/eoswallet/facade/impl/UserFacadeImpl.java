package com.eoswallet.facade.impl;

import com.eoswallet.dto.*;
import com.wallet.entity.User;
import com.wallet.entity.UserStatus;
import com.eoswallet.facade.UserFacade;
import com.wallet.service.UserService;
import com.wallet.service.CustomUserDetailsService;
import com.wallet.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Facade Implementation
 * Xử lý các business workflows liên quan đến User
 * 
 * Chức năng:
 * 1. User registration với JWT token generation
 * 2. User authentication và authorization
 * 3. User profile management
 * 4. Cross-service coordination cho user operations
 */
@Service
@Transactional
public class UserFacadeImpl implements UserFacade {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        // Tách fullName thành firstName và lastName
        String[] names = request.getFullName().split(" ", 2);
        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";
        
        // Tạo user mới thông qua UserService
        User user = userService.createUser(
            request.getEmail(), 
            request.getEmail(), 
            request.getPassword(), 
            firstName, 
            lastName
        );
        
        return UserRegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .message("User registered successfully")
                .build();
    }

    @Override
    public UserLoginResponse authenticateUser(UserLoginRequest request) {
        // Tìm user theo email
        var userOpt = userService.getUserByUsernameOrEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Verify password bằng PasswordEncoder
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        // Generate JWT tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenUtil.generateAccessToken(userDetails, user.getUserId(), user.getEmail());
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails, user.getUserId());
        
        return UserLoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role("USER")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Authentication successful")
                .build();
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role("USER")
                .isActive(user.getStatus() == UserStatus.ACTIVE)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        // Tách full name thành first name và last name
        String[] names = request.getFullName().split(" ", 2);
        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";
        
        User user = userService.updateUserProfile(userId.toString(), firstName, lastName);
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role("USER")
                .isActive(user.getStatus() == UserStatus.ACTIVE)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public void deactivateUser(Long userId) {
        userService.updateUserStatus(userId.toString(), UserStatus.INACTIVE);
    }

    @Override
    public void reactivateUser(Long userId) {
        userService.updateUserStatus(userId.toString(), UserStatus.ACTIVE);
    }

    @Override
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userService.getUsers(pageable);
        
        return users.map(user -> UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role("USER")
                .isActive(user.getStatus() == UserStatus.ACTIVE)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    @Override
    public boolean validateUserCredentials(String email, String password) {
        try {
            var userOpt = userService.getUserByUsernameOrEmail(email);
            return userOpt.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void initiatePasswordReset(String email) {
        // TODO: Implement password reset với existing service
        throw new RuntimeException("Password reset not implemented yet");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // TODO: Implement password reset với existing service
        throw new RuntimeException("Password reset not implemented yet");
    }

    @Override
    public void refreshUserSession(Long userId) {
        userService.updateLastLoginTime(userId.toString(), java.time.LocalDateTime.now());
    }

    @Override
    public UserProfileResponse promoteToAdmin(Long userId) {
        // TODO: Implement role promotion với existing service
        throw new RuntimeException("Role promotion not implemented yet");
    }

    @Override
    public UserProfileResponse demoteFromAdmin(Long userId) {
        // TODO: Implement role demotion với existing service
        throw new RuntimeException("Role demotion not implemented yet");
    }
}