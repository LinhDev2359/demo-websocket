package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * WebSocket TaskScheduler Configuration
 * 
 * Chức năng:
 * 1. Provide TaskScheduler cho WebSocket heartbeat functionality
 * 2. Configure thread pool cho scheduled tasks
 * 3. Enable heartbeat support trong SimpleBrokerMessageHandler
 * 
 * Note: TaskScheduler required nếu muốn enable heartbeat trong message broker
 */
@Configuration
@Slf4j
public class WebSocketTaskSchedulerConfig {

    /**
     * TaskScheduler bean cho WebSocket heartbeat
     * 
     * Configuration:
     * - Pool size: 10 threads
     * - Thread name prefix: websocket-heartbeat-
     * - Initialize on startup
     * 
     * Note: Renamed to avoid conflict with Spring's default messageBrokerTaskScheduler
     */
    @Bean
    public TaskScheduler webSocketHeartbeatTaskScheduler() {
        log.info("Creating TaskScheduler for WebSocket message broker heartbeat");
        
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("websocket-heartbeat-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(60);
        taskScheduler.initialize();
        
        log.info("✅ WebSocket TaskScheduler configured with 10 threads");
        return taskScheduler;
    }
}