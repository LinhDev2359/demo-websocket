package com.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

/**
 * Custom error handler cho EOS API calls
 * Handle các lỗi từ EOS blockchain API
 */
@Slf4j
public class EOSClientErrorHandler extends DefaultResponseErrorHandler {
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode().value());
        
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                log.warn("EOS API client error: {} {}", statusCode.value(), statusCode.getReasonPhrase());
                if (statusCode == HttpStatus.BAD_REQUEST) {
                    throw new HttpClientErrorException(statusCode, "Invalid EOS API request");
                } else if (statusCode == HttpStatus.NOT_FOUND) {
                    throw new HttpClientErrorException(statusCode, "EOS wallet or resource not found");
                } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    throw new HttpClientErrorException(statusCode, "EOS API rate limit exceeded");
                } else {
                    throw new HttpClientErrorException(statusCode, "EOS API client error");
                }
                
            case SERVER_ERROR:
                log.error("EOS API server error: {} {}", statusCode.value(), statusCode.getReasonPhrase());
                if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                    throw new HttpServerErrorException(statusCode, "EOS API service unavailable");
                } else if (statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                    throw new HttpServerErrorException(statusCode, "EOS API gateway timeout");
                } else {
                    throw new HttpServerErrorException(statusCode, "EOS API server error");
                }
                
            default:
                log.warn("Unexpected EOS API response: {} {}", statusCode.value(), statusCode.getReasonPhrase());
                super.handleError(response);
        }
    }
}