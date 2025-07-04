package com.eoswallet.controller;

import com.eoswallet.dto.*;
import com.eoswallet.facade.WalletFacade;
import com.wallet.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wallet REST Controller (No Swagger Dependencies)
 * Comprehensive REST API for EOS wallet management
 * 
 * Features:
 * 1. Complete CRUD operations for wallets
 * 2. Input validation with Bean Validation
 * 3. Security with JWT authentication
 * 4. Error handling with proper HTTP status codes
 * 5. Pagination support for large datasets
 * 
 * Note: This version doesn't include Swagger annotations to avoid import issues.
 * After adding springdoc-openapi dependency, you can use the full version with Swagger.
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WalletController {

    private final WalletFacade walletFacade;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * Create new EOS wallet
     * Creates a new EOS wallet for the authenticated user
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletCreateResponse> createWallet(
            HttpServletRequest httpRequest,
            @Valid @RequestBody WalletCreateRequest request) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Create wallet request for userId: {}", userId);
        
        WalletCreateResponse response = walletFacade.createWallet(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get wallet by ID
     * Retrieves wallet information by wallet ID for authenticated user
     */
    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.debug("Get wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.getWallet(walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get my wallets with pagination
     * Retrieves all wallets for authenticated user with pagination support
     */
    @GetMapping("/my-wallets")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<WalletResponse>> getMyWallets(
            HttpServletRequest httpRequest,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.debug("Get user wallets request for userId: {}", userId);
        
        Page<WalletResponse> response = walletFacade.getUserWallets(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Update wallet information
     * Updates wallet name, description and status for authenticated user
     */
    @PutMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody WalletUpdateRequest request) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Update wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.updateWallet(walletId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete wallet
     * Soft deletes a wallet (cannot delete primary wallet)
     */
    @DeleteMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteWallet(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Delete wallet request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.deleteWallet(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set primary wallet
     * Sets the specified wallet as the user's primary wallet
     */
    @PostMapping("/{walletId}/set-primary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> setPrimaryWallet(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Set primary wallet request for walletId: {}, userId: {}", walletId, userId);
        
        WalletResponse response = walletFacade.setPrimaryWallet(walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get primary wallet
     * Retrieves the user's primary wallet
     */
    @GetMapping("/my-primary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> getMyPrimaryWallet(
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.debug("Get primary wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.getPrimaryWallet(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get wallet balance
     * Retrieves balance information for a specific wallet
     */
    @GetMapping("/{walletId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BalanceResponse> getWalletBalance(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.debug("Get wallet balance request for walletId: {}, userId: {}", walletId, userId);
        
        BalanceResponse response = walletFacade.getWalletBalance(walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all user balances
     * Retrieves balance information for all user wallets
     */
    @GetMapping("/my-balances")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BalanceResponse>> getMyBalances(
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.debug("Get all user balances request for userId: {}", userId);
        
        List<BalanceResponse> response = walletFacade.getAllUserBalances(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transfer between wallets
     * Transfers funds between user's wallets
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transferBetweenWallets(
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Transfer between wallets request for userId: {}", userId);
        
        TransferResponse response = walletFacade.transferBetweenWallets(
            request.getFromWalletId(), 
            request.getToWalletId(), 
            request.getAmount(), 
            userId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Sync wallet with blockchain
     * Synchronizes wallet balance with EOS blockchain
     */
    @PostMapping("/{walletId}/sync")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> syncWalletWithBlockchain(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Sync wallet with blockchain request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.syncWalletWithBlockchain(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sync all user wallets
     * Synchronizes all user wallets with EOS blockchain
     */
    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> syncAllMyWalletsWithBlockchain(
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Sync all user wallets with blockchain request for userId: {}", userId);
        
        walletFacade.syncAllUserWalletsWithBlockchain(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validate EOS address
     * Validates if the provided EOS address format is correct
     */
    @PostMapping("/validate-address")
    public ResponseEntity<Boolean> validateEosAddress(
            @RequestParam @NotNull String eosAddress) {
        log.debug("Validate EOS address request for address: {}", eosAddress);
        
        boolean isValid = walletFacade.validateEosAddress(eosAddress);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Import existing wallet
     * Imports an existing EOS wallet using private key
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> importExistingWallet(
            HttpServletRequest httpRequest,
            @RequestParam @NotNull String privateKey,
            @RequestParam @NotNull String eosAddress) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Import existing wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.importExistingWallet(userId, privateKey, eosAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Export wallet private key
     * Exports wallet private key (requires password verification)
     */
    @PostMapping("/{walletId}/export")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> exportWalletPrivateKey(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest,
            @RequestParam @NotNull String password) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Export wallet private key request for walletId: {}, userId: {}", walletId, userId);
        
        String privateKey = walletFacade.exportWalletPrivateKey(walletId, userId, password);
        return ResponseEntity.ok(privateKey);
    }

    /**
     * Backup wallet
     * Creates a secure backup of the wallet
     */
    @PostMapping("/{walletId}/backup")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> backupWallet(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Backup wallet request for walletId: {}, userId: {}", walletId, userId);
        
        walletFacade.backupWallet(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore wallet from backup
     * Restores a wallet from backup data
     */
    @PostMapping("/restore")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> restoreWallet(
            HttpServletRequest httpRequest,
            @RequestParam @NotNull String backupData,
            @RequestParam @NotNull String password) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Restore wallet request for userId: {}", userId);
        
        WalletResponse response = walletFacade.restoreWallet(userId, backupData, password);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method để extract userId từ JWT token trong request
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtTokenUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("No valid JWT token found");
    }

    /**
     * Get all tokens for a specific wallet
     * Retrieves all fungible tokens from Hyperion API
     */
    @GetMapping("/{walletId}/tokens")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<com.wallet.dto.WalletTokensResponse> getWalletTokens(
            @PathVariable @NotNull Long walletId,
            HttpServletRequest httpRequest) {
        String userId = extractUserIdFromRequest(httpRequest);
        log.info("Get wallet tokens request for walletId: {}, userId: {}", walletId, userId);
        
        com.wallet.dto.WalletTokensResponse response = walletFacade.getWalletTokens(walletId, userId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
