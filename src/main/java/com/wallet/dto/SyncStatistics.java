package com.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Statistics cho balance sync operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatistics {
    
    private Long totalWallets;
    private LocalDateTime lastSyncTime;
    private Long successfulSyncs;
    private Long failedSyncs;
    private Double averageResponseTime;
    
    /**
     * Calculate success rate
     */
    public double getSuccessRate() {
        long total = successfulSyncs + failedSyncs;
        if (total == 0) return 0.0;
        return (double) successfulSyncs / total * 100.0;
    }
}