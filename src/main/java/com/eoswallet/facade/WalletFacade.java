package com.eoswallet.facade;

import com.eoswallet.dto.WalletCreateRequest;
import com.eoswallet.dto.WalletCreateResponse;
import com.eoswallet.dto.WalletResponse;
import com.eoswallet.dto.WalletUpdateRequest;
import com.eoswallet.dto.BalanceResponse;
import com.eoswallet.dto.TransferRequest;
import com.eoswallet.dto.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface WalletFacade {
    
    WalletCreateResponse createWallet(Long userId, WalletCreateRequest request);
    
    WalletResponse getWallet(Long walletId, Long userId);
    
    Page<WalletResponse> getUserWallets(Long userId, Pageable pageable);
    
    WalletResponse updateWallet(Long walletId, Long userId, WalletUpdateRequest request);
    
    void deleteWallet(Long walletId, Long userId);
    
    WalletResponse setPrimaryWallet(Long walletId, Long userId);
    
    WalletResponse getPrimaryWallet(Long userId);
    
    BalanceResponse getWalletBalance(Long walletId, Long userId);
    
    List<BalanceResponse> getAllUserBalances(Long userId);
    
    BalanceResponse updateBalance(Long walletId, BigDecimal amount, String operation);
    
    TransferResponse transferBetweenWallets(Long fromWalletId, Long toWalletId, 
                                          BigDecimal amount, Long userId);
    
    void syncWalletWithBlockchain(Long walletId, Long userId);
    
    void syncAllUserWalletsWithBlockchain(Long userId);
    
    boolean validateEosAddress(String eosAddress);
    
    WalletResponse importExistingWallet(Long userId, String privateKey, String eosAddress);
    
    String exportWalletPrivateKey(Long walletId, Long userId, String password);
    
    void backupWallet(Long walletId, Long userId);
    
    WalletResponse restoreWallet(Long userId, String backupData, String password);
}