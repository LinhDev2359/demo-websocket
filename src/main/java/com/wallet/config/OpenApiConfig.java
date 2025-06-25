package com.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * Configures Swagger UI and API documentation
 * 
 * Features:
 * 1. JWT Bearer authentication setup
 * 2. API information and metadata
 * 3. Server configuration for different environments
 * 4. Security schemes for protected endpoints
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(securityRequirement())
                .schemaRequirement("Bearer Authentication", securityScheme());
    }

    private Info apiInfo() {
        return new Info()
                .title("EOS Wallet Management System API")
                .description("Comprehensive REST API for EOS blockchain wallet management with real-time features")
                .version("1.0.0")
                .contact(new Contact()
                        .name("EOS Wallet Team")
                        .email("support@eoswallet.com")
                        .url("https://eoswallet.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> serverList() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Server devServer = new Server()
                .url("https://dev-api.eoswallet.com")
                .description("Development server");

        Server prodServer = new Server()
                .url("https://api.eoswallet.com")
                .description("Production server");

        return List.of(localServer, devServer, prodServer);
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("Bearer Authentication");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("Enter JWT token (without 'Bearer ' prefix)");
    }
}