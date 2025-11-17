package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.HyperliquidApiException;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.model.hyperliquid.*;
import com.trading.hyperliquid.util.NonceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for interacting with Hyperliquid API.
 * Currently in MOCK mode for POC - logs orders to console instead of making real API calls.
 */
@Service
public class HyperliquidService {

    private static final Logger logger = LoggerFactory.getLogger(HyperliquidService.class);

    private final NonceManager nonceManager;

    @Value("${hyperliquid.api.mock-mode:true}")
    private boolean mockMode;

    @Value("${hyperliquid.api.testnet-url}")
    private String testnetUrl;

    @Value("${hyperliquid.api.mainnet-url}")
    private String mainnetUrl;

    public HyperliquidService(NonceManager nonceManager) {
        this.nonceManager = nonceManager;
    }

    /**
     * Get the appropriate API URL based on user's account type
     *
     * @param user User with account configuration
     * @return API URL (testnet or mainnet)
     */
    private String getApiUrl(User user) {
        return user.getIsTestnet() ? testnetUrl : mainnetUrl;
    }

    /**
     * Place an order on Hyperliquid
     *
     * @param action Buy or Sell
     * @param config Trading configuration
     * @param user User with Hyperliquid credentials
     * @return Simulated order ID
     */
    public String placeOrder(String action, Config config, User user) {
        try {
            // Validate inputs
            validateOrderRequest(action, config, user);

            // Determine API endpoint based on user account type
            String apiUrl = getApiUrl(user);
            String accountType = user.getIsTestnet() ? "TESTNET (DEMO)" : "MAINNET (REAL)";
            logger.debug("Using {} endpoint: {}", accountType, apiUrl);

            // Determine buy/sell
            boolean isBuy = action.equalsIgnoreCase("buy");

            // Create mock price (in real implementation, this would come from market data)
            BigDecimal mockPrice = getMockPrice(config.getAsset());

            // Build Hyperliquid order
            Order order = isBuy
                    ? Order.limitBuy(
                            config.getAssetId(),
                            mockPrice.toPlainString(),
                            config.getLotSize().toPlainString(),
                            config.getTimeInForce()
                    )
                    : Order.limitSell(
                            config.getAssetId(),
                            mockPrice.toPlainString(),
                            config.getLotSize().toPlainString(),
                            config.getTimeInForce()
                    );

            // Create order action
            OrderAction orderAction = OrderAction.placeOrder(order);

            // Generate nonce
            long nonce = nonceManager.getNextNonce(user.getHyperliquidAddress());

            // In real implementation, this would:
            // 1. Sign the order using HyperliquidSignerService
            // 2. Make HTTP POST to Hyperliquid API
            // 3. Parse and return actual response

            if (mockMode) {
                return executeMockOrder(action, config, user, mockPrice, order, nonce, apiUrl, accountType);
            } else {
                // Real API call (not implemented in POC)
                // TODO: Implement real HTTP POST to apiUrl with signed request
                throw new HyperliquidApiException("Real API integration not implemented in POC mode");
            }

        } catch (Exception e) {
            logger.error("Failed to place order: {}", e.getMessage(), e);
            throw new HyperliquidApiException("Failed to place order: " + e.getMessage(), e);
        }
    }

    /**
     * Mock order execution - logs to console instead of making real API call
     */
    private String executeMockOrder(String action, Config config, User user,
                                    BigDecimal price, Order order, long nonce,
                                    String apiUrl, String accountType) {
        String orderId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8);

        // Calculate SL/TP prices
        BigDecimal slPrice = null;
        BigDecimal tpPrice = null;

        if (config.getSlPercent() != null) {
            BigDecimal slMultiplier = config.getSlPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            slPrice = action.equalsIgnoreCase("buy")
                    ? price.subtract(price.multiply(slMultiplier))
                    : price.add(price.multiply(slMultiplier));
        }

        if (config.getTpPercent() != null) {
            BigDecimal tpMultiplier = config.getTpPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            tpPrice = action.equalsIgnoreCase("buy")
                    ? price.add(price.multiply(tpMultiplier))
                    : price.subtract(price.multiply(tpMultiplier));
        }

        // Log order execution
        logger.info("╔══════════════════════════════════════════════════════════╗");
        logger.info("║          HYPERLIQUID ORDER EXECUTED (MOCK MODE)          ║");
        logger.info("╠══════════════════════════════════════════════════════════╣");
        logger.info("║ Order ID      : {}", orderId);
        logger.info("║ Action        : {}", action.toUpperCase());
        logger.info("║ Asset         : {}", config.getAsset());
        logger.info("║ Asset ID      : {}", config.getAssetId());
        logger.info("║ Size          : {}", config.getLotSize());
        logger.info("║ Price         : ${}", price);
        logger.info("║ Leverage      : {}x", config.getLeverage());
        logger.info("║ Order Type    : {}", config.getOrderType());
        logger.info("║ Time In Force : {}", config.getTimeInForce());
        logger.info("║ Account Type  : {}", accountType);
        logger.info("║ API Endpoint  : {}", apiUrl);
        if (slPrice != null) {
            logger.info("║ Stop Loss     : ${} ({}%)", slPrice.setScale(2, RoundingMode.HALF_UP), config.getSlPercent());
        }
        if (tpPrice != null) {
            logger.info("║ Take Profit   : ${} ({}%)", tpPrice.setScale(2, RoundingMode.HALF_UP), config.getTpPercent());
        }
        logger.info("║ User          : {}", user.getUsername());
        logger.info("║ Wallet        : {}", maskAddress(user.getHyperliquidAddress()));
        logger.info("║ Nonce         : {}", nonce);
        logger.info("║ Status        : EXECUTED");
        logger.info("╚══════════════════════════════════════════════════════════╝");

        return orderId;
    }

    /**
     * Get mock market price for asset
     */
    private BigDecimal getMockPrice(String asset) {
        return switch (asset.toUpperCase()) {
            case "BTC" -> new BigDecimal("45000.00");
            case "ETH" -> new BigDecimal("2500.00");
            case "SOL" -> new BigDecimal("110.00");
            case "AVAX" -> new BigDecimal("35.00");
            case "MATIC" -> new BigDecimal("0.85");
            default -> new BigDecimal("100.00");
        };
    }

    /**
     * Mask wallet address for security
     */
    private String maskAddress(String address) {
        if (address == null || address.length() < 10) {
            return "****";
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    /**
     * Validate order request
     */
    private void validateOrderRequest(String action, Config config, User user) {
        if (action == null || (!action.equalsIgnoreCase("buy") && !action.equalsIgnoreCase("sell"))) {
            throw new IllegalArgumentException("Action must be 'buy' or 'sell'");
        }

        if (config == null) {
            throw new IllegalArgumentException("Config is required");
        }

        if (user == null || user.getHyperliquidAddress() == null) {
            throw new IllegalArgumentException("User with Hyperliquid address is required");
        }

        if (config.getLotSize().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lot size must be positive");
        }
    }

    /**
     * Cancel order (mock implementation)
     */
    public void cancelOrder(String orderId, Integer assetId, User user) {
        logger.info("[HyperliquidService] MOCK: Cancelled order {} for asset {} by user {}",
                orderId, assetId, user.getUsername());
    }
}
