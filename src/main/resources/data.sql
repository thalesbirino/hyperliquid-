-- Seed data for Hyperliquid Trading Bot POC
-- User passwords: "password123" (BCrypt hashed)
-- Strategy passwords: "Admin@9090" (BCrypt hashed)

-- Insert Users
INSERT INTO users (username, email, password, role, hyperliquid_private_key, hyperliquid_address, active, created_at, updated_at)
VALUES
    ('admin', 'admin@tradingbot.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'ADMIN', '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef', '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('trader001', 'trader001@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER', '0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890', '0x8626f6940E2eb28930eFb4CeF49B2d1F2C9C1199', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('trader002', 'trader002@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER', '0x9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba', '0xdD2FD4581271e230360230F9337D5c0430Bf44C0', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Configs
INSERT INTO configs (name, asset, asset_id, lot_size, sl_percent, tp_percent, leverage, order_type, time_in_force, created_at, updated_at)
VALUES
    ('ETH Scalping Config', 'ETH', 1, 0.1, 2.00, 5.00, 5, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BTC Conservative Config', 'BTC', 0, 0.01, 1.50, 3.00, 2, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOL Aggressive Config', 'SOL', 2, 5.0, 3.00, 10.00, 10, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AVAX Swing Trade Config', 'AVAX', 3, 2.5, 2.50, 7.50, 3, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Strategies (password is BCrypt hashed "Admin@9090")
INSERT INTO strategies (name, strategy_id, password, config_id, user_id, active, description, created_at, updated_at)
VALUES
    ('ETH Scalping Strategy', '66e858a5-ca3c-4c2c-909c-34c605b3e5c7', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 1, 2, true, 'Short-term scalping strategy for ETH with 5x leverage', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BTC Long-term Hold', 'f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 2, 2, true, 'Conservative BTC accumulation strategy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOL Momentum Trading', 'a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 3, 3, true, 'High leverage momentum trading on SOL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AVAX Swing Strategy', 'b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 4, 3, true, 'Medium-term swing trading for AVAX', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
