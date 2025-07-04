package com.wallet.service;

import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import com.wallet.dto.SyncStatistics;
import com.wallet.entity.*;
import com.wallet.repository.BalanceRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho BalanceSyncService
 * Test coverage cho sync operations và performance
 */
@ExtendWith(MockitoExtension.class)
class BalanceSyncServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private EOSCircuitBreakerService eosCircuitBreakerService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BalanceSyncService balanceSyncService;

    private Wallet testWallet;
    private Balance testBalance;
    private EOSBalanceResponse successResponse;
    private EOSBalanceResponse errorResponse;

    @BeforeEach
    void setUp() {
        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUserId("test-user-123");
        testWallet.setWalletAddress("testwalleteos");
        testWallet.setWalletType(WalletType.EOS);
        testWallet.setStatus(WalletStatus.ACTIVE);

        testBalance = new Balance();
        testBalance.setId(1L);
        testBalance.setWalletId(1L);
        testBalance.setTokenType(TokenType.A);
        testBalance.setBalance(BigDecimal.valueOf(10.0));
        testBalance.setStatus(BalanceStatus.ACTIVE);
        testBalance.setLastUpdated(LocalDateTime.now());

        successResponse = EOSBalanceResponse.success(
            "testwalleteos",
            "eosio.token",
            "EOS",
            "15.0000 EOS",
            100L
        );

        errorResponse = EOSBalanceResponse.error(
            "testwalleteos",
            "API Error",
            200L
        );
    }

    /**
     * Test: Individual wallet sync thành công
     */
    @Test
    void syncWalletBalance_WithValidWallet_ShouldUpdateBalance() {
        // Given
        when(walletRepository.findByWalletAddress("testwalleteos"))
            .thenReturn(Optional.of(testWallet));
        when(eosCircuitBreakerService.getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        CompletableFuture<Boolean> result = balanceSyncService.syncWalletBalance("testwalleteos");
        Boolean success = result.join();

        // Then
        assertTrue(success);

        verify(walletRepository).findByWalletAddress("testwalleteos");
        verify(eosCircuitBreakerService).getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class));
        verify(balanceRepository).findByWalletIdAndTokenType(1L, TokenType.A);
        verify(balanceRepository).save(any(Balance.class));
        verify(messagingTemplate).convertAndSend(anyString(), any());
    }

    /**
     * Test: Sync wallet không tồn tại
     */
    @Test
    void syncWalletBalance_WithNonExistentWallet_ShouldReturnFalse() {
        // Given
        when(walletRepository.findByWalletAddress("nonexistent"))
            .thenReturn(Optional.empty());

        // When
        CompletableFuture<Boolean> result = balanceSyncService.syncWalletBalance("nonexistent");
        Boolean success = result.join();

        // Then
        assertFalse(success);

        verify(walletRepository).findByWalletAddress("nonexistent");
        verify(eosCircuitBreakerService, never()).getWalletBalanceWithCircuitBreaker(any());
    }

    /**
     * Test: Sync với EOS API error
     */
    @Test
    void syncWalletBalance_WithEOSApiError_ShouldReturnFalse() {
        // Given
        when(walletRepository.findByWalletAddress("testwalleteos"))
            .thenReturn(Optional.of(testWallet));
        when(eosCircuitBreakerService.getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(errorResponse));

        // When
        CompletableFuture<Boolean> result = balanceSyncService.syncWalletBalance("testwalleteos");
        Boolean success = result.join();

        // Then
        assertFalse(success);

        verify(walletRepository).findByWalletAddress("testwalleteos");
        verify(eosCircuitBreakerService).getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class));
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    /**
     * Test: Update balance với balance mới
     */
    @Test
    void updateWalletBalance_WithNewBalance_ShouldUpdateDatabase() {
        // Given
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        balanceSyncService.updateWalletBalance(testWallet, successResponse);

        // Then
        verify(balanceRepository).findByWalletIdAndTokenType(1L, TokenType.A);
        verify(balanceRepository).save(argThat(balance -> 
            balance.getBalance().equals(new BigDecimal("15.0000"))
        ));
    }

    /**
     * Test: Update balance không thay đổi
     */
    @Test
    void updateWalletBalance_WithSameBalance_ShouldNotUpdate() {
        // Given
        testBalance.setBalance(new BigDecimal("15.0000")); // Same as response
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));

        // When
        balanceSyncService.updateWalletBalance(testWallet, successResponse);

        // Then
        verify(balanceRepository).findByWalletIdAndTokenType(1L, TokenType.A);
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    /**
     * Test: Tạo balance mới cho wallet
     */
    @Test
    void updateWalletBalance_WithNewWallet_ShouldCreateBalance() {
        // Given
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        balanceSyncService.updateWalletBalance(testWallet, successResponse);

        // Then
        verify(balanceRepository).findByWalletIdAndTokenType(1L, TokenType.A);
        verify(balanceRepository).save(argThat(balance -> 
            balance.getWalletId().equals(1L) &&
            balance.getTokenType().equals(TokenType.A) &&
            balance.getBalance().equals(new BigDecimal("15.0000"))
        ));
    }

    /**
     * Test: Sync all balances với pagination
     */
    @Test
    void syncAllBalances_WithMultipleWallets_ShouldProcessInBatches() {
        // Given
        List<Wallet> wallets = Arrays.asList(testWallet);
        Page<Wallet> walletPage = new PageImpl<>(wallets);

        when(walletRepository.findAllActiveWallets(any(Pageable.class)))
            .thenReturn(walletPage)
            .thenReturn(Page.empty()); // Second call returns empty to stop pagination

        when(eosCircuitBreakerService.getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));

        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        balanceSyncService.syncAllBalances();

        // Then
        verify(walletRepository, atLeastOnce()).findAllActiveWallets(any(Pageable.class));
        verify(eosCircuitBreakerService).getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class));
        verify(balanceRepository).save(any(Balance.class));
    }

    /**
     * Test: Get sync statistics
     */
    @Test
    void getSyncStatistics_ShouldReturnCorrectStats() {
        // Given
        when(walletRepository.countByStatus(WalletStatus.ACTIVE)).thenReturn(100L);
        when(balanceRepository.count()).thenReturn(500L);

        // When
        SyncStatistics stats = balanceSyncService.getSyncStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalWallets());
        assertEquals(500L, stats.getSuccessfulSyncs());
        assertEquals(0L, stats.getFailedSyncs());
        assertNotNull(stats.getLastSyncTime());

        verify(walletRepository).countByStatus(WalletStatus.ACTIVE);
        verify(balanceRepository).count();
    }

    /**
     * Test: Error handling trong update balance
     */
    @Test
    void updateWalletBalance_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            balanceSyncService.updateWalletBalance(testWallet, successResponse);
        });

        verify(balanceRepository).findByWalletIdAndTokenType(1L, TokenType.A);
    }

    /**
     * Test: WebSocket message sending
     */
    @Test
    void updateWalletBalance_ShouldSendWebSocketUpdate() {
        // Given
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        balanceSyncService.updateWalletBalance(testWallet, successResponse);

        // Then
        verify(messagingTemplate).convertAndSend(
            eq("/topic/balance/" + testWallet.getUserId()),
            argThat(message -> message.toString().contains("testwalleteos"))
        );
    }

    /**
     * Test: Performance với large dataset
     */
    @Test
    void syncAllBalances_WithLargeDataset_ShouldPerformWell() {
        // Given
        List<Wallet> manyWallets = Arrays.asList(
            testWallet, testWallet, testWallet, testWallet, testWallet
        );
        Page<Wallet> walletPage = new PageImpl<>(manyWallets);

        when(walletRepository.findAllActiveWallets(any(Pageable.class)))
            .thenReturn(walletPage)
            .thenReturn(Page.empty());

        when(eosCircuitBreakerService.getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));

        when(balanceRepository.findByWalletIdAndTokenType(anyLong(), eq(TokenType.A)))
            .thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class)))
            .thenReturn(testBalance);

        // When
        long startTime = System.currentTimeMillis();
        balanceSyncService.syncAllBalances();
        long endTime = System.currentTimeMillis();

        // Then
        // Performance assertion - should complete within 5000ms for 5 wallets
        assertTrue((endTime - startTime) < 5000, "Sync should complete within 5000ms");

        verify(eosCircuitBreakerService, times(5))
            .getWalletBalanceWithCircuitBreaker(any(EOSBalanceRequest.class));
    }

    /**
     * Test: Null safety trong WebSocket message
     */
    @Test
    void updateWalletBalance_WithNullFields_ShouldHandleGracefully() {
        // Given
        testWallet.setUserId(null); // Null user ID
        when(balanceRepository.findByWalletIdAndTokenType(1L, TokenType.A))
            .thenReturn(Optional.of(testBalance));

        // When & Then
        assertDoesNotThrow(() -> {
            balanceSyncService.updateWalletBalance(testWallet, successResponse);
        });

        // Should not send WebSocket message due to validation
        verify(messagingTemplate, never()).convertAndSend(anyString(), any());
    }
}