package com.wallet.performance;

import com.wallet.service.WalletService;
import com.wallet.service.BalanceSyncService;
import com.wallet.entity.WalletType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive performance test suite
 * Tests system performance under realistic load conditions
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PerformanceTestSuite {

    @Autowired
    private WalletService walletService;

    @Autowired
    private BalanceSyncService balanceSyncService;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(100);
    }

    /**
     * Test wallet creation performance under high load
     * Target: 1000 wallets created within 10 seconds
     */
    @Test
    void testWalletCreationPerformance() throws Exception {
        int totalWallets = 1000;
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = IntStream.range(0, totalWallets)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        String userId = "user-" + i;
                        String walletAddress = "wallet-" + i + ".eos";
                        walletService.createWallet(userId, walletAddress, WalletType.EOS, "Test Wallet " + i);
                    } catch (Exception e) {
                        fail("Wallet creation failed: " + e.getMessage());
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);

        long duration = System.currentTimeMillis() - startTime;
        double walletsPerSecond = (totalWallets * 1000.0) / duration;

        System.out.println("Performance Results:");
        System.out.println("- Total wallets created: " + totalWallets);
        System.out.println("- Duration: " + duration + "ms");
        System.out.println("- Wallets per second: " + walletsPerSecond);

        assertTrue(duration < 10000, "Should create 1000 wallets within 10 seconds");
        assertTrue(walletsPerSecond > 100, "Should achieve >100 wallets/second throughput");
    }

    /**
     * Test concurrent balance sync performance
     * Target: 500 balance syncs within 30 seconds
     */
    @Test
    void testBalanceSyncPerformance() throws Exception {
        int totalSyncs = 500;
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = IntStream.range(0, totalSyncs)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        String walletAddress = "testwallet" + i + ".eos";
                        balanceSyncService.syncWalletBalance(walletAddress);
                    } catch (Exception e) {
                        // Expected for non-existent wallets in test
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(35, TimeUnit.SECONDS);

        long duration = System.currentTimeMillis() - startTime;
        double syncsPerSecond = (totalSyncs * 1000.0) / duration;

        System.out.println("Balance Sync Performance Results:");
        System.out.println("- Total syncs attempted: " + totalSyncs);
        System.out.println("- Duration: " + duration + "ms");
        System.out.println("- Syncs per second: " + syncsPerSecond);

        assertTrue(duration < 30000, "Should complete 500 syncs within 30 seconds");
        assertTrue(syncsPerSecond > 15, "Should achieve >15 syncs/second throughput");
    }

    /**
     * Memory usage optimization test
     * Ensures no memory leaks during high-volume operations
     */
    @Test
    void testMemoryOptimization() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Perform memory-intensive operations
        for (int i = 0; i < 1000; i++) {
            String userId = "memory-test-user-" + i;
            String walletAddress = "memory-test-wallet-" + i + ".eos";
            try {
                walletService.createWallet(userId, walletAddress, WalletType.EOS, "Memory Test");
            } catch (Exception e) {
                // Handle expected validation errors
            }
        }

        // Force garbage collection
        System.gc();
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);

        System.out.println("Memory Usage Analysis:");
        System.out.println("- Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("- Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("- Memory increase: " + memoryIncreaseMB + " MB");

        assertTrue(memoryIncreaseMB < 100, "Memory increase should be less than 100MB for 1000 operations");
    }
}