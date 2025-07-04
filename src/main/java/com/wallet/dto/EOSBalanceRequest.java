package com.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for EOS blockchain balance request
 * Sử dụng để gửi request tới EOS API lấy balance của wallet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EOSBalanceRequest {
    
    /**
     * EOS wallet address - phải đúng format EOS
     * VD: "eosio.token", "mywalletname"
     */
    @NotBlank(message = "Wallet address không được để trống")
    @Pattern(regexp = "^[a-z1-5.]{1,12}$", message = "EOS wallet address không đúng format")
    private String walletAddress;
    
    /**
     * Token contract address - mặc định là "eosio.token" cho EOS token
     */
    @Builder.Default
    private String tokenContract = "eosio.token";
    
    /**
     * Token symbol - mặc định là "EOS"
     */
    @Builder.Default
    private String tokenSymbol = "EOS";
    
    /**
     * Tạo request cho EOS token
     */
    public static EOSBalanceRequest forEOSToken(String walletAddress) {
        return EOSBalanceRequest.builder()
                .walletAddress(walletAddress)
                .tokenContract("eosio.token")
                .tokenSymbol("EOS")
                .build();
    }
    
    /**
     * Tạo request cho RAM token
     */
    public static EOSBalanceRequest forRAMToken(String walletAddress) {
        return EOSBalanceRequest.builder()
                .walletAddress(walletAddress)
                .tokenContract("eosio")
                .tokenSymbol("RAM")
                .build();
    }
}