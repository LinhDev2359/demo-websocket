package com.eoswallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WalletCreateRequest {
    
    @NotBlank(message = "Wallet name is required")
    @Size(min = 2, max = 50, message = "Wallet name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "EOS address is required")
    private String eosAddress;
}