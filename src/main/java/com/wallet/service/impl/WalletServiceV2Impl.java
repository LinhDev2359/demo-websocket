package com.wallet.service.impl;

import com.wallet.entity.*;
import com.wallet.repository.*;
import com.wallet.service.WalletService;
import com.wallet.service.UserService;
import com.wallet.service.CryptoService;
import com.wallet.config.DatabaseShardingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wallet Service Implementation V2
 * 
 * ✅ REFACTORED: Sử dụng Junction Tables thay vì @OneToMany/@ManyToOne
 * 
 * Key Changes:
 * 1. ❌ NO MORE wallet.getUser() or user.getWallets()  
 * 2. ✅ Sử dụng UserWalletRepository cho User-Wallet relationships
 * 3. ✅ Sử dụng WalletBalanceRepository cho Wallet-Balance relationships
 * 4. ✅ Better performance với explicit queries
 * 5. ✅ Cleaner, more maintainable code
 */
@Service("walletServiceV2")
@Slf4j
@Transactional
public class WalletServiceV2Impl implements WalletService {
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // ✅ NEW: Junction Table Repositories
    @Autowired
    private UserWalletRepository userWalletRepository;
    
    @Autowired
    private WalletBalanceRepository walletBalanceRepository;
    
    @Autowired(required = false)
    private DatabaseShardingConfig.DatabaseShardingUtils shardingUtils;
    
    @Autowired
    private CryptoService cryptoService;
    
    // EOS address pattern for validation
    private static final Pattern EOS_ADDRESS_PATTERN = Pattern.compile("^[a-z1-5]{12}$");
    
    // Redis cache keys and TTL
    private static final String WALLET_EXISTS_CACHE_KEY = "wallet:exists:";
    private static final String USER_WALLETS_CACHE_KEY = "user:wallets:";
    private static final String PRIMARY_WALLET_CACHE_KEY = "user:primary:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    @Override
    public Wallet createWallet(String userId, String walletAddress, String walletName) {
        log.info("Creating new wallet for user: {} with address: {} (V2 Junction Tables)", userId, walletAddress);
        
        // Require wallet address to be provided by user
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            throw new RuntimeException("Wallet address is required. Please provide your EOS wallet address.");
        }
        
        logShardingInfo("Creating wallet", userId, walletAddress);
        
        // Validate input
        validateWalletInput(userId, walletAddress, walletName);
        
        // Check if wallet address already exists (check with encrypted address)
        if (isWalletAddressExists(walletAddress)) {
            throw new RuntimeException("Wallet address already exists: " + walletAddress);
        }
        
