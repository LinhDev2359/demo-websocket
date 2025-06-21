package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WalletResponse {
    private Long walletId;
    private String name;
    private String eosAddress;
    private Boolean isPrimary;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}