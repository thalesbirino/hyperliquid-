package com.trading.hyperliquid.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.hyperliquid.exception.HyperliquidApiException;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.model.hyperliquid.Order;
import com.trading.hyperliquid.model.hyperliquid.OrderAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Real API Client for Hyperliquid Integration
 *
 * This class orchestrates the complete flow for real Hyperliquid API calls:
 * 1. Creates HyperliquidSigner from user's private key
 * 2. Builds and signs order actions using EIP-712
 * 3. Sends signed requests to Hyperliquid via HTTP
 * 4. Parses and returns responses
 *
 * Only active when mock-mode is disabled in configuration.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hyperliquid.api.mock-mode", havingValue = "false")
public class HyperliquidRealApiClient {

    private final HyperliquidHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Hyperliquid contract addresses for signature verification
    private static final String TESTNET_CONTRACT = "0x0000000000000000000000000000000000000000";
    private static final String MAINNET_CONTRACT = "0x0000000000000000000000000000000000000000";

    public HyperliquidRealApiClient(HyperliquidHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        log.info("✅ HyperliquidRealApiClient initialized - REAL MODE ACTIVE");
    }

    /**
     * Places a real order on Hyperliquid
     *
     * @param user User with credentials
     * @param order Order to place
     * @param grouping Grouping type ("positionTpsl" or "na")
     * @param apiUrl API endpoint URL
     * @return Order ID from Hyperliquid
     * @throws HyperliquidApiException if order placement fails
     */
    public String placeOrder(User user, Order order, String grouping, String apiUrl) throws HyperliquidApiException {
        try {
            log.info("🚀 Placing REAL order on Hyperliquid for user: {}", user.getUsername());

            // 1. Create signer from user's private key
            HyperliquidSigner signer = new HyperliquidSigner(user.getHyperliquidPrivateKey());

            // Verify address matches
            if (!signer.getAddress().equalsIgnoreCase(user.getHyperliquidAddress())) {
                throw new HyperliquidApiException(
                    "Address mismatch! Private key does not match stored address. " +
                    "Expected: " + user.getHyperliquidAddress() + ", Got: " + signer.getAddress()
                );
            }

            // 2. Build order action
            OrderAction orderAction = OrderAction.builder()
                .type("order")
                .orders(List.of(order))
                .grouping(grouping)
                .build();

            // 3. Convert to JSON for hashing
            String actionJson = objectMapper.writeValueAsString(orderAction);
            byte[] actionHash = Hash.sha3(actionJson.getBytes(StandardCharsets.UTF_8));

            log.debug("Order action JSON: {}", actionJson);
            log.debug("Action hash: {}", bytesToHex(actionHash));

            // 4. Sign the action
            long chainId = HyperliquidSigner.getChainId(user.getIsTestnet());
            String verifyingContract = user.getIsTestnet() ? TESTNET_CONTRACT : MAINNET_CONTRACT;

            var signatureData = signer.signAction(actionHash, chainId, verifyingContract);
            var signature = signer.formatSignature(signatureData);

            log.debug("Signature: {}", signature);

            // 5. Send to Hyperliquid
            Map<String, Object> orderPayload = convertToMap(orderAction);
            Map<String, Object> response = httpClient.placeOrder(
                apiUrl,
                orderPayload,
                signature,
                null // vaultAddress - null for personal accounts
            );

            // 6. Parse response
            String orderId = extractOrderId(response);
            log.info("✅ Order placed successfully! Order ID: {}", orderId);

            return orderId;

        } catch (Exception e) {
            log.error("❌ Failed to place real order: {}", e.getMessage(), e);
            throw new HyperliquidApiException("Order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels orders on Hyperliquid
     *
     * @param user User with credentials
     * @param assetId Asset ID
     * @param orderIds List of order IDs to cancel
     * @param apiUrl API endpoint URL
     * @return Response from Hyperliquid
     * @throws HyperliquidApiException if cancel fails
     */
    public Map<String, Object> cancelOrders(User user, int assetId, List<Long> orderIds, String apiUrl)
            throws HyperliquidApiException {
        try {
            log.info("🚫 Canceling {} orders for user: {}", orderIds.size(), user.getUsername());

            // 1. Create signer
            HyperliquidSigner signer = new HyperliquidSigner(user.getHyperliquidPrivateKey());

            // 2. Build cancel action
            List<OrderAction.CancelRequest> cancels = orderIds.stream()
                .map(oid -> new OrderAction.CancelRequest(assetId, oid, null))
                .toList();

            OrderAction cancelAction = OrderAction.builder()
                .type("cancel")
                .cancels(cancels)
                .build();

            // 3. Sign
            String actionJson = objectMapper.writeValueAsString(cancelAction);
            byte[] actionHash = Hash.sha3(actionJson.getBytes(StandardCharsets.UTF_8));

            long chainId = HyperliquidSigner.getChainId(user.getIsTestnet());
            String verifyingContract = user.getIsTestnet() ? TESTNET_CONTRACT : MAINNET_CONTRACT;

            var signatureData = signer.signAction(actionHash, chainId, verifyingContract);
            var signature = signer.formatSignature(signatureData);

            // 4. Send to Hyperliquid
            Map<String, Object> cancelPayload = convertToMap(cancelAction);
            Map<String, Object> response = httpClient.cancelOrders(apiUrl, cancelPayload, signature, null);

            log.info("✅ Orders canceled successfully");
            return response;

        } catch (Exception e) {
            log.error("❌ Failed to cancel orders: {}", e.getMessage(), e);
            throw new HyperliquidApiException("Cancel failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets account information from Hyperliquid
     *
     * @param user User with credentials
     * @param apiUrl API endpoint URL
     * @return Account information
     * @throws HyperliquidApiException if request fails
     */
    public Map<String, Object> getAccountInfo(User user, String apiUrl) throws HyperliquidApiException {
        try {
            log.debug("Fetching account info for user: {}", user.getUsername());
            return httpClient.getAccountInfo(apiUrl, user.getHyperliquidAddress());
        } catch (IOException e) {
            log.error("Failed to get account info: {}", e.getMessage());
            throw new HyperliquidApiException("Failed to get account info: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts order ID from Hyperliquid response
     *
     * Expected response format:
     * {
     *   "status": "ok",
     *   "response": {
     *     "type": "order",
     *     "data": {
     *       "statuses": [
     *         {
     *           "filled": { "oid": 123456789 }
     *         }
     *       ]
     *     }
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    private String extractOrderId(Map<String, Object> response) throws HyperliquidApiException {
        try {
            Object statusObj = response.get("status");
            if (statusObj == null) {
                throw new HyperliquidApiException("Missing 'status' in response");
            }

            String status = statusObj.toString();
            if (!"ok".equalsIgnoreCase(status)) {
                throw new HyperliquidApiException("Order failed with status: " + status + " - " + response);
            }

            Map<String, Object> responseData = (Map<String, Object>) response.get("response");
            if (responseData == null) {
                throw new HyperliquidApiException("Missing 'response' in API response");
            }

            Map<String, Object> data = (Map<String, Object>) responseData.get("data");
            if (data == null) {
                throw new HyperliquidApiException("Missing 'data' in response");
            }

            List<Map<String, Object>> statuses = (List<Map<String, Object>>) data.get("statuses");
            if (statuses == null || statuses.isEmpty()) {
                throw new HyperliquidApiException("No order statuses in response");
            }

            Map<String, Object> firstStatus = statuses.get(0);
            Map<String, Object> filled = (Map<String, Object>) firstStatus.get("filled");

            if (filled != null && filled.containsKey("oid")) {
                return String.valueOf(filled.get("oid"));
            }

            throw new HyperliquidApiException("Could not extract order ID from response: " + response);

        } catch (ClassCastException | NullPointerException e) {
            throw new HyperliquidApiException("Invalid response format: " + response, e);
        }
    }

    /**
     * Converts object to Map for JSON serialization
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }

    /**
     * Converts byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
