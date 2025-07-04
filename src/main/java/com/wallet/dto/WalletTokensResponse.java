package com.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho wallet tokens endpoint
 * 
 * Trả về danh sách tokens của một wallet cùng với metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTokensResponse {
    
    /**
     * Wallet ID
     */
    private Long walletId;
    
    /**
     * EOS wallet address
     */
    private String walletAddress;
    
    /**
     * Wallet name
     */
    private String walletName;
    
    /**
     * Danh sách tokens trong wallet
     */
    private List<TokenInfo> tokens;
    
    /**
     * Tổng số loại token
     */
    private Integer totalTokenTypes;
    
    /**
     * Tổng USD value của tất cả tokens (nếu có price data)
     */
    private BigDecimal totalUsdValue;
    
    /**
     * Số lượng verified tokens
     */
    private Integer verifiedTokenCount;
    
    /**
     * Thời gian lấy data
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Data source (e.g. "Hyperion API")
     */
    private String dataSource;
    
    /**
     * Response status
     */
    private String status;
    
    /**
     * Error message (if any)
     */
    private String errorMessage;
    
    /**
     * Factory method để tạo successful response
     * 
     * @param walletId wallet ID
     * @param walletAddress EOS address
     * @param walletName wallet name
     * @param tokens list of tokens
     * @return WalletTokensResponse
     */
    public static WalletTokensResponse success(Long walletId, String walletAddress, 
                                             String walletName, List<TokenInfo> tokens) {
        
        // Calculate statistics
        int totalTypes = tokens != null ? tokens.size() : 0;
        int verifiedCount = tokens != null ? 
            (int) tokens.stream().filter(t -> Boolean.TRUE.equals(t.getIsVerified())).count() : 0;
        
        BigDecimal totalUsd = tokens != null ?
            tokens.stream()
                .filter(t -> t.getTotalUsdValue() != null)
                .map(TokenInfo::getTotalUsdValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        
        return WalletTokensResponse.builder()
                .walletId(walletId)
                .walletAddress(walletAddress)
                .walletName(walletName)
                .tokens(tokens)
                .totalTokenTypes(totalTypes)
                .totalUsdValue(totalUsd)
                .verifiedTokenCount(verifiedCount)
                .lastUpdated(LocalDateTime.now())
                .dataSource("Hyperion API")
                .status("SUCCESS")
                .build();
    }
    
    /**
     * Factory method để tạo error response
     * 
     * @param walletId wallet ID
     * @param walletAddress EOS address
     * @param errorMessage error message
     * @return WalletTokensResponse with error
     */
    public static WalletTokensResponse error(Long walletId, String walletAddress, String errorMessage) {
        return WalletTokensResponse.builder()
                .walletId(walletId)
                .walletAddress(walletAddress)
                .tokens(List.of()) // Empty list
                .totalTokenTypes(0)
                .totalUsdValue(BigDecimal.ZERO)
                .verifiedTokenCount(0)
                .lastUpdated(LocalDateTime.now())
                .dataSource("Hyperion API")
                .status("ERROR")
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Check if response có tokens
     * 
     * @return true if có ít nhất 1 token
     */
    public boolean hasTokens() {
        return tokens != null && !tokens.isEmpty();
    }
    
    /**
     * Check if response successful
     * 
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Get only tokens with positive balance
     * 
     * @return list of tokens with balance > 0
     */
    public List<TokenInfo> getTokensWithBalance() {
        if (tokens == null) {
            return List.of();
        }
        
        return tokens.stream()
                .filter(TokenInfo::hasBalance)
                .toList();
    }
    
    /**
     * Get only verified tokens
     * 
     * @return list of verified tokens
     */
    public List<TokenInfo> getVerifiedTokens() {
        if (tokens == null) {
            return List.of();
        }
        
        return tokens.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsVerified()))
                .toList();
    }
}