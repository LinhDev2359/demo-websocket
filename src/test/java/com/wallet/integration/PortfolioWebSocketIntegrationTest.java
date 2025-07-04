package com.wallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.PortfolioResponse;
import com.wallet.dto.PortfolioUpdateRequest;
import com.wallet.security.JwtTokenUtil;
import com.wallet.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration Tests cho Portfolio WebSocket
 * 
 * Testing Strategy:
 * 1. Test WebSocket connection establishment
 * 2. Test JWT authentication over WebSocket
 * 3. Test portfolio subscription and data streaming
 * 4. Test error handling and disconnection scenarios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortfolioWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private PortfolioService portfolioService;

    private WebSocketStompClient stompClient;
    private ObjectMapper objectMapper;
    private String jwtToken;
    private final String testUserId = "test-user-123";

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        objectMapper = new ObjectMapper();
        
        // Generate test JWT token
        jwtToken = jwtTokenUtil.generateToken(testUserId, "testuser");
        
        // Mock portfolio service responses
        PortfolioResponse mockPortfolio = createMockPortfolioResponse();
        when(portfolioService.getUserPortfolio(anyString(), anyBoolean()))
            .thenReturn(mockPortfolio);
    }

    @Test
    void testWebSocketConnection_WithValidJWT_ShouldConnect() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionFuture.complete(session);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, 
                                      StompHeaders headers, byte[] payload, Throwable exception) {
                sessionFuture.completeExceptionally(exception);
            }
        };

        // When
        stompClient.connect(url, headers, sessionHandler);
        StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);

        // Then
        assertTrue(session.isConnected());
        
        // Cleanup
        session.disconnect();
    }

    @Test
    void testWebSocketConnection_WithInvalidJWT_ShouldFail() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer invalid-token");

        CompletableFuture<Throwable> exceptionFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command, 
                                      StompHeaders headers, byte[] payload, Throwable exception) {
                exceptionFuture.complete(exception);
            }
        };

        // When & Then
        stompClient.connect(url, headers, sessionHandler);
        Throwable exception = exceptionFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("WebSocket authentication failed") ||
                  exception instanceof java.net.ConnectException);
    }

    @Test
    void testPortfolioSubscription_ShouldReceiveInitialData() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        CompletableFuture<PortfolioResponse> portfolioFuture = new CompletableFuture<>();
        CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionFuture.complete(session);
            }
        };

        // When
        StompSession session = stompClient.connect(url, headers, sessionHandler)
            .get(5, TimeUnit.SECONDS);

        // Subscribe to portfolio updates
        session.subscribe("/app/portfolio/subscribe/" + testUserId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PortfolioResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                portfolioFuture.complete((PortfolioResponse) payload);
            }
        });

        // Then
        PortfolioResponse portfolioResponse = portfolioFuture.get(10, TimeUnit.SECONDS);
        
        assertNotNull(portfolioResponse);
        assertEquals(testUserId, portfolioResponse.getUserId());
        assertEquals("testuser", portfolioResponse.getUsername());
        assertEquals(1, portfolioResponse.getTotalWallets());

        // Cleanup
        session.disconnect();
    }

    @Test
    void testPortfolioUpdate_ShouldReceiveUpdatedData() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        CompletableFuture<PortfolioResponse> updateFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {};
        StompSession session = stompClient.connect(url, headers, sessionHandler)
            .get(5, TimeUnit.SECONDS);

        // Subscribe to user-specific queue
        session.subscribe("/user/queue/portfolio", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PortfolioResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                updateFuture.complete((PortfolioResponse) payload);
            }
        });

        // When - Send portfolio update request
        PortfolioUpdateRequest updateRequest = new PortfolioUpdateRequest();
        updateRequest.setUserId(testUserId);
        updateRequest.setRefreshCache(true);
        
        session.send("/app/portfolio/update", updateRequest);

        // Then
        PortfolioResponse updatedPortfolio = updateFuture.get(10, TimeUnit.SECONDS);
        
        assertNotNull(updatedPortfolio);
        assertEquals(testUserId, updatedPortfolio.getUserId());

        // Cleanup
        session.disconnect();
    }

    @Test
    void testPortfolioRefresh_ShouldTriggerCacheRefresh() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {};
        StompSession session = stompClient.connect(url, headers, sessionHandler)
            .get(5, TimeUnit.SECONDS);

        CompletableFuture<PortfolioResponse> refreshFuture = new CompletableFuture<>();
        
        session.subscribe("/user/queue/portfolio", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PortfolioResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                refreshFuture.complete((PortfolioResponse) payload);
            }
        });

        // When
        session.send("/app/portfolio/refresh", testUserId);

        // Then
        PortfolioResponse refreshedPortfolio = refreshFuture.get(10, TimeUnit.SECONDS);
        
        assertNotNull(refreshedPortfolio);
        assertEquals(testUserId, refreshedPortfolio.getUserId());

        // Cleanup
        session.disconnect();
    }

    @Test
    void testUnauthorizedAccess_ShouldReceiveError() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        CompletableFuture<String> errorFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {};
        StompSession session = stompClient.connect(url, headers, sessionHandler)
            .get(5, TimeUnit.SECONDS);

        // Subscribe to error queue
        session.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                errorFuture.complete((String) payload);
            }
        });

        // When - Try to access another user's portfolio
        session.subscribe("/app/portfolio/subscribe/another-user-id", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PortfolioResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Should not reach here
            }
        });

        // Then
        String errorMessage = errorFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Access denied") || errorMessage.contains("error"));

        // Cleanup
        session.disconnect();
    }

    @Test
    void testSessionDisconnection_ShouldCleanupProperly() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);

        CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();
        CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionFuture.complete(session);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                disconnectFuture.complete("disconnected");
            }
        };

        // When
        StompSession session = stompClient.connect(url, headers, sessionHandler)
            .get(5, TimeUnit.SECONDS);
        
        assertTrue(session.isConnected());
        
        // Disconnect
        session.disconnect();

        // Then
        // Verify session is properly disconnected
        assertFalse(session.isConnected());
    }

    private PortfolioResponse createMockPortfolioResponse() {
        PortfolioResponse portfolio = new PortfolioResponse(testUserId, "testuser");
        portfolio.setTotalWallets(1);
        
        // Create mock wallet info
        PortfolioResponse.WalletPortfolioInfo walletInfo = 
            new PortfolioResponse.WalletPortfolioInfo(1L, "test.eos.address", "EOS");
        walletInfo.setIsPrimary(true);
        
        // Create mock balance info
        PortfolioResponse.BalanceInfo balanceInfo = 
            new PortfolioResponse.BalanceInfo("A", new BigDecimal("100.50000000"));
        walletInfo.setBalances(java.util.Arrays.asList(balanceInfo));
        
        portfolio.setWallets(java.util.Arrays.asList(walletInfo));
        
        // Create mock balance summary
        PortfolioResponse.BalanceSummary summary = new PortfolioResponse.BalanceSummary();
        summary.setTotalEos(new BigDecimal("100.50000000"));
        portfolio.setTotalBalance(summary);
        
        return portfolio;
    }
}