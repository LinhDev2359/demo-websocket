package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho Resilience4j patterns
 * Setup Circuit Breaker, Retry, và Time Limiter cho EOS API
 * 
 * ❌ DISABLED: Programmatic configuration
 * ✅ USING: application.yml configuration instead
 * 
 * See application.yml section:
 * resilience4j:
 *   circuitbreaker:
 *     instances:
 *       eosApi: ...
 *   retry:
 *     instances:
 *       eosApi: ...
 */
@Slf4j
@Configuration
public class Resilience4jConfig {
    
    // Configuration is now done in application.yml
    // No @Bean methods needed
    
}