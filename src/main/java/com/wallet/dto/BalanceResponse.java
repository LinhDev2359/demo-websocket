package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance Response DTO
 * API response for EOS token balance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "EOS token balance information")
public class BalanceResponse {

    @Schema(description = "Balance ID", example = "1")
    private Long id;

    @Schema(description = "Wallet ID", example = "123")
    private Long walletId;

    @Schema(description = "Token type", example = "A")
    private String tokenType;

    @Schema(description = "Token symbol", example = "EOS")
    private String tokenSymbol;

    @Schema(description = "Token contract address", example = "eosio.token")
    private String tokenContract;

    @Schema(description = "Token display name", example = "EOS Native Token")
    private String tokenName;

    @Schema(description = "Total balance", example = "100.5000")
    private BigDecimal balance;

    @Schema(description = "Available balance for transactions", example = "85.5000")
    private BigDecimal availableBalance;

    @Schema(description = "Locked balance", example = "10.0000")
    private BigDecimal lockedBalance;

    @Schema(description = "Staked balance", example = "5.0000")
    private BigDecimal stakedBalance;

    @Schema(description = "USD value of total balance", example = "125.75")
    private BigDecimal usdValue;

    @Schema(description = "Current token price in USD", example = "1.25")
    private BigDecimal tokenPriceUsd;

    @Schema(description = "24h price change percentage", example = "2.50")
    private BigDecimal priceChange24h;

    @Schema(description = "Last updated timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;

    @Schema(description = "Last transaction timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTransactionAt;

    @Schema(description = "Balance status", example = "ACTIVE")
    private String status;

    @Schema(description = "Number of decimal places", example = "4")
    private Integer decimalPlaces;

    @Schema(description = "Indicates if balance is positive", example = "true")
    public boolean hasBalance() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
    }

    @Schema(description = "Indicates if balance can be used for transactions", example = "true")
    public boolean isAvailableForTransactions() {
        return availableBalance != null && availableBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    @Schema(description = "Percentage of balance that is available", example = "85.50")
    public BigDecimal getAvailablePercentage() {
        if (balance == null || balance.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            return BigDecimal.ZERO;
        }
        return availableBalance.divide(balance, 4, BigDecimal.ROUND_HALF_UP)
                              .multiply(BigDecimal.valueOf(100));
    }

    @Schema(description = "Formatted balance string", example = "100.5000 EOS")
    public String getFormattedBalance() {
        if (balance == null) {
            return "0.0000 " + (tokenSymbol != null ? tokenSymbol : "TOKEN");
        }
        return String.format("%.4f %s", balance, tokenSymbol != null ? tokenSymbol : "TOKEN");
    }

    @Schema(description = "Formatted USD value", example = "$125.75")
    public String getFormattedUsdValue() {
        if (usdValue == null) {
            return "$0.00";
        }
        return String.format("$%.2f", usdValue);
    }
}