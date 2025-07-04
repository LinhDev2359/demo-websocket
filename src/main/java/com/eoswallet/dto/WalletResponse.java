package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for wallet information
 * Contains all wallet details for API responses
 * 
 * Includes:
 * 1. Basic wallet information
 * 2. Status and metadata
 * 3. Balance information
 * 4. Timestamps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long walletId;
    private String name;
    private String eosAddress;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private String status;
    private BigDecimal totalBalance;
    private String walletType;
    private Integer securityLevel;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}