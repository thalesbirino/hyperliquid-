package com.trading.hyperliquid.service;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.OrderExecution;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * Parameter object to encapsulate order placement context.
 * This reduces method parameter count and improves code maintainability.
 *
 * Following Clean Code principle: "The ideal number of arguments for a function is zero."
 * When we need more than 3 parameters, we should consider grouping them into an object.
 */
@Getter
@Builder
class OrderPlacementContext {

    private final WebhookRequest request;
    private final Strategy strategy;
    private final Config config;
    private final User user;
    private final OrderExecution.OrderSide requestedSide;
    private final OrderExecution.OrderSide lastOrderSide;

    /**
     * Factory method to create context from webhook validation results.
     */
    static OrderPlacementContext from(
            WebhookRequest request,
            Strategy strategy,
            OrderExecution.OrderSide requestedSide,
            OrderExecution.OrderSide lastOrderSide) {

        return OrderPlacementContext.builder()
                .request(request)
                .strategy(strategy)
                .config(strategy.getConfig())
                .user(strategy.getUser())
                .requestedSide(requestedSide)
                .lastOrderSide(lastOrderSide)
                .build();
    }

    /**
     * Factory method for first order (no last order side).
     */
    static OrderPlacementContext forFirstOrder(
            WebhookRequest request,
            Strategy strategy,
            OrderExecution.OrderSide requestedSide) {

        return OrderPlacementContext.builder()
                .request(request)
                .strategy(strategy)
                .config(strategy.getConfig())
                .user(strategy.getUser())
                .requestedSide(requestedSide)
                .lastOrderSide(null)
                .build();
    }
}
