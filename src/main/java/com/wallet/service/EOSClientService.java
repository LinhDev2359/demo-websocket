package com.wallet.service;

import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service để gọi EOS blockchain API
 * Implement với timeout, retry mechanism và error handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EOSClientService {
    
    private final RestTemplate restTemplate;
    
    @Value("${eos.api.url:https://api.eosn.io}")
    private String eosApiUrl;
    
    @Value("#{'${eos.api.urls:https://eos.greymass.com,https://api.eossweden.org,https://mainnet.eos.dfuse.io,https://api.eosn.io}'.split(',')}")
    private List<String> eosApiUrls;
    
    // Testnet configuration
    @Value("${eos.api.testnet.url:https://jungle4.greymass.com}")
    private String eosTestnetApiUrl;
    
    @Value("#{'${eos.api.testnet.urls:https://jungle4.greymass.com,https://jungle4.cryptolions.io,https://jungle4.api.eosnation.io}'.split(',')}")
    private List<String> eosTestnetApiUrls;
    
    @Value("${eos.api.network:testnet}")
    private String eosNetwork;
    
    @Value("${eos.api.failover-enabled:true}")
    private boolean failoverEnabled;
    
    @Value("${eos.api.timeout:30000}")
    private int timeoutMs;
    
    @Value("${eos.api.max-retries:3}")
    private int maxRetries;
    
    /**
     * Lấy balance của một wallet từ EOS blockchain
     * Sử dụng EOS API endpoint: /v1/chain/get_currency_balance
     * Manual retry implementation thay vì Spring Retry
     */
    public EOSBalanceResponse getWalletBalance(EOSBalanceRequest request) {
        if (failoverEnabled) {
            return getWalletBalanceWithFailover(request);
        } else {
            String apiUrl = getSelectedApiUrl();
            return getWalletBalanceWithRetry(request, maxRetries, apiUrl);
        }
    }
    
    /**
     * Get API URL based on network configuration
     */
    private String getSelectedApiUrl() {
        if ("testnet".equalsIgnoreCase(eosNetwork)) {
            log.info("Using EOS testnet: {}", eosTestnetApiUrl);
            return eosTestnetApiUrl;
        } else {
            log.info("Using EOS mainnet: {}", eosApiUrl);
            return eosApiUrl;
        }
    }
    
    /**
     * Get API URLs list based on network configuration
     */
    private List<String> getSelectedApiUrls() {
        if ("testnet".equalsIgnoreCase(eosNetwork)) {
            log.info("Using EOS testnet endpoints: {}", eosTestnetApiUrls);
            return eosTestnetApiUrls;
        } else {
            log.info("Using EOS mainnet endpoints: {}", eosApiUrls);
            return eosApiUrls;
        }
    }
    
    /**
     * Attempt to get balance with failover across multiple EOS endpoints
     */
    private EOSBalanceResponse getWalletBalanceWithFailover(EOSBalanceRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        List<String> selectedApiUrls = getSelectedApiUrls();
        
        for (String apiUrl : selectedApiUrls) {
            try {
                log.info("Trying EOS API endpoint: {} for wallet: {}", apiUrl, request.getWalletAddress());
                EOSBalanceResponse response = getWalletBalanceWithRetry(request, 2, apiUrl); // Reduce retries per endpoint
                
                if (response.isSuccess()) {
                    log.info("Successfully retrieved balance using endpoint: {}", apiUrl);
                    return response;
                }
                
            } catch (Exception e) {
                log.warn("Failed to get balance from endpoint: {}, error: {}", apiUrl, e.getMessage());
            }
        }
        
        // All endpoints failed
        long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        return EOSBalanceResponse.error(
            request.getWalletAddress(),
            "All EOS API endpoints failed",
            responseTimeMs
        );
    }
    
    /**
     * Internal method với manual retry logic
     */
    private EOSBalanceResponse getWalletBalanceWithRetry(EOSBalanceRequest request, int maxAttempts, String apiUrl) {
        LocalDateTime startTime = LocalDateTime.now();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Getting balance for wallet: {} from EOS blockchain (attempt {}/{}) using {}", 
                        request.getWalletAddress(), attempt, maxAttempts, apiUrl);
                
                return executeEOSApiCall(request, startTime, apiUrl);
                
            } catch (RestClientException e) {
                if (attempt == maxAttempts) {
                    // Last attempt failed
                    long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
                    log.error("All {} attempts failed for wallet: {} using {}, final error: {}", 
                            maxAttempts, request.getWalletAddress(), apiUrl, e.getMessage());
                    
                    return EOSBalanceResponse.error(
                        request.getWalletAddress(),
                        "EOS API error after " + maxAttempts + " attempts: " + e.getMessage(),
                        responseTimeMs
                    );
                } else {
                    // Retry with exponential backoff
                    long waitTime = (long) (1000 * Math.pow(2, attempt - 1));
                    log.warn("Attempt {} failed for wallet: {}, retrying in {}ms: {}", 
                            attempt, request.getWalletAddress(), waitTime, e.getMessage());
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return EOSBalanceResponse.error(
                            request.getWalletAddress(),
                            "Interrupted during retry: " + ie.getMessage(),
                            Duration.between(startTime, LocalDateTime.now()).toMillis()
                        );
                    }
                }
            } catch (Exception e) {
                // Non-retryable error
                long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
                log.error("Non-retryable error for wallet: {}: {}", 
                        request.getWalletAddress(), e.getMessage());
                
                return EOSBalanceResponse.error(
                    request.getWalletAddress(),
                    "Unexpected error: " + e.getMessage(),
                    responseTimeMs
                );
            }
        }
        
        // Should not reach here
        return EOSBalanceResponse.error(
            request.getWalletAddress(),
            "Unexpected retry loop exit",
            Duration.between(startTime, LocalDateTime.now()).toMillis()
        );
    }
    
    /**
     * Execute actual EOS API call - throws exceptions for retry logic
     */
    private EOSBalanceResponse executeEOSApiCall(EOSBalanceRequest request, LocalDateTime startTime, String apiUrl) throws RestClientException {
        // Tạo request body cho EOS API
        Map<String, Object> requestBody = createEOSApiRequest(request);
        
        // Tạo headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Gọi EOS API
        String url = apiUrl + "/v1/chain/get_currency_balance";
        
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<String>() {}
        );
        
        // Tính response time
        long responseTimeMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        
        // Parse response - EOS API trả về JSON array: ["100.0000 EOS"]
        String balances = response.getBody();
        if (balances == null || balances.isEmpty()) {
            log.warn("No balance found for wallet: {}", request.getWalletAddress());
            return EOSBalanceResponse.success(
                request.getWalletAddress(),
                request.getTokenContract(),
                request.getTokenSymbol(),
                "0.0000 " + request.getTokenSymbol(),
                responseTimeMs
            );
        }
        
        // Parse JSON array để lấy balance string đầu tiên
        String rawBalance = parseBalanceFromJsonArray(balances);
        if (rawBalance == null) {
            log.warn("Cannot parse balance from response: {}", balances);
            return EOSBalanceResponse.success(
                request.getWalletAddress(),
                request.getTokenContract(),
                request.getTokenSymbol(),
                "0.0000 " + request.getTokenSymbol(),
                responseTimeMs
            );
        }
        
        log.info("Successfully retrieved balance for wallet: {}, balance: {}, response time: {}ms", 
                request.getWalletAddress(), rawBalance, responseTimeMs);
        
        EOSBalanceResponse result = EOSBalanceResponse.success(
            request.getWalletAddress(),
            request.getTokenContract(),
            request.getTokenSymbol(),
            rawBalance,
            responseTimeMs
        );
        result.setRawResponse(Collections.singletonList(balances));
        
        return result;
    }
    
    /**
     * Lấy balance async để improve performance
     */
    public CompletableFuture<EOSBalanceResponse> getWalletBalanceAsync(EOSBalanceRequest request) {
        return CompletableFuture.supplyAsync(() -> getWalletBalance(request));
    }
    
    /**
     * Lấy balance cho multiple wallets song song
     */
    public List<EOSBalanceResponse> getMultipleWalletBalances(List<EOSBalanceRequest> requests) {
        log.info("Getting balances for {} wallets", requests.size());
        
        // Sử dụng parallel streams để gọi async
        List<CompletableFuture<EOSBalanceResponse>> futures = requests.stream()
                .map(this::getWalletBalanceAsync)
                .toList();
        
        // Chờ tất cả complete
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
    
    /**
     * Tạo request body cho EOS API
     */
    private Map<String, Object> createEOSApiRequest(EOSBalanceRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("code", request.getTokenContract());
        requestBody.put("account", request.getWalletAddress());
        requestBody.put("symbol", request.getTokenSymbol());
        
        return requestBody;
    }
    
    /**
     * Check EOS API health - tests all endpoints
     */
    public boolean isEOSApiHealthy() {
        if (failoverEnabled) {
            List<String> selectedApiUrls = getSelectedApiUrls();
            return selectedApiUrls.stream().anyMatch(this::isEndpointHealthy);
        } else {
            String selectedApiUrl = getSelectedApiUrl();
            return isEndpointHealthy(selectedApiUrl);
        }
    }
    
    /**
     * Check specific endpoint health
     */
    private boolean isEndpointHealthy(String apiUrl) {
        try {
            String url = apiUrl + "/v1/chain/get_info";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            return response.getStatusCode() == HttpStatus.OK && 
                   response.getBody() != null && 
                   response.getBody().containsKey("chain_id");
                   
        } catch (Exception e) {
            log.debug("EOS API health check failed for {}: {}", apiUrl, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get EOS network info
     */
    public Map<String, Object> getNetworkInfo() {
        try {
            String selectedApiUrl = getSelectedApiUrl();
            String url = selectedApiUrl + "/v1/chain/get_info";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> networkInfo = response.getBody();
                if (networkInfo != null) {
                    // Add network type info
                    networkInfo.put("network_type", eosNetwork);
                    networkInfo.put("api_endpoint", selectedApiUrl);
                }
                return networkInfo;
            }
            
        } catch (Exception e) {
            log.error("Error getting EOS network info: {}", e.getMessage());
        }
        
        return Collections.emptyMap();
    }
    
    
    /**
     * Parse balance từ EOS API JSON array response
     * Input: ["100.0000 EOS"] hoặc []
     * Output: "100.0000 EOS" hoặc null nếu empty
     */
    private String parseBalanceFromJsonArray(String jsonArrayString) {
        try {
            // Remove brackets và quotes
            String cleaned = jsonArrayString.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
                
                if (cleaned.isEmpty()) {
                    return null; // Empty array []
                }
                
                // Remove quotes nếu có
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                
                return cleaned.trim();
            }
            
            log.warn("Invalid JSON array format: {}", jsonArrayString);
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing balance JSON array: {}, error: {}", jsonArrayString, e.getMessage());
            return null;
        }
    }
}