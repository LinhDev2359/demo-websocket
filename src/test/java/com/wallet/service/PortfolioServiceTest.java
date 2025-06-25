package com.wallet.service;

import com.wallet.dto.PortfolioResponse;
import com.wallet.entity.*;
import com.wallet.repository.*;
import com.wallet.service.impl.PortfolioServiceV2Impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests cho PortfolioService
 * Testing Strategy:
 * 1. Test core business logic
 * 2. Test caching behavior
 * 3. Test error scenarios
 * 4. Test performance optimization
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private BalanceRepository balanceRepository;
    
    @Mock
    private UserWalletRepository userWalletRepository;
    
    @Mock
    private WalletBalanceRepository walletBalanceRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PortfolioServiceV2Impl portfolioService;

    private User testUser;
    private Wallet testWallet;
    private Balance testBalance;
    private UserWallet testUserWallet;
    private WalletBalance testWalletBalance;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setUserId("test-user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUserId("test-user-123");
        testWallet.setWalletAddress("test.eos.address");
        testWallet.setWalletType(WalletType.EOS);
        testWallet.setStatus(WalletStatus.ACTIVE);
        testWallet.setIsPrimary(true);
        testWallet.setCreatedAt(LocalDateTime.now());

        testBalance = new Balance();
        testBalance.setId(1L);
        testBalance.setWalletId(1L);
        testBalance.setTokenType(TokenType.A);
        testBalance.setBalance(new BigDecimal("100.50000000"));
        testBalance.setUsdValue(new BigDecimal("150.75"));

        testUserWallet = new UserWallet();
        testUserWallet.setUserId("test-user-123");
        testUserWallet.setWalletId(1L);
        testUserWallet.setWalletType(WalletType.EOS);
        testUserWallet.setIsPrimary(true);
        testUserWallet.setStatus(UserWallet.UserWalletStatus.ACTIVE);

        testWalletBalance = new WalletBalance();
        testWalletBalance.setWalletId(1L);
        testWalletBalance.setBalanceId(1L);
        testWalletBalance.setTokenType(TokenType.A);
        testWalletBalance.setCurrentAmount(new BigDecimal("100.50000000"));
        testWalletBalance.setStatus(WalletBalance.WalletBalanceStatus.ACTIVE);

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getUserPortfolio_WithValidUserId_ShouldReturnPortfolio() {
        // Given
        String userId = "test-user-123";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList(testWallet));
        when(balanceRepository.findByWalletId(testWallet.getId()))
            .thenReturn(Arrays.asList(testBalance));
        when(valueOperations.get(anyString())).thenReturn(null); // Cache miss

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, false);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals(1, result.getTotalWallets());
        assertEquals(1, result.getWallets().size());
        
        PortfolioResponse.WalletPortfolioInfo walletInfo = result.getWallets().get(0);
        assertEquals(testWallet.getId(), walletInfo.getWalletId());
        assertEquals(testWallet.getWalletAddress(), walletInfo.getWalletAddress());
        assertTrue(walletInfo.getIsPrimary());
        
        // Verify cache was called
        verify(valueOperations).set(anyString(), eq(result), anyLong(), any());
    }

    @Test
    void getUserPortfolio_WithCacheHit_ShouldReturnCachedData() {
        // Given
        String userId = "test-user-123";
        PortfolioResponse cachedPortfolio = new PortfolioResponse(userId, "testuser");
        when(valueOperations.get("portfolio:user:" + userId)).thenReturn(cachedPortfolio);

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, false);

        // Then
        assertNotNull(result);
        assertEquals(cachedPortfolio, result);
        
        // Verify database was not called
        verify(userRepository, never()).findByUserId(anyString());
        verify(walletRepository, never()).findByUserIdAndStatus(anyString(), any());
    }

    @Test
    void getUserPortfolio_WithRefreshCache_ShouldSkipCache() {
        // Given
        String userId = "test-user-123";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList(testWallet));
        when(balanceRepository.findByWalletId(testWallet.getId()))
            .thenReturn(Arrays.asList(testBalance));

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, true);

        // Then
        assertNotNull(result);
        
        // Verify cache get was not called (refresh = true)
        verify(valueOperations, never()).get(anyString());
        // Verify cache set was called
        verify(valueOperations).set(anyString(), eq(result), anyLong(), any());
    }

    @Test
    void getUserPortfolio_WithNonExistentUser_ShouldReturnNull() {
        // Given
        String userId = "non-existent-user";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, false);

        // Then
        assertNull(result);
        
        // Verify no cache was set for null result
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getUserPortfolio_WithNoWallets_ShouldReturnEmptyPortfolio() {
        // Given
        String userId = "test-user-123";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList()); // Empty wallet list
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, false);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalWallets());
        assertTrue(result.getWallets().isEmpty());
        assertNotNull(result.getTotalBalance());
        assertEquals(BigDecimal.ZERO, result.getTotalBalance().getTotalEos());
    }

    @Test
    void getUserPortfolio_WithPagination_ShouldReturnPagedResults() {
        // Given
        String userId = "test-user-123";
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(10);
        
        Page<Wallet> walletPage = new PageImpl<>(Arrays.asList(testWallet));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(userId, pageable)).thenReturn(walletPage);
        when(balanceRepository.findByWalletId(testWallet.getId()))
            .thenReturn(Arrays.asList(testBalance));

        // When
        PortfolioResponse result = portfolioService.getUserPortfolio(userId, pageable, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalWallets());
        assertEquals(1, result.getWallets().size());
    }

    @Test
    void getPortfolioSummary_WithValidData_ShouldReturnSummary() {
        // Given
        String userId = "test-user-123";
        when(walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList(testWallet));
        when(balanceRepository.findByWalletIdIn(Arrays.asList(testWallet.getId())))
            .thenReturn(Arrays.asList(testBalance));
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        PortfolioResponse.BalanceSummary result = portfolioService.getPortfolioSummary(userId, false);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("100.50000000"), result.getTotalEos());
        assertEquals(BigDecimal.ZERO, result.getTotalRam());
    }

    @Test
    void hasWallets_WithActiveWallets_ShouldReturnTrue() {
        // Given
        String userId = "test-user-123";
        when(walletRepository.countByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(1L);

        // When
        boolean result = portfolioService.hasWallets(userId);

        // Then
        assertTrue(result);
    }

    @Test
    void hasWallets_WithNoWallets_ShouldReturnFalse() {
        // Given
        String userId = "test-user-123";
        when(walletRepository.countByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(0L);

        // When
        boolean result = portfolioService.hasWallets(userId);

        // Then
        assertFalse(result);
    }

    @Test
    void refreshPortfolioCache_ShouldInvalidateAndReload() {
        // Given
        String userId = "test-user-123";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserIdAndStatus(userId, WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList(testWallet));
        when(balanceRepository.findByWalletId(testWallet.getId()))
            .thenReturn(Arrays.asList(testBalance));

        // When
        portfolioService.refreshPortfolioCache(userId);

        // Then
        verify(redisTemplate, times(2)).delete(anyString()); // portfolio + summary
        verify(valueOperations, times(2)).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getMultipleUserPortfolios_WithValidUsers_ShouldReturnAllPortfolios() {
        // Given
        List<String> userIds = Arrays.asList("user1", "user2");
        User user1 = new User();
        user1.setUserId("user1");
        user1.setUsername("user1");
        
        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findByUserId("user2")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdAndStatus("user1", WalletStatus.ACTIVE))
            .thenReturn(Arrays.asList());
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        List<PortfolioResponse> result = portfolioService.getMultipleUserPortfolios(userIds, false);

        // Then
        assertEquals(1, result.size()); // Only user1 portfolio returned (user2 not found)
        assertEquals("user1", result.get(0).getUserId());
    }
}