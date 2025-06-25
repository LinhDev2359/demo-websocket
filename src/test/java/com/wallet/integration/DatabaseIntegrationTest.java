package com.wallet.integration;

import com.wallet.entity.*;
import com.wallet.repository.BalanceRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests cho Database operations với TestContainers
 * Test real database interactions và performance
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class DatabaseIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("wallet_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private User testUser;
    private Wallet testWallet;
    private Balance testBalance;

    @BeforeEach
    void setUp() {
        // Cleanup and setup test data
        balanceRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUserId("test-user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed-password");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create test wallet
        testWallet = new Wallet();
        testWallet.setUserId(testUser.getUserId());
        testWallet.setWalletAddress("testwallet123");
        testWallet.setWalletType(WalletType.EOS);
        testWallet.setWalletName("Test Wallet");
        testWallet.setIsPrimary(false);
        testWallet.setStatus(WalletStatus.ACTIVE);
        testWallet.setCreatedAt(LocalDateTime.now());
        testWallet.setUpdatedAt(LocalDateTime.now());
        testWallet = walletRepository.save(testWallet);

        // Create test balance
        testBalance = new Balance();
        testBalance.setWalletId(testWallet.getId());
        testBalance.setTokenType(TokenType.A);
        testBalance.setTokenSymbol("EOS");
        testBalance.setTokenContract("eosio.token");
        testBalance.setBalance(new BigDecimal("100.0000"));
        testBalance.setAvailableBalance(new BigDecimal("90.0000"));
        testBalance.setLockedBalance(new BigDecimal("10.0000"));
        testBalance.setStatus(BalanceStatus.ACTIVE);
        testBalance.setLastUpdated(LocalDateTime.now());
        testBalance = balanceRepository.save(testBalance);
    }

    /**
     * Test: CRUD operations cho User entity
     */
    @Test
    void userCRUDOperations_ShouldWorkCorrectly() {
        // Create
        User newUser = new User();
        newUser.setUserId("new-user-456");
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPasswordHash("new-hashed-password");
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser.getId());
        assertEquals("new-user-456", savedUser.getUserId());

        // Read
        Optional<User> foundUser = userRepository.findByUserId("new-user-456");
        assertTrue(foundUser.isPresent());
        assertEquals("newuser", foundUser.get().getUsername());

        // Update
        foundUser.get().setUsername("updateduser");
        User updatedUser = userRepository.save(foundUser.get());
        assertEquals("updateduser", updatedUser.getUsername());

        // Delete
        userRepository.delete(updatedUser);
        Optional<User> deletedUser = userRepository.findByUserId("new-user-456");
        assertFalse(deletedUser.isPresent());
    }

    /**
     * Test: Wallet repository methods
     */
    @Test
    void walletRepositoryMethods_ShouldWorkCorrectly() {
        // Test findByWalletAddress
        Optional<Wallet> foundWallet = walletRepository.findByWalletAddress("testwallet123");
        assertTrue(foundWallet.isPresent());
        assertEquals(testWallet.getId(), foundWallet.get().getId());

        // Test findByUserId
        List<Wallet> userWallets = walletRepository.findByUserId(testUser.getUserId());
        assertEquals(1, userWallets.size());
        assertEquals("testwallet123", userWallets.get(0).getWalletAddress());

        // Test pagination
        Pageable pageable = PageRequest.of(0, 10);
        Page<Wallet> walletPage = walletRepository.findByUserId(testUser.getUserId(), pageable);
        assertEquals(1, walletPage.getTotalElements());

        // Test countByUserId
        long count = walletRepository.countByUserId(testUser.getUserId());
        assertEquals(1, count);

        // Test countByStatus
        long activeCount = walletRepository.countByStatus(WalletStatus.ACTIVE);
        assertEquals(1, activeCount);
    }

    /**
     * Test: Balance repository methods
     */
    @Test
    void balanceRepositoryMethods_ShouldWorkCorrectly() {
        // Test findByWalletIdAndTokenType
        Optional<Balance> foundBalance = balanceRepository.findByWalletIdAndTokenType(
                testWallet.getId(), TokenType.A);
        assertTrue(foundBalance.isPresent());
        assertEquals(testBalance.getId(), foundBalance.get().getId());

        // Test findByWalletId
        List<Balance> walletBalances = balanceRepository.findByWalletId(testWallet.getId());
        assertEquals(1, walletBalances.size());

        // Test balance operations
        Balance balance = foundBalance.get();
        balance.setBalance(new BigDecimal("150.0000"));
        Balance updatedBalance = balanceRepository.save(balance);
        assertEquals(new BigDecimal("150.0000"), updatedBalance.getBalance());
    }

    /**
     * Test: Complex queries và joins
     */
    @Test
    void complexQueries_ShouldWorkCorrectly() {
        // Test wallet statistics
        Object[] stats = walletRepository.getWalletStatistics(testUser.getUserId());
        assertNotNull(stats);
        assertEquals(1L, stats[0]); // total wallets
        assertEquals(1L, stats[1]); // active wallets

        // Test wallets with balance
        List<Wallet> walletsWithBalance = walletRepository.findWalletsWithBalance(testUser.getUserId());
        assertEquals(1, walletsWithBalance.size());
    }

    /**
     * Test: Transaction isolation và rollback
     */
    @Test
    void transactionHandling_ShouldWorkCorrectly() {
        // Create transaction scope
        long initialCount = walletRepository.count();

        try {
            // Create new wallet
            Wallet newWallet = new Wallet();
            newWallet.setUserId(testUser.getUserId());
            newWallet.setWalletAddress("transactiontest");
            newWallet.setWalletType(WalletType.EOS);
            newWallet.setStatus(WalletStatus.ACTIVE);
            newWallet.setCreatedAt(LocalDateTime.now());
            newWallet.setUpdatedAt(LocalDateTime.now());
            
            walletRepository.save(newWallet);

            // Verify count increased
            assertEquals(initialCount + 1, walletRepository.count());

            // Simulate error to trigger rollback
            if (true) { // Always true to trigger rollback
                throw new RuntimeException("Test rollback");
            }

        } catch (RuntimeException e) {
            // Expected rollback
        }

        // After rollback, count should be back to initial
        // Note: In @Transactional test, rollback happens automatically
        assertEquals(initialCount, walletRepository.count());
    }

    /**
     * Test: Performance với large dataset
     */
    @Test
    void performanceWithLargeDataset_ShouldHandleWell() {
        // Given - Create many wallets
        int walletCount = 100;
        for (int i = 0; i < walletCount; i++) {
            Wallet wallet = new Wallet();
            wallet.setUserId("perf-user-" + (i % 10)); // 10 users with 10 wallets each
            wallet.setWalletAddress("perfwallet" + i);
            wallet.setWalletType(WalletType.EOS);
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setCreatedAt(LocalDateTime.now());
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);
        }

        // When - Query performance test
        long startTime = System.currentTimeMillis();
        
        Pageable pageable = PageRequest.of(0, 50);
        Page<Wallet> results = walletRepository.findByWalletType(WalletType.EOS, pageable);
        
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(50, results.getContent().size());
        assertTrue(results.getTotalElements() >= walletCount);
        
        // Performance assertion
        assertTrue((endTime - startTime) < 1000, 
                  "Query should complete within 1000ms");
    }

    /**
     * Test: Concurrent database access
     */
    @Test
    void concurrentDatabaseAccess_ShouldHandleCorrectly() throws InterruptedException {
        // Given
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        final int[] successCount = {0};

        // When - Create concurrent database operations
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    Wallet wallet = new Wallet();
                    wallet.setUserId("concurrent-user-" + threadId);
                    wallet.setWalletAddress("concurrentwallet" + threadId);
                    wallet.setWalletType(WalletType.EOS);
                    wallet.setStatus(WalletStatus.ACTIVE);
                    wallet.setCreatedAt(LocalDateTime.now());
                    wallet.setUpdatedAt(LocalDateTime.now());
                    
                    walletRepository.save(wallet);
                    
                    synchronized (successCount) {
                        successCount[0]++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertEquals(threadCount, successCount[0]);
        
        // Verify all wallets were created
        long totalWallets = walletRepository.count();
        assertTrue(totalWallets >= threadCount);
    }

    /**
     * Test: Data integrity constraints
     */
    @Test
    void dataIntegrityConstraints_ShouldBeEnforced() {
        // Test unique constraint on wallet address
        assertThrows(Exception.class, () -> {
            Wallet duplicateWallet = new Wallet();
            duplicateWallet.setUserId(testUser.getUserId());
            duplicateWallet.setWalletAddress("testwallet123"); // Duplicate address
            duplicateWallet.setWalletType(WalletType.EOS);
            duplicateWallet.setStatus(WalletStatus.ACTIVE);
            duplicateWallet.setCreatedAt(LocalDateTime.now());
            duplicateWallet.setUpdatedAt(LocalDateTime.now());
            
            walletRepository.save(duplicateWallet);
            walletRepository.flush(); // Force constraint check
        });

        // Test unique constraint on user email
        assertThrows(Exception.class, () -> {
            User duplicateUser = new User();
            duplicateUser.setUserId("duplicate-user");
            duplicateUser.setUsername("duplicateuser");
            duplicateUser.setEmail("test@example.com"); // Duplicate email
            duplicateUser.setPasswordHash("password");
            duplicateUser.setStatus(UserStatus.ACTIVE);
            duplicateUser.setCreatedAt(LocalDateTime.now());
            duplicateUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(duplicateUser);
            userRepository.flush();
        });
    }

    /**
     * Test: Database indexing performance
     */
    @Test
    void databaseIndexing_ShouldImprovePerformance() {
        // Create many wallets for same user
        String userId = "index-test-user";
        for (int i = 0; i < 1000; i++) {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setWalletAddress("indexwallet" + i);
            wallet.setWalletType(WalletType.EOS);
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setCreatedAt(LocalDateTime.now());
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);
        }

        // Test indexed query performance
        long startTime = System.currentTimeMillis();
        
        List<Wallet> userWallets = walletRepository.findByUserId(userId);
        
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(1000, userWallets.size());
        
        // Should be fast due to index on user_id
        assertTrue((endTime - startTime) < 500, 
                  "Indexed query should complete within 500ms");
    }
}