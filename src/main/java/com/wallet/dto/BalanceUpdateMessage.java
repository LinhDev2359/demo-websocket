package com.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket message cho balance updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdateMessage {
    
    private String walletAddress;
    private String tokenType;
    private BigDecimal balance;
    private String rawBalance;
    private LocalDateTime timestamp;
    
    /**
     * Message type cho WebSocket routing
     */
    @Builder.Default
    private String messageType = "BALANCE_UPDATE";
}