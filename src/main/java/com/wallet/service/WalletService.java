package com.wallet.service;

import com.wallet.entity.Wallet;
import com.wallet.entity.WalletStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Wallet Service Interface
 * Business Logic Layer cho Wallet operations
 * 
 * Chức năng:
 * 1. Wallet management (CRUD)
 * 2. EOS wallet address validation
 * 3. Primary wallet management
 * 4. Wallet statistics và reporting
 */
public interface WalletService {
    
    /**
     * Tạo wallet mới cho user
     * 
     * @param userId user ID
     * @param walletAddress EOS wallet address
     * @param walletName user-defined wallet name
     * @return created Wallet
     * @throws RuntimeException nếu wallet address đã tồn tại hoặc invalid
     */
    Wallet createWallet(String userId, String walletAddress, String walletName);
    
    /**
     * Get wallet theo wallet address
     * 
     * @param walletAddress wallet address
     * @return Optional Wallet
     */
    Optional<Wallet> getWalletByAddress(String walletAddress);
    
    /**
     * Get tất cả wallets của user
     * 
     * @param userId user ID
     * @param pageable pagination info
     * @return Page of Wallets
     */
    Page<Wallet> getUserWallets(String userId, Pageable pageable);
    
    /**
     * Get active wallets của user
     * 
     * @param userId user ID
     * @return List of active Wallets
     */
    List<Wallet> getActiveUserWallets(String userId);
    
    /**
     * Get primary wallet của user
     * 
     * @param userId user ID
     * @return Optional primary Wallet
     */
    Optional<Wallet> getPrimaryWallet(String userId);
    
    /**
     * Set wallet làm primary
     * 
     * @param userId user ID
     * @param walletAddress wallet address
     * @return updated Wallet
     * @throws RuntimeException nếu wallet không thuộc về user
     */
    Wallet setPrimaryWallet(String userId, String walletAddress);
    
    /**
     * Update wallet name
     * 
     * @param userId user ID
     * @param walletAddress wallet address
     * @param newName new wallet name
     * @return updated Wallet
     * @throws RuntimeException nếu wallet không thuộc về user
     */
    Wallet updateWalletName(String userId, String walletAddress, String newName);
    
    /**
     * Update wallet status
     * 
     * @param userId user ID
     * @param walletAddress wallet address
     * @param status new status
     * @return updated Wallet
     * @throws RuntimeException nếu wallet không thuộc về user
     */
    Wallet updateWalletStatus(String userId, String walletAddress, WalletStatus status);
    
    /**
     * Delete wallet (soft delete)
     * 
     * @param userId user ID
     * @param walletAddress wallet address
     * @throws RuntimeException nếu wallet không thuộc về user hoặc là primary
     */
    void deleteWallet(String userId, String walletAddress);
    
    /**
     * Validate EOS wallet address format
     * 
     * @param walletAddress wallet address
     * @return true nếu valid
     */
    boolean isValidEOSAddress(String walletAddress);
    
    /**
     * Check xem wallet address đã tồn tại chưa
     * 
     * @param walletAddress wallet address
     * @return true nếu tồn tại
     */
    boolean isWalletAddressExists(String walletAddress);
    
    /**
     * Get wallets có balance > 0
     * 
     * @param userId user ID
     * @return List of Wallets có balance
     */
    List<Wallet> getWalletsWithBalance(String userId);
    
    /**
     * Get wallet count cho user
     * 
     * @param userId user ID
     * @return wallet count
     */
    long getUserWalletCount(String userId);
    
    /**
     * Bulk update wallet status
     * 
     * @param walletAddresses list of wallet addresses
     * @param status new status
     * @return number of updated wallets
     */
    int bulkUpdateWalletStatus(List<String> walletAddresses, WalletStatus status);
    
    /**
     * Get wallet statistics cho user
     * 
     * @param userId user ID
     * @return wallet statistics
     */
    WalletStatistics getUserWalletStatistics(String userId);
    
    /**
     * Get global wallet statistics
     * 
     * @return global wallet statistics
     */
    WalletStatistics getGlobalWalletStatistics();
    
    /**
     * Inner class cho wallet statistics
     */
    class WalletStatistics {
        private final long totalWallets;
        private final long activeWallets;
        private final long inactiveWallets;
        private final long deletedWallets;
        private final long primaryWallets;
        
        public WalletStatistics(long totalWallets, long activeWallets, long inactiveWallets, 
                              long deletedWallets, long primaryWallets) {
            this.totalWallets = totalWallets;
            this.activeWallets = activeWallets;
            this.inactiveWallets = inactiveWallets;
            this.deletedWallets = deletedWallets;
            this.primaryWallets = primaryWallets;
        }
        
        // Getters
        public long getTotalWallets() { return totalWallets; }
        public long getActiveWallets() { return activeWallets; }
        public long getInactiveWallets() { return inactiveWallets; }
        public long getDeletedWallets() { return deletedWallets; }
        public long getPrimaryWallets() { return primaryWallets; }
    }
}