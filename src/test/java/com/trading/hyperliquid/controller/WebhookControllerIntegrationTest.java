package com.trading.hyperliquid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WebhookController.
 * Tests HTTP layer including request validation and response format.
 */
@ExtendWith(MockitoExtension.class)
class WebhookControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    private ObjectMapper objectMapper;

    private WebhookRequest validBuyRequest;
    private WebhookRequest validSellRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
        objectMapper = new ObjectMapper();

        // Setup valid buy request
        validBuyRequest = new WebhookRequest();
        validBuyRequest.setAction("buy");
        validBuyRequest.setStrategyId("66e858a5-ca3c-4c2c-909c-34c605b3e5c7");
        validBuyRequest.setPassword("Admin@9090");

        // Setup valid sell request
        validSellRequest = new WebhookRequest();
        validSellRequest.setAction("sell");
        validSellRequest.setStrategyId("66e858a5-ca3c-4c2c-909c-34c605b3e5c7");
        validSellRequest.setPassword("Admin@9090");
    }

    // ========================================================================
    // SUCCESSFUL ORDER TESTS
    // ========================================================================
    @Nested
    @DisplayName("Successful Order Tests")
    class SuccessfulOrderTests {

        @Test
        @DisplayName("Should process BUY webhook successfully")
        void shouldProcessBuyWebhookSuccessfully() throws Exception {
            // Given
            WebhookResponse successResponse = WebhookResponse.success(
                    "ORDER-123",
                    "BUY",
                    "ETH",
                    "0.004",
                    "2500.00",
                    "EXECUTED"
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(successResponse);

            // When
            ResultActions result = performWebhookRequest(validBuyRequest);

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderId").value("ORDER-123"))
                    .andExpect(jsonPath("$.action").value("BUY"))
                    .andExpect(jsonPath("$.asset").value("ETH"));
        }

        @Test
        @DisplayName("Should process SELL webhook successfully")
        void shouldProcessSellWebhookSuccessfully() throws Exception {
            // Given
            WebhookResponse successResponse = WebhookResponse.success(
                    "ORDER-456",
                    "SELL",
                    "ETH",
                    "0.004",
                    "2600.00",
                    "EXECUTED"
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(successResponse);

            // When
            ResultActions result = performWebhookRequest(validSellRequest);

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.action").value("SELL"));
        }

        @Test
        @DisplayName("Should accept case-insensitive action (BUY)")
        void shouldAcceptCaseInsensitiveActionBuy() throws Exception {
            // Given
            validBuyRequest.setAction("BUY");
            WebhookResponse successResponse = WebhookResponse.success(
                    "ORDER-789",
                    "BUY",
                    "ETH",
                    "0.004",
                    "2500.00",
                    "EXECUTED"
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(successResponse);

            // When
            ResultActions result = performWebhookRequest(validBuyRequest);

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ========================================================================
    // ERROR HANDLING TESTS
    // ========================================================================
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error for invalid credentials")
        void shouldReturnErrorForInvalidCredentials() throws Exception {
            // Given
            WebhookResponse errorResponse = WebhookResponse.error("Invalid strategy ID or password");
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(errorResponse);

            // When
            ResultActions result = performWebhookRequest(validBuyRequest);

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid strategy ID or password"));
        }

        @Test
        @DisplayName("Should return error for inactive strategy")
        void shouldReturnErrorForInactiveStrategy() throws Exception {
            // Given
            WebhookResponse errorResponse = WebhookResponse.error("Strategy is inactive");
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(errorResponse);

            // When
            ResultActions result = performWebhookRequest(validBuyRequest);

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Strategy is inactive"));
        }

        @Test
        @DisplayName("Should return error when pyramid=false blocks same direction")
        void shouldReturnErrorWhenPyramidBlocks() throws Exception {
            // Given
            WebhookResponse errorResponse = WebhookResponse.error(
                    "Cannot add to existing position (Pyramid=FALSE)"
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(errorResponse);

            // When
            ResultActions result = performWebhookRequest(validBuyRequest);

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ========================================================================
    // POSITION CLOSED (INVERSE=FALSE) TESTS
    // ========================================================================
    @Nested
    @DisplayName("Position Closed (Inverse=FALSE) Tests")
    class PositionClosedTests {

        @Test
        @DisplayName("Should return closed status when inverse=false")
        void shouldReturnClosedStatusWhenInverseFalse() throws Exception {
            // Given
            WebhookResponse closedResponse = WebhookResponse.success(
                    "CLOSED",
                    "SELL",
                    "ETH",
                    "0",
                    "0",
                    "Position closed (Inverse=FALSE). No opposite order placed."
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(closedResponse);

            // When
            ResultActions result = performWebhookRequest(validSellRequest);

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderId").value("CLOSED"))
                    .andExpect(jsonPath("$.status").value("Position closed (Inverse=FALSE). No opposite order placed."));
        }
    }

    // ========================================================================
    // CONTENT TYPE TESTS
    // ========================================================================
    @Nested
    @DisplayName("Content Type Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should accept application/json content type")
        void shouldAcceptJsonContentType() throws Exception {
            // Given
            WebhookResponse successResponse = WebhookResponse.success(
                    "ORDER-123", "BUY", "ETH", "0.004", "2500.00", "EXECUTED"
            );
            when(webhookService.processWebhook(any(WebhookRequest.class)))
                    .thenReturn(successResponse);

            // When/Then
            mockMvc.perform(post("/api/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validBuyRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    private ResultActions performWebhookRequest(WebhookRequest request) throws Exception {
        return mockMvc.perform(post("/api/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
