-- ============================================
-- V2: Create Configs Table
-- ============================================

CREATE TABLE configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    asset VARCHAR(20) NOT NULL,
    asset_id INTEGER NOT NULL,
    lot_size DECIMAL(18, 8) NOT NULL,
    sl_percent DECIMAL(5, 2),
    tp_percent DECIMAL(5, 2),
    leverage INTEGER NOT NULL DEFAULT 1,
    order_type VARCHAR(20) NOT NULL DEFAULT 'LIMIT',
    time_in_force VARCHAR(10) DEFAULT 'Gtc',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_configs_asset ON configs(asset);
CREATE INDEX idx_configs_asset_id ON configs(asset_id);
