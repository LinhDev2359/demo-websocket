package com.eoswallet.facade;

import com.eoswallet.dto.WalletCreateRequest;
import com.eoswallet.dto.WalletCreateResponse;
import com.eoswallet.dto.WalletResponse;
import com.eoswallet.dto.WalletUpdateRequest;
import com.eoswallet.dto.BalanceResponse;
import com.eoswallet.dto.TransferRequest;
import com.eoswallet.dto.TransferResponse;
import com.wallet.dto.WalletTokensResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface WalletFacade {
    
    WalletCreateResponse createWallet(String userId, WalletCreateRequest request);
    
    WalletResponse getWallet(Long walletId, String userId);
    
    Page<WalletResponse> getUserWallets(String userId, Pageable pageable);
    
    WalletResponse updateWallet(Long walletId, String userId, WalletUpdateRequest request);
    
    void deleteWallet(Long walletId, String userId);
    
    WalletResponse setPrimaryWallet(Long walletId, String userId);
    
    WalletResponse getPrimaryWallet(String userId);
    
    BalanceResponse getWalletBalance(Long walletId, String userId);
    
    List<BalanceResponse> getAllUserBalances(String userId);
    
    BalanceResponse updateBalance(Long walletId, BigDecimal amount, String operation);
    
    TransferResponse transferBetweenWallets(Long fromWalletId, Long toWalletId, 
                                          BigDecimal amount, String userId);
    
    void syncWalletWithBlockchain(Long walletId, String userId);
    
    void syncAllUserWalletsWithBlockchain(String userId);
    
    boolean validateEosAddress(String eosAddress);
    
    WalletResponse importExistingWallet(String userId, String privateKey, String eosAddress);
    
    String exportWalletPrivateKey(Long walletId, String userId, String password);
    
    void backupWallet(Long walletId, String userId);
    
    WalletResponse restoreWallet(String userId, String backupData, String password);
    
    /**
     * Get all tokens for a specific wallet
     * 
     * @param walletId wallet ID
     * @param userId user ID for authorization
     * @return WalletTokensResponse with list of tokens
     */
    WalletTokensResponse getWalletTokens(Long walletId, String userId);
}