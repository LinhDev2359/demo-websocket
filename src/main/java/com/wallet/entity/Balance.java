package com.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_address", "token_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Balance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;
    
    @Column(name = "balance", precision = 19, scale = 8, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "usd_value", precision = 19, scale = 2)
    private BigDecimal usdValue = BigDecimal.ZERO;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Version
    @Column(name = "version")
    private Long version = 0L;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_address", referencedColumnName = "wallet_address", insertable = false, updatable = false)
    private Wallet wallet;
    
    public enum TokenType {
        EOS,    // EOS native token
        RAM,    // EOS RAM
        CPU,    // EOS CPU
        NET     // EOS NET
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }
}