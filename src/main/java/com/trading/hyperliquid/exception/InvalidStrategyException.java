package com.trading.hyperliquid.exception;

public class InvalidStrategyException extends RuntimeException {

    public InvalidStrategyException(String message) {
        super(message);
    }

    public InvalidStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
