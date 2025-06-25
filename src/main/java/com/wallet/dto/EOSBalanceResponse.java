package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for EOS blockchain balance response
 * Nhận response từ EOS API get_currency_balance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EOSBalanceResponse {
    
    /**
     * Wallet address được query
     */
    private String walletAddress;
    
    /**
     * Token contract address
     */
    private String tokenContract;
    
    /**
     * Token symbol
     */
    private String tokenSymbol;
    
    /**
     * Balance amount - parsing từ string "10.0000 EOS" thành BigDecimal
     */
    private BigDecimal balance;
    
    /**
     * Raw balance string từ EOS API
     * VD: "10.0000 EOS"
     */
    private String rawBalance;
    
    /**
     * Timestamp khi query balance
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Có thành công hay không
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * Error message nếu có lỗi
     */
    private String errorMessage;
    
    /**
     * Response time trong milliseconds
     */
    private Long responseTimeMs;
    
    /**
     * Raw response từ EOS API (cho debugging)
     */
    @JsonProperty("raw_response")
    private List<String> rawResponse;
    
    /**
     * Tạo response thành công
     */
    public static EOSBalanceResponse success(String walletAddress, String tokenContract, 
                                           String tokenSymbol, String rawBalance, 
                                           Long responseTimeMs) {
        BigDecimal balance = parseBalance(rawBalance);
        
        return EOSBalanceResponse.builder()
                .walletAddress(walletAddress)
                .tokenContract(tokenContract)
                .tokenSymbol(tokenSymbol)
                .balance(balance)
                .rawBalance(rawBalance)
                .success(true)
                .responseTimeMs(responseTimeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Tạo response lỗi
     */
    public static EOSBalanceResponse error(String walletAddress, String errorMessage, 
                                         Long responseTimeMs) {
        return EOSBalanceResponse.builder()
                .walletAddress(walletAddress)
                .success(false)
                .errorMessage(errorMessage)
                .balance(BigDecimal.ZERO)
                .responseTimeMs(responseTimeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Parse balance từ string "10.0000 EOS" thành BigDecimal
     * Improved error handling and validation
     * Supports both "10.0000 EOS" and "10.0000" formats
     */
    private static BigDecimal parseBalance(String rawBalance) {
        if (rawBalance == null || rawBalance.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            String cleanBalance = rawBalance.trim();
            
            // Case 1: "10.0000 EOS" format - tách số từ string
            if (cleanBalance.contains(" ")) {
                String[] parts = cleanBalance.split("\\s+");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    cleanBalance = parts[0];
                }
            }
            
            // Case 2: "10.0000" format - validate và parse
            if (cleanBalance.matches("^-?\\d+(\\.\\d+)?$")) {
                return new BigDecimal(cleanBalance);
            } else {
                System.err.println("Invalid balance format: " + cleanBalance + " (from raw: " + rawBalance + ")");
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Cannot parse balance: " + rawBalance + ", error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error parsing balance: " + rawBalance + ", error: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Check xem balance có > 0 không
     */
    public boolean hasBalance() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
    }
}