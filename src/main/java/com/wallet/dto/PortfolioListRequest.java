package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Portfolio List Request DTO for WebSocket API
 * 
 * Sử dụng cho:
 * - /app/portfolio/list - Get portfolio list với pagination
 * - /app/portfolio/list/refresh - Refresh portfolio list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioListRequest {
    
    @JsonProperty("user_id")
    @NotNull(message = "User ID cannot be null")
    private String userId;
    
    @JsonProperty("page")
    @Min(value = 0, message = "Page number cannot be negative")
    private int page = 0;
    
    @JsonProperty("size")
    @Min(value = 1, message = "Page size must be at least 1")
    private int size = 20;
    
    @JsonProperty("refresh_cache")
    private boolean refreshCache = false;
    
    @JsonProperty("include_balances")
    private boolean includeBalances = true;
    
    @JsonProperty("include_inactive")
    private boolean includeInactive = false;
    
    @JsonProperty("min_balance_usd")
    private Double minBalanceUsd = 0.0;
    
    @JsonProperty("token_filter")
    private String tokenFilter; // Filter by token symbol
    
    @JsonProperty("wallet_type_filter")
    private String walletTypeFilter; // Filter by wallet type
    
    /**
     * Simple constructor cho basic request
     */
    public PortfolioListRequest(String userId) {
        this.userId = userId;
    }
    
    /**
     * Constructor với pagination
     */
    public PortfolioListRequest(String userId, int page, int size) {
        this.userId = userId;
        this.page = page;
        this.size = size;
    }
    
    /**
     * Constructor với refresh option
     */
    public PortfolioListRequest(String userId, boolean refreshCache) {
        this.userId = userId;
        this.refreshCache = refreshCache;
    }
}