        // Check if this will be the user's first (primary) wallet
        List<UserWallet> existingUserWallets = userWalletRepository.findByUserIdAndStatus(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        boolean isPrimaryWallet = existingUserWallets.isEmpty();
        
        // If user wants to set as primary, ensure no other primary wallet exists for this wallet type
        if (isPrimaryWallet) {
            Optional<Wallet> existingPrimaryWallet = walletRepository.findByUserIdAndWalletTypeAndIsPrimary(
                userId, WalletType.EOS, true);
            if (existingPrimaryWallet.isPresent()) {
                // Unset the existing primary wallet
                Wallet existing = existingPrimaryWallet.get();
                existing.setIsPrimary(false);
                walletRepository.save(existing);
                log.info("Unset primary flag for existing wallet: {} for user: {}", 
                        existing.getId(), userId);
            }
        }
        
        // ✅ STEP 1: Create Wallet entity (NO User reference)
        Wallet wallet = new Wallet();
        wallet.setUserId(userId); // ✅ Simple String reference instead of @ManyToOne
        wallet.setWalletAddress(cryptoService.encryptWalletAddress(walletAddress)); // ✅ Mã hóa địa chỉ ví trước khi lưu
        wallet.setWalletName(walletName);
        wallet.setWalletType(WalletType.EOS);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setIsPrimary(isPrimaryWallet); // Set primary flag based on existing wallets
        
        // Save wallet first
        Wallet savedWallet = walletRepository.save(wallet);
        
        // ✅ STEP 2: Create Junction Table entry
        UserWallet userWallet = new UserWallet(
            userId, 
            savedWallet.getId(), 
            WalletType.EOS,
            isPrimaryWallet // Use the same primary wallet flag
        );
        userWallet.setAssignedAt(LocalDateTime.now());
        userWallet.setAssignedBy(userId); // Self-assigned
        userWallet.setNotes("Created via API");
        
        userWalletRepository.save(userWallet);
        
        // Clear related cache entries
        clearWalletCache(userId, walletAddress);
        
        log.info("Successfully created wallet with ID: {} for user: {} using Junction Tables", 
                savedWallet.getId(), userId);
        return savedWallet;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByAddress(String walletAddress) {
        // Mã hóa địa chỉ trước khi tìm kiếm
        String encryptedAddress = cryptoService.encryptWalletAddress(walletAddress);
        Optional<Wallet> walletOpt = walletRepository.findByWalletAddress(encryptedAddress);
        
        // Giải mã địa chỉ ví trước khi trả về
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setWalletAddress(cryptoService.decryptWalletAddress(wallet.getWalletAddress()));
        }
        
        return walletOpt;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Wallet> getUserWallets(String userId, Pageable pageable) {
        // ✅ NEW APPROACH: Query through Junction Table
        Page<UserWallet> userWalletPage = userWalletRepository.findByUserIdAndStatus(
            userId, UserWallet.UserWalletStatus.ACTIVE, pageable);
        
        // Get wallet IDs from junction table
        List<Long> walletIds = userWalletPage.getContent().stream()
            .map(UserWallet::getWalletId)
            .collect(Collectors.toList());
        
        // Get actual wallets và giải mã địa chỉ
        List<Wallet> wallets = walletRepository.findAllById(walletIds);
        wallets.forEach(wallet -> {
            wallet.setWalletAddress(cryptoService.decryptWalletAddress(wallet.getWalletAddress()));
        });
        
        // Return as Page (manually constructed)
        return new org.springframework.data.domain.PageImpl<>(
            wallets, pageable, userWalletPage.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Wallet> getActiveUserWallets(String userId) {
        // ✅ NEW APPROACH: Query through Junction Table
        List<Long> walletIds = userWalletRepository.findWalletIdsByUserId(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        
        List<Wallet> wallets = walletRepository.findAllById(walletIds);
        // Giải mã địa chỉ ví trước khi trả về
        wallets.forEach(wallet -> {
            wallet.setWalletAddress(cryptoService.decryptWalletAddress(wallet.getWalletAddress()));
        });
        return wallets;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getPrimaryWallet(String userId) {
        // Check cache first (if Redis is available)
        String cacheKey = PRIMARY_WALLET_CACHE_KEY + userId;
        if (redisTemplate != null) {
            Wallet cached = (Wallet) redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                log.debug("Primary wallet cache hit for user: {}", userId);
                return Optional.of(cached);
            }
        }
        
        // ✅ NEW APPROACH: Query through Junction Table
        Optional<UserWallet> primaryUserWallet = userWalletRepository
            .findByUserIdAndIsPrimaryTrueAndStatus(userId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (primaryUserWallet.isEmpty()) {
            return Optional.empty();
        }
        
        // Get actual wallet
        Optional<Wallet> primaryWallet = walletRepository.findById(primaryUserWallet.get().getWalletId());
        
        // Giải mã địa chỉ ví trước khi trả về
        if (primaryWallet.isPresent()) {
            Wallet wallet = primaryWallet.get();
            wallet.setWalletAddress(cryptoService.decryptWalletAddress(wallet.getWalletAddress()));
            
            // Cache the result if present (if Redis is available)
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, wallet, CACHE_TTL);
                log.debug("Cached primary wallet for user: {}", userId);
            }
        }
        
        return primaryWallet;
    }
    
    @Override
    public Wallet setPrimaryWallet(String userId, String walletAddress) {
        log.info("Setting primary wallet for user: {} to address: {} (V2)", userId, walletAddress);
        
        // Get the wallet
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // ✅ NEW APPROACH: Verify ownership through Junction Table
        boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
            userId, wallet.getId(), UserWallet.UserWalletStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        // ✅ STEP 1: Unset current primary
        userWalletRepository.unsetPrimaryWallets(userId, WalletType.EOS);
        
        // ✅ STEP 2: Set new primary
        Optional<UserWallet> userWalletOpt = userWalletRepository
            .findByUserIdAndWalletIdAndStatus(userId, wallet.getId(), UserWallet.UserWalletStatus.ACTIVE);
        
        if (userWalletOpt.isPresent()) {
            UserWallet userWallet = userWalletOpt.get();
            userWallet.setIsPrimary(true);
            userWalletRepository.save(userWallet);
        }
        
        // Clear primary wallet cache
        clearPrimaryWalletCache(userId);
        
        log.info("Successfully set primary wallet for user: {} using Junction Tables", userId);
        return wallet;
    }
    
    @Override
    public Wallet updateWalletName(String userId, String walletAddress, String newName) {
        log.info("Updating wallet name for user: {} and address: {} (V2)", userId, walletAddress);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // ✅ NEW APPROACH: Verify ownership through Junction Table
        boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
            userId, wallet.getId(), UserWallet.UserWalletStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        // Validate new name
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }
        
        if (newName.length() > 100) {
            throw new IllegalArgumentException("Wallet name cannot exceed 100 characters");
        }
        
        wallet.setWalletName(newName.trim());
        Wallet updatedWallet = walletRepository.save(wallet);
        
        // Clear related cache entries
        clearWalletCache(userId, walletAddress);
        
        log.info("Successfully updated wallet name for user: {} using Junction Tables", userId);
        return updatedWallet;
    }
    
    @Override
    public Wallet updateWalletStatus(String userId, String walletAddress, WalletStatus status) {
        log.info("Updating wallet status for user: {} and address: {} to {} (V2)", 
                userId, walletAddress, status);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // ✅ NEW APPROACH: Verify ownership through Junction Table
        boolean hasAccess = userWalletRepository.existsByUserIdAndWalletIdAndStatus(
            userId, wallet.getId(), UserWallet.UserWalletStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        wallet.setStatus(status);
        Wallet updatedWallet = walletRepository.save(wallet);
        
        // Also update Junction Table status if wallet is being deactivated
        if (status != WalletStatus.ACTIVE) {
            UserWallet.UserWalletStatus newJunctionStatus = status == WalletStatus.DELETED 
                ? UserWallet.UserWalletStatus.SUSPENDED 
                : UserWallet.UserWalletStatus.INACTIVE;
                
            userWalletRepository.bulkUpdateUserWalletStatus(
                userId, UserWallet.UserWalletStatus.ACTIVE, newJunctionStatus);
        }
        
        clearWalletCache(userId, walletAddress);
        
        log.info("Successfully updated wallet status for user: {} using Junction Tables", userId);
        return updatedWallet;
    }
    
    @Override
    public void deleteWallet(String userId, String walletAddress) {
        log.info("Deleting wallet for user: {} and address: {} (V2)", userId, walletAddress);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // ✅ NEW APPROACH: Check if primary through Junction Table
        Optional<UserWallet> userWalletOpt = userWalletRepository
            .findByUserIdAndWalletIdAndStatus(userId, wallet.getId(), UserWallet.UserWalletStatus.ACTIVE);
        
        if (userWalletOpt.isEmpty()) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        UserWallet userWallet = userWalletOpt.get();
        if (userWallet.getIsPrimary()) {
            throw new RuntimeException("Cannot delete primary wallet. Set another wallet as primary first.");
        }
        
        // ✅ STEP 1: Update Wallet status (soft delete)
        wallet.setStatus(WalletStatus.DELETED);
        walletRepository.save(wallet);
        
        // ✅ STEP 2: Update Junction Table status
        userWallet.setStatus(UserWallet.UserWalletStatus.SUSPENDED);
        userWallet.setNotes("Deleted by user");
        userWalletRepository.save(userWallet);
        
        clearWalletCache(userId, walletAddress);
        
        log.info("Successfully deleted wallet for user: {} using Junction Tables", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isValidEOSAddress(String walletAddress) {
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            return false;
        }
        
        return EOS_ADDRESS_PATTERN.matcher(walletAddress.trim()).matches();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWalletAddressExists(String walletAddress) {
        // Mã hóa địa chỉ trước khi kiểm tra
        String encryptedAddress = cryptoService.encryptWalletAddress(walletAddress);
        
        // Check cache first (if Redis is available) - use encrypted address for cache key
        String cacheKey = WALLET_EXISTS_CACHE_KEY + encryptedAddress;
        if (redisTemplate != null) {
            Boolean cached = (Boolean) redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                log.debug("Wallet existence check cache hit for address: {}", walletAddress);
                return cached;
            }
        }
        
        // Query database with encrypted address
        boolean exists = walletRepository.existsByWalletAddress(encryptedAddress);
        
        // Cache the result (if Redis is available)
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, exists, CACHE_TTL);
            log.debug("Cached wallet existence result for address: {} = {}", walletAddress, exists);
        }
        
        return exists;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Wallet> getWalletsWithBalance(String userId) {
        // ✅ NEW APPROACH: Use Junction Tables to find wallets with balances
        List<Long> walletIds = userWalletRepository.findWalletIdsByUserId(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (walletIds.isEmpty()) {
            return List.of();
        }
        
        // Find wallets that have non-zero balances
        List<WalletBalance> walletsWithBalances = walletBalanceRepository
            .findByWalletIdInAndStatusAndCurrentAmountGreaterThan(
                walletIds, WalletBalance.WalletBalanceStatus.ACTIVE, java.math.BigDecimal.ZERO);
        
        List<Long> walletIdsWithBalance = walletsWithBalances.stream()
            .map(WalletBalance::getWalletId)
            .distinct()
            .collect(Collectors.toList());
        
        List<Wallet> wallets = walletRepository.findAllById(walletIdsWithBalance);
        // Giải mã địa chỉ ví trước khi trả về
        wallets.forEach(wallet -> {
            wallet.setWalletAddress(cryptoService.decryptWalletAddress(wallet.getWalletAddress()));
        });
        return wallets;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUserWalletCount(String userId) {
        return userWalletRepository.countByUserIdAndStatus(userId, UserWallet.UserWalletStatus.ACTIVE);
    }
    
    @Override
    public int bulkUpdateWalletStatus(List<String> walletAddresses, WalletStatus status) {
        log.info("Bulk updating {} wallets to status: {} (V2)", walletAddresses.size(), status);
        
        if (walletAddresses.isEmpty()) {
            return 0;
        }
        
        // Use repository bulk update for better performance
        int updatedCount = walletRepository.bulkUpdateStatus(walletAddresses, status);
        
        // Clear cache for affected wallets
        for (String address : walletAddresses) {
            clearWalletExistenceCache(address);
        }
        
        log.info("Successfully bulk updated {} wallets using Junction Tables", updatedCount);
        return updatedCount;
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletStatistics getUserWalletStatistics(String userId) {
        // ✅ NEW APPROACH: Get statistics through Junction Table
        Object[] stats = userWalletRepository.getUserWalletStatistics(
            userId, UserWallet.UserWalletStatus.ACTIVE);
        
        if (stats == null || stats.length < 3) {
            return new WalletStatistics(0, 0, 0, 0, 0);
        }
        
        long totalWallets = ((Number) stats[0]).longValue();
        long primaryWallets = ((Number) stats[1]).longValue();
        long fullAccessWallets = ((Number) stats[2]).longValue();
        
        // Get inactive and deleted counts separately
        long inactiveWallets = userWalletRepository.countByUserIdAndStatus(
            userId, UserWallet.UserWalletStatus.INACTIVE);
        long deletedWallets = userWalletRepository.countByUserIdAndStatus(
            userId, UserWallet.UserWalletStatus.SUSPENDED);
        
        return new WalletStatistics(totalWallets, totalWallets, inactiveWallets, deletedWallets, primaryWallets);
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletStatistics getGlobalWalletStatistics() {
        long totalWallets = walletRepository.count();
        long activeWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);
        long inactiveWallets = walletRepository.countByStatus(WalletStatus.INACTIVE);
        long deletedWallets = walletRepository.countByStatus(WalletStatus.DELETED);
        long primaryWallets = walletRepository.countByIsPrimaryTrue();
        
        return new WalletStatistics(totalWallets, activeWallets, inactiveWallets, deletedWallets, primaryWallets);
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    private void validateWalletInput(String userId, String walletAddress, String walletName) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        // Wallet address validation - must be provided by user
        if (walletAddress != null && !walletAddress.trim().isEmpty() && !isValidEOSAddress(walletAddress)) {
            throw new IllegalArgumentException("Invalid EOS wallet address format");
        }
        
        if (walletName == null || walletName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet name is required");
        }
        
        if (walletName.length() > 100) {
            throw new IllegalArgumentException("Wallet name cannot exceed 100 characters");
        }
    }
    
    private void clearWalletCache(String userId, String walletAddress) {
        clearWalletExistenceCache(walletAddress);
        clearUserWalletsCache(userId);
        clearPrimaryWalletCache(userId);
    }
    
    private void clearWalletExistenceCache(String walletAddress) {
        if (redisTemplate != null) {
            // Sử dụng địa chỉ mã hóa cho cache key
            String encryptedAddress = cryptoService.encryptWalletAddress(walletAddress);
            String cacheKey = WALLET_EXISTS_CACHE_KEY + encryptedAddress;
            redisTemplate.delete(cacheKey);
            log.debug("Cleared wallet existence cache for address: {}", walletAddress);
        }
    }
    
    private void clearUserWalletsCache(String userId) {
        if (redisTemplate != null) {
            String cacheKey = USER_WALLETS_CACHE_KEY + userId;
            redisTemplate.delete(cacheKey);
            log.debug("Cleared user wallets cache for user: {}", userId);
        }
    }
    
    private void clearPrimaryWalletCache(String userId) {
        if (redisTemplate != null) {
            String cacheKey = PRIMARY_WALLET_CACHE_KEY + userId;
            redisTemplate.delete(cacheKey);
            log.debug("Cleared primary wallet cache for user: {}", userId);
        }
    }
    
    private void logShardingInfo(String operation, String userId, String walletAddress) {
        if (shardingUtils != null && log.isDebugEnabled()) {
            int userShard = shardingUtils.getUserShard(userId);
            int walletShard = shardingUtils.getWalletShard(walletAddress, userId);
            int balanceShard = shardingUtils.getBalanceShard(walletAddress);
            
            log.debug("{} - Sharding info: user={} (shard={}), wallet={} (shard={}), balance shard={}", 
                     operation, userId, userShard, walletAddress, walletShard, balanceShard);
        }
    }
    
    // Additional Junction Table helper methods
    private Optional<UserWallet> findUserWalletRelation(String userId, Long walletId) {
        return userWalletRepository.findByUserIdAndWalletIdAndStatus(
            userId, walletId, UserWallet.UserWalletStatus.ACTIVE);
    }
    
    public DatabaseShardingConfig.ShardingStats getShardingStats() {
        if (shardingUtils != null) {
            return shardingUtils.getShardingStats();
        }
        return null;
    }
}