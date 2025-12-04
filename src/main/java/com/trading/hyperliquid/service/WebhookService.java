package com.trading.hyperliquid.service;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.OrderExecution;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.model.hyperliquid.OrderResult;
import com.trading.hyperliquid.util.TradingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final StrategyService strategyService;
    private final HyperliquidService hyperliquidService;
    private final OrderExecutionService orderExecutionService;

    /**
     * Process TradingView webhook and execute order following flowchart logic.
     *
     * Flowchart Decision Tree:
     * 1. Fetch last order for strategy
     * 2. Is this First Order? → Yes: Place order and exit
     * 3. Is Previous Position Closed? → Yes: Place new order and exit
     * 4. Check prev order action vs current action:
     *    - Same action → Pyramid=TRUE? → Yes: Allow / No: Reject
     *    - Opp action → Cancel SL → Square off → Inverse=TRUE? → Yes: Place opposite / No: Just close
     */
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        log.info("Processing webhook - Action: {}, StrategyID: {}", request.getAction(), request.getStrategyId());

        try {
            // ================================================================
            // STEP 1: Validate strategy credentials
            // ================================================================
            Strategy strategy = strategyService.validateStrategyCredentials(
                    request.getStrategyId(),
                    request.getPassword()
            );

            Config config = strategy.getConfig();
            User user = strategy.getUser();
            OrderExecution.OrderSide requestedSide = OrderExecution.OrderSide.fromAction(request.getAction());

            log.info("Strategy '{}' validated. Requested action: {}, Pyramid: {}, Inverse: {}",
                    strategy.getName(), request.getAction(), strategy.getPyramid(), strategy.getInverse());

            // ================================================================
            // FLOWCHART: Fetch last order for this particular strategy
            // ================================================================
            Optional<OrderExecution> lastOrderOpt = orderExecutionService.getLastOrderByStrategy(strategy.getId());

            // ================================================================
            // For inverse mode, calculate the actual order side that will be placed
            // ================================================================
            OrderExecution.OrderSide actualOrderSide = requestedSide;
            if (strategy.getInverse()) {
                actualOrderSide = requestedSide.isBuy()
                        ? OrderExecution.OrderSide.SELL
                        : OrderExecution.OrderSide.BUY;
                log.info("INVERSE MODE: Requested {} will be placed as {}", requestedSide, actualOrderSide);
            }

            // ================================================================
            // FLOWCHART DECISION 1: Is this First Order?
            // ================================================================
            if (lastOrderOpt.isEmpty()) {
                log.info("FLOWCHART PATH: First order for strategy - placing immediately");
                return placeOrderAndReturnResponse(request, strategy, config, user, actualOrderSide);
            }

            OrderExecution lastOrder = lastOrderOpt.get();
            log.debug("Last order found: ID={}, Side={}, ClosedAt={}",
                    lastOrder.getId(), lastOrder.getOrderSide(), lastOrder.getClosedAt());

            // ================================================================
            // FLOWCHART DECISION 2: Is Previous Position Closed?
            // ================================================================
            if (lastOrder.getClosedAt() != null) {
                log.info("FLOWCHART PATH: Previous position closed - placing new order");
                return placeOrderAndReturnResponse(request, strategy, config, user, actualOrderSide);
            }

            // ================================================================
            // FLOWCHART DECISION 3: Check prev order action and current action
            // Compare the ACTUAL order sides (not the requested webhook action)
            // ================================================================
            OrderExecution.OrderSide lastOrderSide = lastOrder.getOrderSide();
            boolean isSameDirection = (lastOrderSide == actualOrderSide);
            log.debug("Direction check: lastOrderSide={}, actualOrderSide={}, isSameDirection={}",
                    lastOrderSide, actualOrderSide, isSameDirection);

            OrderPlacementContext context = OrderPlacementContext.from(
                    request, strategy, actualOrderSide, lastOrderSide);

            if (isSameDirection) {
                return handleSameDirectionOrder(context);
            } else {
                return handleOppositeDirectionOrder(context);
            }

        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return WebhookResponse.error("Order execution failed: " + e.getMessage());
        }
    }

    /**
     * Handle same direction order (pyramid logic).
     * FLOWCHART DECISION 4: Pyramid = TRUE?
     *
     * @param context Order placement context
     * @return WebhookResponse
     */
    private WebhookResponse handleSameDirectionOrder(OrderPlacementContext context) {

        log.debug("FLOWCHART PATH: Same direction detected ({} == {})",
                context.getLastOrderSide(), context.getRequestedSide());

        // ================================================================
        // FLOWCHART DECISION 4: Pyramid = TRUE?
        // ================================================================
        if (context.getStrategy().getPyramid()) {
            log.info("FLOWCHART PATH: Pyramid=TRUE - allowing add-on order (same direction)");
            // context.getRequestedSide() already contains the actual order side (after inverse adjustment)
            return placeOrderAndReturnResponse(
                    context.getRequest(),
                    context.getStrategy(),
                    context.getConfig(),
                    context.getUser(),
                    context.getRequestedSide());
        } else {
            log.warn("FLOWCHART PATH: Pyramid=FALSE - rejecting order (same direction not allowed)");
            return WebhookResponse.error(
                    "Cannot add to existing position (Pyramid=FALSE). " +
                    "Enable pyramid flag to allow multiple positions in same direction."
            );
        }
    }

    /**
     * Handle opposite direction order (inverse logic).
     * Steps: Cancel SL → Square off → Check inverse flag
     *
     * @param context Order placement context
     * @return WebhookResponse
     */
    private WebhookResponse handleOppositeDirectionOrder(OrderPlacementContext context) {

        log.info("FLOWCHART PATH: Opposite direction detected ({} vs {})",
                context.getLastOrderSide(), context.getRequestedSide());

        // ================================================================
        // FLOWCHART CRITICAL STEP: Cancel Pending SL Orders (RED HIGHLIGHT)
        // ================================================================
        List<OrderExecution> openPositions = orderExecutionService
                .getOpenPositionsByStrategy(context.getStrategy().getId());
        cancelPendingStopLossOrders(openPositions, context.getConfig(), context.getUser());

        // ================================================================
        // EXECUTE CLOSE ORDER ON HYPERLIQUID
        // This is critical - we need to actually close the position on the exchange!
        // To close a position, we must place an order OPPOSITE to the lastOrderSide:
        // - If lastOrderSide is BUY (we have a LONG), we need to SELL to close
        // - If lastOrderSide is SELL (we have a SHORT), we need to BUY to close
        // ================================================================
        String closeOrderId = null;
        try {
            // Determine close action based on the ACTUAL position side in DB/exchange
            // This is independent of inverse mode - we're closing whatever position exists
            String closeAction = context.getLastOrderSide().isBuy() ? "sell" : "buy";

            // Calculate total position size to close (sum of all open positions)
            // For pyramid mode, this may be larger than a single lot size
            java.math.BigDecimal totalCloseSize = openPositions.stream()
                    .map(OrderExecution::getOrderSize)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            log.info("Closing position: lastOrderSide={}, closeAction={}, totalSize={}",
                    context.getLastOrderSide(), closeAction, totalCloseSize);

            log.info("Executing close order on Hyperliquid (reduce_only=true)...");
            closeOrderId = hyperliquidService.placeCloseOrder(
                    closeAction,
                    context.getConfig(),
                    context.getUser(),
                    totalCloseSize
            );
            log.info("Close order executed. Order ID: {}", closeOrderId);
        } catch (Exception e) {
            log.error("Failed to execute close order on Hyperliquid: {}", e.getMessage());
            // Continue with closing in DB even if exchange order fails
        }

        // ================================================================
        // FLOWCHART: Square off position (Close all open orders in DB)
        // ================================================================
        orderExecutionService.closeAllPositions(openPositions);
        log.info("Squared off {} positions", openPositions.size());

        // ================================================================
        // FLOWCHART DECISION 5: Inverse = TRUE?
        // ================================================================
        if (context.getStrategy().getInverse()) {
            log.info("FLOWCHART PATH: Inverse=TRUE - placing new order in opposite direction");
            // context.getRequestedSide() already contains the actual order side (after inverse adjustment)
            // For inverse mode strategy, we want to open a new position in this direction
            return placeOrderAndReturnResponse(
                    context.getRequest(),
                    context.getStrategy(),
                    context.getConfig(),
                    context.getUser(),
                    context.getRequestedSide());
        } else {
            log.info("FLOWCHART PATH: Inverse=FALSE - marked closed, NO new position");
            return WebhookResponse.success(
                    closeOrderId != null ? closeOrderId : "CLOSED",
                    context.getRequest().getAction().toUpperCase(),
                    context.getConfig().getAsset(),
                    context.getConfig().getLotSize().toString(),
                    getMockPrice(context.getConfig().getAsset()).toString(),
                    "Position closed (Inverse=FALSE). No opposite order placed."
            );
        }
    }

    /**
     * Cancel all active stop-loss orders for open positions.
     * This is a critical step before position reversal (highlighted in RED on flowchart).
     *
     * @param openPositions List of open positions
     * @param config Config entity
     * @param user User entity
     */
    private void cancelPendingStopLossOrders(
            List<OrderExecution> openPositions,
            Config config,
            User user) {

        log.info("Cancelling {} pending SL orders before square-off", openPositions.size());

        for (OrderExecution position : openPositions) {
            if (position.getStopLossStatus() == OrderExecution.StopLossStatus.ACTIVE &&
                position.getStopLossOrderId() != null) {
                try {
                    hyperliquidService.cancelOrder(
                            position.getStopLossOrderId(),
                            config.getAssetId(),
                            user
                    );
                    log.info("Cancelled SL order: {}", position.getStopLossOrderId());
                } catch (Exception e) {
                    log.error("Failed to cancel SL order {}: {}",
                            position.getStopLossOrderId(), e.getMessage());
                    // Continue - don't fail the entire operation
                }
            }
        }
    }

    /**
     * Helper method to place order and return response.
     * This encapsulates the common flow: place order → create execution → place SL → return response
     *
     * @param request Webhook request
     * @param strategy Strategy entity
     * @param config Config entity
     * @param user User entity
     * @param orderSide Side of the order (BUY/SELL)
     * @return WebhookResponse
     */
    private WebhookResponse placeOrderAndReturnResponse(
            WebhookRequest request,
            Strategy strategy,
            Config config,
            User user,
            OrderExecution.OrderSide orderSide) {

        try {
            // Determine actual order action - invert if strategy is inverse mode
            String actualAction = request.getAction();
            if (strategy.getInverse()) {
                actualAction = "buy".equalsIgnoreCase(request.getAction()) ? "sell" : "buy";
                log.info("INVERSE MODE: Original action '{}' inverted to '{}'", request.getAction(), actualAction);
            }

            // 1. Place primary order on Hyperliquid - returns OrderResult with order ID and execution price
            OrderResult orderResult = hyperliquidService.placeOrder(actualAction, config, user);
            String orderId = orderResult.getOrderId();
            log.info("Primary order placed. Order ID: {}", orderId);

            // 2. Get fill price from order result (real market price), fallback to mock if not available
            BigDecimal fillPrice = orderResult.getExecutionPrice();
            if (fillPrice == null) {
                fillPrice = getMockPrice(config.getAsset());
                log.warn("No execution price in order result, using mock price: {}", fillPrice);
            } else {
                log.info("Using actual execution price for SL calculation: {}", fillPrice);
            }

            // 3. Create OrderExecution record
            OrderExecution execution = orderExecutionService.createOrderExecution(
                    orderId,
                    orderSide,
                    fillPrice,
                    config.getLotSize(),
                    strategy,
                    user
            );
            log.info("OrderExecution created. ID: {}", execution.getId());

            // 4. Place stop-loss order if configured
            String stopLossOrderId = placeStopLossIfConfigured(execution, orderSide, fillPrice, config, user);

            // 5. Return success response
            return WebhookResponse.success(
                    orderId,
                    request.getAction().toUpperCase(),
                    config.getAsset(),
                    config.getLotSize().toPlainString(),
                    fillPrice.toPlainString(),
                    "EXECUTED" + (stopLossOrderId != null ? " (SL Active)" : "")
            );

        } catch (Exception e) {
            log.error("Failed to place order: {}", e.getMessage(), e);
            throw new RuntimeException("Order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Place stop-loss order if configured in the config.
     * Returns the stop-loss order ID if successful, null otherwise.
     *
     * @param execution Order execution record
     * @param orderSide Order side (BUY/SELL)
     * @param fillPrice Fill price of the primary order
     * @param config Config entity
     * @param user User entity
     * @return Stop-loss order ID or null
     */
    private String placeStopLossIfConfigured(
            OrderExecution execution,
            OrderExecution.OrderSide orderSide,
            BigDecimal fillPrice,
            Config config,
            User user) {

        if (config.getSlPercent() == null || config.getSlPercent().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal stopLossPrice = null;
        try {
            stopLossPrice = calculateStopLossPrice(fillPrice, orderSide, config.getSlPercent());

            log.info("Placing SL order. Price: {}, Percentage: {}%",
                    stopLossPrice, config.getSlPercent());

            String stopLossOrderId = hyperliquidService.placeStopLossOrder(
                    orderSide,
                    config.getAssetId(),
                    stopLossPrice,
                    config.getLotSize(),
                    OrderExecution.StopLossGrouping.POSITION_BASED,
                    config,
                    user
            );

            orderExecutionService.updateStopLossOrder(
                    execution.getId(),
                    stopLossOrderId,
                    stopLossPrice,
                    OrderExecution.StopLossGrouping.POSITION_BASED,
                    OrderExecution.StopLossStatus.ACTIVE
            );

            log.info("SL order placed. SL Order ID: {}", stopLossOrderId);
            return stopLossOrderId;

        } catch (Exception slException) {
            log.error("Failed to place SL: {}", slException.getMessage(), slException);

            orderExecutionService.updateStopLossOrder(
                    execution.getId(),
                    null,
                    stopLossPrice,
                    OrderExecution.StopLossGrouping.POSITION_BASED,
                    OrderExecution.StopLossStatus.FAILED
            );

            log.warn("Primary order executed but SL placement failed");
            return null;
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
