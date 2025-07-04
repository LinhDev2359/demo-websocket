package com.eoswallet.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Facade Layer
 * Ensures proper component scanning for the new architecture layers
 */
@Configuration
@ComponentScan(basePackages = {
    "com.eoswallet.facade",
    "com.eoswallet.controller",
    "com.wallet.service",
    "com.wallet.repository"
})
public class FacadeConfig {
    // Configuration class to ensure proper component scanning
    // This ensures all facade, service, and repository beans are properly registered
}