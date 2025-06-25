package com.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.wallet", "com.eoswallet"})
public class WalletSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WalletSystemApplication.class, args);
    }
}