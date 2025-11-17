package com.trading.hyperliquid.util;

/**
 * Centralized error messages for the application.
 * Provides consistent error messaging across all layers.
 */
public final class ErrorMessages {

    // Order Validation Messages
    public static final String INVALID_ACTION = "Action must be 'buy' or 'sell'";
    public static final String CONFIG_REQUIRED = "Config is required";
    public static final String USER_REQUIRED = "User with Hyperliquid address is required";
    public static final String POSITIVE_LOT_SIZE = "Lot size must be positive";
    public static final String INVALID_ASSET = "Invalid asset";

    // Authentication Messages
    public static final String INVALID_CREDENTIALS = "Invalid username or password";
    public static final String INVALID_TOKEN = "Invalid or expired token";
    public static final String UNAUTHORIZED = "Unauthorized access";

    // Strategy Messages
    public static final String STRATEGY_INACTIVE = "Strategy is inactive";
    public static final String INVALID_STRATEGY_CREDENTIALS = "Invalid strategy ID or password";

    // Duplication Messages
    public static final String DUPLICATE_USERNAME = "Username already exists";
    public static final String DUPLICATE_EMAIL = "Email already exists";
    public static final String DUPLICATE_STRATEGY_ID = "Strategy ID already exists";

    // API Integration Messages
    public static final String API_INTEGRATION_NOT_IMPLEMENTED = "Real API integration not implemented in POC mode";
    public static final String ORDER_EXECUTION_FAILED = "Failed to place order";

    // Resource Not Found Messages Templates
    private static final String NOT_FOUND_TEMPLATE = "%s not found with %s: %s";

    public static String userNotFound(Long id) {
        return String.format(NOT_FOUND_TEMPLATE, "User", "id", id);
    }

    public static String configNotFound(Long id) {
        return String.format(NOT_FOUND_TEMPLATE, "Config", "id", id);
    }

    public static String strategyNotFound(Long id) {
        return String.format(NOT_FOUND_TEMPLATE, "Strategy", "id", id);
    }

    public static String strategyNotFoundByStrategyId(String strategyId) {
        return String.format(NOT_FOUND_TEMPLATE, "Strategy", "strategyId", strategyId);
    }

    public static String duplicateField(String fieldName, String value) {
        return String.format("%s already exists: %s", fieldName, value);
    }

    private ErrorMessages() {
        throw new AssertionError("ErrorMessages class cannot be instantiated");
    }
}
