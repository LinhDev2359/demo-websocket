package com.wallet.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Utility Class
 * Xử lý việc tạo, validate và extract thông tin từ JWT tokens
 * 
 * Chức năng chính:
 * 1. Generate JWT access tokens và refresh tokens
 * 2. Validate tokens và check expiration
 * 3. Extract username và claims từ tokens
 * 4. Hỗ trợ millions of users với high performance
 */
@Component
@Slf4j
public class JwtTokenUtil {
    
    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);
    
    // JWT Configuration từ application.yml
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:86400}") // Default 24 hours
    private Long expiration;
    
    @Value("${jwt.refresh-expiration:604800}") // Default 7 days  
    private Long refreshExpiration;
    
    // Claims constants
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    
    // Token types
    private static final String ACCESS_TOKEN = "ACCESS";
    private static final String REFRESH_TOKEN = "REFRESH";

    /**
     * Tạo secret key từ string secret
     * Đảm bảo secret đủ mạnh cho JWT signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extract username từ JWT token
     * @param token JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extract user ID từ JWT token
     * @param token JWT token  
     * @return user ID
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get(CLAIM_USER_ID, String.class));
    }

    /**
     * Extract email từ JWT token
     * @param token JWT token
     * @return email
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get(CLAIM_EMAIL, String.class));
    }

    /**
     * Extract expiration date từ JWT token
     * @param token JWT token
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim từ JWT token
     * @param token JWT token
     * @param claimsResolver function để extract claim
     * @return claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract tất cả claims từ JWT token
     * @param token JWT token
     * @return all claims
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check xem JWT token đã expired chưa
     * @param token JWT token
     * @return true nếu expired
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Generate JWT access token cho user
     * @param userDetails user details
     * @param userId user ID
     * @param email user email
     * @return JWT access token
     */
    public String generateAccessToken(UserDetails userDetails, String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_EMAIL, email);
        claims.put(CLAIM_TOKEN_TYPE, ACCESS_TOKEN);
        claims.put(CLAIM_AUTHORITIES, userDetails.getAuthorities());
        
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    /**
     * Generate JWT refresh token cho user
     * @param userDetails user details
     * @param userId user ID
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_TYPE, REFRESH_TOKEN);
        
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    /**
     * Tạo JWT token với claims và expiration time
     * @param claims token claims
     * @param subject token subject (username)
     * @param expirationTime expiration time in seconds
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate JWT token với user details
     * @param token JWT token
     * @param userDetails user details
     * @return true nếu valid
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate JWT token mà không cần user details
     * Dùng cho WebSocket authentication
     * @param token JWT token
     * @return true nếu valid
     */
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check xem token có phải là access token không
     * @param token JWT token
     * @return true nếu là access token
     */
    public Boolean isAccessToken(String token) {
        try {
            String tokenType = getClaimFromToken(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
            return ACCESS_TOKEN.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check xem token có phải là refresh token không
     * @param token JWT token
     * @return true nếu là refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = getClaimFromToken(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
            return REFRESH_TOKEN.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get remaining time until token expires (in seconds)
     * Useful cho cache TTL và session management
     * @param token JWT token
     * @return remaining seconds
     */
    public Long getRemainingTimeToExpire(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            return (expiration.getTime() - now.getTime()) / 1000;
        } catch (Exception e) {
            return 0L;
        }
    }
}