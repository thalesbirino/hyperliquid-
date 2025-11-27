-- ============================================
-- V1: Create Users Table
-- ============================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    hyperliquid_private_key VARCHAR(66),
    hyperliquid_address VARCHAR(42),
    api_wallet_private_key VARCHAR(66),
    api_wallet_address VARCHAR(42),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_testnet BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);
