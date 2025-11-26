package com.trading.hyperliquid.service;

import com.trading.hyperliquid.client.PythonHyperliquidClient;
import com.trading.hyperliquid.exception.HyperliquidApiException;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.OrderExecution;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.model.enums.OrderSide;
import com.trading.hyperliquid.model.hyperliquid.*;
import com.trading.hyperliquid.util.ErrorMessages;
import com.trading.hyperliquid.util.NonceManager;
import com.trading.hyperliquid.util.TradingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Service for interacting with Hyperliquid API.
 * Supports both MOCK mode (simulation) and REAL mode (actual API calls).
 */
@Service
public class HyperliquidService {

    private static final Logger logger = LoggerFactory.getLogger(HyperliquidService.class);

    private final NonceManager nonceManager;

    @Autowired(required = false)
    private PythonHyperliquidClient pythonClient;

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
     * Place an order on Hyperliquid Exchange.
     * Currently in MOCK mode - logs order details to console instead of making real API calls.
     *
     * @param action the order action string ("buy" or "sell")
     * @param config the trading configuration containing asset, lot size, leverage, and risk management params
     * @param user the user with Hyperliquid wallet credentials and testnet/mainnet configuration
     * @return the order ID (mock ID in POC mode, real order ID when integrated with API)
     * @throws IllegalArgumentException if validation fails (invalid action, missing config, invalid lot size)
     * @throws HyperliquidApiException if order placement fails
     */
    public String placeOrder(String action, Config config, User user) {
        try {
            // Parse and validate order side
            OrderSide orderSide = OrderSide.fromAction(action);
            validateOrderRequest(orderSide, config, user);

            // Determine API endpoint based on user account type
            String apiUrl = getApiUrl(user);
            String accountType = user.getIsTestnet() ? "TESTNET (DEMO)" : "MAINNET (REAL)";
            logger.debug("Using {} endpoint: {}", accountType, apiUrl);

            // Create mock price (in real implementation, this would come from market data)
            BigDecimal mockPrice = getMockPrice(config.getAsset());

            // Build Hyperliquid order
            Order order = orderSide.isBuy()
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
                return executeMockOrder(orderSide, config, user, mockPrice, order, nonce, apiUrl, accountType);
            } else {
                // Real API call to Hyperliquid via Python SDK
                if (pythonClient == null) {
                    throw new HyperliquidApiException("Python API client not initialized. Check application configuration.");
                }
                logger.info("REAL MODE: Placing order via Python SDK - Asset: {}, Side: {}, User: {}",
                    config.getAsset(), orderSide.getAction(), user.getUsername());
                return pythonClient.placeOrder(user, order, "na", apiUrl);
            }

        } catch (Exception e) {
            logger.error("Failed to place order: {}", e.getMessage(), e);
            throw new HyperliquidApiException(ErrorMessages.ORDER_EXECUTION_FAILED + ": " + e.getMessage(), e);
        }
    }

    /**
     * Mock order execution - logs to console instead of making real API call.
     * Calculates stop-loss and take-profit prices based on configuration.
     *
     * @param orderSide the side of the order (BUY or SELL)
     * @param config the trading configuration
     * @param user the user placing the order
     * @param price the order price
     * @param order the constructed order object
     * @param nonce the nonce for the order
     * @param apiUrl the API endpoint URL
     * @param accountType the account type string for display
     * @return the mock order ID
     */
    private String executeMockOrder(OrderSide orderSide, Config config, User user,
                                    BigDecimal price, Order order, long nonce,
                                    String apiUrl, String accountType) {
        String orderId = TradingConstants.MOCK_ORDER_ID_PREFIX +
                        UUID.randomUUID().toString().substring(0, TradingConstants.MOCK_ORDER_ID_LENGTH);

        // Calculate SL/TP prices
        BigDecimal slPrice = null;
        BigDecimal tpPrice = null;

        if (config.getSlPercent() != null) {
            BigDecimal slMultiplier = config.getSlPercent().divide(
                BigDecimal.valueOf(TradingConstants.HUNDRED_PERCENT),
                TradingConstants.PERCENTAGE_SCALE,
                RoundingMode.HALF_UP
            );
            slPrice = orderSide.isBuy()
                    ? price.subtract(price.multiply(slMultiplier))
                    : price.add(price.multiply(slMultiplier));
        }

        if (config.getTpPercent() != null) {
            BigDecimal tpMultiplier = config.getTpPercent().divide(
                BigDecimal.valueOf(TradingConstants.HUNDRED_PERCENT),
                TradingConstants.PERCENTAGE_SCALE,
                RoundingMode.HALF_UP
            );
            tpPrice = orderSide.isBuy()
                    ? price.add(price.multiply(tpMultiplier))
                    : price.subtract(price.multiply(tpMultiplier));
        }

        // Log order execution
        logger.info("╔══════════════════════════════════════════════════════════╗");
        logger.info("║          HYPERLIQUID ORDER EXECUTED (MOCK MODE)          ║");
        logger.info("╠══════════════════════════════════════════════════════════╣");
        logger.info("║ Order ID      : {}", orderId);
        logger.info("║ Action        : {}", orderSide.getAction().toUpperCase());
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
            logger.info("║ Stop Loss     : ${} ({}%)",
                slPrice.setScale(TradingConstants.PRICE_DISPLAY_SCALE, RoundingMode.HALF_UP),
                config.getSlPercent());
        }
        if (tpPrice != null) {
            logger.info("║ Take Profit   : ${} ({}%)",
                tpPrice.setScale(TradingConstants.PRICE_DISPLAY_SCALE, RoundingMode.HALF_UP),
                config.getTpPercent());
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
            case "BTC" -> TradingConstants.MOCK_BTC_PRICE;
            case "ETH" -> TradingConstants.MOCK_ETH_PRICE;
            case "SOL" -> TradingConstants.MOCK_SOL_PRICE;
            case "AVAX" -> TradingConstants.MOCK_AVAX_PRICE;
            case "MATIC" -> TradingConstants.MOCK_MATIC_PRICE;
            default -> TradingConstants.MOCK_DEFAULT_PRICE;
        };
    }

    /**
     * Mask wallet address for security
     */
    private String maskAddress(String address) {
        if (address == null || address.length() < TradingConstants.MIN_ADDRESS_LENGTH) {
            return TradingConstants.ADDRESS_MASK_FALLBACK;
        }
        return address.substring(0, TradingConstants.ADDRESS_PREFIX_LENGTH) +
               TradingConstants.ADDRESS_MASK +
               address.substring(address.length() - TradingConstants.ADDRESS_SUFFIX_LENGTH);
    }

    /**
     * Validate order request parameters.
     *
     * @param orderSide the order side (BUY or SELL)
     * @param config the trading configuration
     * @param user the user placing the order
     * @throws IllegalArgumentException if any validation fails
     */
    private void validateOrderRequest(OrderSide orderSide, Config config, User user) {
        if (orderSide == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_ACTION);
        }

        if (config == null) {
            throw new IllegalArgumentException(ErrorMessages.CONFIG_REQUIRED);
        }

        if (user == null || user.getHyperliquidAddress() == null) {
            throw new IllegalArgumentException(ErrorMessages.USER_REQUIRED);
        }

        if (config.getLotSize().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ErrorMessages.POSITIVE_LOT_SIZE);
        }
    }

    /**
     * Place a stop-loss order on Hyperliquid Exchange.
     * Creates a trigger order that executes when price reaches the stop-loss level.
     * Supports both position-based (positionTpsl) and order-based (normalTpsl) grouping.
     *
     * @param orderSide the side of the PRIMARY order (BUY or SELL) - SL will be opposite
     * @param assetId the Hyperliquid asset ID
     * @param stopLossPrice the trigger price for stop-loss
     * @param size the order size
     * @param groupingType the grouping type (POSITION_BASED or ORDER_BASED)
     * @param config the trading configuration
     * @param user the user placing the order
     * @return the stop-loss order ID
     * @throws HyperliquidApiException if stop-loss placement fails
     */
    public String placeStopLossOrder(
            OrderExecution.OrderSide orderSide,
            Integer assetId,
            BigDecimal stopLossPrice,
            BigDecimal size,
            OrderExecution.StopLossGrouping groupingType,
            Config config,
            User user) {

        try {
            validateStopLossRequest(orderSide, assetId, stopLossPrice, size, user);

            // Determine API endpoint
            String apiUrl = getApiUrl(user);
            String accountType = user.getIsTestnet() ? "TESTNET (DEMO)" : "MAINNET (REAL)";

            // Stop-loss order is OPPOSITE side of primary order (to close position)
            OrderExecution.OrderSide slOrderSide = orderSide.opposite();

            // Create trigger order for stop-loss
            OrderType orderType = OrderType.trigger(
                    stopLossPrice.toPlainString(),
                    TradingConstants.TPSL_STOP_LOSS,
                    true  // isMarket = true (execute as market order when triggered)
            );

            // Build stop-loss order
            Order slOrder = slOrderSide.isBuy()
                    ? Order.limitBuy(
                            assetId,
                            stopLossPrice.toPlainString(),
                            size.toPlainString(),
                            config.getTimeInForce()
                    )
                    : Order.limitSell(
                            assetId,
                            stopLossPrice.toPlainString(),
                            size.toPlainString(),
                            config.getTimeInForce()
                    );

            // Override with trigger order type
            slOrder.setT(orderType);

            // Set reduce-only flag (ensures SL only closes positions, never increases them)
            slOrder.setR(true);

            // Format grouping string for Hyperliquid API
            String grouping = getGroupingString(groupingType);

            // Create order action with grouping
            OrderAction orderAction = OrderAction.builder()
                    .type("order")
                    .orders(List.of(slOrder))
                    .grouping(grouping)
                    .build();

            // Generate nonce
            long nonce = nonceManager.getNextNonce(user.getHyperliquidAddress());

            if (mockMode) {
                return executeMockStopLoss(orderSide, slOrderSide, assetId, stopLossPrice, size,
                        groupingType, config, user, nonce, apiUrl, accountType);
            } else {
                // Real API call to Hyperliquid with stop-loss via Python SDK
                if (pythonClient == null) {
                    throw new HyperliquidApiException("Python API client not initialized. Check application configuration.");
                }
                logger.info("REAL MODE: Placing stop-loss order via Python SDK - Asset ID: {}, Price: {}, Grouping: {}, User: {}",
                    assetId, stopLossPrice, grouping, user.getUsername());
                return pythonClient.placeOrder(user, slOrder, grouping, apiUrl);
            }

        } catch (Exception e) {
            logger.error("Failed to place stop-loss order: {}", e.getMessage(), e);
            throw new HyperliquidApiException("Stop-loss order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mock stop-loss order execution - logs to console instead of making real API call.
     *
     * @param primaryOrderSide the side of the primary order
     * @param slOrderSide the side of the stop-loss order (opposite of primary)
     * @param assetId the asset ID
     * @param stopLossPrice the stop-loss trigger price
     * @param size the order size
     * @param groupingType the grouping type
     * @param config the trading configuration
     * @param user the user placing the order
     * @param nonce the nonce for the order
     * @param apiUrl the API endpoint URL
     * @param accountType the account type string for display
     * @return the mock stop-loss order ID
     */
    private String executeMockStopLoss(
            OrderExecution.OrderSide primaryOrderSide,
            OrderExecution.OrderSide slOrderSide,
            Integer assetId,
            BigDecimal stopLossPrice,
            BigDecimal size,
            OrderExecution.StopLossGrouping groupingType,
            Config config,
            User user,
            long nonce,
            String apiUrl,
            String accountType) {

        String slOrderId = TradingConstants.MOCK_ORDER_ID_PREFIX +
                UUID.randomUUID().toString().substring(0, TradingConstants.MOCK_ORDER_ID_LENGTH);

        String groupingName = groupingType == OrderExecution.StopLossGrouping.POSITION_BASED
                ? "Position-Based (positionTpsl)"
                : "Order-Based (normalTpsl)";

        // Log stop-loss order execution
        logger.info("╔══════════════════════════════════════════════════════════╗");
        logger.info("║        HYPERLIQUID STOP-LOSS PLACED (MOCK MODE)         ║");
        logger.info("╠══════════════════════════════════════════════════════════╣");
        logger.info("║ SL Order ID   : {}", slOrderId);
        logger.info("║ Primary Side  : {}", primaryOrderSide.getAction().toUpperCase());
        logger.info("║ SL Side       : {} (OPPOSITE)", slOrderSide.getAction().toUpperCase());
        logger.info("║ Asset         : {}", config.getAsset());
        logger.info("║ Asset ID      : {}", assetId);
        logger.info("║ Size          : {}", size);
        logger.info("║ Trigger Price : ${}", stopLossPrice);
        logger.info("║ Order Type    : TRIGGER (Market when triggered)");
        logger.info("║ Trigger Type  : STOP-LOSS");
        logger.info("║ Grouping      : {}", groupingName);
        logger.info("║ Reduce-Only   : true");
        logger.info("║ Account Type  : {}", accountType);
        logger.info("║ API Endpoint  : {}", apiUrl);
        logger.info("║ User          : {}", user.getUsername());
        logger.info("║ Wallet        : {}", maskAddress(user.getHyperliquidAddress()));
        logger.info("║ Nonce         : {}", nonce);
        logger.info("║ Status        : ACTIVE (Waiting for trigger)");
        logger.info("╚══════════════════════════════════════════════════════════╝");

        return slOrderId;
    }

    /**
     * Get grouping string for Hyperliquid API based on grouping type.
     *
     * @param groupingType the grouping enum
     * @return the Hyperliquid API grouping string
     */
    private String getGroupingString(OrderExecution.StopLossGrouping groupingType) {
        return groupingType == OrderExecution.StopLossGrouping.POSITION_BASED
                ? TradingConstants.GROUPING_POSITION_TPSL
                : TradingConstants.GROUPING_NORMAL_TPSL;
    }

    /**
     * Validate stop-loss order request parameters.
     *
     * @param orderSide the primary order side
     * @param assetId the asset ID
     * @param stopLossPrice the stop-loss trigger price
     * @param size the order size
     * @param user the user placing the order
     * @throws IllegalArgumentException if any validation fails
     */
    private void validateStopLossRequest(
            OrderExecution.OrderSide orderSide,
            Integer assetId,
            BigDecimal stopLossPrice,
            BigDecimal size,
            User user) {

        if (orderSide == null) {
            throw new IllegalArgumentException("Order side is required for stop-loss placement");
        }

        if (assetId == null) {
            throw new IllegalArgumentException("Asset ID is required for stop-loss placement");
        }

        if (stopLossPrice == null || stopLossPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid stop-loss price is required");
        }

        if (size == null || size.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid order size is required");
        }

        if (user == null || user.getHyperliquidAddress() == null) {
            throw new IllegalArgumentException(ErrorMessages.USER_REQUIRED);
        }
    }

    /**
     * Cancel an order on Hyperliquid Exchange.
     * Used for cancelling both regular orders and stop-loss orders.
     *
     * @param orderId the order ID to cancel (oid from Hyperliquid)
     * @param assetId the asset ID
     * @param user the user who placed the order
     * @throws HyperliquidApiException if cancellation fails
     */
    public void cancelOrder(String orderId, Integer assetId, User user) {
        try {
            // Determine API endpoint
            String apiUrl = getApiUrl(user);
            String accountType = user.getIsTestnet() ? "TESTNET (DEMO)" : "MAINNET (REAL)";

            // Generate nonce
            long nonce = nonceManager.getNextNonce(user.getHyperliquidAddress());

            if (mockMode) {
                logger.info("╔══════════════════════════════════════════════════════════╗");
                logger.info("║          HYPERLIQUID ORDER CANCELLED (MOCK MODE)         ║");
                logger.info("╠══════════════════════════════════════════════════════════╣");
                logger.info("║ Order ID      : {}", orderId);
                logger.info("║ Asset ID      : {}", assetId);
                logger.info("║ Account Type  : {}", accountType);
                logger.info("║ API Endpoint  : {}", apiUrl);
                logger.info("║ User          : {}", user.getUsername());
                logger.info("║ Wallet        : {}", maskAddress(user.getHyperliquidAddress()));
                logger.info("║ Nonce         : {}", nonce);
                logger.info("║ Status        : CANCELLED");
                logger.info("╚══════════════════════════════════════════════════════════╝");
            } else {
                // Real API call (not implemented in POC)
                // TODO: Implement real HTTP POST to apiUrl with cancel action
                throw new HyperliquidApiException(ErrorMessages.API_INTEGRATION_NOT_IMPLEMENTED);
            }

        } catch (Exception e) {
            logger.error("Failed to cancel order {}: {}", orderId, e.getMessage(), e);
            throw new HyperliquidApiException("Order cancellation failed: " + e.getMessage(), e);
        }
    }
}
