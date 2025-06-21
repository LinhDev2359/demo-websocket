package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletCreateResponse {
    private Long walletId;
    private String name;
    private String eosAddress;
    private Boolean isPrimary;
    private String message;
}