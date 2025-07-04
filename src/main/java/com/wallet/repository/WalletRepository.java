package com.wallet.repository;

import com.wallet.entity.Wallet;
import com.wallet.entity.WalletStatus;
import com.wallet.entity.WalletType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Wallet Repository Interface
 * Data Access Layer cho Wallet entity
 * 
 * Chức năng:
 * 1. CRUD operations cho Wallet
 * 2. User-specific wallet operations
 * 3. Bulk operations cho performance
 * 4. Statistics và reporting queries
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    /**
     * Tìm wallet theo wallet address
     * 
     * @param walletAddress EOS wallet address
     * @return Optional Wallet
     */
    Optional<Wallet> findByWalletAddress(String walletAddress);
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use findByUserId(String userId, Pageable pageable) instead
     * 
     * Page<Wallet> findByUserUserId(String userId, Pageable pageable);
     */
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use findByUserIdAndStatus(String userId, WalletStatus status) instead
     * 
     * List<Wallet> findByUserUserIdAndStatus(String userId, WalletStatus status);
     */
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use UserWalletRepository.findByUserIdAndIsPrimaryTrueAndStatus() instead
     * 
     * Optional<Wallet> findByUserUserIdAndIsPrimaryTrue(String userId);
     */
    
    /**
     * Tìm wallets theo wallet type
     * 
     * @param walletType wallet type (EOS)
     * @param pageable pagination info
     * @return Page of Wallets
     */
    Page<Wallet> findByWalletType(WalletType walletType, Pageable pageable);
    
    /**
     * Check xem wallet address đã tồn tại chưa
     * 
     * @param walletAddress wallet address
     * @return true nếu tồn tại
     */
    boolean existsByWalletAddress(String walletAddress);
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use countByUserId(String userId) instead
     * 
     * long countByUserUserId(String userId);
     */
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use countByUserIdAndStatus(String userId, WalletStatus status) instead
     * 
     * long countByUserUserIdAndStatus(String userId, WalletStatus status);
     */
    
    /**
     * Tìm wallets được tạo trong khoảng thời gian
     * 
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination info
     * @return Page of Wallets
     */
    Page<Wallet> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Custom query để tìm wallets với balance > 0
     * ✅ FIXED: Use native SQL for Junction Tables (simpler approach)
     * 
     * @param userId user ID
     * @return List of Wallets có balance
     */
    @Query(value = "SELECT DISTINCT w.* FROM wallets w " +
                   "INNER JOIN wallet_balances wb ON w.id = wb.wallet_id " +
                   "INNER JOIN balances b ON wb.balance_id = b.id " +
                   "WHERE w.user_id = :userId AND w.status = 0 " +
                   "AND wb.status = 0 AND b.balance > 0",
           nativeQuery = true)
    List<Wallet> findWalletsWithBalance(@Param("userId") String userId);
    
    /**
     * Custom query để get wallet statistics
     * ✅ FIXED: Use simple userId field instead of entity relationship
     * 
     * @param userId user ID
     * @return wallet statistics as Object array
     */
    @Query("SELECT COUNT(w), " +
           "SUM(CASE WHEN w.status = com.wallet.entity.WalletStatus.ACTIVE THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN w.isPrimary = true THEN 1 ELSE 0 END) " +
           "FROM Wallet w WHERE w.userId = :userId")
    Object[] getWalletStatistics(@Param("userId") String userId);
    
    /**
     * Bulk update wallet status
     * Sử dụng cho batch operations
     * 
     * @param walletAddresses list of wallet addresses
     * @param status new status
     * @return number of updated records
     */
    @Query("UPDATE Wallet w SET w.status = :status, w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.walletAddress IN :walletAddresses")
    int bulkUpdateStatus(@Param("walletAddresses") List<String> walletAddresses, 
                        @Param("status") WalletStatus status);
    
    /**
     * Find wallets by user with ordering
     * Order by primary first, then by creation date
     * ✅ FIXED: Use simple userId field instead of entity relationship
     * 
     * @param userId user ID
     * @return List of Wallets ordered
     */
    @Query("SELECT w FROM Wallet w " +
           "WHERE w.userId = :userId AND w.status = com.wallet.entity.WalletStatus.ACTIVE " +
           "ORDER BY w.isPrimary DESC, w.createdAt ASC")
    List<Wallet> findByUserIdWithBalances(@Param("userId") String userId);
    
    /**
     * Count wallets by status globally
     * 
     * @param status wallet status
     * @return count of wallets
     */
    long countByStatus(WalletStatus status);
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use UserWalletRepository.countByUserIdAndIsPrimaryTrueAndStatus() instead
     * 
     * long countByUserUserIdAndIsPrimaryTrue(String userId);
     */
    
    /**
     * Count primary wallets globally
     * 
     * @return count of primary wallets
     */
    long countByIsPrimaryTrue();
    
    // ✅ NEW METHODS: Added to support refactored entity structure
    
    /**
     * Find wallets by userId (simple string field)
     */
    List<Wallet> findByUserId(String userId);
    
    /**
     * Find wallets by userId with pagination
     */
    Page<Wallet> findByUserId(String userId, Pageable pageable);
    
    /**
     * Find wallets by userId and status
     */
    List<Wallet> findByUserIdAndStatus(String userId, WalletStatus status);
    
    /**
     * Count wallets by userId
     */
    long countByUserId(String userId);
    
    /**
     * Count wallets by userId and status
     */
    long countByUserIdAndStatus(String userId, WalletStatus status);
    
    /**
     * Find wallets by status (for global queries)
     */
    List<Wallet> findByStatus(WalletStatus status);
    
    /**
     * Find all active wallets for balance sync
     * 
     * @param pageable pagination info
     * @return Page of active wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.status = com.wallet.entity.WalletStatus.ACTIVE ORDER BY w.updatedAt ASC")
    Page<Wallet> findAllActiveWallets(Pageable pageable);
    
    /**
     * Find primary wallet by userId, walletType and isPrimary flag
     * 
     * @param userId user ID
     * @param walletType wallet type
     * @param isPrimary primary flag
     * @return Optional primary wallet
     */
    Optional<Wallet> findByUserIdAndWalletTypeAndIsPrimary(String userId, WalletType walletType, Boolean isPrimary);
}