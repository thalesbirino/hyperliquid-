package com.trading.hyperliquid.model.hyperliquid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of an order placement on Hyperliquid.
 * Contains order ID and execution price for SL/TP calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {

    /**
     * Order ID from Hyperliquid (oid)
     */
    private String orderId;

    /**
     * Actual execution price (for SL/TP calculation)
     * For MARKET orders, this is the market price with slippage
     * For LIMIT orders, this is the limit price
     */
    private BigDecimal executionPrice;

    /**
     * Create from just order ID (backward compatibility)
     */
    public static OrderResult fromOrderId(String orderId) {
        return OrderResult.builder()
                .orderId(orderId)
                .build();
    }

    /**
     * Create with order ID and execution price
     */
    public static OrderResult of(String orderId, BigDecimal executionPrice) {
        return OrderResult.builder()
                .orderId(orderId)
                .executionPrice(executionPrice)
                .build();
    }
}
