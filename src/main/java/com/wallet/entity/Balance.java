package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances",
       indexes = {
           @Index(name = "idx_balance_wallet", columnList = "wallet_id"),
           @Index(name = "idx_balance_token", columnList = "token_type"),
           @Index(name = "idx_balance_updated", columnList = "last_updated"),
           @Index(name = "idx_balance_status", columnList = "status")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_wallet_token", 
                           columnNames = {"wallet_id", "token_type", "token_contract", "token_symbol"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Balance extends BaseEntity {
    
    // ❌ REMOVED: @ManyToOne relationship with Wallet
    // ✅ THAY THẾ: Sử dụng simple Long reference
    @Column(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet ID cannot be null")
    private Long walletId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    @NotNull(message = "Token type cannot be null")
    private TokenType tokenType;
    
    @Column(name = "token_symbol", nullable = false, length = 20)
    @NotBlank(message = "Token symbol cannot be blank")
    @Size(max = 20, message = "Token symbol cannot exceed 20 characters")
    private String tokenSymbol;
    
    @Column(name = "token_contract", length = 256)
    @Size(max = 256, message = "Token contract cannot exceed 256 characters")
    private String tokenContract;
    
    @Column(name = "token_name", length = 100)
    @Size(max = 100, message = "Token name cannot exceed 100 characters")
    private String tokenName;
    
    @Column(name = "balance", precision = 30, scale = 8, nullable = false)
    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "available_balance", precision = 30, scale = 8, nullable = false)
    @NotNull(message = "Available balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Available balance cannot be negative")
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    @Column(name = "locked_balance", precision = 30, scale = 8, nullable = false)
    @NotNull(message = "Locked balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Locked balance cannot be negative")
    private BigDecimal lockedBalance = BigDecimal.ZERO;
    
    @Column(name = "staked_balance", precision = 30, scale = 8)
    @DecimalMin(value = "0.0", inclusive = true, message = "Staked balance cannot be negative")
    private BigDecimal stakedBalance = BigDecimal.ZERO;
    
    @Column(name = "usd_value", precision = 19, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "USD value cannot be negative")
    private BigDecimal usdValue = BigDecimal.ZERO;
    
    @Column(name = "token_price_usd", precision = 19, scale = 8)
    @DecimalMin(value = "0.0", inclusive = true, message = "Token price cannot be negative")
    private BigDecimal tokenPriceUsd = BigDecimal.ZERO;
    
    @Column(name = "price_change_24h", precision = 10, scale = 4)
    private BigDecimal priceChange24h = BigDecimal.ZERO;
    
    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated cannot be null")
    private LocalDateTime lastUpdated;
    
    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @NotNull(message = "Balance status cannot be null")
    private BalanceStatus status = BalanceStatus.ACTIVE;
    
    @Column(name = "decimal_places")
    @Min(value = 0, message = "Decimal places cannot be negative")
    @Max(value = 18, message = "Decimal places cannot exceed 18")
    private Integer decimalPlaces = 8;
    
    @Column(name = "is_native_token", nullable = false)
    private Boolean isNativeToken = false;
    
    @Column(name = "sync_block_number")
    private Long syncBlockNumber = 0L;
    
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
        validateBalanceConsistency();
    }
    
    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        lastUpdated = LocalDateTime.now();
        validateBalanceConsistency();
    }
    
    private void validateBalanceConsistency() {
        if (balance != null && availableBalance != null && lockedBalance != null) {
            BigDecimal calculatedTotal = availableBalance.add(lockedBalance);
            if (stakedBalance != null) {
                calculatedTotal = calculatedTotal.add(stakedBalance);
            }
            if (balance.compareTo(calculatedTotal) != 0) {
                throw new IllegalStateException("Balance consistency check failed: total balance does not match sum of available, locked, and staked balances");
            }
        }
    }
}