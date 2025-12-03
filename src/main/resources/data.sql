-- ============================================
-- Hyperliquid Trading Bot - Seed Data
-- ============================================
-- This file populates the H2 database with demo data on startup.
-- Data resets on every restart (in-memory database).
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

-- Trader 001 (mainnet - CONFIGURE VIA API)
-- After deployment, update this user with real credentials via PUT /api/user/2
('trader001', 'trader001@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER',
 NULL,
 '0x0000000000000000000000000000000000000002',
 '0x0000000000000000000000000000000000000000000000000000000000000002',
 '0x0000000000000000000000000000000000000002',
 false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- CONFIG (ETH/USDC Only)
-- ==========================================
-- Single ETH config used by all 4 strategy modes

INSERT INTO configs (name, asset, asset_id, lot_size, sl_percent, tp_percent, leverage, order_type, time_in_force, created_at, updated_at) VALUES
('ETH Trading Config', 'ETH', 1, 0.01, 2.00, 5.00, 20, 'MARKET', 'Ioc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- STRATEGIES (4 Modes - All ETH/USDC)
-- ==========================================
-- Password for all strategies: password123 (BCrypt hashed - same as users)
-- All strategies use the same ETH config (config_id=1) and trader001 (user_id=2)

INSERT INTO strategies (name, strategy_id, password, description, config_id, user_id, inverse, pyramid, active, created_at, updated_at) VALUES

-- MODE 1: Normal (pyramid=false, inverse=false)
-- buy signal = open long, sell signal = close long
('ETH Mode 1 - Normal', '11111111-1111-1111-1111-111111111111',
 '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G',
 'MODE 1: Normal trading. Buy=Open Long, Sell=Close Long',
 1, 2, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- MODE 2: Pyramid (pyramid=true, inverse=false)
-- buy signal = add to long, sell signal = reduce long
('ETH Mode 2 - Pyramid', '22222222-2222-2222-2222-222222222222',
 '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G',
 'MODE 2: Pyramid mode. Buy=Add Long, Sell=Reduce Long',
 1, 2, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- MODE 3: Inverse (pyramid=false, inverse=true)
-- buy signal = open short, sell signal = close short
('ETH Mode 3 - Inverse', '33333333-3333-3333-3333-333333333333',
 '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G',
 'MODE 3: Inverse trading. Buy=Open Short, Sell=Close Short',
 1, 2, true, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- MODE 4: Inverse Pyramid (pyramid=true, inverse=true)
-- buy signal = add to short, sell signal = reduce short
('ETH Mode 4 - Inverse Pyramid', '44444444-4444-4444-4444-444444444444',
 '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G',
 'MODE 4: Inverse Pyramid. Buy=Add Short, Sell=Reduce Short',
 1, 2, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
