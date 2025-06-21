package com.wallet.service.impl;

import com.wallet.entity.Wallet;
import com.wallet.repository.WalletRepository;
import com.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Wallet Service Implementation
 * Implementation của WalletService interface
 * 
 * Chức năng:
 * 1. Business logic cho wallet operations
 * 2. Validation và error handling
 * 3. Transaction management
 * 4. EOS address validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {
    
    private final WalletRepository walletRepository;
    
    // EOS address pattern for validation
    private static final Pattern EOS_ADDRESS_PATTERN = Pattern.compile("^[a-z1-5]{12}$");
    
    @Override
    public Wallet createWallet(String userId, String walletAddress, String walletName) {
        log.info("Creating new wallet for user: {} with address: {}", userId, walletAddress);
        
        // Validate input
        validateWalletInput(userId, walletAddress, walletName);
        
        // Check if wallet address already exists
        if (isWalletAddressExists(walletAddress)) {
            throw new RuntimeException("Wallet address already exists: " + walletAddress);
        }
        
        // Create new wallet
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setWalletAddress(walletAddress);
        wallet.setWalletName(walletName);
        wallet.setWalletType(Wallet.WalletType.EOS);
        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        
        // Set as primary if this is the first wallet for the user
        List<Wallet> userWallets = getActiveUserWallets(userId);
        wallet.setIsPrimary(userWallets.isEmpty());
        
        Wallet savedWallet = walletRepository.save(wallet);
        
        log.info("Successfully created wallet with ID: {} for user: {}", savedWallet.getId(), userId);
        return savedWallet;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByAddress(String walletAddress) {
        return walletRepository.findByWalletAddress(walletAddress);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Wallet> getUserWallets(String userId, Pageable pageable) {
        return walletRepository.findByUserId(userId, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Wallet> getActiveUserWallets(String userId) {
        return walletRepository.findByUserIdAndStatus(userId, Wallet.WalletStatus.ACTIVE);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getPrimaryWallet(String userId) {
        return walletRepository.findByUserIdAndIsPrimaryTrue(userId);
    }
    
    @Override
    public Wallet setPrimaryWallet(String userId, String walletAddress) {
        log.info("Setting primary wallet for user: {} to address: {}", userId, walletAddress);
        
        // Get the wallet to set as primary
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // Verify wallet belongs to user
        if (!wallet.getUserId().equals(userId)) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        // Remove primary flag from current primary wallet
        Optional<Wallet> currentPrimary = getPrimaryWallet(userId);
        if (currentPrimary.isPresent()) {
            Wallet currentPrimaryWallet = currentPrimary.get();
            currentPrimaryWallet.setIsPrimary(false);
            walletRepository.save(currentPrimaryWallet);
        }
        
        // Set new primary wallet
        wallet.setIsPrimary(true);
        Wallet updatedWallet = walletRepository.save(wallet);
        
        log.info("Successfully set primary wallet for user: {}", userId);
        return updatedWallet;
    }
    
    @Override
    public Wallet updateWalletName(String userId, String walletAddress, String newName) {
        log.info("Updating wallet name for user: {} and address: {}", userId, walletAddress);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // Verify wallet belongs to user
        if (!wallet.getUserId().equals(userId)) {
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
        
        log.info("Successfully updated wallet name for user: {}", userId);
        return updatedWallet;
    }
    
    @Override
    public Wallet updateWalletStatus(String userId, String walletAddress, Wallet.WalletStatus status) {
        log.info("Updating wallet status for user: {} and address: {} to {}", userId, walletAddress, status);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // Verify wallet belongs to user
        if (!wallet.getUserId().equals(userId)) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        wallet.setStatus(status);
        Wallet updatedWallet = walletRepository.save(wallet);
        
        log.info("Successfully updated wallet status for user: {}", userId);
        return updatedWallet;
    }
    
    @Override
    public void deleteWallet(String userId, String walletAddress) {
        log.info("Deleting wallet for user: {} and address: {}", userId, walletAddress);
        
        Wallet wallet = getWalletByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
        
        // Verify wallet belongs to user
        if (!wallet.getUserId().equals(userId)) {
            throw new RuntimeException("Wallet does not belong to user: " + userId);
        }
        
        // Cannot delete primary wallet
        if (wallet.getIsPrimary()) {
            throw new RuntimeException("Cannot delete primary wallet. Set another wallet as primary first.");
        }
        
        // Soft delete by setting status to DELETED
        wallet.setStatus(Wallet.WalletStatus.DELETED);
        walletRepository.save(wallet);
        
        log.info("Successfully deleted wallet for user: {}", userId);
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
        return walletRepository.existsByWalletAddress(walletAddress);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Wallet> getWalletsWithBalance(String userId) {
        return walletRepository.findWalletsWithBalance(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUserWalletCount(String userId) {
        return walletRepository.countByUserIdAndStatus(userId, Wallet.WalletStatus.ACTIVE);
    }
    
    @Override
    public int bulkUpdateWalletStatus(List<String> walletAddresses, Wallet.WalletStatus status) {
        log.info("Bulk updating {} wallets to status: {}", walletAddresses.size(), status);
        
        int updatedCount = 0;
        for (String address : walletAddresses) {
            Optional<Wallet> walletOpt = getWalletByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                wallet.setStatus(status);
                walletRepository.save(wallet);
                updatedCount++;
            }
        }
        
        log.info("Successfully updated {} wallets", updatedCount);
        return updatedCount;
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletStatistics getUserWalletStatistics(String userId) {
        long totalWallets = walletRepository.countByUserId(userId);
        long activeWallets = walletRepository.countByUserIdAndStatus(userId, Wallet.WalletStatus.ACTIVE);
        long inactiveWallets = walletRepository.countByUserIdAndStatus(userId, Wallet.WalletStatus.INACTIVE);
        long deletedWallets = walletRepository.countByUserIdAndStatus(userId, Wallet.WalletStatus.DELETED);
        long primaryWallets = walletRepository.countByUserIdAndIsPrimaryTrue(userId);
        
        return new WalletStatistics(totalWallets, activeWallets, inactiveWallets, deletedWallets, primaryWallets);
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletStatistics getGlobalWalletStatistics() {
        long totalWallets = walletRepository.count();
        long activeWallets = walletRepository.countByStatus(Wallet.WalletStatus.ACTIVE);
        long inactiveWallets = walletRepository.countByStatus(Wallet.WalletStatus.INACTIVE);
        long deletedWallets = walletRepository.countByStatus(Wallet.WalletStatus.DELETED);
        long primaryWallets = walletRepository.countByIsPrimaryTrue();
        
        return new WalletStatistics(totalWallets, activeWallets, inactiveWallets, deletedWallets, primaryWallets);
    }
    
    /**
     * Validate wallet input parameters
     */
    private void validateWalletInput(String userId, String walletAddress, String walletName) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet address is required");
        }
        
        if (!isValidEOSAddress(walletAddress)) {
            throw new IllegalArgumentException("Invalid EOS wallet address format");
        }
        
        if (walletName == null || walletName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet name is required");
        }
        
        if (walletName.length() > 100) {
            throw new IllegalArgumentException("Wallet name cannot exceed 100 characters");
        }
    }
}