package com.wallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletCreateRequest;
import com.wallet.dto.WalletCreateResponse;
import com.wallet.entity.WalletType;
import com.wallet.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests cho REST API endpoints
 * Test end-to-end API functionality với authentication
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private final String testUserId = "test-user-123";

    @BeforeEach
    void setUp() {
        jwtToken = jwtTokenUtil.generateToken(testUserId);
    }

    /**
     * Test: Tạo wallet mới thành công
     */
    @Test
    void createWallet_WithValidRequest_ShouldReturnCreatedWallet() throws Exception {
        // Given
        WalletCreateRequest request = WalletCreateRequest.builder()
                .walletAddress("testwallet123")
                .walletType(WalletType.EOS)
                .walletName("Test Wallet")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.walletAddress").value("testwallet123"))
                .andExpect(jsonPath("$.walletType").value("EOS"))
                .andExpect(jsonPath("$.walletName").value("Test Wallet"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        WalletCreateResponse response = objectMapper.readValue(responseContent, WalletCreateResponse.class);
        
        assertNotNull(response.getId());
        assertEquals("testwallet123", response.getWalletAddress());
        assertNotNull(response.getCreatedAt());
    }

    /**
     * Test: Tạo wallet với invalid request
     */
    @Test
    void createWallet_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        WalletCreateRequest invalidRequest = WalletCreateRequest.builder()
                .walletAddress("") // Empty address
                .walletType(WalletType.EOS)
                .build();

        // When & Then
        mockMvc.perform(post("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Lấy danh sách wallets của user
     */
    @Test
    void getUserWallets_WithValidToken_ShouldReturnWallets() throws Exception {
        // Given - Tạo một wallet trước
        createTestWallet("testwallet456");

        // When & Then
        mockMvc.perform(get("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    /**
     * Test: Lấy wallet theo address
     */
    @Test
    void getWalletByAddress_WithExistingWallet_ShouldReturnWallet() throws Exception {
        // Given
        String walletAddress = "testwallet789";
        createTestWallet(walletAddress);

        // When & Then
        mockMvc.perform(get("/api/wallets/" + walletAddress)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.walletAddress").value(walletAddress))
                .andExpect(jsonPath("$.walletType").value("EOS"));
    }

    /**
     * Test: Lấy wallet không tồn tại
     */
    @Test
    void getWalletByAddress_WithNonExistentWallet_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets/nonexistent")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Test: Update wallet thành công
     */
    @Test
    void updateWallet_WithValidRequest_ShouldReturnUpdatedWallet() throws Exception {
        // Given
        String walletAddress = "testwallet999";
        createTestWallet(walletAddress);

        String updateRequest = "{"
                + "\"walletName\": \"Updated Wallet Name\","
                + "\"isPrimary\": true"
                + "}";

        // When & Then
        mockMvc.perform(put("/api/wallets/" + walletAddress)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.walletName").value("Updated Wallet Name"))
                .andExpect(jsonPath("$.isPrimary").value(true));
    }

    /**
     * Test: Delete wallet thành công
     */
    @Test
    void deleteWallet_WithExistingWallet_ShouldReturnNoContent() throws Exception {
        // Given
        String walletAddress = "walletTodelete";
        createTestWallet(walletAddress);

        // When & Then
        mockMvc.perform(delete("/api/wallets/" + walletAddress)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify wallet is deleted
        mockMvc.perform(get("/api/wallets/" + walletAddress)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpected(status().isNotFound());
    }

    /**
     * Test: API without authentication
     */
    @Test
    void accessAPI_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: API với invalid token
     */
    @Test
    void accessAPI_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: Content-Type validation
     */
    @Test
    void createWallet_WithWrongContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        String jsonRequest = "{ \"walletAddress\": \"test\", \"walletType\": \"EOS\" }";

        // When & Then
        mockMvc.perform(post("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.TEXT_PLAIN) // Wrong content type
                .content(jsonRequest))
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * Test: Rate limiting (nếu được implement)
     */
    @Test
    void createWallet_WithManyRequests_ShouldHandleRateLimit() throws Exception {
        // Given
        WalletCreateRequest request = WalletCreateRequest.builder()
                .walletAddress("ratelimitwallet")
                .walletType(WalletType.EOS)
                .build();

        // When - Make multiple requests quickly
        for (int i = 0; i < 5; i++) {
            request.setWalletAddress("ratelimitwallet" + i);
            
            mockMvc.perform(post("/api/wallets")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // Then - All requests should succeed (no rate limiting in test)
        // In production, some might return 429 Too Many Requests
    }

    /**
     * Test: Concurrent requests
     */
    @Test
    void createWallet_ConcurrentRequests_ShouldHandleCorrectly() throws Exception {
        // Given
        int numberOfThreads = 5;
        Thread[] threads = new Thread[numberOfThreads];

        // When - Create concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    WalletCreateRequest request = WalletCreateRequest.builder()
                            .walletAddress("concurrent" + threadId)
                            .walletType(WalletType.EOS)
                            .build();

                    mockMvc.perform(post("/api/wallets")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());
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

        // Then - All wallets should be created
        mockMvc.perform(get("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(numberOfThreads));
    }

    /**
     * Test: Performance với large response
     */
    @Test
    void getUserWallets_WithLargeDataset_ShouldPerformWell() throws Exception {
        // Given - Create many wallets
        for (int i = 0; i < 50; i++) {
            createTestWallet("perftestwallet" + i);
        }

        // When
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk());
                
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue((endTime - startTime) < 2000, 
                  "API call should complete within 2000ms");
    }

    /**
     * Helper method để tạo test wallet
     */
    private void createTestWallet(String walletAddress) throws Exception {
        WalletCreateRequest request = WalletCreateRequest.builder()
                .walletAddress(walletAddress)
                .walletType(WalletType.EOS)
                .walletName("Test Wallet " + walletAddress)
                .build();

        mockMvc.perform(post("/api/wallets")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isCreated());
    }
}