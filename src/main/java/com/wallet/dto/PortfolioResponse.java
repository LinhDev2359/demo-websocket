package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Portfolio response DTO for WebSocket real-time streaming
 * Contains aggregated wallet and balance information for a user
 */
public class PortfolioResponse {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("total_wallets")
    private Integer totalWallets;
    
    @JsonProperty("wallets")
    private List<WalletPortfolioInfo> wallets;
    
    @JsonProperty("total_balance")
    private BalanceSummary totalBalance;
    
    @JsonProperty("last_updated")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    @JsonProperty("message_type")
    private String messageType = "PORTFOLIO_UPDATE";
    
    // Constructors
    public PortfolioResponse() {}
    
    public PortfolioResponse(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Inner class for wallet portfolio information
    public static class WalletPortfolioInfo {
        @JsonProperty("wallet_id")
        private Long walletId;
        
        @JsonProperty("wallet_address")
        private String walletAddress;
        
        @JsonProperty("wallet_type")
        private String walletType;
        
        @JsonProperty("balances")
        private List<BalanceInfo> balances;
        
        @JsonProperty("is_primary")
        private Boolean isPrimary;
        
        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        // Constructors
        public WalletPortfolioInfo() {}
        
        public WalletPortfolioInfo(Long walletId, String walletAddress, String walletType) {
            this.walletId = walletId;
            this.walletAddress = walletAddress;
            this.walletType = walletType;
        }
        
        // Getters and Setters
        public Long getWalletId() { return walletId; }
        public void setWalletId(Long walletId) { this.walletId = walletId; }
        
        public String getWalletAddress() { return walletAddress; }
        public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
        
        public String getWalletType() { return walletType; }
        public void setWalletType(String walletType) { this.walletType = walletType; }
        
        public List<BalanceInfo> getBalances() { return balances; }
        public void setBalances(List<BalanceInfo> balances) { this.balances = balances; }
        
        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
    
    // Inner class for balance information
    public static class BalanceInfo {
        @JsonProperty("token_type")
        private String tokenType;
        
        @JsonProperty("balance")
        private BigDecimal balance;
        
        @JsonProperty("last_updated")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;
        
        // Constructors
        public BalanceInfo() {}
        
        public BalanceInfo(String tokenType, BigDecimal balance) {
            this.tokenType = tokenType;
            this.balance = balance;
            this.lastUpdated = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    // Inner class for balance summary
    public static class BalanceSummary {
        @JsonProperty("total_eos")
        private BigDecimal totalEos = BigDecimal.ZERO;
        
        @JsonProperty("total_ram")
        private BigDecimal totalRam = BigDecimal.ZERO;
        
        @JsonProperty("total_rams")
        private BigDecimal totalRams = BigDecimal.ZERO;
        
        @JsonProperty("total_wram")
        private BigDecimal totalWram = BigDecimal.ZERO;
        
        // Constructors
        public BalanceSummary() {}
        
        // Getters and Setters
        public BigDecimal getTotalEos() { return totalEos; }
        public void setTotalEos(BigDecimal totalEos) { this.totalEos = totalEos; }
        
        public BigDecimal getTotalRam() { return totalRam; }
        public void setTotalRam(BigDecimal totalRam) { this.totalRam = totalRam; }
        
        public BigDecimal getTotalRams() { return totalRams; }
        public void setTotalRams(BigDecimal totalRams) { this.totalRams = totalRams; }
        
        public BigDecimal getTotalWram() { return totalWram; }
        public void setTotalWram(BigDecimal totalWram) { this.totalWram = totalWram; }
    }
    
    // Main class getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public Integer getTotalWallets() { return totalWallets; }
    public void setTotalWallets(Integer totalWallets) { this.totalWallets = totalWallets; }
    
    public List<WalletPortfolioInfo> getWallets() { return wallets; }
    public void setWallets(List<WalletPortfolioInfo> wallets) { this.wallets = wallets; }
    
    public BalanceSummary getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BalanceSummary totalBalance) { this.totalBalance = totalBalance; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}