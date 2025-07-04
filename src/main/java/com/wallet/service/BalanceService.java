package com.wallet.service;

import com.wallet.dto.BalanceResponse;
import com.wallet.entity.Balance;
import com.wallet.entity.TokenType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Balance Service Interface
 * Business logic for EOS token balance operations
 */
public interface BalanceService {

    /**
     * Get all balances for a user with filtering and pagination
     * 
     * @param userId user ID
     * @param pageable pagination parameters
     * @param minBalance minimum balance filter (optional)
     * @param tokenType filter by token type (optional)
     * @return paginated balance responses
     */
    Page<BalanceResponse> getUserBalances(String userId, Pageable pageable, 
                                        BigDecimal minBalance, TokenType tokenType);

    /**
     * Get balances for a specific wallet
     * 
     * @param walletAddress wallet address
     * @param userId user ID for authorization
     * @return list of balance responses
     */
    List<BalanceResponse> getWalletBalances(String walletAddress, String userId);

    /**
     * Get balance summary grouped by token type
     * 
     * @param userId user ID
     * @return map of token type to total balance
     */
    Map<String, BigDecimal> getBalanceSummary(String userId);

    /**
     * Get total portfolio value in USD
     * 
     * @param userId user ID
     * @return total portfolio value
     */
    BigDecimal getTotalPortfolioValue(String userId);

    /**
     * Get balance for specific wallet and token type
     * 
     * @param walletAddress wallet address
     * @param tokenType token type
     * @param userId user ID for authorization
     * @return balance response if found
     */
    Optional<BalanceResponse> getWalletTokenBalance(String walletAddress, TokenType tokenType, String userId);

    /**
     * Get balance statistics for a user
     * 
     * @param userId user ID
     * @return balance statistics
     */
    Map<String, Object> getBalanceStatistics(String userId);

    /**
     * Refresh balance for a specific wallet (triggers sync)
     * 
     * @param walletAddress wallet address
     * @param userId user ID for authorization
     * @return true if sync was triggered successfully
     */
    boolean refreshWalletBalance(String walletAddress, String userId);

    /**
     * Get balance history for a wallet (if available)
     * 
     * @param walletAddress wallet address
     * @param userId user ID for authorization
     * @param days number of days to look back
     * @return balance history data
     */
    List<Map<String, Object>> getBalanceHistory(String walletAddress, String userId, int days);
}