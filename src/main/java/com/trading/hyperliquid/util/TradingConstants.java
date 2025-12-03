package com.trading.hyperliquid.util;

import java.math.BigDecimal;

/**
 * Trading constants used throughout the application.
 * Centralizes magic numbers and strings for better maintainability.
 */
public final class TradingConstants {

    // Order Configuration Constants
    public static final int DEFAULT_LEVERAGE = 1;
    public static final String DEFAULT_TIME_IN_FORCE = "Gtc";
    public static final String TIME_IN_FORCE_IOC = "Ioc";
    public static final String TIME_IN_FORCE_ALO = "Alo";

    // Mock Order Constants
    public static final int MOCK_ORDER_ID_LENGTH = 8;
    public static final String MOCK_ORDER_ID_PREFIX = "MOCK-";

    // Mock Asset Prices (for POC/Development)
    // Note: These prices are used for IOC orders - should be close to market price
    public static final BigDecimal MOCK_BTC_PRICE = new BigDecimal("98000.00");
    public static final BigDecimal MOCK_ETH_PRICE = new BigDecimal("3900.00");
    public static final BigDecimal MOCK_SOL_PRICE = new BigDecimal("230.00");
    public static final BigDecimal MOCK_AVAX_PRICE = new BigDecimal("50.00");
    public static final BigDecimal MOCK_MATIC_PRICE = new BigDecimal("0.55");
    public static final BigDecimal MOCK_DEFAULT_PRICE = new BigDecimal("100.00");

    // Security/Display Constants
    public static final int ADDRESS_PREFIX_LENGTH = 6;
    public static final int ADDRESS_SUFFIX_LENGTH = 4;
    public static final int MIN_ADDRESS_LENGTH = 10;
    public static final String ADDRESS_MASK = "...";
    public static final String ADDRESS_MASK_FALLBACK = "****";

    // JWT Constants
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    public static final int JWT_TOKEN_PREFIX_LENGTH = 7;

    // Calculation Constants
    public static final int PERCENTAGE_SCALE = 4;
    public static final int PRICE_DISPLAY_SCALE = 2;
    public static final int HUNDRED_PERCENT = 100;

    // Hyperliquid API Constants
    public static final int SPOT_ASSET_ID_OFFSET = 10000;

    // Stop-Loss Constants
    public static final String TPSL_STOP_LOSS = "sl";
    public static final String TPSL_TAKE_PROFIT = "tp";
    public static final String GROUPING_NONE = "na";
    public static final String GROUPING_POSITION_TPSL = "positionTpsl";
    public static final String GROUPING_NORMAL_TPSL = "normalTpsl";

    // Stop-Loss Validation Constants
    public static final BigDecimal MIN_SL_PERCENT = new BigDecimal("0.1");
    public static final BigDecimal MAX_SL_PERCENT = new BigDecimal("50.0");
    public static final int SL_PRICE_SCALE = 8;

    // Stop-Loss Retry Configuration
    public static final int SL_MAX_RETRY_ATTEMPTS = 3;
    public static final long SL_RETRY_DELAY_MS = 1000;

    private TradingConstants() {
        throw new AssertionError("TradingConstants class cannot be instantiated");
    }
}
