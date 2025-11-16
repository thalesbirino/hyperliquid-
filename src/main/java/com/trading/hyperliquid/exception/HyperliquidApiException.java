package com.trading.hyperliquid.exception;

public class HyperliquidApiException extends RuntimeException {

    public HyperliquidApiException(String message) {
        super(message);
    }

    public HyperliquidApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
