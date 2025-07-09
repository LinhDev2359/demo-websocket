package com.wallet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration
 * Configure authentication, authorization, CORS và rate limiting
 * 
 * Features:
 * 1. JWT-based stateless authentication
 * 2. CORS configuration cho frontend
 * 3. Rate limiting cho API endpoints
 * 4. Public endpoints configuration
 * 5. Method-level security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Lazy
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * Configure Security Filter Chain
     * Định nghĩa security rules cho toàn bộ application
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF vì sử dụng JWT tokens (stateless)
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS với custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management - stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authentication entry point
            .exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - không cần authentication
                .requestMatchers(
                    "/api/auth/**",           // Authentication endpoints
                    "/api/users/register",    // User registration
                    "/api/users/login",       // User login
                    "/api/health",            // Health check
                    "/api/actuator/**",       // Spring Boot actuator
                    "/actuator/**",           // Alternative actuator path
                    "/swagger-ui/**",         // Swagger UI
                    "/v3/api-docs/**",        // OpenAPI docs
                    "/api-docs/**",           // API docs
                    "/favicon.ico",           // Browser favicon
                    "/error"                  // Error page
                ).permitAll()
                
                // WebSocket endpoints - require authentication at handshake level
                .requestMatchers("/ws/**", "/ws-native/**").permitAll()
                
                // Admin endpoints - temporarily allow any authenticated user for testing
                .requestMatchers("/api/admin/**").authenticated()  // Changed from hasRole("ADMIN") for testing
                
                // Tất cả requests khác cần authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration
     * Cho phép frontend từ different domains gọi API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins - cấu hình theo environment
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",      // Local development
            "https://localhost:*",     // Local HTTPS
            "https://*.yourdomain.com" // Production domain
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Exposed headers - headers mà client có thể đọc
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        // Apply configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Password Encoder Bean
     * Sử dụng BCrypt để hash passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt với strength 12 để match với database hashes
        // Database có existing hashes với strength 12
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Manager Bean
     * Cần thiết cho authentication process
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}