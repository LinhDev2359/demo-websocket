package com.wallet.service;

import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho EOSClientService
 * Test coverage cho EOS API integration và error handling
 */
@ExtendWith(MockitoExtension.class)
class EOSClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EOSClientService eosClientService;

    private EOSBalanceRequest testRequest;
    private String testWalletAddress = "testwalleteos";
    private String testEosApiUrl = "https://test-eos-api.com";

    @BeforeEach
    void setUp() {
        testRequest = EOSBalanceRequest.forEOSToken(testWalletAddress);
        
        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(eosClientService, "eosApiUrl", testEosApiUrl);
        ReflectionTestUtils.setField(eosClientService, "timeoutMs", 30000);
        ReflectionTestUtils.setField(eosClientService, "maxRetries", 3);
    }

    /**
     * Test: Lấy balance thành công
     */
    @Test
    void getWalletBalance_WithValidRequest_ShouldReturnBalance() {
        // Given
        List<String> mockResponse = Arrays.asList("10.0000 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testWalletAddress, result.getWalletAddress());
        assertEquals(new BigDecimal("10.0000"), result.getBalance());
        assertEquals("10.0000 EOS", result.getRawBalance());
        assertNotNull(result.getResponseTimeMs());

        verify(restTemplate).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
    }

    /**
     * Test: Lấy balance với empty response (balance = 0)
     */
    @Test
    void getWalletBalance_WithEmptyResponse_ShouldReturnZeroBalance() {
        // Given
        List<String> emptyResponse = Collections.emptyList();
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(emptyResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testWalletAddress, result.getWalletAddress());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("0.0000 EOS", result.getRawBalance());
    }

    /**
     * Test: EOS API trả về lỗi
     */
    @Test
    void getWalletBalance_WhenAPIThrowsException_ShouldReturnErrorResponse() {
        // Given
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("API Error"));

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(testWalletAddress, result.getWalletAddress());
        assertTrue(result.getErrorMessage().contains("EOS API error after 3 attempts"));
        assertEquals(BigDecimal.ZERO, result.getBalance());
    }

    /**
     * Test: Async balance retrieval
     */
    @Test
    void getWalletBalanceAsync_WithValidRequest_ShouldReturnCompletableFuture() {
        // Given
        List<String> mockResponse = Arrays.asList("5.0000 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        CompletableFuture<EOSBalanceResponse> future = eosClientService.getWalletBalanceAsync(testRequest);
        EOSBalanceResponse result = future.join();

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("5.0000"), result.getBalance());
    }

    /**
     * Test: Multiple wallet balances
     */
    @Test
    void getMultipleWalletBalances_WithValidRequests_ShouldReturnAllBalances() {
        // Given
        List<EOSBalanceRequest> requests = Arrays.asList(
            EOSBalanceRequest.forEOSToken("wallet1"),
            EOSBalanceRequest.forEOSToken("wallet2"),
            EOSBalanceRequest.forEOSToken("wallet3")
        );

        List<String> mockResponse = Arrays.asList("10.0000 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        List<EOSBalanceResponse> results = eosClientService.getMultipleWalletBalances(requests);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        
        for (EOSBalanceResponse response : results) {
            assertTrue(response.isSuccess());
            assertEquals(new BigDecimal("10.0000"), response.getBalance());
        }

        verify(restTemplate, times(3)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
    }

    /**
     * Test: EOS API health check thành công
     */
    @Test
    void isEOSApiHealthy_WhenAPIRespondsCorrectly_ShouldReturnTrue() {
        // Given
        ResponseEntity<Object> healthResponse = new ResponseEntity<>(
            Collections.singletonMap("chain_id", "test-chain"), 
            HttpStatus.OK
        );

        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
            .thenReturn(healthResponse);

        // When
        boolean result = eosClientService.isEOSApiHealthy();

        // Then
        assertTrue(result);

        verify(restTemplate).getForEntity(
            eq(testEosApiUrl + "/v1/chain/get_info"),
            eq(Object.class)
        );
    }

    /**
     * Test: EOS API health check thất bại
     */
    @Test
    void isEOSApiHealthy_WhenAPIThrowsException_ShouldReturnFalse() {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // When
        boolean result = eosClientService.isEOSApiHealthy();

        // Then
        assertFalse(result);
    }

    /**
     * Test: Parse balance từ string
     */
    @Test
    void parseBalance_WithValidBalanceString_ShouldReturnCorrectValue() {
        // Given
        List<String> mockResponse = Arrays.asList("123.4567 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertEquals(new BigDecimal("123.4567"), result.getBalance());
        assertEquals("123.4567 EOS", result.getRawBalance());
    }

    /**
     * Test: Retry mechanism
     */
    @Test
    void getWalletBalance_WithRetryableError_ShouldRetryAndSucceed() {
        // Given
        List<String> mockResponse = Arrays.asList("10.0000 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        ))
        .thenThrow(new RestClientException("Temporary error"))
        .thenThrow(new RestClientException("Temporary error"))
        .thenReturn(responseEntity);

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("10.0000"), result.getBalance());

        // Verify 3 attempts were made (2 failures + 1 success)
        verify(restTemplate, times(3)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
    }

    /**
     * Test: Performance test cho API calls
     */
    @Test
    void getWalletBalance_PerformanceTest_ShouldCompleteWithinTimeout() {
        // Given
        List<String> mockResponse = Arrays.asList("10.0000 EOS");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        long startTime = System.currentTimeMillis();
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(result.isSuccess());
        
        // Performance assertion - should complete within 1000ms (excluding network)
        assertTrue((endTime - startTime) < 1000, "API call should complete within 1000ms");
    }

    /**
     * Test: Invalid response format
     */
    @Test
    void getWalletBalance_WithInvalidResponseFormat_ShouldHandleGracefully() {
        // Given
        List<String> invalidResponse = Arrays.asList("invalid balance format");
        ResponseEntity<List<String>> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // When
        EOSBalanceResponse result = eosClientService.getWalletBalance(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.ZERO, result.getBalance()); // Should default to 0 for invalid format
        assertEquals("invalid balance format", result.getRawBalance());
    }
}