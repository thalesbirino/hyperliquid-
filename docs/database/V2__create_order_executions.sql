-- ============================================================================
-- Flyway Migration V2: Order Executions Table
-- ============================================================================
-- Description: Creates order_executions table for tracking orders and stop-loss
-- Author: Trading Bot Team
-- Date: 2025-11-17
-- ============================================================================
-- NOTE: This file is for REFERENCE ONLY in the POC version
-- The H2 in-memory database auto-creates tables from JPA entities
-- Use this script when migrating to production (PostgreSQL/MySQL) with Flyway
-- ============================================================================

-- ============================================================================
-- Table: order_executions
-- Description: Tracks all order executions and their associated stop-loss orders
-- Provides complete audit trail for trading activity
-- ============================================================================
CREATE TABLE order_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- ========================================================================
    -- Primary Order Details
    -- ========================================================================
    primary_order_id VARCHAR(100) NOT NULL
        COMMENT 'Order ID returned from Hyperliquid API (oid field in response)',

    order_side VARCHAR(10) NOT NULL
        CHECK (order_side IN ('BUY', 'SELL'))
        COMMENT 'Side of the primary order',

    fill_price DECIMAL(18, 8)
        COMMENT 'Actual fill price from Hyperliquid exchange',

    order_size DECIMAL(18, 8) NOT NULL
        COMMENT 'Order size in base currency',

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED', 'FAILED'))
        COMMENT 'Current status of the primary order',

    -- ========================================================================
    -- Foreign Keys (Relations)
    -- ========================================================================
    strategy_id BIGINT NOT NULL
        COMMENT 'Reference to the strategy that triggered this order',

    user_id BIGINT NOT NULL
        COMMENT 'Reference to the user who owns this order',

    -- ========================================================================
    -- Stop-Loss Order Details
    -- ========================================================================
    stop_loss_order_id VARCHAR(100)
        COMMENT 'Stop-loss order ID from Hyperliquid API (oid field)',

    stop_loss_price DECIMAL(18, 8)
        COMMENT 'Calculated stop-loss trigger price',

    stop_loss_status VARCHAR(20) DEFAULT 'NONE'
        CHECK (stop_loss_status IN ('NONE', 'PENDING', 'ACTIVE', 'TRIGGERED', 'CANCELLED', 'FAILED'))
        COMMENT 'Current status of the stop-loss order',

    grouping_type VARCHAR(20)
        CHECK (grouping_type IN ('POSITION_BASED', 'ORDER_BASED'))
        COMMENT 'Hyperliquid grouping type: POSITION_BASED (positionTpsl) or ORDER_BASED (normalTpsl)',

    -- ========================================================================
    -- Lifecycle Timestamps
    -- ========================================================================
    executed_at TIMESTAMP NOT NULL
        COMMENT 'When the primary order was executed',

    stop_loss_placed_at TIMESTAMP
        COMMENT 'When the stop-loss order was placed',

    stop_loss_cancelled_at TIMESTAMP
        COMMENT 'When the stop-loss order was cancelled',

    closed_at TIMESTAMP
        COMMENT 'When the position was fully closed',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Record creation timestamp',

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Record last update timestamp',

    -- ========================================================================
    -- Indexes for Query Performance
    -- ========================================================================
    INDEX idx_order_execution_primary_order_id (primary_order_id)
        COMMENT 'Fast lookup by primary order ID',

    INDEX idx_order_execution_stop_loss_order_id (stop_loss_order_id)
        COMMENT 'Fast lookup by stop-loss order ID',

    INDEX idx_order_execution_strategy_id (strategy_id)
        COMMENT 'Fast lookup by strategy',

    INDEX idx_order_execution_user_id (user_id)
        COMMENT 'Fast lookup by user',

    INDEX idx_order_execution_status (status)
        COMMENT 'Fast filtering by order status',

    INDEX idx_order_execution_stop_loss_status (stop_loss_status)
        COMMENT 'Fast filtering by stop-loss status',

    INDEX idx_order_execution_executed_at (executed_at)
        COMMENT 'Fast sorting by execution time',

    -- ========================================================================
    -- Foreign Key Constraints
    -- ========================================================================
    CONSTRAINT fk_order_execution_strategy
        FOREIGN KEY (strategy_id) REFERENCES strategies(id)
        ON DELETE CASCADE
        COMMENT 'Link to strategy - cascade delete if strategy is removed',

    CONSTRAINT fk_order_execution_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        COMMENT 'Link to user - cascade delete if user is removed'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Complete audit trail of order executions and stop-loss orders';

-- ============================================================================
-- Sample Queries (for reference)
-- ============================================================================

-- Find all active stop-loss orders for a strategy
-- SELECT * FROM order_executions
-- WHERE strategy_id = ?
--   AND status IN ('FILLED', 'PARTIALLY_FILLED')
--   AND stop_loss_status = 'ACTIVE'
--   AND closed_at IS NULL;

-- Find orders with failed stop-loss placement
-- SELECT * FROM order_executions
-- WHERE stop_loss_status = 'FAILED'
--   AND executed_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

-- Get total profit/loss for a user (requires additional price data)
-- SELECT
--     user_id,
--     COUNT(*) as total_orders,
--     SUM(CASE WHEN stop_loss_status = 'TRIGGERED' THEN 1 ELSE 0 END) as stop_losses_hit
-- FROM order_executions
-- WHERE user_id = ?
-- GROUP BY user_id;

-- ============================================================================
-- End of Migration V2
-- ============================================================================
