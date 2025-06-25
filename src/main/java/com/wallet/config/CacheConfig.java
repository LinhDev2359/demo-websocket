package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho Spring Cache
 * Enable caching cho EOS balance responses
 */
@Slf4j
@Configuration
// @EnableCaching  // Temporarily disabled for Redis debugging
public class CacheConfig {
    
    // Redis cache configuration is in application.yml
    // Cache names:
    // - eosBalance: 5 minutes TTL
    // - portfolioCache: 5 minutes TTL
    
}