package com.eoswallet.facade.impl;

import com.eoswallet.dto.*;
import com.wallet.entity.Wallet;
import com.wallet.entity.Balance;
import com.eoswallet.facade.WalletFacade;
import com.wallet.service.WalletService;
import com.wallet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

@Service
@Transactional
public class WalletFacadeImpl implements WalletFacade {

    @Autowired
    private WalletService walletService;
    
    @Autowired
    private UserService userService;

    @Override
    public WalletCreateResponse createWallet(Long userId, WalletCreateRequest request) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        Wallet wallet = walletService.createWallet(userId.toString(), request.getEosAddress(), 
                                                 request.getName());
        
        return WalletCreateResponse.builder()
                .walletId(wallet.getId())
                .name(wallet.getWalletName())
                .eosAddress(wallet.getWalletAddress())
                .isPrimary(wallet.getIsPrimary())
                .message("Wallet created successfully")
                .build();
    }

    @Override
    public WalletResponse getWallet(Long walletId, Long userId) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        var walletOpt = walletService.getWalletByAddress(walletId.toString());
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found");
        }
        
        Wallet wallet = walletOpt.get();
        return mapToWalletResponse(wallet);
    }

    @Override
    public Page<WalletResponse> getUserWallets(Long userId, Pageable pageable) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        Page<Wallet> wallets = walletService.getUserWallets(userId.toString(), pageable);
        return wallets.map(this::mapToWalletResponse);
    }

    @Override
    public WalletResponse updateWallet(Long walletId, Long userId, WalletUpdateRequest request) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        var walletOpt = walletService.getWalletByAddress(walletId.toString());
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found");
        }
        
        Wallet wallet = walletService.updateWalletName(userId.toString(), 
                                                      walletOpt.get().getWalletAddress(), 
                                                      request.getName());
        return mapToWalletResponse(wallet);
    }

    @Override
    public void deleteWallet(Long walletId, Long userId) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        var walletOpt = walletService.getWalletByAddress(walletId.toString());
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found");
        }
        
        walletService.deleteWallet(userId.toString(), walletOpt.get().getWalletAddress());
    }

    @Override
    public WalletResponse setPrimaryWallet(Long walletId, Long userId) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        var walletOpt = walletService.getWalletByAddress(walletId.toString());
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet not found");
        }
        
        Wallet wallet = walletService.setPrimaryWallet(userId.toString(), 
                                                      walletOpt.get().getWalletAddress());
        return mapToWalletResponse(wallet);
    }

    @Override
    public WalletResponse getPrimaryWallet(Long userId) {
        var userOpt = userService.getUserById(userId.toString());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        var walletOpt = walletService.getPrimaryWallet(userId.toString());
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Primary wallet not found");
        }
        
        return mapToWalletResponse(walletOpt.get());
    }

    @Override
    public BalanceResponse getWalletBalance(Long walletId, Long userId) {
        // TODO: Implement balance service when available
        return BalanceResponse.builder()
                .balanceId(1L)
                .walletId(walletId)
                .amount(BigDecimal.ZERO)
                .currency("EOS")
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public List<BalanceResponse> getAllUserBalances(Long userId) {
        // TODO: Implement balance service when available
        return Collections.emptyList();
    }

    @Override
    public BalanceResponse updateBalance(Long walletId, BigDecimal amount, String operation) {
        // TODO: Implement balance service when available
        throw new RuntimeException("Balance operations not implemented yet");
    }

    @Override
    public TransferResponse transferBetweenWallets(Long fromWalletId, Long toWalletId, 
                                                 BigDecimal amount, Long userId) {
        // TODO: Implement transfer functionality
        throw new RuntimeException("Transfer functionality not implemented yet");
    }

    @Override
    public void syncWalletWithBlockchain(Long walletId, Long userId) {
        // TODO: Implement blockchain sync
        throw new RuntimeException("Blockchain sync not implemented yet");
    }

    @Override
    public void syncAllUserWalletsWithBlockchain(Long userId) {
        // TODO: Implement blockchain sync
        throw new RuntimeException("Blockchain sync not implemented yet");
    }

    @Override
    public boolean validateEosAddress(String eosAddress) {
        return walletService.isValidEOSAddress(eosAddress);
    }

    @Override
    public WalletResponse importExistingWallet(Long userId, String privateKey, String eosAddress) {
        // TODO: Implement wallet import
        throw new RuntimeException("Wallet import not implemented yet");
    }

    @Override
    public String exportWalletPrivateKey(Long walletId, Long userId, String password) {
        // TODO: Implement wallet export
        throw new RuntimeException("Wallet export not implemented yet");
    }

    @Override
    public void backupWallet(Long walletId, Long userId) {
        // TODO: Implement wallet backup
        throw new RuntimeException("Wallet backup not implemented yet");
    }

    @Override
    public WalletResponse restoreWallet(Long userId, String backupData, String password) {
        // TODO: Implement wallet restore
        throw new RuntimeException("Wallet restore not implemented yet");
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .name(wallet.getWalletName())
                .eosAddress(wallet.getWalletAddress())
                .isPrimary(wallet.getIsPrimary())
                .isActive(wallet.getStatus() == Wallet.WalletStatus.ACTIVE)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}