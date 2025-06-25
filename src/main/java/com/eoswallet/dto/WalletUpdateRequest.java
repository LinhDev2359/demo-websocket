package com.eoswallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for updating wallet information
 * Contains validation rules for wallet updates
 * 
 * Business Rules:
 * 1. Wallet name: 2-100 characters, required
 * 2. Optional description update
 * 3. Status update (active/inactive)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateRequest {
    
    @NotBlank(message = "Wallet name is required")
    @Size(min = 2, max = 100, message = "Wallet name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    private Boolean isActive;
}