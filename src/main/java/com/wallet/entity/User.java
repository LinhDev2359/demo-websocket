package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", 
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_email", columnList = "email"),
           @Index(name = "idx_username", columnList = "username"),
           @Index(name = "idx_status", columnList = "status")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 50, message = "User ID cannot exceed 50 characters")
    private String userId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.@-]+$", message = "Username can contain letters, numbers, dots, underscores, @ and hyphens")
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Column(name = "password_hash", nullable = false)
    @NotBlank(message = "Password hash cannot be blank")
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @NotNull(message = "User status cannot be null")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "phone_number", length = 20)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Column(name = "country_code", length = 3)
    @Size(max = 3, message = "Country code cannot exceed 3 characters")
    private String countryCode;

    @Column(name = "kyc_level")
    @Min(value = 0, message = "KYC level cannot be negative")
    @Max(value = 3, message = "KYC level cannot exceed 3")
    private Integer kycLevel = 0;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;
    
    // ❌ REMOVED: @OneToMany relationship with Wallet
    // ✅ THAY THẾ: Sử dụng UserWallet junction table
    // 
    // Lý do loại bỏ @OneToMany:
    // 1. Tránh N+1 query problems
    // 2. Better performance cho large datasets
    // 3. Flexible hơn cho complex relationships
    // 4. Easier testing và mocking
    // 
    // Cách sử dụng mới:
    // - Query wallets thông qua UserWalletRepository
    // - Sử dụng explicit joins khi cần
    // - Better control over loading strategy
}