package com.trading.hyperliquid.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe nonce manager for Hyperliquid API requests.
 * Ensures monotonically increasing nonces per wallet address.
 */
@Component
public class NonceManager {

    private static final Logger logger = LoggerFactory.getLogger(NonceManager.class);

    private final Map<String, Long> lastNonceByAddress = new ConcurrentHashMap<>();

    /**
     * Get next valid nonce for the given address.
     * Nonce is based on current timestamp in milliseconds and guaranteed to be unique.
     *
     * @param address Wallet address
     * @return Next valid nonce
     */
    public synchronized long getNextNonce(String address) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = lastNonceByAddress.getOrDefault(address, 0L);

        // Ensure nonce is always greater than the last used
        long nextNonce = Math.max(currentTime, lastUsed + 1);

        lastNonceByAddress.put(address, nextNonce);

        logger.debug("Generated nonce {} for address {}", nextNonce, address);
        return nextNonce;
    }

    /**
     * Reset nonce tracking for an address (useful for testing).
     *
     * @param address Wallet address
     */
    public void resetNonce(String address) {
        lastNonceByAddress.remove(address);
        logger.debug("Reset nonce for address {}", address);
    }

    /**
     * Get last used nonce for an address.
     *
     * @param address Wallet address
     * @return Last used nonce or 0 if none exists
     */
    public long getLastNonce(String address) {
        return lastNonceByAddress.getOrDefault(address, 0L);
    }
}
