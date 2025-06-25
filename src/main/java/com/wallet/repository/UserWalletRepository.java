package com.wallet.repository;

import com.wallet.entity.UserWallet;
import com.wallet.entity.UserWallet.UserWalletStatus;
import com.wallet.entity.WalletType;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserWallet Repository - Junction Table Repository
 * 
 * Thay thế cho @OneToMany relationship giữa User và Wallet
 * 
 * Ưu điểm:
 * 1. Explicit control over queries
 * 2. Better performance với large datasets
 * 3. Có thể thêm metadata về relationship
 * 4. Tránh N+1 query problems
 * 5. Easier testing và mocking
 */
@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    
    /**
     * Tìm tất cả wallets của user
     * THAY THẾ CHO: user.getWallets()
     */
    List<UserWallet> findByUserIdAndStatus(String userId, UserWallet.UserWalletStatus status);
    
    /**
     * Tìm active wallets của user với pagination
     * THAY THẾ CHO: user.getWallets() với pagination
     */
    Page<UserWallet> findByUserIdAndStatus(String userId, UserWallet.UserWalletStatus status, Pageable pageable);
    
    /**
     * Tìm primary wallet của user
     * THAY THẾ CHO: user.getWallets().stream().filter(w -> w.isPrimary())
     */
    Optional<UserWallet> findByUserIdAndIsPrimaryTrueAndStatus(String userId, UserWallet.UserWalletStatus status);
    
    /**
     * Count số wallets của user
     * THAY THẾ CHO: user.getWallets().size()
     */
    long countByUserIdAndStatus(String userId, UserWallet.UserWalletStatus status);
    
    /**
     * Tìm users có wallet specific
     * (Reverse lookup - điều mà @OneToMany không làm được dễ dàng)
     */
    List<UserWallet> findByWalletIdAndStatus(Long walletId, UserWallet.UserWalletStatus status);
    
    /**
     * Tìm wallets theo type
     */
    List<UserWallet> findByUserIdAndWalletTypeAndStatus(String userId, WalletType walletType, UserWallet.UserWalletStatus status);
    
    /**
     * Check xem user có access tới wallet không
     * (Security check - rất hữu ích cho authorization)
     */
    boolean existsByUserIdAndWalletIdAndStatus(String userId, Long walletId, UserWallet.UserWalletStatus status);
    
    /**
     * Tìm wallets được assign trong khoảng thời gian
     */
    List<UserWallet> findByUserIdAndAssignedAtBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Custom query để get wallet IDs của user
     * (Performance optimization - chỉ lấy IDs thay vì full objects)
     */
    @Query("SELECT uw.walletId FROM UserWallet uw WHERE uw.userId = :userId AND uw.status = :status")
    List<Long> findWalletIdsByUserId(@Param("userId") String userId, @Param("status") UserWallet.UserWalletStatus status);
    
    /**
     * Custom query để get user statistics
     */
    @Query("SELECT COUNT(uw), " +
           "SUM(CASE WHEN uw.isPrimary = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN uw.permissionLevel = 5 THEN 1 ELSE 0 END) " +
           "FROM UserWallet uw WHERE uw.userId = :userId AND uw.status = :status")
    Object[] getUserWalletStatistics(@Param("userId") String userId, @Param("status") UserWallet.UserWalletStatus status);
    
    /**
     * Bulk update status
     */
    @Modifying
    @Query("UPDATE UserWallet uw SET uw.status = :newStatus WHERE uw.userId = :userId AND uw.status = :currentStatus")
    int bulkUpdateUserWalletStatus(@Param("userId") String userId, 
                                  @Param("currentStatus") UserWallet.UserWalletStatus currentStatus,
                                  @Param("newStatus") UserWallet.UserWalletStatus newStatus);
    
    /**
     * Set primary wallet (unset others first)
     */
    @Modifying
    @Query("UPDATE UserWallet uw SET uw.isPrimary = false WHERE uw.userId = :userId AND uw.walletType = :walletType")
    int unsetPrimaryWallets(@Param("userId") String userId, @Param("walletType") WalletType walletType);
    
    /**
     * Find user wallet relation by user ID, wallet ID and status
     */
    Optional<UserWallet> findByUserIdAndWalletIdAndStatus(String userId, Long walletId, UserWalletStatus status);
    
    /**
     * Complex query với JOIN để get wallet details
     * (Thay thế cho lazy loading của @OneToMany)
     */
    @Query("SELECT uw, w FROM UserWallet uw JOIN Wallet w ON uw.walletId = w.id " +
           "WHERE uw.userId = :userId AND uw.status = :status " +
           "ORDER BY uw.isPrimary DESC, uw.assignedAt ASC")
    List<Object[]> findUserWalletsWithDetails(@Param("userId") String userId, @Param("status") UserWallet.UserWalletStatus status);
    
    /**
     * Find wallets with specific permission level
     */
    List<UserWallet> findByUserIdAndPermissionLevelGreaterThanEqualAndStatus(
        String userId, Integer permissionLevel, UserWallet.UserWalletStatus status);
    
    /**
     * Find recently assigned wallets
     */
    @Query("SELECT uw FROM UserWallet uw WHERE uw.assignedAt >= :since ORDER BY uw.assignedAt DESC")
    Page<UserWallet> findRecentlyAssignedWallets(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * Performance query - get essential data only
     */
    @Query("SELECT new map(uw.walletId as walletId, uw.isPrimary as isPrimary, uw.walletType as walletType) " +
           "FROM UserWallet uw WHERE uw.userId = :userId AND uw.status = :status")
    List<Map<String, Object>> findEssentialWalletData(@Param("userId") String userId, @Param("status") UserWallet.UserWalletStatus status);
}