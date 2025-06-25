package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration cho EOS blockchain integration
 * Setup RestTemplate với timeout và connection pooling
 */
@Slf4j
@Configuration
public class EOSConfig {
    
    @Value("${eos.api.timeout:30000}")
    private int timeoutMs;
    
    @Value("${eos.api.connection-timeout:10000}")
    private int connectionTimeoutMs;
    
    @Value("${eos.api.read-timeout:25000}")
    private int readTimeoutMs;
    
    /**
     * RestTemplate với timeout configuration cho EOS API calls
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Creating RestTemplate for EOS API with timeout: {}ms", timeoutMs);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Connection timeout - thời gian chờ để establish connection
        factory.setConnectTimeout(connectionTimeoutMs);
        
        // Read timeout - thời gian chờ để đọc response
        factory.setReadTimeout(readTimeoutMs);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add error handler
        restTemplate.setErrorHandler(new EOSClientErrorHandler());
        
        log.info("RestTemplate configured successfully for EOS integration");
        return restTemplate;
    }
    
    /**
     * Configuration properties cho EOS integration
     */
    @Bean
    public EOSProperties eosProperties() {
        return EOSProperties.builder()
                .timeout(Duration.ofMillis(timeoutMs))
                .connectionTimeout(Duration.ofMillis(connectionTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}