package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Junction Table: Wallet-Balance Relationship
 * 
 * Giải thích:
 * - Thay thế @OneToMany relationship giữa Wallet và Balance
 * - Cho phép một wallet có nhiều loại balance
 * - Dễ dàng track history và changes
 * 
 * Ưu điểm so với @OneToMany:
 * 1. Có thể thêm metadata về balance relationship
 * 2. Dễ dàng implement balance history
 * 3. Better performance cho complex queries
 * 4. Flexible cho future requirements
 */
@Entity
@Table(name = "wallet_balances",
       indexes = {
           @Index(name = "idx_wallet_balance_wallet_id", columnList = "wallet_id"),
           @Index(name = "idx_wallet_balance_balance_id", columnList = "balance_id"),
           @Index(name = "idx_wallet_balance_token", columnList = "token_type"),
           @Index(name = "idx_wallet_balance_status", columnList = "status"),
           @Index(name = "idx_wallet_balance_updated", columnList = "last_synced_at"),
           @Index(name = "idx_wallet_balance_amount", columnList = "current_amount")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_wallet_balance", columnNames = {"wallet_id", "balance_id"}),
           @UniqueConstraint(name = "uk_wallet_token_type", columnNames = {"wallet_id", "token_type", "token_contract"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WalletBalance extends BaseEntity {
    
    /**
     * Reference tới Wallet (không dùng @ManyToOne)
     */
    @Column(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet ID cannot be null")
    private Long walletId;
    
    /**
     * Reference tới Balance (không dùng @ManyToOne)
     */
    @Column(name = "balance_id", nullable = false)
    @NotNull(message = "Balance ID cannot be null")
    private Long balanceId;
    
    /**
     * Cached information từ Balance entity để tránh JOIN
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "token_type", nullable = false)
    @NotNull(message = "Token type cannot be null")
    private TokenType tokenType;
    
    @Column(name = "token_symbol", nullable = false, length = 20)
    @NotBlank(message = "Token symbol cannot be blank")
    private String tokenSymbol;
    
    @Column(name = "token_contract", length = 256)
    private String tokenContract;
    
    /**
     * Current amount (cached từ Balance để performance)
     */
    @Column(name = "current_amount", precision = 30, scale = 8, nullable = false)
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount cannot be negative")
    private BigDecimal currentAmount = BigDecimal.ZERO;
    
    /**
     * Previous amount (để track changes)
     */
    @Column(name = "previous_amount", precision = 30, scale = 8)
    private BigDecimal previousAmount = BigDecimal.ZERO;
    
    /**
     * USD value (cached để performance)
     */
    @Column(name = "usd_value", precision = 20, scale = 8)
    @DecimalMin(value = "0.0", inclusive = true, message = "USD value cannot be negative")
    private BigDecimal usdValue = BigDecimal.ZERO;
    
    /**
     * Metadata về relationship
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private WalletBalanceStatus status = WalletBalanceStatus.ACTIVE;
    
    /**
     * Khi nào balance này được add vào wallet
     */
    @Column(name = "first_detected_at", nullable = false)
    private LocalDateTime firstDetectedAt = LocalDateTime.now();
    
    /**
     * Lần cuối sync với blockchain
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    /**
     * Sync status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "sync_status")
    private SyncStatus syncStatus = SyncStatus.PENDING;
    
    /**
     * Có phải là balance chính của wallet không
     */
    @Column(name = "is_primary_balance", nullable = false)
    private Boolean isPrimaryBalance = false;
    
    /**
     * Minimum threshold để show balance này
     */
    @Column(name = "display_threshold", precision = 30, scale = 8)
    private BigDecimal displayThreshold = BigDecimal.ZERO;
    
    /**
     * Notes về balance này
     */
    @Column(name = "notes", length = 500)
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    // Enums - Ordered to match database ENUM values
    public enum WalletBalanceStatus {
        ACTIVE,         // Index 0 = DB ENUM 'ACTIVE' (1st position)
        INACTIVE,       // Index 1 = DB ENUM 'INACTIVE' (2nd position)
        HIDDEN,         // Index 2 = DB ENUM 'HIDDEN' (3rd position)
        FROZEN          // Index 3 = DB ENUM 'FROZEN' (4th position)
    }
    
    public enum SyncStatus {
        PENDING,        // Index 0 = DB ENUM 'PENDING' (1st position)
        SYNCED,         // Index 1 = DB ENUM 'SYNCED' (2nd position)
        FAILED,         // Index 2 = DB ENUM 'FAILED' (3rd position)
        STALE           // Index 3 = DB ENUM 'STALE' (4th position)
    }
    
    /**
     * Constructor đơn giản
     */
    public WalletBalance(Long walletId, Long balanceId, TokenType tokenType, String tokenSymbol) {
        this.walletId = walletId;
        this.balanceId = balanceId;
        this.tokenType = tokenType;
        this.tokenSymbol = tokenSymbol;
        this.firstDetectedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor với amount
     */
    public WalletBalance(Long walletId, Long balanceId, TokenType tokenType, 
                        String tokenSymbol, BigDecimal currentAmount) {
        this(walletId, balanceId, tokenType, tokenSymbol);
        this.currentAmount = currentAmount;
    }
    
    /**
     * Helper method để check nếu balance đã thay đổi
     */
    public boolean hasBalanceChanged() {
        if (previousAmount == null) return true;
        return currentAmount.compareTo(previousAmount) != 0;
    }
    
    /**
     * Helper method để calculate change percentage
     */
    public BigDecimal getChangePercentage() {
        if (previousAmount == null || previousAmount.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal change = currentAmount.subtract(previousAmount);
        return change.divide(previousAmount, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
    }
}