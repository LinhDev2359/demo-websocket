package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for wallet creation
 * Contains created wallet information and success message
 * 
 * Includes:
 * 1. Created wallet details
 * 2. Success message
 * 3. Creation timestamp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreateResponse {
    private Long walletId;
    private String name;
    private String eosAddress;
    private String description;
    private Boolean isPrimary;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}