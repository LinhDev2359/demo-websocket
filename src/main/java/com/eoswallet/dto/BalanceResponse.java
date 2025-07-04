package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BalanceResponse {
    private Long balanceId;
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime lastUpdated;
}