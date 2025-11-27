-- ============================================
-- V3: Create Strategies Table
-- ============================================

CREATE TABLE strategies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    strategy_id VARCHAR(36) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    config_id BIGINT NOT NULL REFERENCES configs(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    inverse BOOLEAN NOT NULL DEFAULT FALSE,
    pyramid BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_strategies_strategy_id ON strategies(strategy_id);
CREATE INDEX idx_strategies_config_id ON strategies(config_id);
CREATE INDEX idx_strategies_user_id ON strategies(user_id);
CREATE INDEX idx_strategies_active ON strategies(active);
