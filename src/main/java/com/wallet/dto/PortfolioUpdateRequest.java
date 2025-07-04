package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Portfolio update request DTO for WebSocket messages
 * Used when clients request portfolio updates via WebSocket
 */
public class PortfolioUpdateRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("refresh_cache")
    private Boolean refreshCache = false;
    
    @JsonProperty("include_inactive_wallets")
    private Boolean includeInactiveWallets = false;
    
    @JsonProperty("wallet_address_filter")
    private String walletAddressFilter;
    
    @JsonProperty("subscription_type")
    private SubscriptionType subscriptionType = SubscriptionType.REAL_TIME;
    
    // Enum for subscription types
    public enum SubscriptionType {
        @JsonProperty("real_time")
        REAL_TIME,
        
        @JsonProperty("on_demand")
        ON_DEMAND,
        
        @JsonProperty("periodic")
        PERIODIC
    }
    
    // Constructors
    public PortfolioUpdateRequest() {}
    
    public PortfolioUpdateRequest(String userId) {
        this.userId = userId;
    }
    
    public PortfolioUpdateRequest(String userId, Boolean refreshCache) {
        this.userId = userId;
        this.refreshCache = refreshCache;
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Boolean getRefreshCache() { return refreshCache; }
    public void setRefreshCache(Boolean refreshCache) { this.refreshCache = refreshCache; }
    
    public Boolean getIncludeInactiveWallets() { return includeInactiveWallets; }
    public void setIncludeInactiveWallets(Boolean includeInactiveWallets) { this.includeInactiveWallets = includeInactiveWallets; }
    
    public String getWalletAddressFilter() { return walletAddressFilter; }
    public void setWalletAddressFilter(String walletAddressFilter) { this.walletAddressFilter = walletAddressFilter; }
    
    public SubscriptionType getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(SubscriptionType subscriptionType) { this.subscriptionType = subscriptionType; }
    
    @Override
    public String toString() {
        return "PortfolioUpdateRequest{" +
                "userId='" + userId + '\'' +
                ", refreshCache=" + refreshCache +
                ", includeInactiveWallets=" + includeInactiveWallets +
                ", walletAddressFilter='" + walletAddressFilter + '\'' +
                ", subscriptionType=" + subscriptionType +
                '}';
    }
}