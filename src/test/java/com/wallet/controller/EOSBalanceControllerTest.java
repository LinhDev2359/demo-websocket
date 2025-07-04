package com.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.EOSBalanceRequest;
import com.wallet.dto.EOSBalanceResponse;
import com.wallet.service.EOSClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for EOSBalanceController
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@WebMvcTest(EOSBalanceController.class)
class EOSBalanceControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private EOSClientService eosClientService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalance_Success() throws Exception {
        // Given
        EOSBalanceRequest request = EOSBalanceRequest.builder()
                .walletAddress("testaccount1")
                .tokenContract("eosio.token")
                .tokenSymbol("EOS")
                .build();

        EOSBalanceResponse mockResponse = EOSBalanceResponse.success(
                "testaccount1",
                "eosio.token", 
                "EOS",
                "100.0000 EOS",
                250L
        );

        when(eosClientService.getWalletBalance(any(EOSBalanceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.walletAddress").value("testaccount1"))
                .andExpect(jsonPath("$.rawBalance").value("100.0000 EOS"))
                .andExpect(jsonPath("$.balance").value(100.0000))
                .andExpect(jsonPath("$.tokenContract").value("eosio.token"))
                .andExpect(jsonPath("$.tokenSymbol").value("EOS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalance_Error() throws Exception {
        // Given
        EOSBalanceRequest request = EOSBalanceRequest.builder()
                .walletAddress("invalidacct")
                .tokenContract("eosio.token")
                .tokenSymbol("EOS")
                .build();

        EOSBalanceResponse mockResponse = EOSBalanceResponse.error(
                "invalidacct",
                "Account not found",
                150L
        );

        when(eosClientService.getWalletBalance(any(EOSBalanceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Account not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalanceSimple_Success() throws Exception {
        // Given
        EOSBalanceResponse mockResponse = EOSBalanceResponse.success(
                "testaccount2",
                "eosio.token",
                "EOS", 
                "50.0000 EOS",
                200L
        );

        when(eosClientService.getWalletBalance(any(EOSBalanceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/testaccount2")
                .param("contract", "eosio.token")
                .param("symbol", "EOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.walletAddress").value("testaccount2"))
                .andExpect(jsonPath("$.rawBalance").value("50.0000 EOS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalanceSimple_DefaultParams() throws Exception {
        // Given
        EOSBalanceResponse mockResponse = EOSBalanceResponse.success(
                "testaccount3",
                "eosio.token",
                "EOS",
                "25.0000 EOS", 
                180L
        );

        when(eosClientService.getWalletBalance(any(EOSBalanceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/testaccount3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tokenContract").value("eosio.token"))
                .andExpect(jsonPath("$.tokenSymbol").value("EOS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetMultiTokenBalances_Success() throws Exception {
        // Given
        List<EOSBalanceRequest> requests = Arrays.asList(
                EOSBalanceRequest.builder()
                        .walletAddress("testaccount1")
                        .tokenContract("eosio.token")
                        .tokenSymbol("EOS")
                        .build(),
                EOSBalanceRequest.builder()
                        .walletAddress("testaccount1")
                        .tokenContract("eosio")
                        .tokenSymbol("RAM")
                        .build()
        );

        List<EOSBalanceResponse> mockResponses = Arrays.asList(
                EOSBalanceResponse.success("testaccount1", "eosio.token", "EOS", "100.0000 EOS", 200L),
                EOSBalanceResponse.success("testaccount1", "eosio", "RAM", "1024 RAM", 150L)
        );

        when(eosClientService.getMultipleWalletBalances(anyList()))
                .thenReturn(mockResponses);

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/multi-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tokenSymbol").value("EOS"))
                .andExpect(jsonPath("$[1].tokenSymbol").value("RAM"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalanceAsync_Success() throws Exception {
        // Given
        EOSBalanceRequest request = EOSBalanceRequest.builder()
                .walletAddress("testaccount4")
                .tokenContract("eosio.token")
                .tokenSymbol("EOS")
                .build();

        EOSBalanceResponse mockResponse = EOSBalanceResponse.success(
                "testaccount4",
                "eosio.token",
                "EOS",
                "75.0000 EOS",
                300L
        );

        when(eosClientService.getWalletBalanceAsync(any(EOSBalanceRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/async")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.walletAddress").value("testaccount4"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCheckEOSApiHealth_Healthy() throws Exception {
        // Given
        when(eosClientService.isEOSApiHealthy()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("EOS Blockchain API"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCheckEOSApiHealth_Unhealthy() throws Exception {
        // Given
        when(eosClientService.isEOSApiHealthy()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.healthy").value(false))
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSNetworkInfo_Success() throws Exception {
        // Given
        Map<String, Object> mockNetworkInfo = Map.of(
                "chain_id", "aca376f206b8fc25a6ed44dbdc66547c36c6c33e3a119ffbeaef943642f0e906",
                "head_block_num", 123456789,
                "head_block_time", LocalDateTime.now().toString(),
                "version", "v2.0.0"
        );

        when(eosClientService.getNetworkInfo()).thenReturn(mockNetworkInfo);

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/network-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chain_id").value("aca376f206b8fc25a6ed44dbdc66547c36c6c33e3a119ffbeaef943642f0e906"))
                .andExpect(jsonPath("$.head_block_num").value(123456789))
                .andExpect(jsonPath("$.version").value("v2.0.0"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSNetworkInfo_Empty() throws Exception {
        // Given
        when(eosClientService.getNetworkInfo()).thenReturn(Map.of());

        // When & Then
        mockMvc.perform(get("/api/v1/eos/balance/network-info"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Unable to retrieve network information"));
    }

    @Test
    void testGetEOSBalance_Unauthorized() throws Exception {
        // Given
        EOSBalanceRequest request = EOSBalanceRequest.builder()
                .walletAddress("testaccount1")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetEOSBalance_InvalidRequest() throws Exception {
        // Given - invalid wallet address
        EOSBalanceRequest request = EOSBalanceRequest.builder()
                .walletAddress("") // empty wallet address
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/eos/balance/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}