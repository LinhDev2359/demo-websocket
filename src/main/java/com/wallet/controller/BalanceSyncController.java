package com.wallet.controller;

import com.wallet.dto.SyncStatistics;
import com.wallet.service.BalanceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Controller để manage và monitor balance sync operations
 * Admin endpoints để control sync process
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")  // Temporarily disabled for testing
public class BalanceSyncController {
    
    private final BalanceSyncService balanceSyncService;
    
    /**
     * Trigger manual sync cho một wallet
     * 
     * POST /api/admin/sync/wallet/{walletAddress}
     */
    @PostMapping("/wallet/{walletAddress}")
    public ResponseEntity<String> syncWalletBalance(@PathVariable String walletAddress) {
        log.info("Manual sync requested for wallet: {}", walletAddress);
        
        try {
            CompletableFuture<Boolean> result = balanceSyncService.syncWalletBalance(walletAddress);
            
            // Không chờ kết quả, return immediately
            return ResponseEntity.accepted()
                    .body("Balance sync started for wallet: " + walletAddress);
                    
        } catch (Exception e) {
            log.error("Error starting manual sync for wallet: {}", walletAddress, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start sync: " + e.getMessage());
        }
    }
    
    /**
     * Trigger manual sync cho tất cả wallets
     * 
     * POST /api/admin/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<String> syncAllBalances() {
        log.info("Manual sync all balances requested");
        
        try {
            balanceSyncService.syncAllBalances();
            
            return ResponseEntity.accepted()
                    .body("Full balance sync started");
                    
        } catch (Exception e) {
            log.error("Error starting manual sync all", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start full sync: " + e.getMessage());
        }
    }
    
    /**
     * Get sync statistics
     * 
     * GET /api/admin/sync/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<SyncStatistics> getSyncStatistics() {
        try {
            SyncStatistics stats = balanceSyncService.getSyncStatistics();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting sync statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check cho sync service
     * 
     * GET /api/admin/sync/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> getSyncHealth() {
        // Basic health check
        return ResponseEntity.ok("Balance sync service is running");
    }
}