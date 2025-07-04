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
@Table(name = "wallets",
       indexes = {
           @Index(name = "idx_wallet_user_id", columnList = "user_id"),
           @Index(name = "idx_wallet_address", columnList = "wallet_address"),
           @Index(name = "idx_wallet_type", columnList = "wallet_type"),
           @Index(name = "idx_wallet_status", columnList = "status"),
           @Index(name = "idx_wallet_primary", columnList = "is_primary")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_wallet_address", 
                           columnNames = {"wallet_address"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Wallet extends BaseEntity {

    // ❌ REMOVED: @ManyToOne relationship with User
    // ✅ THAY THẾ: Sử dụng simple String reference
    @Column(name = "user_id", nullable = false, length = 50)
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @Column(name = "wallet_address", unique = true, nullable = false, length = 256)
    @NotBlank(message = "Wallet address cannot be blank")
    @Size(max = 256, message = "Wallet address cannot exceed 256 characters")
    private String walletAddress;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "wallet_type", nullable = false)
    @NotNull(message = "Wallet type cannot be null")
    private WalletType walletType = WalletType.EOS;

    @Column(name = "wallet_name", length = 100)
    @Size(max = 100, message = "Wallet name cannot exceed 100 characters")
    private String walletName;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @NotNull(message = "Wallet status cannot be null")
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(name = "private_key_encrypted", columnDefinition = "TEXT")
    private String privateKeyEncrypted;

    @Column(name = "public_key", length = 512)
    @Size(max = 512, message = "Public key cannot exceed 512 characters")
    private String publicKey;

    @Column(name = "mnemonic_encrypted", columnDefinition = "TEXT")
    private String mnemonicEncrypted;

    @Column(name = "derivation_path", length = 100)
    @Size(max = 100, message = "Derivation path cannot exceed 100 characters")
    private String derivationPath;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_block_number")
    private Long syncBlockNumber = 0L;

    @Column(name = "total_balance", precision = 30, scale = 8)
    @DecimalMin(value = "0.0", inclusive = true, message = "Total balance cannot be negative")
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(name = "is_watch_only", nullable = false)
    private Boolean isWatchOnly = false;

    @Column(name = "security_level")
    @Min(value = 1, message = "Security level must be at least 1")
    @Max(value = 5, message = "Security level cannot exceed 5")
    private Integer securityLevel = 1;
    
    // ❌ REMOVED: @OneToMany relationship with Balance
    // ✅ THAY THẾ: Sử dụng WalletBalance junction table
    // 
    // Lý do loại bỏ @OneToMany:
    // 1. Tránh automatic loading của all balances
    // 2. Better performance cho portfolio queries
    // 3. Flexible hơn cho balance aggregation
    // 4. Dễ dàng implement balance history tracking
    // 
    // Cách sử dụng mới:
    // - Query balances thông qua WalletBalanceRepository
    // - Có thể cache balance summary riêng biệt
    // - Better control over which balances to load
}