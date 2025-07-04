package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Portfolio List Response DTO for WebSocket API
 * 
 * Chứa danh sách portfolio items với pagination info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioListResponse {
    
    @JsonProperty("message_type")
    private String messageType = "PORTFOLIO_LIST";
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("portfolios")
    private List<PortfolioItem> portfolios;
    
    @JsonProperty("pagination")
    private PaginationInfo pagination;
    
    @JsonProperty("summary")
    private PortfolioSummary summary;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("cache_hit")
    private boolean cacheHit = false;
    
    /**
     * Portfolio Item - Thông tin từng wallet
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioItem {
        
        @JsonProperty("wallet_id")
        private Long walletId;
        
        @JsonProperty("wallet_address")
        private String walletAddress;
        
        @JsonProperty("wallet_name")
        private String walletName;
        
        @JsonProperty("wallet_type")
        private String walletType;
        
        @JsonProperty("is_primary")
        private boolean isPrimary;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("balances")
        private List<BalanceItem> balances;
        
        @JsonProperty("total_balance_usd")
        private BigDecimal totalBalanceUsd = BigDecimal.ZERO;
        
        @JsonProperty("total_tokens")
        private int totalTokens = 0;
        
        @JsonProperty("last_updated")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;
        
        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
    
    /**
     * Balance Item - Thông tin từng token balance
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceItem {
        
        @JsonProperty("balance_id")
        private Long balanceId;
        
        @JsonProperty("token_symbol")
        private String tokenSymbol;
        
        @JsonProperty("token_name")
        private String tokenName;
        
        @JsonProperty("token_contract")
        private String tokenContract;
        
        @JsonProperty("balance")
        private BigDecimal balance = BigDecimal.ZERO;
        
        @JsonProperty("available_balance")
        private BigDecimal availableBalance = BigDecimal.ZERO;
        
        @JsonProperty("locked_balance")
        private BigDecimal lockedBalance = BigDecimal.ZERO;
        
        @JsonProperty("staked_balance")
        private BigDecimal stakedBalance = BigDecimal.ZERO;
        
        @JsonProperty("usd_value")
        private BigDecimal usdValue = BigDecimal.ZERO;
        
        @JsonProperty("token_price_usd")
        private BigDecimal tokenPriceUsd = BigDecimal.ZERO;
        
        @JsonProperty("price_change_24h")
        private BigDecimal priceChange24h = BigDecimal.ZERO;
        
        @JsonProperty("is_native_token")
        private boolean isNativeToken = false;
        
        @JsonProperty("decimal_places")
        private int decimalPlaces = 8;
        
        @JsonProperty("last_updated")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;
    }
    
    /**
     * Pagination Information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationInfo {
        
        @JsonProperty("current_page")
        private int currentPage;
        
        @JsonProperty("page_size")
        private int pageSize;
        
        @JsonProperty("total_pages")
        private int totalPages;
        
        @JsonProperty("total_elements")
        private long totalElements;
        
        @JsonProperty("has_next")
        private boolean hasNext;
        
        @JsonProperty("has_previous")
        private boolean hasPrevious;
        
        @JsonProperty("is_first")
        private boolean isFirst;
        
        @JsonProperty("is_last")
        private boolean isLast;
    }
    
    /**
     * Portfolio Summary - Tổng quan portfolio
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioSummary {
        
        @JsonProperty("total_wallets")
        private int totalWallets = 0;
        
        @JsonProperty("active_wallets")
        private int activeWallets = 0;
        
        @JsonProperty("total_balance_usd")
        private BigDecimal totalBalanceUsd = BigDecimal.ZERO;
        
        @JsonProperty("total_tokens")
        private int totalTokens = 0;
        
        @JsonProperty("primary_wallet_address")
        private String primaryWalletAddress;
        
        @JsonProperty("last_sync_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastSyncTime;
        
        @JsonProperty("next_sync_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime nextSyncTime;
    }
}