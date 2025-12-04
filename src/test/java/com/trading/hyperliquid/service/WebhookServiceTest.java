package com.trading.hyperliquid.service;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.model.entity.*;
import com.trading.hyperliquid.model.hyperliquid.OrderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookService covering all trading rules:
 *
 * FLOWCHART DECISION TREE:
 * 1. First Order? -> Place order
 * 2. Previous Position Closed? -> Place new order
 * 3. Same Direction:
 *    - Pyramid=TRUE -> Allow add-on
 *    - Pyramid=FALSE -> Reject
 * 4. Opposite Direction:
 *    - Cancel SL -> Square off
 *    - Inverse=TRUE -> Place opposite
 *    - Inverse=FALSE -> Just close
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private StrategyService strategyService;

    @Mock
    private HyperliquidService hyperliquidService;

    @Mock
    private OrderExecutionService orderExecutionService;

    @InjectMocks
    private WebhookService webhookService;

    private User testUser;
    private Config testConfig;
    private Strategy testStrategy;
    private WebhookRequest buyRequest;
    private WebhookRequest sellRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .hyperliquidAddress("0x1234567890abcdef")
                .apiWalletPrivateKey("0xprivatekey")
                .isTestnet(false)
                .active(true)
                .build();

        // Setup test config
        testConfig = Config.builder()
                .id(1L)
                .name("ETH Test Config")
                .asset("ETH")
                .assetId(1)
                .lotSize(new BigDecimal("0.004"))
                .slPercent(new BigDecimal("2.00"))
                .leverage(5)
                .orderType(Config.OrderType.LIMIT)
                .timeInForce("Gtc")
                .build();

        // Setup test strategy (default: pyramid=false, inverse=false)
        testStrategy = Strategy.builder()
                .id(1L)
                .name("Test Strategy")
                .strategyId("test-strategy-uuid")
                .config(testConfig)
                .user(testUser)
                .active(true)
                .pyramid(false)
                .inverse(false)
                .build();

        // Setup webhook requests
        buyRequest = new WebhookRequest();
        buyRequest.setAction("buy");
        buyRequest.setStrategyId("test-strategy-uuid");
        buyRequest.setPassword("password123");

        sellRequest = new WebhookRequest();
        sellRequest.setAction("sell");
        sellRequest.setStrategyId("test-strategy-uuid");
        sellRequest.setPassword("password123");
    }

    // ========================================================================
    // FIRST ORDER TESTS
    // ========================================================================
    @Nested
    @DisplayName("First Order Tests")
    class FirstOrderTests {

        @Test
        @DisplayName("Should place order when no previous orders exist")
        void shouldPlaceFirstOrder() {
            // Given: No previous orders
            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.empty());
            when(hyperliquidService.placeOrder(anyString(), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("ORDER-123", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("ORDER-123", OrderExecution.OrderSide.BUY));

            // When
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("ORDER-123");
            assertThat(response.getAction()).isEqualTo("BUY");
            verify(hyperliquidService).placeOrder("buy", testConfig, testUser);
        }

        @Test
        @DisplayName("Should place SELL as first order")
        void shouldPlaceFirstSellOrder() {
            // Given
            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.empty());
            when(hyperliquidService.placeOrder(anyString(), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("ORDER-456", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("ORDER-456", OrderExecution.OrderSide.SELL));

            // When
            WebhookResponse response = webhookService.processWebhook(sellRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getAction()).isEqualTo("SELL");
        }
    }

    // ========================================================================
    // CLOSED POSITION TESTS
    // ========================================================================
    @Nested
    @DisplayName("Closed Position Tests")
    class ClosedPositionTests {

        @Test
        @DisplayName("Should place new order when previous position is closed")
        void shouldPlaceOrderWhenPreviousPositionClosed() {
            // Given: Last order exists but is closed
            OrderExecution closedOrder = createMockOrderExecution("OLD-ORDER", OrderExecution.OrderSide.SELL);
            closedOrder.setClosedAt(LocalDateTime.now().minusHours(1));

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(closedOrder));
            when(hyperliquidService.placeOrder(anyString(), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("NEW-ORDER-789", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("NEW-ORDER-789", OrderExecution.OrderSide.BUY));

            // When
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("NEW-ORDER-789");
        }
    }

    // ========================================================================
    // MODE 1: PYRAMID=FALSE, INVERSE=FALSE (Most Restrictive)
    // ========================================================================
    @Nested
    @DisplayName("MODE 1: Pyramid=FALSE, Inverse=FALSE")
    class Mode1RestrictiveTests {

        @BeforeEach
        void setMode1() {
            testStrategy.setPyramid(false);
            testStrategy.setInverse(false);
        }

        @Test
        @DisplayName("Should REJECT same direction order when Pyramid=FALSE")
        void shouldRejectSameDirectionWhenPyramidFalse() {
            // Given: Open BUY position exists
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));

            // When: Try to place another BUY
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then: Should be rejected
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("Pyramid=FALSE");
            verify(hyperliquidService, never()).placeOrder(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should close position and NOT place opposite when Inverse=FALSE")
        void shouldCloseWithoutOppositeWhenInverseFalse() {
            // Given: Open BUY position exists
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);
            openBuyOrder.setStopLossStatus(OrderExecution.StopLossStatus.ACTIVE);
            openBuyOrder.setStopLossOrderId("SL-ORDER-123");

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openBuyOrder));

            // When: Try to place SELL (opposite direction)
            WebhookResponse response = webhookService.processWebhook(sellRequest);

            // Then: Should close without placing new order
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).contains("Inverse=FALSE");

            // Verify SL was cancelled and positions closed
            verify(hyperliquidService).cancelOrder(eq("SL-ORDER-123"), anyInt(), any(User.class));
            verify(orderExecutionService).closeAllPositions(anyList());

            // Verify NO new order was placed
            verify(hyperliquidService, never()).placeOrder(anyString(), any(), any());
        }
    }

    // ========================================================================
    // MODE 2: PYRAMID=FALSE, INVERSE=TRUE (Reversals Allowed)
    // ========================================================================
    @Nested
    @DisplayName("MODE 2: Pyramid=FALSE, Inverse=TRUE")
    class Mode2ReversalTests {

        @BeforeEach
        void setMode2() {
            testStrategy.setPyramid(false);
            testStrategy.setInverse(true);
        }

        @Test
        @DisplayName("Should REJECT same direction when Pyramid=FALSE")
        void shouldRejectSameDirectionEvenWithInverseTrue() {
            // Given: Open BUY position
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));

            // When: Try another BUY
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then: Rejected
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("Pyramid=FALSE");
        }

        @Test
        @DisplayName("Should close and place OPPOSITE when Inverse=TRUE")
        void shouldCloseAndPlaceOppositeWhenInverseTrue() {
            // Given: Open BUY position
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openBuyOrder));
            when(hyperliquidService.placeOrder(eq("sell"), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("SELL-ORDER-999", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("SELL-ORDER-999", OrderExecution.OrderSide.SELL));

            // When: Place SELL (opposite)
            WebhookResponse response = webhookService.processWebhook(sellRequest);

            // Then: Should close old position AND place new SELL
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("SELL-ORDER-999");
            assertThat(response.getAction()).isEqualTo("SELL");

            verify(orderExecutionService).closeAllPositions(anyList());
            verify(hyperliquidService).placeOrder("sell", testConfig, testUser);
        }
    }

    // ========================================================================
    // MODE 3: PYRAMID=TRUE, INVERSE=FALSE (Pyramiding Only)
    // ========================================================================
    @Nested
    @DisplayName("MODE 3: Pyramid=TRUE, Inverse=FALSE")
    class Mode3PyramidTests {

        @BeforeEach
        void setMode3() {
            testStrategy.setPyramid(true);
            testStrategy.setInverse(false);
        }

        @Test
        @DisplayName("Should ALLOW same direction when Pyramid=TRUE")
        void shouldAllowSameDirectionWhenPyramidTrue() {
            // Given: Open BUY position
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER-1", OrderExecution.OrderSide.BUY);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(hyperliquidService.placeOrder(eq("buy"), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("BUY-ORDER-2", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("BUY-ORDER-2", OrderExecution.OrderSide.BUY));

            // When: Place another BUY (pyramid)
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then: Should allow the add-on order
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("BUY-ORDER-2");
            verify(hyperliquidService).placeOrder("buy", testConfig, testUser);
        }

        @Test
        @DisplayName("Should close but NOT place opposite when Inverse=FALSE")
        void shouldCloseWithoutOppositeWhenInverseFalse() {
            // Given: Open BUY position
            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openBuyOrder));

            // When: Place SELL (opposite)
            WebhookResponse response = webhookService.processWebhook(sellRequest);

            // Then: Close only, no new order
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).contains("Inverse=FALSE");
            verify(hyperliquidService, never()).placeOrder(anyString(), any(), any());
        }
    }

    // ========================================================================
    // MODE 4: PYRAMID=TRUE, INVERSE=TRUE (Maximum Flexibility)
    // ========================================================================
    @Nested
    @DisplayName("MODE 4: Pyramid=TRUE, Inverse=TRUE")
    class Mode4FlexibleTests {

        @BeforeEach
        void setMode4() {
            testStrategy.setPyramid(true);
            testStrategy.setInverse(true);
        }

        @Test
        @DisplayName("Should ALLOW same direction (pyramid)")
        void shouldAllowPyramiding() {
            // Given: Open SELL position
            OrderExecution openSellOrder = createMockOrderExecution("SELL-ORDER-1", OrderExecution.OrderSide.SELL);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openSellOrder));
            when(hyperliquidService.placeOrder(eq("sell"), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("SELL-ORDER-2", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("SELL-ORDER-2", OrderExecution.OrderSide.SELL));

            // When: Another SELL
            WebhookResponse response = webhookService.processWebhook(sellRequest);

            // Then: Allowed
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("SELL-ORDER-2");
        }

        @Test
        @DisplayName("Should close and place opposite (reversal)")
        void shouldAllowReversal() {
            // Given: Open SELL position
            OrderExecution openSellOrder = createMockOrderExecution("SELL-ORDER", OrderExecution.OrderSide.SELL);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openSellOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openSellOrder));
            when(hyperliquidService.placeOrder(eq("buy"), any(Config.class), any(User.class)))
                    .thenReturn(OrderResult.of("BUY-ORDER-REVERSAL", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("BUY-ORDER-REVERSAL", OrderExecution.OrderSide.BUY));

            // When: Place BUY (opposite to SELL)
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then: Should close and place new BUY
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("BUY-ORDER-REVERSAL");
            assertThat(response.getAction()).isEqualTo("BUY");
            verify(orderExecutionService).closeAllPositions(anyList());
        }
    }

    // ========================================================================
    // STOP-LOSS CANCELLATION TESTS
    // ========================================================================
    @Nested
    @DisplayName("Stop-Loss Cancellation Tests")
    class StopLossCancellationTests {

        @Test
        @DisplayName("Should cancel active SL before squaring off")
        void shouldCancelStopLossBeforeSquareOff() {
            // Given: Mode 4 with open position with active SL
            testStrategy.setPyramid(true);
            testStrategy.setInverse(true);

            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);
            openBuyOrder.setStopLossStatus(OrderExecution.StopLossStatus.ACTIVE);
            openBuyOrder.setStopLossOrderId("SL-123");

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openBuyOrder));
            when(hyperliquidService.placeOrder(anyString(), any(), any()))
                    .thenReturn(OrderResult.of("NEW-ORDER", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("NEW-ORDER", OrderExecution.OrderSide.SELL));

            // When: Place opposite direction
            webhookService.processWebhook(sellRequest);

            // Then: SL should be cancelled
            verify(hyperliquidService).cancelOrder(eq("SL-123"), eq(1), any(User.class));
        }

        @Test
        @DisplayName("Should not cancel SL if status is not ACTIVE")
        void shouldNotCancelInactiveStopLoss() {
            // Given
            testStrategy.setPyramid(true);
            testStrategy.setInverse(true);

            OrderExecution openBuyOrder = createMockOrderExecution("BUY-ORDER", OrderExecution.OrderSide.BUY);
            openBuyOrder.setStopLossStatus(OrderExecution.StopLossStatus.NONE);

            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.of(openBuyOrder));
            when(orderExecutionService.getOpenPositionsByStrategy(anyLong()))
                    .thenReturn(List.of(openBuyOrder));
            when(hyperliquidService.placeOrder(anyString(), any(), any()))
                    .thenReturn(OrderResult.of("NEW-ORDER", BigDecimal.valueOf(3900)));
            when(orderExecutionService.createOrderExecution(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(createMockOrderExecution("NEW-ORDER", OrderExecution.OrderSide.SELL));

            // When
            webhookService.processWebhook(sellRequest);

            // Then: Cancel should not be called
            verify(hyperliquidService, never()).cancelOrder(anyString(), anyInt(), any());
        }
    }

    // ========================================================================
    // ERROR HANDLING TESTS
    // ========================================================================
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error response when strategy validation fails")
        void shouldReturnErrorWhenValidationFails() {
            // Given
            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Invalid credentials"));

            // When
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("Should return error response when order placement fails")
        void shouldReturnErrorWhenOrderPlacementFails() {
            // Given
            when(strategyService.validateStrategyCredentials(anyString(), anyString()))
                    .thenReturn(testStrategy);
            when(orderExecutionService.getLastOrderByStrategy(anyLong()))
                    .thenReturn(Optional.empty());
            when(hyperliquidService.placeOrder(anyString(), any(), any()))
                    .thenThrow(new RuntimeException("API Error"));

            // When
            WebhookResponse response = webhookService.processWebhook(buyRequest);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("API Error");
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    private OrderExecution createMockOrderExecution(String orderId, OrderExecution.OrderSide side) {
        return OrderExecution.builder()
                .id(1L)
                .primaryOrderId(orderId)
                .orderSide(side)
                .fillPrice(new BigDecimal("2500.00"))
                .orderSize(new BigDecimal("0.004"))
                .status(OrderExecution.OrderStatus.FILLED)
                .stopLossStatus(OrderExecution.StopLossStatus.NONE)
                .strategy(testStrategy)
                .user(testUser)
                .executedAt(LocalDateTime.now())
                .closedAt(null)
                .build();
    }
}
