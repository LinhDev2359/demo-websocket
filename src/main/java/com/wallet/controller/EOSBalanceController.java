package com.wallet.controller;

import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import com.wallet.service.EOSClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * EOS Balance Controller
 * Direct API endpoints for EOS blockchain balance queries
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/eos/balance")
@RequiredArgsConstructor
@Tag(name = "EOS Balance", description = "Direct APIs for EOS blockchain balance operations")
public class EOSBalanceController {

    private final EOSClientService eosClientService;

    /**
     * Get EOS token balance for a specific wallet address
     */
    @PostMapping("/get")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get EOS token balance",
        description = "Retrieve token balance from EOS blockchain using get_currency_balance API"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balance")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "500", description = "EOS API error")
    public ResponseEntity<EOSBalanceResponse> getEOSBalance(
            @Valid @RequestBody EOSBalanceRequest request) {

        try {
            log.info("Getting EOS balance for wallet: {} with contract: {} and symbol: {}", 
                    request.getWalletAddress(), request.getTokenContract(), request.getTokenSymbol());

            EOSBalanceResponse response = eosClientService.getWalletBalance(request);

            if (response.isSuccess()) {
                log.info("Successfully retrieved balance for wallet: {} = {}", 
                        request.getWalletAddress(), response.getRawBalance());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to retrieve balance for wallet: {}, error: {}", 
                        request.getWalletAddress(), response.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("Error getting EOS balance for wallet: {}", request.getWalletAddress(), e);
            
            EOSBalanceResponse errorResponse = EOSBalanceResponse.error(
                request.getWalletAddress(),
                "Internal server error: " + e.getMessage(),
                0L
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get EOS token balance using GET method with path parameters
     */
    @GetMapping("/{walletAddress}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get EOS balance (GET method)",
        description = "Retrieve EOS token balance using GET method with default EOS token parameters"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balance")
    @ApiResponse(responseCode = "400", description = "Invalid wallet address")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<EOSBalanceResponse> getEOSBalanceSimple(
            @Parameter(description = "EOS wallet address (12 characters, a-z1-5.)")
            @PathVariable String walletAddress,
            @Parameter(description = "Token contract address")
            @RequestParam(defaultValue = "eosio.token") String contract,
            @Parameter(description = "Token symbol")
            @RequestParam(defaultValue = "EOS") String symbol) {

        try {
            log.info("Getting EOS balance for wallet: {} (GET method)", walletAddress);

            EOSBalanceRequest request = EOSBalanceRequest.builder()
                    .walletAddress(walletAddress)
                    .tokenContract(contract)
                    .tokenSymbol(symbol)
                    .build();

            EOSBalanceResponse response = eosClientService.getWalletBalance(request);

            if (response.isSuccess()) {
                log.info("Successfully retrieved balance for wallet: {} = {}", 
                        walletAddress, response.getRawBalance());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to retrieve balance for wallet: {}, error: {}", 
                        walletAddress, response.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("Error getting EOS balance for wallet: {}", walletAddress, e);
            
            EOSBalanceResponse errorResponse = EOSBalanceResponse.error(
                walletAddress,
                "Internal server error: " + e.getMessage(),
                0L
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get balance for multiple tokens of the same wallet
     */
    @PostMapping("/multi-token")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get multiple token balances for one wallet",
        description = "Retrieve balances for multiple tokens from the same wallet address"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved balances")
    public ResponseEntity<List<EOSBalanceResponse>> getMultiTokenBalances(
            @Valid @RequestBody List<EOSBalanceRequest> requests) {

        try {
            log.info("Getting balances for {} token requests", requests.size());

            List<EOSBalanceResponse> responses = eosClientService.getMultipleWalletBalances(requests);

            long successCount = responses.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            log.info("Retrieved {} balances, {} successful", responses.size(), successCount);

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error getting multiple token balances", e);
            throw new RuntimeException("Failed to retrieve multiple token balances", e);
        }
    }

    /**
     * Get balance asynchronously
     */
    @PostMapping("/async")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get EOS balance asynchronously",
        description = "Retrieve token balance asynchronously for better performance"
    )
    @ApiResponse(responseCode = "200", description = "Successfully initiated balance retrieval")
    public CompletableFuture<ResponseEntity<EOSBalanceResponse>> getEOSBalanceAsync(
            @Valid @RequestBody EOSBalanceRequest request) {

        log.info("Getting EOS balance async for wallet: {}", request.getWalletAddress());

        return eosClientService.getWalletBalanceAsync(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        log.info("Async balance retrieval successful for wallet: {}", 
                                request.getWalletAddress());
                        return ResponseEntity.ok(response);
                    } else {
                        log.warn("Async balance retrieval failed for wallet: {}", 
                                request.getWalletAddress());
                        return ResponseEntity.status(500).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Async balance retrieval error for wallet: {}", 
                            request.getWalletAddress(), throwable);
                    
                    EOSBalanceResponse errorResponse = EOSBalanceResponse.error(
                        request.getWalletAddress(),
                        "Async error: " + throwable.getMessage(),
                        0L
                    );
                    
                    return ResponseEntity.status(500).body(errorResponse);
                });
    }

    /**
     * Check EOS API health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Check EOS API health",
        description = "Check if EOS blockchain API endpoints are healthy"
    )
    @ApiResponse(responseCode = "200", description = "EOS API is healthy")
    @ApiResponse(responseCode = "503", description = "EOS API is unhealthy")
    public ResponseEntity<Map<String, Object>> checkEOSApiHealth() {

        try {
            boolean isHealthy = eosClientService.isEOSApiHealthy();
            
            Map<String, Object> healthStatus = Map.of(
                "healthy", isHealthy,
                "service", "EOS Blockchain API",
                "timestamp", java.time.LocalDateTime.now(),
                "status", isHealthy ? "UP" : "DOWN"
            );

            if (isHealthy) {
                log.info("EOS API health check: HEALTHY");
                return ResponseEntity.ok(healthStatus);
            } else {
                log.warn("EOS API health check: UNHEALTHY");
                return ResponseEntity.status(503).body(healthStatus);
            }

        } catch (Exception e) {
            log.error("Error checking EOS API health", e);
            
            Map<String, Object> errorStatus = Map.of(
                "healthy", false,
                "service", "EOS Blockchain API",
                "timestamp", java.time.LocalDateTime.now(),
                "status", "ERROR",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(503).body(errorStatus);
        }
    }

    /**
     * Get EOS network information
     */
    @GetMapping("/network-info")
    @Operation(
        summary = "Get EOS network information",
        description = "Retrieve EOS blockchain network information"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved network info")
    public ResponseEntity<Map<String, Object>> getEOSNetworkInfo() {

        try {
            Map<String, Object> networkInfo = eosClientService.getNetworkInfo();
            
            if (!networkInfo.isEmpty()) {
                log.info("Retrieved EOS network info successfully");
                return ResponseEntity.ok(networkInfo);
            } else {
                log.warn("Failed to retrieve EOS network info");
                return ResponseEntity.status(503).body(Map.of(
                    "error", "Unable to retrieve network information",
                    "timestamp", java.time.LocalDateTime.now()
                ));
            }

        } catch (Exception e) {
            log.error("Error getting EOS network info", e);
            
            Map<String, Object> errorResponse = Map.of(
                "error", "Failed to retrieve network information: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}