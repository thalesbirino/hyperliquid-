package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.entity.OrderExecution;
import com.trading.hyperliquid.model.entity.OrderExecution.OrderSide;
import com.trading.hyperliquid.model.entity.OrderExecution.OrderStatus;
import com.trading.hyperliquid.model.entity.OrderExecution.StopLossStatus;
import com.trading.hyperliquid.model.entity.OrderExecution.StopLossGrouping;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.OrderExecutionRepository;
import com.trading.hyperliquid.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing order executions and stop-loss tracking.
 * Handles CRUD operations and provides queries for risk management.
 */
@Slf4j
@Service
public class OrderExecutionService extends BaseService<OrderExecution, Long, OrderExecutionRepository> {

    public OrderExecutionService(OrderExecutionRepository orderExecutionRepository) {
        super(orderExecutionRepository, "OrderExecution");
    }

    /**
     * Create and save order execution record
     *
     * @param primaryOrderId Order ID from Hyperliquid
     * @param orderSide BUY or SELL
     * @param fillPrice Actual fill price
     * @param orderSize Order size
     * @param strategy Associated strategy
     * @param user Associated user
     * @return Created OrderExecution
     */
    @Transactional
    public OrderExecution createOrderExecution(
            String primaryOrderId,
            OrderSide orderSide,
            BigDecimal fillPrice,
            BigDecimal orderSize,
            Strategy strategy,
            User user) {

        log.debug("Creating order execution: orderId={}, side={}, price={}, size={}",
                primaryOrderId, orderSide, fillPrice, orderSize);

        OrderExecution execution = OrderExecution.builder()
                .primaryOrderId(primaryOrderId)
                .orderSide(orderSide)
                .fillPrice(fillPrice)
                .orderSize(orderSize)
                .status(OrderStatus.FILLED)
                .strategy(strategy)
                .user(user)
                .stopLossStatus(StopLossStatus.NONE)
                .executedAt(LocalDateTime.now())
                .build();

        OrderExecution saved = save(execution);
        log.info("Created order execution with id: {} for order: {}", saved.getId(), primaryOrderId);

        return saved;
    }

    /**
     * Update stop-loss order details
     *
     * @param executionId OrderExecution ID
     * @param stopLossOrderId Stop-loss order ID from Hyperliquid
     * @param stopLossPrice Calculated stop-loss price
     * @param groupingType POSITION_BASED or ORDER_BASED
     * @param status Stop-loss status
     */
    @Transactional
    public void updateStopLossOrder(
            Long executionId,
            String stopLossOrderId,
            BigDecimal stopLossPrice,
            StopLossGrouping groupingType,
            StopLossStatus status) {

        OrderExecution execution = findById(executionId);

        execution.setStopLossOrderId(stopLossOrderId);
        execution.setStopLossPrice(stopLossPrice);
        execution.setGroupingType(groupingType);
        execution.setStopLossStatus(status);
        execution.setStopLossPlacedAt(LocalDateTime.now());

        save(execution);

        log.info("Updated stop-loss for execution {}: orderId={}, price={}, grouping={}, status={}",
                executionId, stopLossOrderId, stopLossPrice, groupingType, status);
    }

    /**
     * Get active stop-loss orders for a strategy (for cancellation on signal reversal)
     *
     * @param strategyId Strategy ID
     * @return List of active order executions
     */
    @Transactional(readOnly = true)
    public List<OrderExecution> getActiveOrdersByStrategy(Long strategyId) {
        return repository.findActiveOrdersByStrategy(strategyId);
    }

    /**
     * Mark stop-loss as cancelled
     *
     * @param executionId OrderExecution ID
     */
    @Transactional
    public void markStopLossCancelled(Long executionId) {
        OrderExecution execution = findById(executionId);

        execution.setStopLossStatus(StopLossStatus.CANCELLED);
        execution.setStopLossCancelledAt(LocalDateTime.now());

        save(execution);

        log.info("Marked stop-loss as cancelled for execution: {}", executionId);
    }

    /**
     * Close position
     *
     * @param executionId OrderExecution ID
     */
    @Transactional
    public void closePosition(Long executionId) {
        OrderExecution execution = findById(executionId);

        execution.setClosedAt(LocalDateTime.now());

        save(execution);

        log.info("Closed position for execution: {}", executionId);
    }

    /**
     * Get open positions for a user
     *
     * @param userId User ID
     * @return List of open positions
     */
    @Transactional(readOnly = true)
    public List<OrderExecution> getOpenPositions(Long userId) {
        return repository.findOpenPositionsByUser(userId);
    }

    /**
     * Get all orders for a strategy
     *
     * @param strategyId Strategy ID
     * @return List of order executions
     */
    @Transactional(readOnly = true)
    public List<OrderExecution> getOrdersByStrategy(Long strategyId) {
        return repository.findByStrategyIdOrderByExecutedAtDesc(strategyId);
    }

    /**
     * Get failed stop-loss placements (for manual intervention)
     *
     * @return List of failed stop-loss orders
     */
    @Transactional(readOnly = true)
    public List<OrderExecution> getFailedStopLossPlacements() {
        return repository.findFailedStopLossPlacements();
    }

    /**
     * Count active stop-loss orders
     *
     * @return Count of active stop-loss orders
     */
    @Transactional(readOnly = true)
    public long countActiveStopLossOrders() {
        return repository.countActiveStopLossOrders();
    }

    /**
     * Find order execution by primary order ID
     *
     * @param primaryOrderId Order ID from Hyperliquid
     * @return OrderExecution
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public OrderExecution findByPrimaryOrderId(String primaryOrderId) {
        return repository.findByPrimaryOrderId(primaryOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order execution not found with primary order ID: " + primaryOrderId));
    }

    /**
     * Find order execution by stop-loss order ID
     *
     * @param stopLossOrderId Stop-loss order ID from Hyperliquid
     * @return OrderExecution
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public OrderExecution findByStopLossOrderId(String stopLossOrderId) {
        return repository.findByStopLossOrderId(stopLossOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order execution not found with stop-loss order ID: " + stopLossOrderId));
    }
}
