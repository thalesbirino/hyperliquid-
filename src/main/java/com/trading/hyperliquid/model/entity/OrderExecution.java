package com.trading.hyperliquid.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for tracking order executions and their associated stop-loss orders.
 * Maintains a complete audit trail of all trades and risk management actions.
 */
@Entity
@Table(name = "order_executions", indexes = {
        @Index(name = "idx_order_execution_primary_order_id", columnList = "primary_order_id"),
        @Index(name = "idx_order_execution_stop_loss_order_id", columnList = "stop_loss_order_id"),
        @Index(name = "idx_order_execution_strategy_id", columnList = "strategy_id"),
        @Index(name = "idx_order_execution_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Primary order details
    @Column(name = "primary_order_id", nullable = false, length = 100)
    private String primaryOrderId; // Order ID from Hyperliquid (oid)

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false, length = 10)
    private OrderSide orderSide; // BUY or SELL

    @Column(name = "fill_price", precision = 18, scale = 8)
    private BigDecimal fillPrice; // Actual fill price from Hyperliquid

    @Column(name = "order_size", nullable = false, precision = 18, scale = 8)
    private BigDecimal orderSize; // Order size

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Associated entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private Strategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Stop-loss details
    @Column(name = "stop_loss_order_id", length = 100)
    private String stopLossOrderId; // Stop-loss order ID from Hyperliquid

    @Column(name = "stop_loss_price", precision = 18, scale = 8)
    private BigDecimal stopLossPrice; // Calculated SL price

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_loss_status", length = 20)
    @Builder.Default
    private StopLossStatus stopLossStatus = StopLossStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "grouping_type", length = 20)
    private StopLossGrouping groupingType; // POSITION_BASED or ORDER_BASED

    // Timestamps
    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column(name = "stop_loss_placed_at")
    private LocalDateTime stopLossPlacedAt;

    @Column(name = "stop_loss_cancelled_at")
    private LocalDateTime stopLossCancelledAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Order execution status
     */
    public enum OrderStatus {
        PENDING,      // Order submitted but not confirmed
        FILLED,       // Order filled successfully
        PARTIALLY_FILLED, // Partially filled
        CANCELLED,    // Order cancelled
        FAILED        // Order failed
    }

    /**
     * Stop-loss order status
     */
    public enum StopLossStatus {
        NONE,         // No stop-loss configured
        PENDING,      // Stop-loss order submitted
        ACTIVE,       // Stop-loss order active on exchange
        TRIGGERED,    // Stop-loss triggered and executed
        CANCELLED,    // Stop-loss cancelled
        FAILED        // Stop-loss placement failed
    }

    /**
     * Stop-loss grouping type (Hyperliquid API)
     */
    public enum StopLossGrouping {
        POSITION_BASED,  // positionTpsl - SL linked to entire position
        ORDER_BASED      // normalTpsl - SL linked to specific order (OCO)
    }

    /**
     * Order side enum
     */
    public enum OrderSide {
        BUY, SELL;

        public boolean isBuy() {
            return this == BUY;
        }

        public OrderSide opposite() {
            return this == BUY ? SELL : BUY;
        }

        public String getAction() {
            return this.name().toLowerCase();
        }

        public static OrderSide fromAction(String action) {
            return action.equalsIgnoreCase("buy") ? BUY : SELL;
        }
    }
}
