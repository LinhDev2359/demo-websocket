package com.wallet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * Xử lý các request không được authenticate
 * 
 * Chức năng:
 * 1. Trả về JSON response thống nhất cho unauthorized requests
 * 2. Log security events để monitoring
 * 3. Không redirect sang login page (vì là REST API)
 * 4. Hỗ trợ CORS cho frontend calls
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Được gọi khi user không có quyền truy cập vào protected resource
     * 
     * @param request HTTP request
     * @param response HTTP response  
     * @param authException authentication exception
     * @throws IOException nếu có lỗi I/O
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        // Log unauthorized access attempt để security monitoring
        log.warn("Unauthorized access attempt - IP: {}, URI: {}, Method: {}, User-Agent: {}", 
                getClientIpAddress(request),
                request.getRequestURI(),
                request.getMethod(),
                request.getHeader("User-Agent"));

        // Set response headers
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Add CORS headers để frontend có thể đọc response
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");

        // Tạo error response body
        Map<String, Object> errorResponse = createErrorResponse(request, authException);
        
        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Tạo standardized error response
     * 
     * @param request HTTP request
     * @param authException authentication exception
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                   AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        // Basic error information
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Authentication required to access this resource");
        errorResponse.put("path", request.getRequestURI());
        
        // Additional details for debugging (chỉ trong development)
        if (isDebugMode()) {
            errorResponse.put("details", authException.getMessage());
            errorResponse.put("exception", authException.getClass().getSimpleName());
        }
        
        // Helpful information for client
        errorResponse.put("hint", "Please provide a valid JWT token in the Authorization header");
        errorResponse.put("format", "Bearer <token>");
        
        return errorResponse;
    }

    /**
     * Get client IP address từ request
     * Xử lý các trường hợp có proxy/load balancer
     * 
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check các headers thường dùng bởi proxies
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Nếu có multiple IPs, lấy cái đầu tiên
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Check xem có đang ở debug mode không
     * Dựa vào system property hoặc environment variable
     * 
     * @return true nếu debug mode
     */
    private boolean isDebugMode() {
        String profile = System.getProperty("spring.profiles.active", "prod");
        return "dev".equalsIgnoreCase(profile) || "debug".equalsIgnoreCase(profile);
    }
}