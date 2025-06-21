package com.eoswallet.controller;

import com.eoswallet.dto.*;
import com.eoswallet.facade.WalletFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletFacade walletFacade;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletCreateResponse> createWallet(
            @RequestParam Long userId,
            @Valid @RequestBody WalletCreateRequest request) {
        log.info("Create wallet request for userId: {}", userId);
        
        WalletCreateResponse response = walletFacade.createWallet(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.debug("Get wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.getWallet(walletId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<Page<WalletResponse>> getUserWallets(
            @PathVariable Long userId,
            Pageable pageable) {
        log.debug("Get user wallets request for userId: {}", userId);
        
        Page<WalletResponse> response = walletFacade.getUserWallets(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable Long walletId,
            @RequestParam Long userId,
            @Valid @RequestBody WalletUpdateRequest request) {
        log.info("Update wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.updateWallet(walletId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteWallet(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.info("Delete wallet request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.deleteWallet(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{walletId}/set-primary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> setPrimaryWallet(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.info("Set primary wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.setPrimaryWallet(walletId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/primary")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<WalletResponse> getPrimaryWallet(@PathVariable Long userId) {
        log.debug("Get primary wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.getPrimaryWallet(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BalanceResponse> getWalletBalance(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.debug("Get wallet balance request for walletId: {}, userId: {}", walletId, userId);
        
        BalanceResponse response = walletFacade.getWalletBalance(walletId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/balances")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<List<BalanceResponse>> getAllUserBalances(@PathVariable Long userId) {
        log.debug("Get all user balances request for userId: {}", userId);
        
        List<BalanceResponse> response = walletFacade.getAllUserBalances(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transferBetweenWallets(
            @Valid @RequestBody TransferRequest request,
            @RequestParam Long userId) {
        log.info("Transfer between wallets request for userId: {}", userId);
        
        TransferResponse response = walletFacade.transferBetweenWallets(
            request.getFromWalletId(), 
            request.getToWalletId(), 
            request.getAmount(), 
            userId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/sync")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> syncWalletWithBlockchain(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.info("Sync wallet with blockchain request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.syncWalletWithBlockchain(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}/sync-all")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<Void> syncAllUserWalletsWithBlockchain(@PathVariable Long userId) {
        log.info("Sync all user wallets with blockchain request for userId: {}", userId);
        
        walletFacade.syncAllUserWalletsWithBlockchain(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-address")
    public ResponseEntity<Boolean> validateEosAddress(@RequestParam String eosAddress) {
        log.debug("Validate EOS address request for address: {}", eosAddress);
        
        boolean isValid = walletFacade.validateEosAddress(eosAddress);
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> importExistingWallet(
            @RequestParam Long userId,
            @RequestParam String privateKey,
            @RequestParam String eosAddress) {
        log.info("Import existing wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.importExistingWallet(userId, privateKey, eosAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/export")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> exportWalletPrivateKey(
            @PathVariable Long walletId,
            @RequestParam Long userId,
            @RequestParam String password) {
        log.info("Export wallet private key request for walletId: {}, userId: {}", walletId, userId);
        
        String privateKey = walletFacade.exportWalletPrivateKey(walletId, userId, password);
        return ResponseEntity.ok(privateKey);
    }

    @PostMapping("/{walletId}/backup")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> backupWallet(
            @PathVariable Long walletId,
            @RequestParam Long userId) {
        log.info("Backup wallet request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.backupWallet(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> restoreWallet(
            @RequestParam Long userId,
            @RequestParam String backupData,
            @RequestParam String password) {
        log.info("Restore wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.restoreWallet(userId, backupData, password);
        return ResponseEntity.ok(response);
    }
}