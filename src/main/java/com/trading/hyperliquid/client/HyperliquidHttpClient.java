package com.trading.hyperliquid.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Client for Hyperliquid DEX API
 *
 * Handles all HTTP communication with Hyperliquid exchange endpoints.
 * Supports both testnet and mainnet environments.
 *
 * @see <a href="https://hyperliquid.gitbook.io/">Hyperliquid API Documentation</a>
 */
@Slf4j
@Component
public class HyperliquidHttpClient {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    // MediaType constants
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Creates HTTP client with optimized timeouts for trading
     */
    public HyperliquidHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        log.info("Hyperliquid HTTP client initialized");
    }

    /**
     * Posts a request to Hyperliquid exchange endpoint
     *
     * @param url API endpoint URL
     * @param payload Request payload as Map
     * @return Response body as Map
     * @throws IOException if request fails
     */
    public Map<String, Object> post(String url, Map<String, Object> payload) throws IOException {
        try {
            // Serialize payload to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.debug("POST to {}: {}", url, jsonPayload);

            // Build request body
            RequestBody body = RequestBody.create(jsonPayload, JSON);

            // Build request
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

            // Execute request
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";

                log.debug("Response from {}: {} - {}", url, response.code(), responseBody);

                if (!response.isSuccessful()) {
                    log.error("HTTP request failed: {} - {}", response.code(), responseBody);
                    throw new IOException("Unexpected HTTP code " + response.code() + ": " + responseBody);
                }

                // Parse response JSON
                return objectMapper.readValue(responseBody, Map.class);
            }

        } catch (IOException e) {
            log.error("Failed to execute HTTP request to {}: {}", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Posts an order to Hyperliquid /exchange endpoint
     *
     * @param url Exchange endpoint URL
     * @param orderPayload Order action payload
     * @param signature Signature components (r, s, v)
     * @param vaultAddress Vault address (null for personal account)
     * @return API response
     * @throws IOException if request fails
     */
    public Map<String, Object> placeOrder(
        String url,
        Map<String, Object> orderPayload,
        HyperliquidSigner.SignatureComponents signature,
        String vaultAddress
    ) throws IOException {

        // Build complete payload with signature
        Map<String, Object> payload = Map.of(
            "action", orderPayload,
            "signature", Map.of(
                "r", "0x" + signature.r,
                "s", "0x" + signature.s,
                "v", signature.v
            ),
            "vaultAddress", vaultAddress != null ? vaultAddress : ""
        );

        log.info("Placing order to Hyperliquid: {}", orderPayload);
        return post(url + "/exchange", payload);
    }

    /**
     * Cancels orders on Hyperliquid
     *
     * @param url Exchange endpoint URL
     * @param cancelPayload Cancel action payload
     * @param signature Signature components
     * @param vaultAddress Vault address (null for personal account)
     * @return API response
     * @throws IOException if request fails
     */
    public Map<String, Object> cancelOrders(
        String url,
        Map<String, Object> cancelPayload,
        HyperliquidSigner.SignatureComponents signature,
        String vaultAddress
    ) throws IOException {

        Map<String, Object> payload = Map.of(
            "action", cancelPayload,
            "signature", Map.of(
                "r", "0x" + signature.r,
                "s", "0x" + signature.s,
                "v", signature.v
            ),
            "vaultAddress", vaultAddress != null ? vaultAddress : ""
        );

        log.info("Canceling orders on Hyperliquid");
        return post(url + "/exchange", payload);
    }

    /**
     * Gets account information from Hyperliquid /info endpoint
     *
     * @param url Info endpoint URL
     * @param address User address
     * @return Account information
     * @throws IOException if request fails
     */
    public Map<String, Object> getAccountInfo(String url, String address) throws IOException {
        Map<String, Object> payload = Map.of(
            "type", "clearinghouseState",
            "user", address
        );

        log.debug("Fetching account info for address: {}", address);
        return post(url + "/info", payload);
    }

    /**
     * Gets market metadata from Hyperliquid
     *
     * @param url Info endpoint URL
     * @return Market metadata
     * @throws IOException if request fails
     */
    public Map<String, Object> getMarketMetadata(String url) throws IOException {
        Map<String, Object> payload = Map.of(
            "type", "metaAndAssetCtxs"
        );

        log.debug("Fetching market metadata");
        return post(url + "/info", payload);
    }

    /**
     * Gets user fills (trade history)
     *
     * @param url Info endpoint URL
     * @param address User address
     * @return User fills
     * @throws IOException if request fails
     */
    public Map<String, Object> getUserFills(String url, String address) throws IOException {
        Map<String, Object> payload = Map.of(
            "type", "userFills",
            "user", address
        );

        log.debug("Fetching user fills for address: {}", address);
        return post(url + "/info", payload);
    }

    /**
     * Gets open orders for user
     *
     * @param url Info endpoint URL
     * @param address User address
     * @return Open orders
     * @throws IOException if request fails
     */
    public Map<String, Object> getOpenOrders(String url, String address) throws IOException {
        Map<String, Object> payload = Map.of(
            "type", "openOrders",
            "user", address
        );

        log.debug("Fetching open orders for address: {}", address);
        return post(url + "/info", payload);
    }

    /**
     * Shutdown HTTP client gracefully
     */
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        log.info("Hyperliquid HTTP client shut down");
    }
}
