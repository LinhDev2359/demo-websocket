package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Junction Table: User-Wallet Relationship
 * 
 * Giải thích:
 * - Thay thế @OneToMany relationship giữa User và Wallet
 * - Cho phép flexible querying và better performance
 * - Dễ dàng thêm metadata về relationship (như role, permissions)
 * 
 * Ưu điểm so với @OneToMany:
 * 1. Tránh N+1 query problem
 * 2. Có thể thêm thông tin về relationship
 * 3. Dễ dàng query independent
 * 4. Better control over loading strategy
 */
@Entity
@Table(name = "user_wallets",
       indexes = {
           @Index(name = "idx_user_wallet_user_id", columnList = "user_id"),
           @Index(name = "idx_user_wallet_wallet_id", columnList = "wallet_id"),
           @Index(name = "idx_user_wallet_status", columnList = "status"),
           @Index(name = "idx_user_wallet_primary", columnList = "is_primary"),
           @Index(name = "idx_user_wallet_created", columnList = "created_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_wallet", columnNames = {"user_id", "wallet_id"}),
           @UniqueConstraint(name = "uk_user_primary_wallet", columnNames = {"user_id", "is_primary", "wallet_type"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserWallet extends BaseEntity {
    
    /**
     * Reference tới User (không dùng @ManyToOne để tránh automatic loading)
     */
    @Column(name = "user_id", nullable = false, length = 50)
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    /**
     * Reference tới Wallet (không dùng @ManyToOne để tránh automatic loading)
     */
    @Column(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet ID cannot be null")
    private Long walletId;
    
    /**
     * Metadata về relationship
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "wallet_type", nullable = false)
    @NotNull(message = "Wallet type cannot be null")
    private WalletType walletType = WalletType.EOS;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status cannot be null")
    private UserWalletStatus status = UserWalletStatus.ACTIVE;
    
    /**
     * Permissions cho user trên wallet này
     */
    @Column(name = "permission_level")
    @Min(value = 1, message = "Permission level must be at least 1")
    @Max(value = 5, message = "Permission level cannot exceed 5")
    private Integer permissionLevel = 5; // 5 = full access, 1 = read only
    
    /**
     * Khi nào user được add vào wallet này
     */
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();
    
    /**
     * Ai đã assign user này vào wallet (cho audit trail)
     */
    @Column(name = "assigned_by", length = 50)
    private String assignedBy;
    
    /**
     * Notes về việc assign này
     */
    @Column(name = "notes", length = 500)
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    // Enum cho UserWallet status - Ordered to match database ENUM values
    public enum UserWalletStatus {
        ACTIVE,             // Index 0 = DB ENUM 'ACTIVE' (1st position)
        INACTIVE,           // Index 1 = DB ENUM 'INACTIVE' (2nd position)
        SUSPENDED,          // Index 2 = DB ENUM 'SUSPENDED' (3rd position)
        PENDING_APPROVAL    // Index 3 = DB ENUM 'PENDING_APPROVAL' (4th position)
    }
    
    /**
     * Constructor cho việc tạo relationship đơn giản
     */
    public UserWallet(String userId, Long walletId, WalletType walletType) {
        this.userId = userId;
        this.walletId = walletId;
        this.walletType = walletType;
        this.assignedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor cho primary wallet
     */
    public UserWallet(String userId, Long walletId, WalletType walletType, boolean isPrimary) {
        this(userId, walletId, walletType);
        this.isPrimary = isPrimary;
    }
}