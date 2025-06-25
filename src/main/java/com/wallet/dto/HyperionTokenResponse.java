package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO cho Hyperion API Token Response
 * 
 * Mapping cho response từ: GET /v2/state/get_tokens?account={account}
 * 
 * Example Response:
 * [
 *   {
 *     "symbol": "EOS",
 *     "contract": "eosio.token", 
 *     "amount": "123.4567"
 *   }
 * ]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HyperionTokenResponse {
    
    /**
     * Token symbol (e.g. "EOS", "USDT", "USDC")
     */
    @JsonProperty("symbol")
    private String symbol;
    
    /**
     * Smart contract address (e.g. "eosio.token", "tethertether")  
     */
    @JsonProperty("contract")
    private String contract;
    
    /**
     * Token amount as string (e.g. "123.4567")
     * Raw string from API để preserve precision
     */
    @JsonProperty("amount")
    private String amount;
    
    /**
     * Helper method để convert amount sang BigDecimal
     * 
     * @return BigDecimal representation of amount
     */
    public BigDecimal getAmountAsBigDecimal() {
        if (amount == null || amount.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            return new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Check if this is a native EOS token
     * 
     * @return true if symbol is EOS and contract is eosio.token
     */
    public boolean isNativeEOS() {
        return "EOS".equalsIgnoreCase(symbol) && "eosio.token".equalsIgnoreCase(contract);
    }
    
    /**
     * Check if amount is greater than zero
     * 
     * @return true if token has positive balance
     */
    public boolean hasBalance() {
        return getAmountAsBigDecimal().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted display string for amount
     * 
     * @return formatted string like "123.4567 EOS"
     */
    public String getDisplayAmount() {
        return amount + " " + symbol;
    }
}