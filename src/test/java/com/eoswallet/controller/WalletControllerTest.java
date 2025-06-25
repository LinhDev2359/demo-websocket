package com.eoswallet.controller;

import com.eoswallet.dto.WalletCreateRequest;
import com.eoswallet.dto.WalletCreateResponse;
import com.eoswallet.facade.WalletFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WalletController
 * Tests REST API endpoints with proper validation and security
 * 
 * Test Categories:
 * 1. Wallet Creation Tests
 * 2. Wallet Retrieval Tests
 * 3. Validation Tests
 * 4. Security Tests
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletFacade walletFacade;

    private WalletCreateRequest validCreateRequest;
    private WalletCreateResponse mockCreateResponse;

    @BeforeEach
    void setUp() {
        // Setup valid create request
        validCreateRequest = new WalletCreateRequest();
        validCreateRequest.setName("Test Wallet");
        validCreateRequest.setEosAddress("testaccount1");
        validCreateRequest.setDescription("Test wallet description");
        validCreateRequest.setIsPrimary(false);

        // Setup mock response
        mockCreateResponse = WalletCreateResponse.builder()
                .walletId(1L)
                .name("Test Wallet")
                .eosAddress("testaccount1")
                .description("Test wallet description")
                .isPrimary(false)
                .status("ACTIVE")
                .message("Wallet created successfully")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWallet_WithValidData_ShouldReturn201() throws Exception {
        // Given
        when(walletFacade.createWallet(eq(1L), any(WalletCreateRequest.class)))
                .thenReturn(mockCreateResponse);

        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value(1L))
                .andExpect(jsonPath("$.name").value("Test Wallet"))
                .andExpect(jsonPath("$.eosAddress").value("testaccount1"))
                .andExpect(jsonPath("$.message").value("Wallet created successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWallet_WithInvalidEosAddress_ShouldReturn400() throws Exception {
        // Given
        validCreateRequest.setEosAddress("invalid-address-format");

        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWallet_WithEmptyName_ShouldReturn400() throws Exception {
        // Given
        validCreateRequest.setName("");

        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWallet_WithTooLongName_ShouldReturn400() throws Exception {
        // Given
        validCreateRequest.setName("a".repeat(101)); // Exceeds 100 character limit

        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWallet_WithoutAuthentication_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST") // Wrong role
    void createWallet_WithInsufficientRoles_ShouldReturn403() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWallet_WithMissingUserId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateEosAddress_WithValidAddress_ShouldReturn200() throws Exception {
        // Given
        when(walletFacade.validateEosAddress("testaccount1")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/wallets/validate-address")
                        .param("eosAddress", "testaccount1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void validateEosAddress_WithInvalidAddress_ShouldReturn200() throws Exception {
        // Given
        when(walletFacade.validateEosAddress("invalid")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/wallets/validate-address")
                        .param("eosAddress", "invalid"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void validateEosAddress_WithMissingAddress_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/wallets/validate-address"))
                .andExpect(status().isBadRequest());
    }
}