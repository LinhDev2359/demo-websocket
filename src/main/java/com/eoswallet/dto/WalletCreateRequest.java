package com.eoswallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for creating a new wallet
 * Contains validation rules for wallet creation
 * 
 * Business Rules:
 * 1. Wallet name: 2-100 characters, required
 * 2. EOS address: Required, valid EOS format (12 characters, a-z and 1-5)
 * 3. Optional description for wallet
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreateRequest {
    
    @NotBlank(message = "Wallet name is required")
    @Size(min = 2, max = 100, message = "Wallet name must be between 2 and 100 characters")
    private String name;
    
    @Pattern(regexp = "^[a-z1-5]{12}$", 
             message = "Invalid EOS address format. Must be 12 characters containing only a-z and 1-5")
    @NotBlank(message = "EOS address is required")
    private String eosAddress; // Required - user must provide their EOS wallet address
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    private Boolean isPrimary = false;
}