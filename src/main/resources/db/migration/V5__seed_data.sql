-- ============================================
-- V5: Seed Data for Production (Flyway)
-- ============================================
-- This migration populates initial demo data.
-- Used when Flyway is enabled (prod profile).
--
-- IMPORTANT: Wallet credentials are placeholders!
-- After deployment, update users with real credentials via PUT /api/user/{id}

-- ==========================================
-- USERS
-- ==========================================
-- Password for all users: password123 (BCrypt hashed)

INSERT INTO users (username, email, password, role, hyperliquid_private_key, hyperliquid_address, api_wallet_private_key, api_wallet_address, is_testnet, active, created_at, updated_at) VALUES
-- Admin user (no trading wallet needed)
('admin', 'admin@tradingbot.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'ADMIN',
 NULL, NULL, NULL, NULL,
 true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Trader 001 (mainnet with API Wallet - CONFIGURE VIA API)
-- After deployment, update this user with real credentials via PUT /api/user/2
('trader001', 'trader001@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER',
 NULL,
 '0x0000000000000000000000000000000000000002',
 '0x0000000000000000000000000000000000000000000000000000000000000002',
 '0x0000000000000000000000000000000000000002',
 false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Trader 002 (testnet - CONFIGURE VIA API)
('trader002', 'trader002@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER',
 NULL,
 '0x0000000000000000000000000000000000000003',
 '0x0000000000000000000000000000000000000000000000000000000000000003',
 '0x0000000000000000000000000000000000000003',
 true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- CONFIGS (Trading Parameters)
-- ==========================================

INSERT INTO configs (name, asset, asset_id, lot_size, sl_percent, tp_percent, leverage, order_type, time_in_force, created_at, updated_at) VALUES
-- ETH Scalping Config
('ETH Scalping Config', 'ETH', 1, 0.1, 2.00, 5.00, 5, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BTC Long-term Config
('BTC Long-term Config', 'BTC', 0, 0.01, 5.00, 15.00, 3, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SOL Momentum Config
('SOL Momentum Config', 'SOL', 4, 1.0, 3.00, 10.00, 10, 'MARKET', 'Ioc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- AVAX Swing Config
('AVAX Swing Config', 'AVAX', 13, 5.0, 4.00, 12.00, 5, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- STRATEGIES
-- ==========================================
-- Password for all strategies: Admin@9090 (BCrypt hashed)
-- Use these strategyIds in TradingView webhooks

INSERT INTO strategies (name, strategy_id, password, description, config_id, user_id, inverse, pyramid, active, created_at, updated_at) VALUES
-- ETH Scalping Strategy (MODE 1: Normal - pyramid=false, inverse=false)
('ETH Scalping Strategy', '66e858a5-ca3c-4c2c-909c-34c605b3e5c7',
 '$2a$10$8K1p/a0dL1LXMIgoEDFrwOeS/vSB0HJV5yx0aIFnOqXnqYQFaQYmu',
 'Short-term scalping strategy for ETH with tight SL/TP',
 1, 2, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BTC Long-term Strategy (MODE 2: Pyramid - pyramid=true, inverse=false)
('BTC Long-term Hold', 'f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7',
 '$2a$10$8K1p/a0dL1LXMIgoEDFrwOeS/vSB0HJV5yx0aIFnOqXnqYQFaQYmu',
 'Long-term BTC accumulation with pyramid mode',
 2, 2, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SOL Momentum Strategy (MODE 3: Inverse - pyramid=false, inverse=true)
('SOL Momentum Trading', 'a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8',
 '$2a$10$8K1p/a0dL1LXMIgoEDFrwOeS/vSB0HJV5yx0aIFnOqXnqYQFaQYmu',
 'Momentum-based SOL trading with inverse signals',
 3, 3, true, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- AVAX Swing Strategy (MODE 4: Inverse Pyramid - pyramid=true, inverse=true)
('AVAX Swing Strategy', 'b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9',
 '$2a$10$8K1p/a0dL1LXMIgoEDFrwOeS/vSB0HJV5yx0aIFnOqXnqYQFaQYmu',
 'Swing trading AVAX with inverse pyramid mode',
 4, 3, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
