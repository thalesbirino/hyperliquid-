package com.trading.hyperliquid.model.enums;

/**
 * Represents the side of an order (buy or sell).
 * Replaces boolean flags for better code readability and type safety.
 */
public enum OrderSide {
    /**
     * Buy order - opens a long position or closes a short position
     */
    BUY("buy", true),

    /**
     * Sell order - opens a short position or closes a long position
     */
    SELL("sell", false);

    private final String action;
    private final boolean isBuy;

    OrderSide(String action, boolean isBuy) {
        this.action = action;
        this.isBuy = isBuy;
    }

    /**
     * Get the action string representation (e.g., "buy", "sell")
     *
     * @return the action string
     */
    public String getAction() {
        return action;
    }

    /**
     * Check if this is a buy order
     *
     * @return true if buy, false if sell
     */
    public boolean isBuy() {
        return isBuy;
    }

    /**
     * Parse order side from string action
     *
     * @param action the action string ("buy" or "sell", case-insensitive)
     * @return the corresponding OrderSide
     * @throws IllegalArgumentException if action is not "buy" or "sell"
     */
    public static OrderSide fromAction(String action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        String normalizedAction = action.toLowerCase().trim();
        for (OrderSide side : values()) {
            if (side.action.equals(normalizedAction)) {
                return side;
            }
        }

        throw new IllegalArgumentException("Invalid action: " + action + ". Must be 'buy' or 'sell'");
    }

    /**
     * Get the opposite side
     *
     * @return SELL if this is BUY, BUY if this is SELL
     */
    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }

    @Override
    public String toString() {
        return action;
    }
}
