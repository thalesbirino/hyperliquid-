package com.trading.hyperliquid.service;

import com.trading.hyperliquid.client.PythonHyperliquidClient;
import com.trading.hyperliquid.exception.HyperliquidApiException;
import com.trading.hyperliquid.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for account-related operations on Hyperliquid
 * Provides positions, open orders, and order cancellation functionality
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserService userService;
    private final PythonHyperliquidClient pythonClient;

    /**
     * Get positions for a user
     *
     * @param userId User ID
     * @return Map containing position information
     * @throws HyperliquidApiException if request fails
     */
    public Map<String, Object> getPositions(Long userId) throws HyperliquidApiException {
        User user = userService.getUserById(userId);
        log.info("Getting positions for user: {}", user.getUsername());
        return pythonClient.getPositions(user);
    }

    /**
     * Get open orders for a user
     *
     * @param userId User ID
     * @return List of open orders
     * @throws HyperliquidApiException if request fails
     */
    public List<Map<String, Object>> getOpenOrders(Long userId) throws HyperliquidApiException {
        User user = userService.getUserById(userId);
        log.info("Getting open orders for user: {}", user.getUsername());
        return pythonClient.getOpenOrders(user);
    }

    /**
     * Cancel an order for a user
     *
     * @param userId User ID
     * @param asset Asset name (e.g., "ETH", "BTC")
     * @param orderId Order ID to cancel
     * @return Response from Hyperliquid
     * @throws HyperliquidApiException if cancellation fails
     */
    public Map<String, Object> cancelOrder(Long userId, String asset, Long orderId) throws HyperliquidApiException {
        User user = userService.getUserById(userId);
        log.info("Cancelling order {} for user: {} on asset: {}", orderId, user.getUsername(), asset);
        return pythonClient.cancelOrder(user, asset, orderId);
    }
}
