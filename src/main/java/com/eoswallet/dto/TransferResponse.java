package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferResponse {
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String status;
    private String message;
}