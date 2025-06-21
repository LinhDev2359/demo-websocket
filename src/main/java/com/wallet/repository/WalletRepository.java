package com.wallet.repository;

import com.wallet.entity.Wallet;
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
    
    /**
     * Tìm tất cả wallets của user với pagination
     * 
     * @param userId user ID
     * @param pageable pagination info
     * @return Page of Wallets
     */
    Page<Wallet> findByUserId(String userId, Pageable pageable);
    
    /**
     * Tìm active wallets của user
     * 
     * @param userId user ID
     * @return List of active Wallets
     */
    List<Wallet> findByUserIdAndStatus(String userId, Wallet.WalletStatus status);
    
    /**
     * Tìm primary wallet của user
     * 
     * @param userId user ID
     * @return Optional primary Wallet
     */
    Optional<Wallet> findByUserIdAndIsPrimaryTrue(String userId);
    
    /**
     * Tìm wallets theo wallet type
     * 
     * @param walletType wallet type (EOS)
     * @param pageable pagination info
     * @return Page of Wallets
     */
    Page<Wallet> findByWalletType(Wallet.WalletType walletType, Pageable pageable);
    
    /**
     * Check xem wallet address đã tồn tại chưa
     * 
     * @param walletAddress wallet address
     * @return true nếu tồn tại
     */
    boolean existsByWalletAddress(String walletAddress);
    
    /**
     * Count số wallets của user
     * 
     * @param userId user ID
     * @return số lượng wallets
     */
    long countByUserId(String userId);
    
    /**
     * Count active wallets của user
     * 
     * @param userId user ID
     * @param status wallet status
     * @return số lượng active wallets
     */
    long countByUserIdAndStatus(String userId, Wallet.WalletStatus status);
    
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
     * Join với Balance table
     * 
     * @param userId user ID
     * @return List of Wallets có balance
     */
    @Query("SELECT DISTINCT w FROM Wallet w JOIN Balance b ON w.walletAddress = b.walletAddress " +
           "WHERE w.userId = :userId AND w.status = 'ACTIVE' AND b.balance > 0")
    List<Wallet> findWalletsWithBalance(@Param("userId") String userId);
    
    /**
     * Custom query để get wallet statistics
     * 
     * @param userId user ID
     * @return wallet statistics as Object array
     */
    @Query("SELECT COUNT(w), " +
           "SUM(CASE WHEN w.status = 'ACTIVE' THEN 1 ELSE 0 END), " +
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
                        @Param("status") Wallet.WalletStatus status);
    
    /**
     * Find wallets by user with ordering
     * Order by primary first, then by creation date
     * 
     * @param userId user ID
     * @return List of Wallets ordered
     */
    @Query("SELECT w FROM Wallet w " +
           "WHERE w.userId = :userId AND w.status = 'ACTIVE' " +
           "ORDER BY w.isPrimary DESC, w.createdAt ASC")
    List<Wallet> findByUserIdWithBalances(@Param("userId") String userId);
    
    /**
     * Count wallets by status globally
     * 
     * @param status wallet status
     * @return count of wallets
     */
    long countByStatus(Wallet.WalletStatus status);
    
    /**
     * Count primary wallets for user
     * 
     * @param userId user ID
     * @return count of primary wallets
     */
    long countByUserIdAndIsPrimaryTrue(String userId);
    
    /**
     * Count primary wallets globally
     * 
     * @return count of primary wallets
     */
    long countByIsPrimaryTrue();
}