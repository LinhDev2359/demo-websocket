package com.wallet.security;

import com.wallet.entity.User;
import com.wallet.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation that wraps our User entity
 * Provides access to custom user properties like userId, email, etc.
 */
public class CustomUserPrincipal implements UserDetails {
    
    private final User user;
    private final Collection<GrantedAuthority> authorities;
    
    public CustomUserPrincipal(User user) {
        this.user = user;
        this.authorities = getAuthorities(user);
    }
    
    public User getUser() {
        return user;
    }
    
    public String getUserId() {
        return user.getUserId();
    }
    
    public String getEmail() {
        return user.getEmail();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.SUSPENDED;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
    
    private Collection<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Basic role for all users
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // TODO: Add role-based authorities if needed
        // if (user.isAdmin()) {
        //     authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        // }
        
        return authorities;
    }
}