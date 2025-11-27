-- ============================================
-- V4: Create Order Executions Table
-- ============================================

CREATE TABLE order_executions (
    id BIGSERIAL PRIMARY KEY,

    -- Primary order details
    primary_order_id VARCHAR(100) NOT NULL,
    order_side VARCHAR(10) NOT NULL,
    fill_price DECIMAL(18, 8),
    order_size DECIMAL(18, 8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Associated entities
    strategy_id BIGINT NOT NULL REFERENCES strategies(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Stop-loss details
    stop_loss_order_id VARCHAR(100),
    stop_loss_price DECIMAL(18, 8),
    stop_loss_status VARCHAR(20) DEFAULT 'NONE',
    grouping_type VARCHAR(20),

    -- Timestamps
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stop_loss_placed_at TIMESTAMP,
    stop_loss_cancelled_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_order_executions_primary_order_id ON order_executions(primary_order_id);
CREATE INDEX idx_order_executions_stop_loss_order_id ON order_executions(stop_loss_order_id);
CREATE INDEX idx_order_executions_strategy_id ON order_executions(strategy_id);
CREATE INDEX idx_order_executions_user_id ON order_executions(user_id);
CREATE INDEX idx_order_executions_status ON order_executions(status);
CREATE INDEX idx_order_executions_executed_at ON order_executions(executed_at);
