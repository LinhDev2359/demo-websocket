package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configuration cho scheduling và async processing
 * Setup thread pool cho balance sync operations
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig implements SchedulingConfigurer {
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // Thread pool size cho scheduled tasks
        scheduler.setPoolSize(5);
        
        // Thread naming
        scheduler.setThreadNamePrefix("balance-sync-");
        
        // Wait for tasks to complete shutdown
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        
        // Error handling
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            log.error("Balance sync task rejected: {}", r.toString());
        });
        
        scheduler.initialize();
        taskRegistrar.setScheduler(scheduler);
        
        log.info("Scheduling configured with thread pool size: {}", scheduler.getPoolSize());
    }
    
    /**
     * Primary TaskExecutor bean để fix bean conflict
     * Được sử dụng cho @Async methods
     */
    @Bean("taskExecutor")
    @Primary
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size
        executor.setCorePoolSize(10);
        
        // Max pool size
        executor.setMaxPoolSize(50);
        
        // Queue capacity
        executor.setQueueCapacity(10000);
        
        // Thread naming
        executor.setThreadNamePrefix("wallet-async-");
        
        // Wait for tasks to complete shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // Rejection policy
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("Primary TaskExecutor configured: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}