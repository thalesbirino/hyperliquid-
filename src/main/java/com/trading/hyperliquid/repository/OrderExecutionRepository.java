package com.trading.hyperliquid.repository;

import com.trading.hyperliquid.model.entity.OrderExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OrderExecution entity.
 * Provides queries for order tracking and stop-loss management.
 */
@Repository
public interface OrderExecutionRepository extends JpaRepository<OrderExecution, Long> {

    /**
     * Find order execution by primary order ID
     */
    Optional<OrderExecution> findByPrimaryOrderId(String primaryOrderId);

    /**
     * Find order execution by stop-loss order ID
     */
    Optional<OrderExecution> findByStopLossOrderId(String stopLossOrderId);

    /**
     * Find all active orders for a strategy (for cancellation on signal reversal)
     * Active = FILLED or PARTIALLY_FILLED with ACTIVE stop-loss
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.strategy.id = :strategyId " +
           "AND oe.status IN ('FILLED', 'PARTIALLY_FILLED') " +
           "AND oe.stopLossStatus = 'ACTIVE' " +
           "AND oe.closedAt IS NULL")
    List<OrderExecution> findActiveOrdersByStrategy(@Param("strategyId") Long strategyId);

    /**
     * Find all open positions for a strategy (for pyramid/inverse logic)
     * Open = FILLED or PARTIALLY_FILLED and not yet closed (regardless of stop-loss status)
     * This is used to check existing positions before placing new orders
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.strategy.id = :strategyId " +
           "AND oe.status IN ('FILLED', 'PARTIALLY_FILLED') " +
           "AND oe.closedAt IS NULL " +
           "ORDER BY oe.executedAt DESC")
    List<OrderExecution> findOpenPositionsByStrategy(@Param("strategyId") Long strategyId);

    /**
     * Find all open positions for a user
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.user.id = :userId " +
           "AND oe.status IN ('FILLED', 'PARTIALLY_FILLED') " +
           "AND oe.closedAt IS NULL " +
           "ORDER BY oe.executedAt DESC")
    List<OrderExecution> findOpenPositionsByUser(@Param("userId") Long userId);

    /**
     * Find all orders for a strategy (for reporting)
     */
    List<OrderExecution> findByStrategyIdOrderByExecutedAtDesc(Long strategyId);

    /**
     * Find orders within date range
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.executedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY oe.executedAt DESC")
    List<OrderExecution> findByExecutedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find failed stop-loss placements (for manual intervention)
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.stopLossStatus = 'FAILED' " +
           "ORDER BY oe.executedAt DESC")
    List<OrderExecution> findFailedStopLossPlacements();

    /**
     * Count active stop-loss orders
     */
    @Query("SELECT COUNT(oe) FROM OrderExecution oe WHERE oe.stopLossStatus = 'ACTIVE'")
    long countActiveStopLossOrders();

    /**
     * Find orders by user and strategy
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.user.id = :userId " +
           "AND oe.strategy.id = :strategyId " +
           "ORDER BY oe.executedAt DESC")
    List<OrderExecution> findByUserIdAndStrategyId(
            @Param("userId") Long userId,
            @Param("strategyId") Long strategyId
    );

    /**
     * Find the last order execution for a strategy (most recent by executedAt)
     * Used for flowchart logic: "Fetch last order for this particular strategy"
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.strategy.id = :strategyId " +
           "ORDER BY oe.executedAt DESC LIMIT 1")
    Optional<OrderExecution> findLastOrderByStrategy(@Param("strategyId") Long strategyId);

    /**
     * Check if any order exists for a strategy
     * Used for flowchart logic: "Is this First Order?"
     */
    @Query("SELECT COUNT(oe) > 0 FROM OrderExecution oe WHERE oe.strategy.id = :strategyId")
    boolean existsByStrategyId(@Param("strategyId") Long strategyId);
}
