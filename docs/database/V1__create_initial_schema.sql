-- ============================================================================
-- Flyway Migration V1: Initial Database Schema
-- ============================================================================
-- Description: Creates initial tables for users, configs, and strategies
-- Author: Trading Bot Team
-- Date: 2025-11-17
-- ============================================================================
-- NOTE: This file is for REFERENCE ONLY in the POC version
-- The H2 in-memory database auto-creates tables from JPA entities
-- Use this script when migrating to production (PostgreSQL/MySQL) with Flyway
-- ============================================================================

-- ============================================================================
-- Table: users
-- Description: Stores user accounts with Hyperliquid wallet credentials
-- ============================================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- User identification
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    -- User role and status
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    active BOOLEAN NOT NULL DEFAULT true,

    -- Hyperliquid wallet credentials
    hyperliquid_address VARCHAR(42),
    hyperliquid_private_key VARCHAR(66),
    is_testnet BOOLEAN NOT NULL DEFAULT true,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User accounts with Hyperliquid wallet credentials';

-- ============================================================================
-- Table: configs
-- Description: Trading configuration templates (asset, lot size, risk params)
-- ============================================================================
CREATE TABLE configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Configuration identification
    name VARCHAR(100) NOT NULL,

    -- Asset configuration
    asset VARCHAR(20) NOT NULL,
    asset_id INTEGER NOT NULL,

    -- Order configuration
    lot_size DECIMAL(18, 8) NOT NULL,
    leverage INTEGER NOT NULL DEFAULT 1,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
    time_in_force VARCHAR(10) DEFAULT 'Gtc',

    -- Risk management
    sl_percent DECIMAL(5, 2),
    tp_percent DECIMAL(5, 2),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_asset (asset),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Trading configuration templates';

-- ============================================================================
-- Table: strategies
-- Description: Trading strategies linking users and configs
-- ============================================================================
CREATE TABLE strategies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Strategy identification
    name VARCHAR(200) NOT NULL,
    strategy_id VARCHAR(36) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    description VARCHAR(500),

    -- Strategy configuration
    active BOOLEAN NOT NULL DEFAULT true,
    inverse BOOLEAN NOT NULL DEFAULT false,
    pyramid BOOLEAN NOT NULL DEFAULT false,

    -- Foreign keys
    config_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_name (name),
    INDEX idx_active (active),

    -- Foreign key constraints
    CONSTRAINT fk_strategy_config FOREIGN KEY (config_id) REFERENCES configs(id) ON DELETE CASCADE,
    CONSTRAINT fk_strategy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Trading strategies linking users and configs';

-- ============================================================================
-- Initial Data (Optional - for production)
-- ============================================================================
-- Note: In POC, initial data is loaded from src/main/resources/data.sql
-- For production, consider using separate data migration scripts
-- ============================================================================
