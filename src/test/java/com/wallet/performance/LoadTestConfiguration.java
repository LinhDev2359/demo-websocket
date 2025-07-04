package com.wallet.performance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Performance testing configuration for load tests
 * Optimizes thread pools for concurrent user simulation
 */
@TestConfiguration
@Profile("performance-test")
public class LoadTestConfiguration {

    /**
     * Thread pool for simulating concurrent WebSocket connections
     * Optimized for 1000+ concurrent users
     */
    @Bean(name = "websocketTestExecutor")
    public Executor websocketTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("websocket-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for API load testing
     * Configured for high throughput REST API testing
     */
    @Bean(name = "apiLoadTestExecutor")
    public Executor apiLoadTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(500);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("api-load-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}