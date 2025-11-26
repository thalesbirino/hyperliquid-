package com.trading.hyperliquid.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.hyperliquid.exception.HyperliquidApiException;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.model.hyperliquid.Order;
import com.trading.hyperliquid.model.hyperliquid.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;

/**
 * Hyperliquid API Client that delegates to Python SDK via subprocess
 *
 * This approach uses the official hyperliquid-python-sdk which handles all
 * the complex EIP-712 signing, Msgpack encoding, and Phantom Agent logic
 * correctly. The Java code simply passes order data to Python and receives
 * the result.
 *
 * Benefits:
 * - Uses battle-tested SDK maintained by Hyperliquid
 * - Eliminates EIP-712 signing bugs
 * - Easy to debug (Python script can be tested independently)
 * - Automatic updates via pip
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hyperliquid.api.mock-mode", havingValue = "false")
public class PythonHyperliquidClient {

    private final ObjectMapper objectMapper;

    /**
     * Path to the Python script relative to project root
     */
    private static final String PYTHON_SCRIPT = "scripts/order_executor.py";

    /**
     * Mapping from asset ID to asset name
     * Based on Hyperliquid's asset indices
     */
    private static final Map<Integer, String> ASSET_ID_TO_NAME = Map.ofEntries(
            entry(0, "BTC"),
            entry(1, "ETH"),
            entry(2, "SOL"),
            entry(3, "AVAX"),
            entry(4, "MATIC"),
            entry(5, "DOGE"),
            entry(6, "ATOM"),
            entry(7, "APT"),
            entry(8, "ARB"),
            entry(9, "OP"),
            entry(10, "SUI"),
            entry(11, "SEI"),
            entry(12, "INJ"),
            entry(13, "LINK"),
            entry(14, "LTC"),
            entry(15, "BCH"),
            entry(16, "PEPE"),
            entry(17, "WIF"),
            entry(18, "BONK"),
            entry(19, "SHIB")
    );

    /**
     * Timeout for Python script execution in seconds
     */
    private static final int SCRIPT_TIMEOUT_SECONDS = 30;

    public PythonHyperliquidClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("PythonHyperliquidClient initialized - using Python SDK for order execution");
    }

    /**
     * Place an order on Hyperliquid via Python SDK
     *
     * @param user User entity with credentials
     * @param order Order to place
     * @param grouping Order grouping ("na" for single orders)
     * @param apiUrl API URL (testnet or mainnet) - informational only, Python SDK determines from isTestnet
     * @return Order ID from Hyperliquid
     * @throws HyperliquidApiException if order placement fails
     */
    public String placeOrder(User user, Order order, String grouping, String apiUrl) throws HyperliquidApiException {
        log.info("=== PLACING ORDER VIA PYTHON SDK ===");
        log.info("User: {} (testnet={})", user.getUsername(), user.getIsTestnet());
        log.info("Asset: {} | Side: {} | Size: {} | Price: {}",
                getAssetName(order.getA()),
                order.getB() ? "BUY" : "SELL",
                order.getS(),
                order.getP());

        try {
            // Build input data for Python script
            Map<String, Object> inputData = buildOrderInput(user, order);

            // Execute Python script
            Map<String, Object> response = executePythonScript(inputData);

            // Parse response
            String status = String.valueOf(response.get("status"));

            if ("ok".equals(status)) {
                String orderId = extractOrderId(response);
                log.info("Order placed successfully! Order ID: {}", orderId);
                return orderId;
            } else {
                String errorMessage = String.valueOf(response.getOrDefault("response", response));
                log.error("Order failed: {}", errorMessage);
                throw new HyperliquidApiException("Order failed: " + errorMessage);
            }

        } catch (HyperliquidApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to place order via Python SDK", e);
            throw new HyperliquidApiException("Python SDK error: " + e.getMessage(), e);
        }
    }

    /**
     * Build input data map for the Python script
     */
    private Map<String, Object> buildOrderInput(User user, Order order) {
        Map<String, Object> input = new HashMap<>();

        // Action type
        input.put("action", "order");

        // Order details
        input.put("asset", getAssetName(order.getA()));
        input.put("isBuy", order.getB());
        input.put("size", order.getS());
        input.put("price", order.getP());
        input.put("reduceOnly", order.getR());
        input.put("timeInForce", extractTif(order.getT()));

        // User credentials
        input.put("isTestnet", user.getIsTestnet());
        input.put("hyperliquidAddress", user.getHyperliquidAddress());

        // Private key - prefer API wallet if available
        if (user.getApiWalletPrivateKey() != null && !user.getApiWalletPrivateKey().isEmpty()) {
            input.put("apiWalletPrivateKey", user.getApiWalletPrivateKey());
            log.debug("Using API Wallet for signing");
        } else if (user.getHyperliquidPrivateKey() != null) {
            input.put("hyperliquidPrivateKey", user.getHyperliquidPrivateKey());
            log.debug("Using main wallet for signing");
        } else {
            throw new IllegalArgumentException("No private key available for user: " + user.getUsername());
        }

        return input;
    }

    /**
     * Execute the Python script with given input and return parsed result
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executePythonScript(Map<String, Object> input) throws HyperliquidApiException {
        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new HyperliquidApiException("Failed to serialize input to JSON", e);
        }

        log.debug("Python script input: {}", maskSensitiveData(inputJson));

        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder("python", PYTHON_SCRIPT);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(false); // Keep stderr separate for logging

            log.debug("Executing: python {} in {}", PYTHON_SCRIPT, pb.directory());

            Process process = pb.start();

            // Write input to stdin
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(inputJson.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new HyperliquidApiException("Python script timed out after " + SCRIPT_TIMEOUT_SECONDS + " seconds");
            }

            // Read stdout (result JSON)
            String stdout;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                stdout = sb.toString();
            }

            // Read stderr (logging)
            String stderr;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                stderr = sb.toString();
            }

            // Log stderr (Python logging output)
            if (!stderr.isEmpty()) {
                log.debug("Python stderr: {}", stderr.trim());
            }

            int exitCode = process.exitValue();
            log.debug("Python script exit code: {}", exitCode);
            log.debug("Python script output: {}", stdout);

            if (exitCode != 0) {
                throw new HyperliquidApiException("Python script failed (exit code " + exitCode + "): " + stdout);
            }

            if (stdout.isEmpty()) {
                throw new HyperliquidApiException("Python script returned empty output");
            }

            // Parse JSON response
            return objectMapper.readValue(stdout, Map.class);

        } catch (IOException e) {
            log.error("Failed to execute Python script", e);
            throw new HyperliquidApiException("Failed to execute Python script: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HyperliquidApiException("Python script execution interrupted", e);
        }
    }

    /**
     * Extract order ID from successful response
     */
    @SuppressWarnings("unchecked")
    private String extractOrderId(Map<String, Object> response) {
        try {
            // Response structure: {"status": "ok", "response": {"type": "order", "data": {"statuses": [...]}}}
            Map<String, Object> responseData = (Map<String, Object>) response.get("response");
            if (responseData == null) {
                return "UNKNOWN-" + System.currentTimeMillis();
            }

            Map<String, Object> data = (Map<String, Object>) responseData.get("data");
            if (data == null) {
                return "UNKNOWN-" + System.currentTimeMillis();
            }

            List<Map<String, Object>> statuses = (List<Map<String, Object>>) data.get("statuses");
            if (statuses != null && !statuses.isEmpty()) {
                Map<String, Object> firstStatus = statuses.get(0);

                // Check for "filled" or "resting" order
                if (firstStatus.containsKey("filled")) {
                    Map<String, Object> filled = (Map<String, Object>) firstStatus.get("filled");
                    return String.valueOf(filled.get("oid"));
                } else if (firstStatus.containsKey("resting")) {
                    Map<String, Object> resting = (Map<String, Object>) firstStatus.get("resting");
                    return String.valueOf(resting.get("oid"));
                }
            }

            return "UNKNOWN-" + System.currentTimeMillis();

        } catch (Exception e) {
            log.warn("Failed to extract order ID from response: {}", response, e);
            return "UNKNOWN-" + System.currentTimeMillis();
        }
    }

    /**
     * Get asset name from asset ID
     */
    private String getAssetName(int assetId) {
        return ASSET_ID_TO_NAME.getOrDefault(assetId, "UNKNOWN");
    }

    /**
     * Extract time-in-force from order type
     */
    private String extractTif(OrderType orderType) {
        if (orderType == null || orderType.getLimit() == null) {
            return "Gtc";
        }
        return orderType.getLimit().getTif();
    }

    /**
     * Mask sensitive data in logs (private keys)
     */
    private String maskSensitiveData(String json) {
        return json.replaceAll("(PrivateKey\"\\s*:\\s*\")[^\"]+\"", "$1***MASKED***\"");
    }
}
