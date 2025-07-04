package com.eoswallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    
    @NotNull(message = "From wallet ID is required")
    private Long fromWalletId;
    
    @NotNull(message = "To wallet ID is required")
    private Long toWalletId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}