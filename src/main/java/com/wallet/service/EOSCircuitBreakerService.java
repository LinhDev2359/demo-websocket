package com.wallet.service;

import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service với Circuit Breaker pattern cho EOS API calls
 * Implement fallback mechanisms và monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EOSCircuitBreakerService {
    
    private final EOSClientService eosClientService;
    
    /**
     * Get wallet balance với Circuit Breaker protection
     * Fallback to cached value hoặc default value khi EOS API fail
     */
    @CircuitBreaker(name = "eosApi", fallbackMethod = "getWalletBalanceFallback")
    @Retry(name = "eosApi")
    @TimeLimiter(name = "eosApi")
    @Cacheable(value = "eosBalance", key = "#request.walletAddress + '_' + #request.tokenSymbol")
    public CompletableFuture<EOSBalanceResponse> getWalletBalanceWithCircuitBreaker(EOSBalanceRequest request) {
        log.debug("Getting balance for wallet: {} with circuit breaker protection", request.getWalletAddress());
        
        return CompletableFuture.supplyAsync(() -> {
            EOSBalanceResponse response = eosClientService.getWalletBalance(request);
            
            if (!response.isSuccess()) {
                throw new EOSApiException("EOS API call failed: " + response.getErrorMessage());
            }
            
            return response;
        });
    }
    
    /**
     * Fallback method khi Circuit Breaker mở hoặc EOS API fail
     */
    public CompletableFuture<EOSBalanceResponse> getWalletBalanceFallback(EOSBalanceRequest request, Exception ex) {
        log.warn("EOS API fallback triggered for wallet: {}, reason: {}", 
                request.getWalletAddress(), ex.getMessage());
        
        // Thử lấy từ cache hoặc database
        EOSBalanceResponse cachedResponse = getCachedBalance(request);
        if (cachedResponse != null) {
            log.info("Using cached balance for wallet: {}", request.getWalletAddress());
            return CompletableFuture.completedFuture(cachedResponse);
        }
        
        // Fallback to default response
        EOSBalanceResponse fallbackResponse = EOSBalanceResponse.builder()
                .walletAddress(request.getWalletAddress())
                .tokenContract(request.getTokenContract())
                .tokenSymbol(request.getTokenSymbol())
                .balance(BigDecimal.ZERO)
                .rawBalance("0.0000 " + request.getTokenSymbol())
                .success(false)
                .errorMessage("EOS API unavailable - using fallback")
                .timestamp(LocalDateTime.now())
                .build();
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    /**
     * Get multiple wallet balances với Circuit Breaker
     */
    @CircuitBreaker(name = "eosApi", fallbackMethod = "getMultipleWalletBalancesFallback")
    @Retry(name = "eosApi")
    public List<EOSBalanceResponse> getMultipleWalletBalancesWithCircuitBreaker(List<EOSBalanceRequest> requests) {
        log.info("Getting balances for {} wallets with circuit breaker protection", requests.size());
        
        return eosClientService.getMultipleWalletBalances(requests);
    }
    
    /**
     * Fallback cho multiple wallet balances
     */
    public List<EOSBalanceResponse> getMultipleWalletBalancesFallback(List<EOSBalanceRequest> requests, Exception ex) {
        log.warn("Multiple wallets fallback triggered for {} wallets, reason: {}", 
                requests.size(), ex.getMessage());
        
        return requests.stream()
                .map(request -> getWalletBalanceFallback(request, ex).join())
                .toList();
    }
    
    /**
     * Check EOS API health với Circuit Breaker
     */
    @CircuitBreaker(name = "eosApiHealth", fallbackMethod = "isEOSApiHealthyFallback")
    public boolean isEOSApiHealthyWithCircuitBreaker() {
        return eosClientService.isEOSApiHealthy();
    }
    
    /**
     * Fallback cho health check
     */
    public boolean isEOSApiHealthyFallback(Exception ex) {
        log.warn("EOS API health check fallback triggered: {}", ex.getMessage());
        return false;
    }
    
    /**
     * Lấy cached balance từ database hoặc Redis
     * TODO: Implement actual cache lookup from database
     */
    private EOSBalanceResponse getCachedBalance(EOSBalanceRequest request) {
        // Placeholder - trong thực tế sẽ query từ database
        // để lấy balance mới nhất đã sync
        return null;
    }
    
    /**
     * Custom exception cho EOS API errors
     */
    public static class EOSApiException extends RuntimeException {
        public EOSApiException(String message) {
            super(message);
        }
        
        public EOSApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}