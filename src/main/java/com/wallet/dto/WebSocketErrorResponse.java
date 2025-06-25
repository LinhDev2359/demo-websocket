package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * WebSocket error response DTO
 * Used to send error messages through WebSocket connections
 */
public class WebSocketErrorResponse {
    
    @JsonProperty("message_type")
    private String messageType = "ERROR";
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("error_details")
    private String errorDetails;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("request_id")
    private String requestId;
    
    // Constructors
    public WebSocketErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public WebSocketErrorResponse(String errorCode, String errorMessage) {
        this();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public WebSocketErrorResponse(String errorCode, String errorMessage, String userId) {
        this(errorCode, errorMessage);
        this.userId = userId;
    }
    
    public WebSocketErrorResponse(String errorCode, String errorMessage, String errorDetails, String userId) {
        this(errorCode, errorMessage, userId);
        this.errorDetails = errorDetails;
    }
    
    // Static factory methods for common errors
    public static WebSocketErrorResponse unauthorized(String userId) {
        return new WebSocketErrorResponse("UNAUTHORIZED", "Authentication failed", userId);
    }
    
    public static WebSocketErrorResponse accessDenied(String userId, String resource) {
        return new WebSocketErrorResponse("ACCESS_DENIED", 
            "Access denied to resource: " + resource, userId);
    }
    
    public static WebSocketErrorResponse invalidRequest(String userId, String details) {
        return new WebSocketErrorResponse("INVALID_REQUEST", 
            "Invalid request format", details, userId);
    }
    
    public static WebSocketErrorResponse serviceUnavailable(String userId) {
        return new WebSocketErrorResponse("SERVICE_UNAVAILABLE", 
            "Service temporarily unavailable", userId);
    }
    
    public static WebSocketErrorResponse portfolioNotFound(String userId) {
        return new WebSocketErrorResponse("PORTFOLIO_NOT_FOUND", 
            "Portfolio not found for user", userId);
    }
    
    public static WebSocketErrorResponse rateLimitExceeded(String userId) {
        return new WebSocketErrorResponse("RATE_LIMIT_EXCEEDED", 
            "Rate limit exceeded. Please slow down", userId);
    }
    
    // Getters and Setters
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    @Override
    public String toString() {
        return "WebSocketErrorResponse{" +
                "messageType='" + messageType + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}