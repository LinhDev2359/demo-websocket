package com.wallet.service;

import com.wallet.dto.HyperionTokenResponse;
import com.wallet.dto.TokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service để tích hợp với Hyperion API
 * 
 * Chức năng:
 * 1. Lấy danh sách tokens của EOS account
 * 2. Error handling và retry logic
 * 3. Response transformation
 * 4. Caching (future enhancement)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HyperionService {
    
    private final RestTemplate restTemplate;
    
    @Value("${hyperion.api.url:https://eos.hyperion.eosrio.io}")
    private String hyperionApiUrl;
    
    @Value("${hyperion.api.timeout:15000}")
    private int timeoutMs;
    
    @Value("${hyperion.api.max-retries:3}")
    private int maxRetries;
    
    /**
     * Lấy danh sách tokens của một EOS account
     * 
     * @param eosAccount EOS account name (e.g. "eosaccount1")
     * @return List of TokenInfo
     */
    public List<TokenInfo> getAccountTokens(String eosAccount) {
        log.info("Getting tokens for EOS account: {}", eosAccount);
        
        if (eosAccount == null || eosAccount.trim().isEmpty()) {
            log.warn("EOS account is null or empty");
            return Collections.emptyList();
        }
        
        return getAccountTokensWithRetry(eosAccount.trim(), maxRetries);
    }
    
    /**
     * Async version của getAccountTokens
     * 
     * @param eosAccount EOS account name
     * @return CompletableFuture with list of TokenInfo
     */
    public CompletableFuture<List<TokenInfo>> getAccountTokensAsync(String eosAccount) {
        return CompletableFuture.supplyAsync(() -> getAccountTokens(eosAccount));
    }
    
    /**
     * Internal method với retry logic
     * 
     * @param eosAccount EOS account name
     * @param attemptsLeft số lần retry còn lại
     * @return List of TokenInfo
     */
    private List<TokenInfo> getAccountTokensWithRetry(String eosAccount, int attemptsLeft) {
        LocalDateTime startTime = LocalDateTime.now();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Attempting to get tokens for account: {} (attempt {}/{})", 
                        eosAccount, attempt, maxRetries);
                
                return executeHyperionApiCall(eosAccount, startTime);
                
            } catch (RestClientException e) {
                if (attempt == maxRetries) {
                    // Last attempt failed
                    long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
                    log.error("All {} attempts failed for account: {}, final error: {}", 
                            maxRetries, eosAccount, e.getMessage());
                    
                    return Collections.emptyList();
                } else {
                    // Retry with exponential backoff
                    long waitTime = (long) (1000 * Math.pow(2, attempt - 1));
                    log.warn("Attempt {} failed for account: {}, retrying in {}ms: {}", 
                            attempt, eosAccount, waitTime, e.getMessage());
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted during retry for account: {}", eosAccount);
                        return Collections.emptyList();
                    }
                }
            } catch (Exception e) {
                // Non-retryable error
                long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
                log.error("Non-retryable error for account: {}: {}", eosAccount, e.getMessage());
                return Collections.emptyList();
            }
        }
        
        // Should not reach here
        return Collections.emptyList();
    }
    
    /**
     * Execute actual Hyperion API call
     * 
     * @param eosAccount EOS account name
     * @param startTime request start time
     * @return List of TokenInfo
     * @throws RestClientException if API call fails
     */
    private List<TokenInfo> executeHyperionApiCall(String eosAccount, LocalDateTime startTime) 
            throws RestClientException {
        
        // Build URL
        String url = hyperionApiUrl + "/v2/state/get_tokens?account=" + eosAccount;
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "WalletSystem/1.0");
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // Call Hyperion API
        log.debug("Calling Hyperion API: {}", url);
        
        ResponseEntity<List<HyperionTokenResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<HyperionTokenResponse>>() {}
        );
        
        // Calculate response time
        long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        
        // Process response
        List<HyperionTokenResponse> hyperionTokens = response.getBody();
        if (hyperionTokens == null || hyperionTokens.isEmpty()) {
            log.info("No tokens found for account: {}, response time: {}ms", eosAccount, responseTimeMs);
            return Collections.emptyList();
        }
        
        // Transform to internal format
        List<TokenInfo> tokens = hyperionTokens.stream()
                .filter(token -> token != null && token.getSymbol() != null)
                .map(TokenInfo::fromHyperionResponse)
                .filter(token -> token != null)
                .toList();
        
        log.info("Successfully retrieved {} tokens for account: {}, response time: {}ms", 
                tokens.size(), eosAccount, responseTimeMs);
        
        return tokens;
    }
    
    /**
     * Check Hyperion API health
     * 
     * @return true if API is healthy
     */
    public boolean isHyperionApiHealthy() {
        try {
            String url = hyperionApiUrl + "/v2/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            log.debug("Hyperion API health check: {}", isHealthy ? "HEALTHY" : "UNHEALTHY");
            
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("Hyperion API health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get Hyperion API info
     * 
     * @return API info as string
     */
    public String getHyperionApiInfo() {
        try {
            String url = hyperionApiUrl + "/v2/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            
        } catch (Exception e) {
            log.error("Error getting Hyperion API info: {}", e.getMessage());
        }
        
        return "Hyperion API info unavailable";
    }
    
    /**
     * Validate EOS account name format
     * 
     * @param eosAccount account name to validate
     * @return true if valid EOS account format
     */
    public boolean isValidEosAccount(String eosAccount) {
        if (eosAccount == null || eosAccount.trim().isEmpty()) {
            return false;
        }
        
        String account = eosAccount.trim();
        
        // EOS account names are 12 characters, lowercase a-z and 1-5
        return account.matches("^[a-z1-5]{12}$");
    }
    
    /**
     * Get tokens for multiple accounts (parallel processing)
     * 
     * @param eosAccounts list of EOS account names
     * @return List of CompletableFuture for each account
     */
    public List<CompletableFuture<List<TokenInfo>>> getMultipleAccountTokens(List<String> eosAccounts) {
        log.info("Getting tokens for {} accounts", eosAccounts.size());
        
        return eosAccounts.stream()
                .filter(this::isValidEosAccount)
                .map(this::getAccountTokensAsync)
                .toList();
    }
}