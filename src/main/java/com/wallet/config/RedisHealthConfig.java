package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis Health Configuration
 * Custom health check cho Redis connection
 * 
 * Features:
 * 1. Custom health indicator
 * 2. Graceful degradation nếu Redis unavailable
 * 3. Detailed health information
 */
// @Configuration  // Temporarily disabled for Redis debugging
@Slf4j
public class RedisHealthConfig {

    /**
     * Custom Redis Health Indicator
     * Thay thế default Redis health check với better error handling
     */
    // @Bean  // Temporarily disabled for Redis debugging
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                RedisConnection connection = redisConnectionFactory.getConnection();
                try {
                    // Test Redis connection với simple ping
                    String response = connection.ping();
                    if ("PONG".equals(response)) {
                        return Health.up()
                                .withDetail("redis", "Available")
                                .withDetail("response", response)
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("redis", "Unexpected response")
                                .withDetail("response", response)
                                .build();
                    }
                } finally {
                    connection.close();
                }
            } catch (DataAccessException | IllegalStateException e) {
                log.warn("Redis health check failed: {}", e.getMessage());
                return Health.down()
                        .withDetail("redis", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .withDetail("suggestion", "Check if Redis is running and accessible")
                        .build();
            } catch (Exception e) {
                log.error("Unexpected error during Redis health check: {}", e.getMessage());
                return Health.down()
                        .withDetail("redis", "Unexpected error")
                        .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                        .build();
            }
        };
    }
}