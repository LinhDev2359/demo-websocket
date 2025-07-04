package com.wallet.repository;

import com.wallet.entity.Balance;
import com.wallet.entity.TokenType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Balance Repository Interface
 * Data Access Layer cho Balance entity
 * 
 * Chức năng:
 * 1. CRUD operations cho Balance
 * 2. Bulk balance updates cho sync operations
 * 3. Portfolio aggregation queries
 * 4. Performance-optimized balance retrieval
 */
@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use WalletBalanceRepository with Junction Tables instead
     * 
     * Optional<Balance> findByWalletWalletAddressAndTokenType(String walletAddress, TokenType tokenType);
     */
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use findByWalletId(Long walletId) instead
     * 
     * List<Balance> findByWalletWalletAddress(String walletAddress);
     */
    
    /**
     * Tìm balances theo token type
     * 
     * @param tokenType token type
     * @param pageable pagination info
     * @return Page of Balances
     */
    Page<Balance> findByTokenType(TokenType tokenType, Pageable pageable);
    
    /**
     * Tìm balances có giá trị > threshold
     * 
     * @param threshold minimum balance
     * @param pageable pagination info
     * @return Page of Balances
     */
    Page<Balance> findByBalanceGreaterThan(BigDecimal threshold, Pageable pageable);
    
    /**
     * Tìm balances được update trong khoảng thời gian
     * 
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination info
     * @return Page of Balances
     */
    Page<Balance> findByLastUpdatedBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Custom query để get total portfolio value của user
     * ✅ FIXED: Use native SQL for Junction Tables
     * 
     * @param userId user ID
     * @return total USD value
     */
    @Query(value = "SELECT COALESCE(SUM(b.usd_value), 0) FROM balances b " +
                   "INNER JOIN wallet_balances wb ON b.id = wb.balance_id " +
                   "INNER JOIN wallets w ON wb.wallet_id = w.id " +
                   "WHERE w.user_id = :userId AND w.status = 0 " +
                   "AND wb.status = 0",
           nativeQuery = true)
    BigDecimal getTotalPortfolioValue(@Param("userId") String userId);
    
    /**
     * Custom query để get portfolio breakdown theo token type
     * ✅ FIXED: Use native SQL for Junction Tables
     * 
     * @param userId user ID
     * @return portfolio breakdown
     */
    @Query(value = "SELECT b.token_type, SUM(b.balance), SUM(b.usd_value) FROM balances b " +
                   "INNER JOIN wallet_balances wb ON b.id = wb.balance_id " +
                   "INNER JOIN wallets w ON wb.wallet_id = w.id " +
                   "WHERE w.user_id = :userId AND w.status = 0 " +
                   "AND wb.status = 0 AND b.balance > 0 " +
                   "GROUP BY b.token_type",
           nativeQuery = true)
    List<Object[]> getPortfolioBreakdown(@Param("userId") String userId);
    
    /**
     * Custom query để get top holders của token type
     * ✅ FIXED: Use native SQL for Junction Tables
     * 
     * @param tokenType token type
     * @param limit number of top holders
     * @return top holders
     */
    @Query(value = "SELECT w.wallet_address, b.balance, b.usd_value FROM balances b " +
                   "INNER JOIN wallet_balances wb ON b.id = wb.balance_id " +
                   "INNER JOIN wallets w ON wb.wallet_id = w.id " +
                   "WHERE b.token_type = :tokenType AND b.balance > 0 " +
                   "AND wb.status = 0 " +
                   "ORDER BY b.balance DESC",
           nativeQuery = true)
    List<Object[]> getTopHolders(@Param("tokenType") TokenType tokenType, Pageable pageable);
    
    /**
     * Bulk update balances
     * Sử dụng cho sync operations từ EOS blockchain
     * 
     * @param walletAddress wallet address
     * @param tokenType token type
     * @param balance new balance
     * @param usdValue USD value
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Balance b SET b.balance = :balance, b.usdValue = :usdValue, " +
           "b.lastUpdated = CURRENT_TIMESTAMP, b.version = b.version + 1 " +
           "WHERE b.walletId = (SELECT w.id FROM Wallet w WHERE w.walletAddress = :walletAddress) " +
           "AND b.tokenType = :tokenType")
    int updateBalance(@Param("walletAddress") String walletAddress,
                     @Param("tokenType") TokenType tokenType,
                     @Param("balance") BigDecimal balance,
                     @Param("usdValue") BigDecimal usdValue);
    
    /**
     * Batch insert hoặc update balances
     * Sử dụng UPSERT pattern cho performance
     * 
     * @param balances list of balance data
     */
    @Modifying
    @Query(value = "INSERT INTO balances (wallet_id, token_type, balance, usd_value, last_updated, version) " +
                   "VALUES (:#{#balance.walletId}, :#{#balance.tokenType}, :#{#balance.balance}, " +
                   ":#{#balance.usdValue}, CURRENT_TIMESTAMP, 0) " +
                   "ON DUPLICATE KEY UPDATE " +
                   "balance = VALUES(balance), usd_value = VALUES(usd_value), " +
                   "last_updated = CURRENT_TIMESTAMP, version = version + 1",
           nativeQuery = true)
    void upsertBalance(@Param("balance") Balance balance);
    
    /**
     * Delete old balances với balance = 0
     * Cleanup task cho database maintenance
     * 
     * @param beforeDate date threshold
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Balance b WHERE b.balance = 0 AND b.lastUpdated < :beforeDate")
    int deleteZeroBalances(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Get balances cần sync (outdated)
     * 
     * @param lastSyncThreshold time threshold
     * @param pageable pagination info
     * @return balances cần sync
     */
    @Query(value = "SELECT DISTINCT w.wallet_address FROM balances b " +
                   "INNER JOIN wallets w ON b.wallet_id = w.id " +
                   "WHERE b.last_updated < :lastSyncThreshold",
           nativeQuery = true)
    Page<String> getWalletAddressesNeedingSync(@Param("lastSyncThreshold") LocalDateTime lastSyncThreshold, 
                                              Pageable pageable);
    
    /*
     * ❌ REMOVED: Old entity relationship method
     * Use findByWalletIdIn(List<Long> walletIds) instead
     * 
     * List<Balance> findByWalletWalletAddressIn(List<String> walletAddresses);
     */
    
    /**
     * Get balance statistics cho monitoring
     * 
     * @return balance statistics
     */
    @Query("SELECT " +
           "COUNT(b) as totalBalances, " +
           "COUNT(DISTINCT b.walletId) as uniqueWallets, " +
           "SUM(CASE WHEN b.balance > 0 THEN 1 ELSE 0 END) as nonZeroBalances, " +
           "SUM(COALESCE(b.usdValue, 0)) as totalUsdValue " +
           "FROM Balance b")
    Object getBalanceStatistics();
    
    // ✅ NEW METHODS: Added to support refactored entity structure
    
    /**
     * Find balances by walletId (simple Long field)
     */
    List<Balance> findByWalletId(Long walletId);
    
    /**
     * Find balances by multiple walletIds
     */
    List<Balance> findByWalletIdIn(List<Long> walletIds);
    
    /**
     * Find balance by wallet ID and token type
     * Used for balance sync operations
     * 
     * @param walletId wallet ID
     * @param tokenType token type
     * @return Optional Balance
     */
    Optional<Balance> findByWalletIdAndTokenType(Long walletId, TokenType tokenType);
}