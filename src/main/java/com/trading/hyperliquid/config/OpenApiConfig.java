package com.trading.hyperliquid.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hyperliquid Trading Bot API",
                version = "1.0.0",
                description = """
                        TradingView Webhook to Hyperliquid Order Execution API

                        This POC receives webhooks from TradingView Pine Script strategies and executes
                        corresponding trade orders on Hyperliquid DeFi Exchange.

                        **Features:**
                        - TradingView webhook integration
                        - Strategy management with credentials
                        - User management with Hyperliquid wallet integration
                        - Trading configuration management
                        - JWT-based authentication for admin endpoints
                        - Mock order execution (configurable for real API)

                        **Authentication:**
                        Most endpoints require JWT authentication. Use the /api/auth/login endpoint to obtain a token.
                        """,
                contact = @Contact(
                        name = "Trading Bot Support",
                        email = "support@example.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server"),
                @Server(url = "https://api.example.com", description = "Production Server")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Enter JWT token obtained from /api/auth/login endpoint"
)
public class OpenApiConfig {
}
