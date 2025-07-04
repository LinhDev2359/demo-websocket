package com.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Wallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "wallet_address", unique = true, nullable = false)
    private String walletAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type")
    private WalletType walletType = WalletType.EOS;
    
    @Column(name = "wallet_name", length = 100)
    private String walletName;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Enumerated(EnumType.STRING)
    private WalletStatus status = WalletStatus.ACTIVE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum WalletType {
        EOS
    }
    
    public enum WalletStatus {
        ACTIVE, INACTIVE, DELETED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}