package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.entity.*;
import com.trading.hyperliquid.repository.OrderExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderExecutionService covering:
 * - Order creation and tracking
 * - Stop-loss management
 * - Position closing (flowchart support methods)
 */
@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    private OrderExecutionService orderExecutionService;

    private User testUser;
    private Config testConfig;
    private Strategy testStrategy;
    private OrderExecution testOrderExecution;

    @BeforeEach
    void setUp() {
        orderExecutionService = new OrderExecutionService(orderExecutionRepository);

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .hyperliquidAddress("0x1234567890abcdef")
                .active(true)
                .build();

        // Setup test config
        testConfig = Config.builder()
                .id(1L)
                .name("ETH Config")
                .asset("ETH")
                .assetId(1)
                .lotSize(new BigDecimal("0.004"))
                .slPercent(new BigDecimal("2.00"))
                .build();

        // Setup test strategy
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

        // Setup test order execution
        testOrderExecution = OrderExecution.builder()
                .id(1L)
                .primaryOrderId("ORDER-123")
                .orderSide(OrderExecution.OrderSide.BUY)
                .fillPrice(new BigDecimal("2500.00"))
                .orderSize(new BigDecimal("0.004"))
                .status(OrderExecution.OrderStatus.FILLED)
                .stopLossStatus(OrderExecution.StopLossStatus.NONE)
                .strategy(testStrategy)
                .user(testUser)
                .executedAt(LocalDateTime.now())
                .build();
    }

    // ========================================================================
    // ORDER CREATION TESTS
    // ========================================================================
    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Should create order execution successfully")
        void shouldCreateOrderExecutionSuccessfully() {
            // Given
            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> {
                        OrderExecution saved = inv.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            // When
            OrderExecution result = orderExecutionService.createOrderExecution(
                    "NEW-ORDER-123",
                    OrderExecution.OrderSide.BUY,
                    new BigDecimal("2500.00"),
                    new BigDecimal("0.004"),
                    testStrategy,
                    testUser
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPrimaryOrderId()).isEqualTo("NEW-ORDER-123");
            assertThat(result.getOrderSide()).isEqualTo(OrderExecution.OrderSide.BUY);
            assertThat(result.getStatus()).isEqualTo(OrderExecution.OrderStatus.FILLED);
            assertThat(result.getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.NONE);

            verify(orderExecutionRepository).save(any(OrderExecution.class));
        }

        @Test
        @DisplayName("Should create SELL order execution")
        void shouldCreateSellOrderExecution() {
            // Given
            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> {
                        OrderExecution saved = inv.getArgument(0);
                        saved.setId(2L);
                        return saved;
                    });

            // When
            OrderExecution result = orderExecutionService.createOrderExecution(
                    "SELL-ORDER-456",
                    OrderExecution.OrderSide.SELL,
                    new BigDecimal("2600.00"),
                    new BigDecimal("0.008"),
                    testStrategy,
                    testUser
            );

            // Then
            assertThat(result.getOrderSide()).isEqualTo(OrderExecution.OrderSide.SELL);
            assertThat(result.getFillPrice()).isEqualTo(new BigDecimal("2600.00"));
            assertThat(result.getOrderSize()).isEqualTo(new BigDecimal("0.008"));
        }
    }

    // ========================================================================
    // STOP-LOSS MANAGEMENT TESTS
    // ========================================================================
    @Nested
    @DisplayName("Stop-Loss Management Tests")
    class StopLossManagementTests {

        @Test
        @DisplayName("Should update stop-loss order details")
        void shouldUpdateStopLossOrder() {
            // Given
            when(orderExecutionRepository.findById(1L))
                    .thenReturn(Optional.of(testOrderExecution));
            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            orderExecutionService.updateStopLossOrder(
                    1L,
                    "SL-ORDER-789",
                    new BigDecimal("2450.00"),
                    OrderExecution.StopLossGrouping.POSITION_BASED,
                    OrderExecution.StopLossStatus.ACTIVE
            );

            // Then
            ArgumentCaptor<OrderExecution> captor = ArgumentCaptor.forClass(OrderExecution.class);
            verify(orderExecutionRepository).save(captor.capture());

            OrderExecution saved = captor.getValue();
            assertThat(saved.getStopLossOrderId()).isEqualTo("SL-ORDER-789");
            assertThat(saved.getStopLossPrice()).isEqualTo(new BigDecimal("2450.00"));
            assertThat(saved.getGroupingType()).isEqualTo(OrderExecution.StopLossGrouping.POSITION_BASED);
            assertThat(saved.getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.ACTIVE);
            assertThat(saved.getStopLossPlacedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark stop-loss as cancelled")
        void shouldMarkStopLossCancelled() {
            // Given
            testOrderExecution.setStopLossStatus(OrderExecution.StopLossStatus.ACTIVE);
            testOrderExecution.setStopLossOrderId("SL-123");

            when(orderExecutionRepository.findById(1L))
                    .thenReturn(Optional.of(testOrderExecution));
            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            orderExecutionService.markStopLossCancelled(1L);

            // Then
            ArgumentCaptor<OrderExecution> captor = ArgumentCaptor.forClass(OrderExecution.class);
            verify(orderExecutionRepository).save(captor.capture());

            OrderExecution saved = captor.getValue();
            assertThat(saved.getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.CANCELLED);
            assertThat(saved.getStopLossCancelledAt()).isNotNull();
        }
    }

    // ========================================================================
    // FLOWCHART SUPPORT METHODS TESTS
    // ========================================================================
    @Nested
    @DisplayName("Flowchart Support Methods Tests")
    class FlowchartSupportTests {

        @Test
        @DisplayName("Should return true for first order when no orders exist")
        void shouldReturnTrueForFirstOrder() {
            // Given
            when(orderExecutionRepository.existsByStrategyId(1L)).thenReturn(false);

            // When
            boolean result = orderExecutionService.isFirstOrder(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for first order when orders exist")
        void shouldReturnFalseWhenOrdersExist() {
            // Given
            when(orderExecutionRepository.existsByStrategyId(1L)).thenReturn(true);

            // When
            boolean result = orderExecutionService.isFirstOrder(1L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should get last order by strategy")
        void shouldGetLastOrderByStrategy() {
            // Given
            when(orderExecutionRepository.findLastOrderByStrategy(1L))
                    .thenReturn(Optional.of(testOrderExecution));

            // When
            Optional<OrderExecution> result = orderExecutionService.getLastOrderByStrategy(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getPrimaryOrderId()).isEqualTo("ORDER-123");
        }

        @Test
        @DisplayName("Should return empty when no last order exists")
        void shouldReturnEmptyWhenNoLastOrder() {
            // Given
            when(orderExecutionRepository.findLastOrderByStrategy(1L))
                    .thenReturn(Optional.empty());

            // When
            Optional<OrderExecution> result = orderExecutionService.getLastOrderByStrategy(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get open positions by strategy")
        void shouldGetOpenPositionsByStrategy() {
            // Given
            List<OrderExecution> openPositions = List.of(testOrderExecution);
            when(orderExecutionRepository.findOpenPositionsByStrategy(1L))
                    .thenReturn(openPositions);

            // When
            List<OrderExecution> result = orderExecutionService.getOpenPositionsByStrategy(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrimaryOrderId()).isEqualTo("ORDER-123");
        }
    }

    // ========================================================================
    // POSITION CLOSING TESTS
    // ========================================================================
    @Nested
    @DisplayName("Position Closing Tests")
    class PositionClosingTests {

        @Test
        @DisplayName("Should close single position")
        void shouldCloseSinglePosition() {
            // Given
            when(orderExecutionRepository.findById(1L))
                    .thenReturn(Optional.of(testOrderExecution));
            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            orderExecutionService.closePosition(1L);

            // Then
            ArgumentCaptor<OrderExecution> captor = ArgumentCaptor.forClass(OrderExecution.class);
            verify(orderExecutionRepository).save(captor.capture());

            OrderExecution saved = captor.getValue();
            assertThat(saved.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should close all positions and cancel active SLs")
        void shouldCloseAllPositionsAndCancelActiveSLs() {
            // Given
            OrderExecution order1 = createOrderExecution(1L, "ORDER-1", OrderExecution.OrderSide.BUY);
            order1.setStopLossStatus(OrderExecution.StopLossStatus.ACTIVE);
            order1.setStopLossOrderId("SL-1");

            OrderExecution order2 = createOrderExecution(2L, "ORDER-2", OrderExecution.OrderSide.BUY);
            order2.setStopLossStatus(OrderExecution.StopLossStatus.NONE);

            List<OrderExecution> openPositions = List.of(order1, order2);

            when(orderExecutionRepository.save(any(OrderExecution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            orderExecutionService.closeAllPositions(openPositions);

            // Then
            verify(orderExecutionRepository, times(2)).save(any(OrderExecution.class));

            // Order 1 should have SL cancelled
            assertThat(order1.getClosedAt()).isNotNull();
            assertThat(order1.getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.CANCELLED);
            assertThat(order1.getStopLossCancelledAt()).isNotNull();

            // Order 2 should just be closed
            assertThat(order2.getClosedAt()).isNotNull();
            assertThat(order2.getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.NONE);
        }

        @Test
        @DisplayName("Should handle empty position list")
        void shouldHandleEmptyPositionList() {
            // Given
            List<OrderExecution> emptyList = List.of();

            // When
            orderExecutionService.closeAllPositions(emptyList);

            // Then
            verify(orderExecutionRepository, never()).save(any());
        }
    }

    // ========================================================================
    // DIRECTION DETECTION TESTS
    // ========================================================================
    @Nested
    @DisplayName("Direction Detection Tests")
    class DirectionDetectionTests {

        @Test
        @DisplayName("Should detect same direction position (BUY-BUY)")
        void shouldDetectSameDirectionBuy() {
            // Given
            OrderExecution buyOrder = createOrderExecution(1L, "BUY-ORDER", OrderExecution.OrderSide.BUY);
            List<OrderExecution> openPositions = List.of(buyOrder);

            // When
            boolean result = orderExecutionService.hasSameDirectionPosition(
                    openPositions,
                    OrderExecution.OrderSide.BUY
            );

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should detect same direction position (SELL-SELL)")
        void shouldDetectSameDirectionSell() {
            // Given
            OrderExecution sellOrder = createOrderExecution(1L, "SELL-ORDER", OrderExecution.OrderSide.SELL);
            List<OrderExecution> openPositions = List.of(sellOrder);

            // When
            boolean result = orderExecutionService.hasSameDirectionPosition(
                    openPositions,
                    OrderExecution.OrderSide.SELL
            );

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should detect opposite direction position")
        void shouldDetectOppositeDirection() {
            // Given
            OrderExecution buyOrder = createOrderExecution(1L, "BUY-ORDER", OrderExecution.OrderSide.BUY);
            List<OrderExecution> openPositions = List.of(buyOrder);

            // When
            boolean result = orderExecutionService.hasOppositeDirectionPosition(
                    openPositions,
                    OrderExecution.OrderSide.SELL
            );

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no opposite direction exists")
        void shouldReturnFalseWhenNoOppositeDirection() {
            // Given
            OrderExecution buyOrder = createOrderExecution(1L, "BUY-ORDER", OrderExecution.OrderSide.BUY);
            List<OrderExecution> openPositions = List.of(buyOrder);

            // When
            boolean result = orderExecutionService.hasOppositeDirectionPosition(
                    openPositions,
                    OrderExecution.OrderSide.BUY
            );

            // Then
            assertThat(result).isFalse();
        }
    }

    // ========================================================================
    // QUERY TESTS
    // ========================================================================
    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should find by primary order ID")
        void shouldFindByPrimaryOrderId() {
            // Given
            when(orderExecutionRepository.findByPrimaryOrderId("ORDER-123"))
                    .thenReturn(Optional.of(testOrderExecution));

            // When
            OrderExecution result = orderExecutionService.findByPrimaryOrderId("ORDER-123");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPrimaryOrderId()).isEqualTo("ORDER-123");
        }

        @Test
        @DisplayName("Should throw exception when primary order ID not found")
        void shouldThrowExceptionWhenPrimaryOrderIdNotFound() {
            // Given
            when(orderExecutionRepository.findByPrimaryOrderId("NON-EXISTENT"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                    orderExecutionService.findByPrimaryOrderId("NON-EXISTENT"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("primary order ID");
        }

        @Test
        @DisplayName("Should find by stop-loss order ID")
        void shouldFindByStopLossOrderId() {
            // Given
            testOrderExecution.setStopLossOrderId("SL-789");
            when(orderExecutionRepository.findByStopLossOrderId("SL-789"))
                    .thenReturn(Optional.of(testOrderExecution));

            // When
            OrderExecution result = orderExecutionService.findByStopLossOrderId("SL-789");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStopLossOrderId()).isEqualTo("SL-789");
        }

        @Test
        @DisplayName("Should get active orders by strategy")
        void shouldGetActiveOrdersByStrategy() {
            // Given
            List<OrderExecution> activeOrders = List.of(testOrderExecution);
            when(orderExecutionRepository.findActiveOrdersByStrategy(1L))
                    .thenReturn(activeOrders);

            // When
            List<OrderExecution> result = orderExecutionService.getActiveOrdersByStrategy(1L);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get failed stop-loss placements")
        void shouldGetFailedStopLossPlacements() {
            // Given
            testOrderExecution.setStopLossStatus(OrderExecution.StopLossStatus.FAILED);
            List<OrderExecution> failedOrders = List.of(testOrderExecution);
            when(orderExecutionRepository.findFailedStopLossPlacements())
                    .thenReturn(failedOrders);

            // When
            List<OrderExecution> result = orderExecutionService.getFailedStopLossPlacements();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStopLossStatus()).isEqualTo(OrderExecution.StopLossStatus.FAILED);
        }

        @Test
        @DisplayName("Should count active stop-loss orders")
        void shouldCountActiveStopLossOrders() {
            // Given
            when(orderExecutionRepository.countActiveStopLossOrders()).thenReturn(5L);

            // When
            long result = orderExecutionService.countActiveStopLossOrders();

            // Then
            assertThat(result).isEqualTo(5L);
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    private OrderExecution createOrderExecution(Long id, String orderId, OrderExecution.OrderSide side) {
        return OrderExecution.builder()
                .id(id)
                .primaryOrderId(orderId)
                .orderSide(side)
                .fillPrice(new BigDecimal("2500.00"))
                .orderSize(new BigDecimal("0.004"))
                .status(OrderExecution.OrderStatus.FILLED)
                .stopLossStatus(OrderExecution.StopLossStatus.NONE)
                .strategy(testStrategy)
                .user(testUser)
                .executedAt(LocalDateTime.now())
                .build();
    }
}
