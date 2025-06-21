package com.wallet.repository;

import com.wallet.entity.Balance;
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
    
    /**
     * Tìm balance theo wallet address và token type
     * 
     * @param walletAddress wallet address
     * @param tokenType token type (A, ram, rams, wram)
     * @return Optional Balance
     */
    Optional<Balance> findByWalletAddressAndTokenType(String walletAddress, Balance.TokenType tokenType);
    
    /**
     * Tìm tất cả balances của wallet
     * 
     * @param walletAddress wallet address
     * @return List of Balances
     */
    List<Balance> findByWalletAddress(String walletAddress);
    
    /**
     * Tìm balances theo token type
     * 
     * @param tokenType token type
     * @param pageable pagination info
     * @return Page of Balances
     */
    Page<Balance> findByTokenType(Balance.TokenType tokenType, Pageable pageable);
    
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
     * 
     * @param userId user ID
     * @return total USD value
     */
    @Query("SELECT COALESCE(SUM(b.usdValue), 0) FROM Balance b " +
           "JOIN Wallet w ON b.walletAddress = w.walletAddress " +
           "WHERE w.userId = :userId AND w.status = 'ACTIVE'")
    BigDecimal getTotalPortfolioValue(@Param("userId") String userId);
    
    /**
     * Custom query để get portfolio breakdown theo token type
     * 
     * @param userId user ID
     * @return portfolio breakdown
     */
    @Query("SELECT b.tokenType, SUM(b.balance), SUM(b.usdValue) FROM Balance b " +
           "JOIN Wallet w ON b.walletAddress = w.walletAddress " +
           "WHERE w.userId = :userId AND w.status = 'ACTIVE' AND b.balance > 0 " +
           "GROUP BY b.tokenType")
    List<Object[]> getPortfolioBreakdown(@Param("userId") String userId);
    
    /**
     * Custom query để get top holders của token type
     * 
     * @param tokenType token type
     * @param limit number of top holders
     * @return top holders
     */
    @Query("SELECT b.walletAddress, b.balance, b.usdValue FROM Balance b " +
           "WHERE b.tokenType = :tokenType AND b.balance > 0 " +
           "ORDER BY b.balance DESC")
    List<Object[]> getTopHolders(@Param("tokenType") Balance.TokenType tokenType, Pageable pageable);
    
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
           "WHERE b.walletAddress = :walletAddress AND b.tokenType = :tokenType")
    int updateBalance(@Param("walletAddress") String walletAddress,
                     @Param("tokenType") Balance.TokenType tokenType,
                     @Param("balance") BigDecimal balance,
                     @Param("usdValue") BigDecimal usdValue);
    
    /**
     * Batch insert hoặc update balances
     * Sử dụng UPSERT pattern cho performance
     * 
     * @param balances list of balance data
     */
    @Modifying
    @Query(value = "INSERT INTO balances (wallet_address, token_type, balance, usd_value, last_updated, version) " +
                   "VALUES (:#{#balance.walletAddress}, :#{#balance.tokenType}, :#{#balance.balance}, " +
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
    @Query("SELECT DISTINCT b.walletAddress FROM Balance b " +
           "WHERE b.lastUpdated < :lastSyncThreshold")
    Page<String> getWalletAddressesNeedingSync(@Param("lastSyncThreshold") LocalDateTime lastSyncThreshold, 
                                              Pageable pageable);
    
    /**
     * Get balance statistics cho monitoring
     * 
     * @return balance statistics
     */
    @Query("SELECT " +
           "COUNT(b) as totalBalances, " +
           "COUNT(DISTINCT b.walletAddress) as uniqueWallets, " +
           "SUM(CASE WHEN b.balance > 0 THEN 1 ELSE 0 END) as nonZeroBalances, " +
           "SUM(b.usdValue) as totalUsdValue " +
           "FROM Balance b")
    Object getBalanceStatistics();
}