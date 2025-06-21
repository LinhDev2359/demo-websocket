package com.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Request Filter
 * Filter này chạy trước mọi request để validate JWT token
 * 
 * Chức năng:
 * 1. Extract JWT token từ Authorization header
 * 2. Validate token và set authentication context
 * 3. Hỗ trợ millions of users với high performance
 * 4. Skip validation cho public endpoints
 */
@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    @Lazy
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // Authorization header constants
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // Public endpoints không cần authentication
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/register", 
        "/api/auth/refresh",
        "/api/health",
        "/api/actuator/health",
        "/api/actuator/info",
        "/actuator/health",
        "/actuator/info",
        "/swagger-ui/",
        "/api-docs/",
        "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) throws ServletException, IOException {
        
        try {
            // Skip JWT validation cho public endpoints
            if (isPublicEndpoint(request.getRequestURI())) {
                chain.doFilter(request, response);
                return;
            }

            // Extract JWT token từ request header
            String jwtToken = extractJwtFromRequest(request);
            
            if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Get username từ token
                String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                
                if (username != null) {
                    // Load user details từ database
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Validate token với user details
                    if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                        // Set authentication context
                        setAuthenticationContext(userDetails, request);
                        
                        log.debug("JWT authentication successful for user: {}", username);
                    } else {
                        log.warn("JWT token validation failed for user: {}", username);
                    }
                }
            }
        } catch (Exception e) {
            // Log error nhưng không block request
            // Để SecurityConfig handle unauthorized access
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Continue với filter chain
        chain.doFilter(request, response);
    }

    /**
     * Extract JWT token từ Authorization header
     * 
     * @param request HTTP request
     * @return JWT token hoặc null nếu không có
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        
        // Log missing token cho debugging
        if (authorizationHeader != null && !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Authorization header does not start with Bearer: {}", 
                     authorizationHeader.substring(0, Math.min(authorizationHeader.length(), 20)));
        }
        
        return null;
    }

    /**
     * Set authentication context cho current request
     * 
     * @param userDetails user details
     * @param request HTTP request
     */
    private void setAuthenticationContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(
                userDetails, 
                null, 
                userDetails.getAuthorities()
            );
        
        // Set additional details từ request
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        // Set authentication vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    /**
     * Check xem endpoint có phải là public không
     * 
     * @param requestURI request URI
     * @return true nếu là public endpoint
     */
    private boolean isPublicEndpoint(String requestURI) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (requestURI.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Override shouldNotFilter để skip filter cho OPTIONS requests
     * Cần thiết cho CORS preflight requests
     * 
     * @param request HTTP request
     * @return true nếu nên skip filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter cho OPTIONS requests (CORS preflight)
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}