package com.wallet.repository;

import com.wallet.entity.WalletBalance;
import com.wallet.entity.TokenType;
import com.wallet.entity.WalletBalance.WalletBalanceStatus;
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
import java.util.Map;
import java.util.Optional;

/**
 * WalletBalance Repository - Junction Table Repository
 * 
 * Thay thế cho @OneToMany relationship giữa Wallet và Balance
 * 
 * Ưu điểm:
 * 1. Explicit control over balance queries
 * 2. Better performance cho portfolio aggregation
 * 3. Có thể cache balance data riêng biệt
 * 4. Dễ dàng implement balance history
 * 5. Flexible cho complex balance operations
 */
@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {
    
    /**
     * Tìm tất cả balances của wallet
     * THAY THẾ CHO: wallet.getBalances()
     */
    List<WalletBalance> findByWalletIdAndStatus(Long walletId, WalletBalance.WalletBalanceStatus status);
    
    /**
     * Tìm WalletBalance record bằng walletId và balanceId
     * SỬ DỤNG CHO: Balance sync operations
     */
    Optional<WalletBalance> findByWalletIdAndBalanceId(Long walletId, Long balanceId);
    
    /**
     * Tìm balance theo wallet và token type
     * THAY THẾ CHO: wallet.getBalances().stream().filter(b -> b.getTokenType() == tokenType)
     */
    Optional<WalletBalance> findByWalletIdAndTokenTypeAndStatus(Long walletId, TokenType tokenType, WalletBalance.WalletBalanceStatus status);
    
    /**
     * Tìm balances của multiple wallets (cho portfolio aggregation)
     * PERFORMANCE BOOST: Một query thay vì multiple queries
     */
    List<WalletBalance> findByWalletIdInAndStatus(List<Long> walletIds, WalletBalance.WalletBalanceStatus status);
    
    /**
     * Count non-zero balances của wallet
     */
    @Query("SELECT COUNT(wb) FROM WalletBalance wb WHERE wb.walletId = :walletId AND wb.currentAmount > 0 AND wb.status = :status")
    long countNonZeroBalancesByWallet(@Param("walletId") Long walletId, @Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Get total USD value của wallet
     */
    @Query("SELECT COALESCE(SUM(wb.usdValue), 0) FROM WalletBalance wb WHERE wb.walletId = :walletId AND wb.status = :status")
    BigDecimal getTotalUsdValueByWallet(@Param("walletId") Long walletId, @Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Get balance summary của wallet
     * (Thay thế cho việc load all balances rồi calculate)
     */
    @Query("SELECT wb.tokenType, SUM(wb.currentAmount), SUM(wb.usdValue) " +
           "FROM WalletBalance wb WHERE wb.walletId = :walletId AND wb.status = :status AND wb.currentAmount > 0 " +
           "GROUP BY wb.tokenType")
    List<Object[]> getWalletBalanceSummary(@Param("walletId") Long walletId, @Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Get balance summary của multiple wallets (cho portfolio)
     */
    @Query("SELECT wb.tokenType, SUM(wb.currentAmount), SUM(wb.usdValue) " +
           "FROM WalletBalance wb WHERE wb.walletId IN :walletIds AND wb.status = :status AND wb.currentAmount > 0 " +
           "GROUP BY wb.tokenType")
    List<Object[]> getPortfolioBalanceSummary(@Param("walletIds") List<Long> walletIds, @Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Find balances cần sync
     */
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.lastSyncedAt < :threshold OR wb.lastSyncedAt IS NULL")
    Page<WalletBalance> findBalancesNeedingSync(@Param("threshold") LocalDateTime threshold, Pageable pageable);
    
    /**
     * Find balances with recent changes
     */
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.lastSyncedAt >= :since AND wb.currentAmount != wb.previousAmount")
    List<WalletBalance> findRecentlyChangedBalances(@Param("since") LocalDateTime since);
    
    /**
     * Bulk update balance amounts
     */
    @Modifying
    @Query("UPDATE WalletBalance wb SET wb.previousAmount = wb.currentAmount, " +
           "wb.currentAmount = :newAmount, wb.usdValue = :usdValue, " +
           "wb.lastSyncedAt = CURRENT_TIMESTAMP, wb.syncStatus = :syncStatus " +
           "WHERE wb.walletId = :walletId AND wb.tokenType = :tokenType")
    int updateBalanceAmount(@Param("walletId") Long walletId,
                           @Param("tokenType") TokenType tokenType,
                           @Param("newAmount") BigDecimal newAmount,
                           @Param("usdValue") BigDecimal usdValue,
                           @Param("syncStatus") WalletBalance.SyncStatus syncStatus);
    
    /**
     * Get primary balances của wallet
     */
    List<WalletBalance> findByWalletIdAndIsPrimaryBalanceTrueAndStatus(Long walletId, WalletBalance.WalletBalanceStatus status);
    
    /**
     * Find top balances by USD value
     */
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.status = :status AND wb.usdValue > 0 ORDER BY wb.usdValue DESC")
    Page<WalletBalance> findTopBalancesByUsdValue(@Param("status") WalletBalance.WalletBalanceStatus status, Pageable pageable);
    
    /**
     * Performance query - get essential balance data only
     */
    @Query("SELECT new map(wb.walletId as walletId, wb.tokenType as tokenType, " +
           "wb.tokenSymbol as tokenSymbol, wb.currentAmount as amount, wb.usdValue as usdValue) " +
           "FROM WalletBalance wb WHERE wb.walletId IN :walletIds AND wb.status = :status")
    List<Map<String, Object>> findEssentialBalanceData(@Param("walletIds") List<Long> walletIds, 
                                                       @Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Find balances by token type across all wallets
     */
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.tokenType = :tokenType AND wb.currentAmount > 0 AND wb.status = :status ORDER BY wb.currentAmount DESC")
    Page<WalletBalance> findByTokenTypeAndStatus(@Param("tokenType") TokenType tokenType, 
                                               @Param("status") WalletBalance.WalletBalanceStatus status, 
                                               Pageable pageable);
    
    /**
     * Get balance statistics
     */
    @Query("SELECT " +
           "COUNT(wb) as totalBalances, " +
           "COUNT(DISTINCT wb.walletId) as uniqueWallets, " +
           "SUM(CASE WHEN wb.currentAmount > 0 THEN 1 ELSE 0 END) as nonZeroBalances, " +
           "SUM(wb.usdValue) as totalUsdValue " +
           "FROM WalletBalance wb WHERE wb.status = :status")
    Map<String, Object> getBalanceStatistics(@Param("status") WalletBalance.WalletBalanceStatus status);
    
    /**
     * Clean up zero balances
     */
    @Modifying
    @Query("DELETE FROM WalletBalance wb WHERE wb.currentAmount = 0 AND wb.lastSyncedAt < :threshold")
    int deleteZeroBalances(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find balances by wallet IDs, status and amount greater than threshold
     */
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.walletId IN :walletIds AND wb.status = :status AND wb.currentAmount > :minAmount")
    List<WalletBalance> findByWalletIdInAndStatusAndCurrentAmountGreaterThan(
        @Param("walletIds") List<Long> walletIds, 
        @Param("status") WalletBalanceStatus status,
        @Param("minAmount") BigDecimal minAmount);
    
    
    /**
     * Batch upsert balances
     */
    @Modifying
    @Query(value = "INSERT INTO wallet_balances (wallet_id, balance_id, token_type, token_symbol, current_amount, usd_value, status, first_detected_at) " +
                   "VALUES (:#{#wb.walletId}, :#{#wb.balanceId}, :#{#wb.tokenType}, :#{#wb.tokenSymbol}, :#{#wb.currentAmount}, :#{#wb.usdValue}, :#{#wb.status}, CURRENT_TIMESTAMP) " +
                   "ON DUPLICATE KEY UPDATE " +
                   "previous_amount = current_amount, current_amount = VALUES(current_amount), " +
                   "usd_value = VALUES(usd_value), last_synced_at = CURRENT_TIMESTAMP",
           nativeQuery = true)
    void upsertWalletBalance(@Param("wb") WalletBalance walletBalance);
}