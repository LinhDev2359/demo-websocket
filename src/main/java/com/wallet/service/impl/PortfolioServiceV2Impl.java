package com.wallet.service.impl;

import com.wallet.dto.PortfolioResponse;
import com.wallet.entity.*;
import com.wallet.repository.*;
import com.wallet.service.PortfolioService;
import com.wallet.service.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Portfolio Service Implementation V2
 * 
 * REFACTORED: Sử dụng Junction Tables thay vì @OneToMany relationships
 * 
 * Cải tiến:
 * 1. ✅ Tránh N+1 query problems
 * 2. ✅ Better performance với explicit queries
 * 3. ✅ Flexible hơn cho complex operations
 * 4. ✅ Easier testing và mocking
 * 5. ✅ Better control over loading strategy
 */
@Service("portfolioServiceV2")
@Transactional(readOnly = true)
public class PortfolioServiceV2Impl implements PortfolioService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioServiceV2Impl.class);
    
    // Cache keys
    private static final String PORTFOLIO_CACHE_KEY = "portfolio:user:";
    private static final String PORTFOLIO_SUMMARY_CACHE_KEY = "portfolio:summary:";
    private static final int PORTFOLIO_CACHE_TTL_MINUTES = 5;
    private static final int PORTFOLIO_SUMMARY_CACHE_TTL_MINUTES = 2;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private BalanceRepository balanceRepository;
    
    // ✅ NEW: Junction table repositories
    @Autowired
    private UserWalletRepository userWalletRepository;
    
    @Autowired
    private WalletBalanceRepository walletBalanceRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CryptoService cryptoService;
    
    @Override
    public PortfolioResponse getUserPortfolio(String userId, boolean refreshCache) {
        logger.debug("Getting portfolio for userId: {} using Junction Tables approach", userId);
        
        // Check cache first (if Redis is available)
        String cacheKey = PORTFOLIO_CACHE_KEY + userId;
        if (!refreshCache && redisTemplate != null) {
            PortfolioResponse cached = (PortfolioResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.debug("Portfolio found in cache for userId: {}", userId);
                return cached;
            }
        }
        
        try {
            // Get user information
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (userOptional.isEmpty()) {
                logger.warn("User not found: {}", userId);
                return null;
            }
            User user = userOptional.get();
            
            // Create portfolio response
            PortfolioResponse portfolio = new PortfolioResponse(userId, user.getUsername());
            
            // ✅ NEW APPROACH: Query through Junction Table
            // Thay vì: user.getWallets()
            List<UserWallet> userWallets = userWalletRepository.findByUserIdAndStatus(
                userId, UserWallet.UserWalletStatus.ACTIVE);
            
            portfolio.setTotalWallets(userWallets.size());
            
            if (userWallets.isEmpty()) {
                logger.debug("No wallets found for userId: {}", userId);
                portfolio.setWallets(List.of());
                portfolio.setTotalBalance(new PortfolioResponse.BalanceSummary());
                cachePortfolio(cacheKey, portfolio);
                return portfolio;
            }
            
            // ✅ PERFORMANCE BOOST: Get wallet IDs only
            List<Long> walletIds = userWallets.stream()
                .map(UserWallet::getWalletId)
                .collect(Collectors.toList());
            
            // ✅ SINGLE QUERY: Get all wallets at once
            Map<Long, Wallet> walletsMap = walletRepository.findAllById(walletIds)
                .stream()
                .collect(Collectors.toMap(Wallet::getId, wallet -> wallet));
            
            // Convert to portfolio info với Junction Table data
            List<PortfolioResponse.WalletPortfolioInfo> walletInfos = userWallets.stream()
                .map(userWallet -> convertToWalletPortfolioInfoV2(userWallet, walletsMap))
                .filter(info -> info != null)
                .collect(Collectors.toList());
            
            portfolio.setWallets(walletInfos);
            
            // Calculate total balance summary
            PortfolioResponse.BalanceSummary totalBalance = calculateTotalBalanceV2(walletIds);
            portfolio.setTotalBalance(totalBalance);
            
            // Cache the portfolio
            cachePortfolio(cacheKey, portfolio);
            
            logger.info("Portfolio generated for userId: {} with {} wallets using Junction Tables", 
                       userId, userWallets.size());
            return portfolio;
            
        } catch (Exception e) {
            logger.error("Error getting portfolio for userId: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public PortfolioResponse getUserPortfolio(String userId, Pageable pageable, boolean refreshCache) {
        logger.debug("Getting paginated portfolio for userId: {} using Junction Tables approach", userId);
        
        try {
            // Get user information
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (userOptional.isEmpty()) {
                logger.warn("User not found: {}", userId);
                return null;
            }
            User user = userOptional.get();
            
            // Create portfolio response
            PortfolioResponse portfolio = new PortfolioResponse(userId, user.getUsername());
            
            // ✅ NEW APPROACH: Paginated query through Junction Table
            Page<UserWallet> userWalletPage = userWalletRepository.findByUserIdAndStatus(
                userId, UserWallet.UserWalletStatus.ACTIVE, pageable);
            
            portfolio.setTotalWallets(Math.toIntExact(userWalletPage.getTotalElements()));
            
            if (userWalletPage.isEmpty()) {
                portfolio.setWallets(List.of());
                portfolio.setTotalBalance(new PortfolioResponse.BalanceSummary());
                return portfolio;
            }
            
            // Get wallet details for current page
            List<Long> walletIds = userWalletPage.getContent().stream()
                .map(UserWallet::getWalletId)
                .collect(Collectors.toList());
            
            Map<Long, Wallet> walletsMap = walletRepository.findAllById(walletIds)
                .stream()
                .collect(Collectors.toMap(Wallet::getId, wallet -> wallet));
            
            List<PortfolioResponse.WalletPortfolioInfo> walletInfos = userWalletPage.getContent().stream()
                .map(userWallet -> convertToWalletPortfolioInfoV2(userWallet, walletsMap))
                .filter(info -> info != null)
                .collect(Collectors.toList());
            
            portfolio.setWallets(walletInfos);
            
            // For paginated requests, get total balance summary
            PortfolioResponse.BalanceSummary totalBalance = getPortfolioSummary(userId, refreshCache);
            portfolio.setTotalBalance(totalBalance);
            
            logger.info("Paginated portfolio generated for userId: {} with {} wallets on page {}", 
                       userId, walletInfos.size(), pageable.getPageNumber());
            return portfolio;
            
        } catch (Exception e) {
            logger.error("Error getting paginated portfolio for userId: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public PortfolioResponse.BalanceSummary getPortfolioSummary(String userId, boolean refreshCache) {
        logger.debug("Getting portfolio summary for userId: {} using Junction Tables", userId);
        
        String cacheKey = PORTFOLIO_SUMMARY_CACHE_KEY + userId;
        if (!refreshCache && redisTemplate != null) {
            PortfolioResponse.BalanceSummary cached = 
                (PortfolioResponse.BalanceSummary) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        
        try {
            // ✅ PERFORMANCE BOOST: Get wallet IDs only
            List<Long> walletIds = userWalletRepository.findWalletIdsByUserId(
                userId, UserWallet.UserWalletStatus.ACTIVE);
            
            if (walletIds.isEmpty()) {
                PortfolioResponse.BalanceSummary empty = new PortfolioResponse.BalanceSummary();
                cachePortfolioSummary(cacheKey, empty);
                return empty;
            }
            
            // ✅ SINGLE QUERY: Get aggregated balance summary
            PortfolioResponse.BalanceSummary summary = calculateTotalBalanceV2(walletIds);
            
            cachePortfolioSummary(cacheKey, summary);
            
            logger.debug("Portfolio summary calculated for userId: {} with {} wallets using Junction Tables", 
                        userId, walletIds.size());
            return summary;
            
        } catch (Exception e) {
            logger.error("Error getting portfolio summary for userId: {}", userId, e);
            return new PortfolioResponse.BalanceSummary();
        }
    }
    
    @Override
    public void refreshPortfolioCache(String userId) {
        logger.info("Refreshing portfolio cache for userId: {}", userId);
        invalidatePortfolioCache(userId);
        
        // Pre-load cache
        getUserPortfolio(userId, true);
        getPortfolioSummary(userId, true);
    }
    
    @Override
    public void invalidatePortfolioCache(String userId) {
        logger.info("Invalidating portfolio cache for userId: {}", userId);
        
        if (redisTemplate != null) {
            String portfolioCacheKey = PORTFOLIO_CACHE_KEY + userId;
            String summaryCacheKey = PORTFOLIO_SUMMARY_CACHE_KEY + userId;
            
            redisTemplate.delete(portfolioCacheKey);
            redisTemplate.delete(summaryCacheKey);
        }
    }
    
    @Override
    public List<PortfolioResponse> getMultipleUserPortfolios(List<String> userIds, boolean refreshCache) {
        logger.info("Getting portfolios for {} users using Junction Tables", userIds.size());
        
        return userIds.stream()
            .map(userId -> getUserPortfolio(userId, refreshCache))
            .filter(portfolio -> portfolio != null)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasWallets(String userId) {
        try {
            return userWalletRepository.countByUserIdAndStatus(
                userId, UserWallet.UserWalletStatus.ACTIVE) > 0;
        } catch (Exception e) {
            logger.error("Error checking if user has wallets: {}", userId, e);
            return false;
        }
    }
    
    /**
     * ✅ NEW METHOD: Convert UserWallet + Wallet to WalletPortfolioInfo
     * Sử dụng Junction Table data + Wallet entity
     */
    private PortfolioResponse.WalletPortfolioInfo convertToWalletPortfolioInfoV2(
            UserWallet userWallet, Map<Long, Wallet> walletsMap) {
        
        Wallet wallet = walletsMap.get(userWallet.getWalletId());
        if (wallet == null) {
            logger.warn("Wallet not found for ID: {}", userWallet.getWalletId());
            return null;
        }
        
        PortfolioResponse.WalletPortfolioInfo info = new PortfolioResponse.WalletPortfolioInfo(
            wallet.getId(),
            cryptoService.decryptWalletAddress(wallet.getWalletAddress()), // ✅ Giải mã địa chỉ ví
            userWallet.getWalletType().name()
        );
        
        info.setCreatedAt(wallet.getCreatedAt());
        // ✅ Use Junction Table data for isPrimary
        info.setIsPrimary(userWallet.getIsPrimary());
        
        // ✅ NEW APPROACH: Get balances through Junction Table
        List<WalletBalance> walletBalances = walletBalanceRepository.findByWalletIdAndStatus(
            wallet.getId(), WalletBalance.WalletBalanceStatus.ACTIVE);
        
        List<PortfolioResponse.BalanceInfo> balanceInfos = walletBalances.stream()
            .map(this::convertWalletBalanceToBalanceInfo)
            .collect(Collectors.toList());
        
        info.setBalances(balanceInfos);
        
        return info;
    }
    
    /**
     * ✅ NEW METHOD: Convert WalletBalance to BalanceInfo
     */
    private PortfolioResponse.BalanceInfo convertWalletBalanceToBalanceInfo(WalletBalance walletBalance) {
        PortfolioResponse.BalanceInfo balanceInfo = new PortfolioResponse.BalanceInfo(
            walletBalance.getTokenSymbol(),
            walletBalance.getCurrentAmount()
        );
        balanceInfo.setLastUpdated(walletBalance.getLastSyncedAt());
        return balanceInfo;
    }
    
    /**
     * ✅ NEW METHOD: Calculate total balance using Junction Table aggregation
     * PERFORMANCE BOOST: Single query thay vì multiple queries
     */
    private PortfolioResponse.BalanceSummary calculateTotalBalanceV2(List<Long> walletIds) {
        if (walletIds.isEmpty()) {
            return new PortfolioResponse.BalanceSummary();
        }
        
        // ✅ SINGLE QUERY: Get aggregated balance summary
        List<Object[]> balanceSummary = walletBalanceRepository.getPortfolioBalanceSummary(
            walletIds, WalletBalance.WalletBalanceStatus.ACTIVE);
        
        PortfolioResponse.BalanceSummary summary = new PortfolioResponse.BalanceSummary();
        
        for (Object[] row : balanceSummary) {
            TokenType tokenType = (TokenType) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            BigDecimal totalUsdValue = (BigDecimal) row[2];
            
            switch (tokenType.name().toLowerCase()) {
                case "a":
                    summary.setTotalEos(totalAmount);
                    break;
                case "ram":
                    summary.setTotalRam(totalAmount);
                    break;
                case "rams":
                    summary.setTotalRams(totalAmount);
                    break;
                case "wram":
                    summary.setTotalWram(totalAmount);
                    break;
            }
        }
        
        return summary;
    }
    
    /**
     * Cache portfolio response (if Redis is available)
     */
    private void cachePortfolio(String cacheKey, PortfolioResponse portfolio) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, portfolio, PORTFOLIO_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Portfolio cached with key: {}", cacheKey);
            } catch (Exception e) {
                logger.error("Error caching portfolio with key: {}", cacheKey, e);
            }
        }
    }
    
    /**
     * Cache portfolio summary (if Redis is available)
     */
    private void cachePortfolioSummary(String cacheKey, PortfolioResponse.BalanceSummary summary) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, summary, PORTFOLIO_SUMMARY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Portfolio summary cached with key: {}", cacheKey);
            } catch (Exception e) {
                logger.error("Error caching portfolio summary with key: {}", cacheKey, e);
            }
        }
    }
}