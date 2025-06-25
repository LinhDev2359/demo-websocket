package com.wallet.config;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Properties cho EOS configuration
 */
@Data
@Builder
public class EOSProperties {
    private Duration timeout;
    private Duration connectionTimeout;
    private Duration readTimeout;
}