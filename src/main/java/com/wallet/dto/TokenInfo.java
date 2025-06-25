package com.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Internal DTO cho Token Information
 * 
 * Đại diện cho token data sau khi process từ Hyperion API
 * Được sử dụng trong application internal và response cho client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {
    
    /**
     * Token symbol (e.g. "EOS", "USDT", "USDC")
     */
    private String symbol;
    
    /**
     * Token full name (e.g. "EOS Native Token", "Tether USD")
     */
    private String name;
    
    /**
     * Smart contract address
     */
    private String contract;
    
    /**
     * Token balance as BigDecimal
     */
    private BigDecimal balance;
    
    /**
     * Token precision (decimal places)
     */
    private Integer precision;
    
    /**
     * USD value per token (if available)
     */
    private BigDecimal usdPrice;
    
    /**
     * Total USD value of holdings (balance * usdPrice)
     */
    private BigDecimal totalUsdValue;
    
    /**
     * Token logo URL (if available)
     */
    private String logoUrl;
    
    /**
     * Whether this is a verified/trusted token
     */
    private Boolean isVerified;
    
    /**
     * Whether this is the native EOS token
     */
    private Boolean isNative;
    
    /**
     * Last time this token data was updated
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Factory method để tạo TokenInfo từ HyperionTokenResponse
     * 
     * @param hyperionResponse response từ Hyperion API
     * @return TokenInfo instance
     */
    public static TokenInfo fromHyperionResponse(HyperionTokenResponse hyperionResponse) {
        if (hyperionResponse == null) {
            return null;
        }
        
        return TokenInfo.builder()
                .symbol(hyperionResponse.getSymbol())
                .contract(hyperionResponse.getContract())
                .balance(hyperionResponse.getAmountAsBigDecimal())
                .isNative(hyperionResponse.isNativeEOS())
                .isVerified(isVerifiedToken(hyperionResponse.getSymbol(), hyperionResponse.getContract()))
                .name(getTokenName(hyperionResponse.getSymbol(), hyperionResponse.getContract()))
                .precision(getTokenPrecision(hyperionResponse.getSymbol()))
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    /**
     * Helper method để xác định token có verified không
     * 
     * @param symbol token symbol
     * @param contract contract address
     * @return true if token is verified
     */
    private static Boolean isVerifiedToken(String symbol, String contract) {
        // Danh sách các token được verify
        if ("EOS".equalsIgnoreCase(symbol) && "eosio.token".equalsIgnoreCase(contract)) {
            return true;
        }
        if ("USDT".equalsIgnoreCase(symbol) && "tethertether".equalsIgnoreCase(contract)) {
            return true;
        }
        if ("USDC".equalsIgnoreCase(symbol) && "6gbmrlzvpkm".equalsIgnoreCase(contract)) {
            return true;
        }
        
        // Default: unverified
        return false;
    }
    
    /**
     * Helper method để lấy token name
     * 
     * @param symbol token symbol
     * @param contract contract address
     * @return token full name
     */
    private static String getTokenName(String symbol, String contract) {
        // Mapping common tokens
        if ("EOS".equalsIgnoreCase(symbol) && "eosio.token".equalsIgnoreCase(contract)) {
            return "EOS Native Token";
        }
        if ("USDT".equalsIgnoreCase(symbol) && "tethertether".equalsIgnoreCase(contract)) {
            return "Tether USD";
        }
        if ("USDC".equalsIgnoreCase(symbol) && "6gbmrlzvpkm".equalsIgnoreCase(contract)) {
            return "USD Coin";
        }
        
        // Default: use symbol
        return symbol;
    }
    
    /**
     * Helper method để lấy token precision
     * 
     * @param symbol token symbol
     * @return decimal precision
     */
    private static Integer getTokenPrecision(String symbol) {
        // Common precisions
        if ("EOS".equalsIgnoreCase(symbol)) {
            return 4;
        }
        if ("USDT".equalsIgnoreCase(symbol) || "USDC".equalsIgnoreCase(symbol)) {
            return 4;
        }
        
        // Default precision
        return 4;
    }
    
    /**
     * Check if token has positive balance
     * 
     * @return true if balance > 0
     */
    public boolean hasBalance() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted display string
     * 
     * @return formatted string like "123.4567 EOS"
     */
    public String getDisplayBalance() {
        if (balance == null) {
            return "0 " + symbol;
        }
        
        return String.format("%.4f %s", balance, symbol);
    }
}