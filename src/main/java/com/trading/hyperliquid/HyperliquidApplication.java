package com.trading.hyperliquid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.trading.hyperliquid.repository")
public class HyperliquidApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyperliquidApplication.class, args);
    }
}
