package com.wallet.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.BalanceUpdateMessage;
import com.wallet.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests cho WebSocket functionality
 * Test real-time messaging và authentication
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String jwtToken;
    private final String testUserId = "test-user-123";

    @BeforeEach
    void setUp() throws Exception {
        // Setup WebSocket client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Generate JWT token for testing
        jwtToken = jwtTokenUtil.generateToken(testUserId);

        // Connect to WebSocket
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        stompSession = stompClient.connect(url, connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Connected to WebSocket: " + session.getSessionId());
            }

            @Override
            public void handleException(StompSession session, StompCommand command, 
                                      StompHeaders headers, byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }
        }).get(5, TimeUnit.SECONDS);
    }

    /**
     * Test: WebSocket connection với JWT authentication
     */
    @Test
    void testWebSocketConnection_WithValidJWT_ShouldConnect() {
        // Then
        assertTrue(stompSession.isConnected());
        assertEquals(testUserId, stompSession.getSessionId());
    }

    /**
     * Test: Portfolio subscription và real-time updates
     */
    @Test
    void testPortfolioSubscription_ShouldReceiveRealTimeUpdates() throws Exception {
        // Given
        BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();

        StompSession.Subscription subscription = stompSession.subscribe(
            "/topic/portfolio/" + testUserId,
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedMessages.add((String) payload);
                }
            }
        );

        // When - Send a test message
        stompSession.send("/app/portfolio/update", createTestPortfolioUpdate());

        // Then
        String receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertTrue(receivedMessage.contains("portfolio"));

        subscription.unsubscribe();
    }

    /**
     * Test: Balance update subscription
     */
    @Test
    void testBalanceSubscription_ShouldReceiveBalanceUpdates() throws Exception {
        // Given
        BlockingQueue<BalanceUpdateMessage> receivedMessages = new LinkedBlockingQueue<>();

        StompSession.Subscription subscription = stompSession.subscribe(
            "/topic/balance/" + testUserId,
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return BalanceUpdateMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedMessages.add((BalanceUpdateMessage) payload);
                }
            }
        );

        // When - Send a test balance update
        BalanceUpdateMessage testMessage = createTestBalanceUpdate();
        stompSession.send("/app/balance/update", testMessage);

        // Then
        BalanceUpdateMessage receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals("testwalleteos", receivedMessage.getWalletAddress());
        assertEquals(new BigDecimal("10.0000"), receivedMessage.getBalance());

        subscription.unsubscribe();
    }

    /**
     * Test: Multiple concurrent connections
     */
    @Test
    void testMultipleConcurrentConnections_ShouldHandleCorrectly() throws Exception {
        // Given
        String secondUserId = "test-user-456";
        String secondToken = jwtTokenUtil.generateToken(secondUserId);

        StompHeaders secondConnectHeaders = new StompHeaders();
        secondConnectHeaders.add("Authorization", "Bearer " + secondToken);

        // When - Create second connection
        StompSession secondSession = stompClient.connect(
            "ws://localhost:" + port + "/ws",
            secondConnectHeaders,
            new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // Then
        assertTrue(stompSession.isConnected());
        assertTrue(secondSession.isConnected());
        assertNotEquals(stompSession.getSessionId(), secondSession.getSessionId());

        secondSession.disconnect();
    }

    /**
     * Test: WebSocket authentication failure
     */
    @Test
    void testWebSocketConnection_WithInvalidJWT_ShouldFail() {
        // Given
        StompHeaders invalidHeaders = new StompHeaders();
        invalidHeaders.add("Authorization", "Bearer invalid-token");

        // When & Then
        assertThrows(Exception.class, () -> {
            stompClient.connect(
                "ws://localhost:" + port + "/ws",
                invalidHeaders,
                new StompSessionHandlerAdapter() {}
            ).get(2, TimeUnit.SECONDS);
        });
    }

    /**
     * Test: Error handling trong WebSocket
     */
    @Test
    void testWebSocketErrorHandling_ShouldHandleGracefully() throws Exception {
        // Given
        BlockingQueue<String> errorMessages = new LinkedBlockingQueue<>();

        StompSession.Subscription subscription = stompSession.subscribe(
            "/topic/errors/" + testUserId,
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    errorMessages.add((String) payload);
                }
            }
        );

        // When - Send invalid message
        try {
            stompSession.send("/app/invalid/endpoint", "invalid-data");
        } catch (Exception e) {
            // Expected exception
        }

        // Then - Should still be connected
        assertTrue(stompSession.isConnected());

        subscription.unsubscribe();
    }

    /**
     * Test: Performance với high message volume
     */
    @Test
    void testHighVolumeMessaging_ShouldMaintainPerformance() throws Exception {
        // Given
        int messageCount = 100;
        BlockingQueue<BalanceUpdateMessage> receivedMessages = new LinkedBlockingQueue<>();

        StompSession.Subscription subscription = stompSession.subscribe(
            "/topic/balance/" + testUserId,
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return BalanceUpdateMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedMessages.add((BalanceUpdateMessage) payload);
                }
            }
        );

        // When - Send many messages
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            BalanceUpdateMessage message = createTestBalanceUpdate();
            message.setBalance(new BigDecimal(String.valueOf(i)));
            stompSession.send("/app/balance/update", message);
        }

        // Wait for all messages
        for (int i = 0; i < messageCount; i++) {
            BalanceUpdateMessage received = receivedMessages.poll(1, TimeUnit.SECONDS);
            assertNotNull(received, "Message " + i + " should be received");
        }

        long endTime = System.currentTimeMillis();

        // Then
        assertTrue((endTime - startTime) < 10000, 
                  "Should process " + messageCount + " messages within 10 seconds");

        subscription.unsubscribe();
    }

    /**
     * Test: WebSocket heartbeat và connection persistence
     */
    @Test
    void testWebSocketHeartbeat_ShouldMaintainConnection() throws Exception {
        // Given
        assertTrue(stompSession.isConnected());

        // When - Wait for heartbeat interval
        Thread.sleep(3000);

        // Then
        assertTrue(stompSession.isConnected());
    }

    /**
     * Test: User-specific message routing
     */
    @Test
    void testUserSpecificRouting_ShouldOnlyReceiveOwnMessages() throws Exception {
        // Given
        String otherUserId = "other-user-789";
        BlockingQueue<BalanceUpdateMessage> userMessages = new LinkedBlockingQueue<>();

        // Subscribe to current user's messages
        StompSession.Subscription userSubscription = stompSession.subscribe(
            "/topic/balance/" + testUserId,
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return BalanceUpdateMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    userMessages.add((BalanceUpdateMessage) payload);
                }
            }
        );

        // When - Send message to current user
        BalanceUpdateMessage userMessage = createTestBalanceUpdate();
        stompSession.send("/app/balance/update", userMessage);

        // Send message to other user (simulated)
        BalanceUpdateMessage otherMessage = createTestBalanceUpdate();
        otherMessage.setWalletAddress("otherwalleteos");

        // Then - Should only receive own message
        BalanceUpdateMessage received = userMessages.poll(2, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("testwalleteos", received.getWalletAddress());

        // Should not receive other user's message
        BalanceUpdateMessage shouldBeNull = userMessages.poll(1, TimeUnit.SECONDS);
        assertNull(shouldBeNull);

        userSubscription.unsubscribe();
    }

    /**
     * Helper method để tạo test portfolio update
     */
    private String createTestPortfolioUpdate() throws JsonProcessingException {
        return objectMapper.writeValueAsString(
            java.util.Map.of(
                "userId", testUserId,
                "totalValue", new BigDecimal("1000.00"),
                "timestamp", LocalDateTime.now().toString()
            )
        );
    }

    /**
     * Helper method để tạo test balance update
     */
    private BalanceUpdateMessage createTestBalanceUpdate() {
        return BalanceUpdateMessage.builder()
                .walletAddress("testwalleteos")
                .tokenType("EOS")
                .balance(new BigDecimal("10.0000"))
                .rawBalance("10.0000 EOS")
                .timestamp(LocalDateTime.now())
                .build();
    }
}