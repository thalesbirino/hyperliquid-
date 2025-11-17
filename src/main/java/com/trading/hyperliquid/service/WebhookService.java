package com.trading.hyperliquid.service;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.OrderExecution;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.util.TradingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final StrategyService strategyService;
    private final HyperliquidService hyperliquidService;
    private final OrderExecutionService orderExecutionService;

    /**
     * Process TradingView webhook and execute order with automatic stop-loss placement.
     *
     * Flow:
     * 1. Validate strategy credentials
     * 2. Place primary order on Hyperliquid
     * 3. Create OrderExecution record
     * 4. Calculate and place stop-loss order (if slPercent configured)
     * 5. Update OrderExecution with stop-loss details
     */
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        log.info("Processing webhook - Action: {}, StrategyID: {}", request.getAction(), request.getStrategyId());

        try {
            // 1. Validate strategy credentials
            Strategy strategy = strategyService.validateStrategyCredentials(
                    request.getStrategyId(),
                    request.getPassword()
            );

            // 2. Get associated config and user
            Config config = strategy.getConfig();
            User user = strategy.getUser();

            log.debug("Strategy '{}' validated. Asset: {}, User: {}",
                    strategy.getName(), config.getAsset(), user.getUsername());

            // 3. Execute primary order on Hyperliquid
            String orderId = hyperliquidService.placeOrder(request.getAction(), config, user);
            log.info("Primary order placed successfully. Order ID: {}", orderId);

            // 4. Determine order side and get fill price
            OrderExecution.OrderSide orderSide = OrderExecution.OrderSide.fromAction(request.getAction());
            BigDecimal fillPrice = getMockPrice(config.getAsset()); // In real implementation, get from order response

            // 5. Create OrderExecution record
            OrderExecution execution = orderExecutionService.createOrderExecution(
                    orderId,
                    orderSide,
                    fillPrice,
                    config.getLotSize(),
                    strategy,
                    user
            );
            log.info("OrderExecution record created. Execution ID: {}", execution.getId());

            // 6. Place stop-loss order if configured
            String stopLossOrderId = null;
            BigDecimal stopLossPrice = null;

            if (config.getSlPercent() != null && config.getSlPercent().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Calculate stop-loss price
                    stopLossPrice = calculateStopLossPrice(fillPrice, orderSide, config.getSlPercent());

                    log.info("Placing stop-loss order. Price: {}, Percentage: {}%",
                            stopLossPrice, config.getSlPercent());

                    // Place stop-loss order with position-based grouping (as requested by user)
                    stopLossOrderId = hyperliquidService.placeStopLossOrder(
                            orderSide,
                            config.getAssetId(),
                            stopLossPrice,
                            config.getLotSize(),
                            OrderExecution.StopLossGrouping.POSITION_BASED, // Using option 1 as requested
                            config,
                            user
                    );

                    // Update OrderExecution with stop-loss details
                    orderExecutionService.updateStopLossOrder(
                            execution.getId(),
                            stopLossOrderId,
                            stopLossPrice,
                            OrderExecution.StopLossGrouping.POSITION_BASED,
                            OrderExecution.StopLossStatus.ACTIVE
                    );

                    log.info("Stop-loss order placed successfully. SL Order ID: {}", stopLossOrderId);

                } catch (Exception slException) {
                    log.error("Failed to place stop-loss order: {}", slException.getMessage(), slException);

                    // Update execution with failed status
                    orderExecutionService.updateStopLossOrder(
                            execution.getId(),
                            null,
                            stopLossPrice,
                            OrderExecution.StopLossGrouping.POSITION_BASED,
                            OrderExecution.StopLossStatus.FAILED
                    );

                    // Don't fail the entire webhook - primary order was successful
                    log.warn("Primary order executed but stop-loss placement failed. Manual intervention may be required.");
                }
            } else {
                log.debug("Stop-loss not configured for this strategy (slPercent is null or zero)");
            }

            // 7. Build and return success response
            return WebhookResponse.success(
                    orderId,
                    request.getAction().toUpperCase(),
                    config.getAsset(),
                    config.getLotSize().toPlainString(),
                    fillPrice.toPlainString(),
                    "EXECUTED" + (stopLossOrderId != null ? " (Stop-Loss Active)" : "")
            );

        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return WebhookResponse.error("Order execution failed: " + e.getMessage());
        }
    }

    /**
     * Calculate stop-loss price based on fill price and stop-loss percentage.
     *
     * For BUY orders: SL price = fillPrice × (1 - slPercent/100)
     * For SELL orders: SL price = fillPrice × (1 + slPercent/100)
     *
     * @param fillPrice the fill price of the primary order
     * @param orderSide the side of the primary order (BUY or SELL)
     * @param slPercent the stop-loss percentage
     * @return the calculated stop-loss trigger price
     */
    private BigDecimal calculateStopLossPrice(BigDecimal fillPrice, OrderExecution.OrderSide orderSide, BigDecimal slPercent) {
        // Validate slPercent is within acceptable range
        if (slPercent.compareTo(TradingConstants.MIN_SL_PERCENT) < 0 ||
            slPercent.compareTo(TradingConstants.MAX_SL_PERCENT) > 0) {
            throw new IllegalArgumentException(
                    String.format("Stop-loss percentage must be between %s%% and %s%%",
                            TradingConstants.MIN_SL_PERCENT, TradingConstants.MAX_SL_PERCENT));
        }

        // Convert percentage to decimal (e.g., 2% becomes 0.02)
        BigDecimal slMultiplier = slPercent.divide(
                BigDecimal.valueOf(TradingConstants.HUNDRED_PERCENT),
                TradingConstants.PERCENTAGE_SCALE,
                RoundingMode.HALF_UP
        );

        // Calculate stop-loss price
        BigDecimal stopLossPrice;
        if (orderSide.isBuy()) {
            // For BUY: SL below entry price
            stopLossPrice = fillPrice.subtract(fillPrice.multiply(slMultiplier));
        } else {
            // For SELL: SL above entry price
            stopLossPrice = fillPrice.add(fillPrice.multiply(slMultiplier));
        }

        // Round to appropriate scale for Hyperliquid
        return stopLossPrice.setScale(TradingConstants.SL_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Get mock market price for asset.
     * In real implementation, this would come from the order fill response.
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
}
