package com.wallet.service;

import com.wallet.dto.PortfolioResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Portfolio Service Interface
 * Provides business logic for portfolio management and aggregation
 */
public interface PortfolioService {
    
    /**
     * Get user portfolio with all wallets and balances
     * 
     * @param userId User ID
     * @param refreshCache Whether to refresh cache
     * @return Portfolio response with aggregated data
     */
    PortfolioResponse getUserPortfolio(String userId, boolean refreshCache);
    
    /**
     * Get user portfolio with pagination
     * 
     * @param userId User ID
     * @param pageable Pagination parameters
     * @param refreshCache Whether to refresh cache
     * @return Portfolio response with paginated wallets
     */
    PortfolioResponse getUserPortfolio(String userId, Pageable pageable, boolean refreshCache);
    
    /**
     * Get portfolio summary (aggregated balances only)
     * 
     * @param userId User ID
     * @param refreshCache Whether to refresh cache
     * @return Portfolio summary with total balances
     */
    PortfolioResponse.BalanceSummary getPortfolioSummary(String userId, boolean refreshCache);
    
    /**
     * Refresh portfolio cache for user
     * 
     * @param userId User ID
     */
    void refreshPortfolioCache(String userId);
    
    /**
     * Invalidate portfolio cache for user
     * 
     * @param userId User ID
     */
    void invalidatePortfolioCache(String userId);
    
    /**
     * Get portfolio for multiple users (for admin/monitoring)
     * 
     * @param userIds List of user IDs
     * @param refreshCache Whether to refresh cache
     * @return List of portfolio responses
     */
    List<PortfolioResponse> getMultipleUserPortfolios(List<String> userIds, boolean refreshCache);
    
    /**
     * Check if user has any wallets
     * 
     * @param userId User ID
     * @return True if user has wallets
     */
    boolean hasWallets(String userId);
    
    /**
     * Get total portfolio value in USD (if price service is available)
     * 
     * @param userId User ID
     * @param refreshCache Whether to refresh cache
     * @return Total portfolio value
     */
    // BigDecimal getTotalPortfolioValueUSD(String userId, boolean refreshCache);
}