package com.wallet.service;

import com.wallet.entity.User;
import com.wallet.entity.UserStatus;
import com.wallet.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Custom UserDetailsService Implementation
 * Load user từ database để authenticate với Spring Security
 * 
 * Chức năng:
 * 1. Load user by username hoặc email
 * 2. Convert User entity thành UserDetails
 * 3. Hỗ trợ caching cho millions of users
 * 4. Handle user status (active, inactive, suspended)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    /**
     * Load user by username cho Spring Security authentication
     * 
     * @param usernameOrEmail username hoặc email
     * @return UserDetails object
     * @throws UsernameNotFoundException nếu user không tồn tại
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", usernameOrEmail);
        
        Optional<User> userOptional = userService.getUserByUsernameOrEmail(usernameOrEmail);
        
        if (userOptional.isEmpty()) {
            log.warn("User not found in database: {}", usernameOrEmail);
            
            // Debug: list all users to see what's in database
            try {
                List<User> allUsers = userService.getAllUsers();
                log.info("Total users in database: {}", allUsers.size());
                for (User u : allUsers) {
                    log.info("User in DB - ID: {}, Username: {}, Email: {}, Status: {}", 
                            u.getUserId(), u.getUsername(), u.getEmail(), u.getStatus());
                }
            } catch (Exception e) {
                log.error("Error listing users: {}", e.getMessage());
            }
            
            throw new UsernameNotFoundException("User not found: " + usernameOrEmail);
        }
        
        User user = userOptional.get();
        log.info("Found user - ID: {}, Username: {}, Email: {}, Status: {}, EmailVerified: {}", 
                user.getUserId(), user.getUsername(), user.getEmail(), user.getStatus(), user.getEmailVerified());
        
        // Debug: Log password hash info
        String passwordHash = user.getPasswordHash();
        log.info("Password hash info - Length: {}, Starts with: {}", 
                passwordHash != null ? passwordHash.length() : 0, 
                passwordHash != null ? passwordHash.substring(0, Math.min(10, passwordHash.length())) : "null");
        
        return createUserPrincipal(user);
    }

    /**
     * Convert User entity thành Spring Security UserDetails
     * 
     * @param user User entity
     * @return UserDetails object
     */
    private UserDetails createUserPrincipal(User user) {
        return new CustomUserPrincipal(user);
    }

    /**
     * Get authorities (roles/permissions) cho user
     * 
     * @param user User entity
     * @return collection of authorities
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Basic role cho tất cả users
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // TODO: Implement role-based authorities nếu cần
        // if (user.isAdmin()) {
        //     authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        // }
        
        return authorities;
    }

    /**
     * Temporary method để tạo mock user cho development
     * TODO: Remove khi có database integration
     * 
     * @param usernameOrEmail username hoặc email
     * @return mock User object
     */
    private User createMockUser(String usernameOrEmail) {
        User user = new User();
        user.setUserId("user_000001");
        user.setUsername(usernameOrEmail.contains("@") ? "testuser" : usernameOrEmail);
        user.setEmail(usernameOrEmail.contains("@") ? usernameOrEmail : "test@example.com");
        
        // BCrypt hash for password "password123"
        user.setPasswordHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        
        return user;
    }
}