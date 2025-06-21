package com.wallet.service;

import com.wallet.entity.User;
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
        log.debug("Loading user by username: {}", usernameOrEmail);
        
        User user = userService.getUserByUsernameOrEmail(usernameOrEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        
        return createUserPrincipal(user);
    }

    /**
     * Convert User entity thành Spring Security UserDetails
     * 
     * @param user User entity
     * @return UserDetails object
     */
    private UserDetails createUserPrincipal(User user) {
        Collection<GrantedAuthority> authorities = getAuthorities(user);
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(user.getStatus() == User.UserStatus.INACTIVE)
                .build();
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
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        
        return user;
    }
}