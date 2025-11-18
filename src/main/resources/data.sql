-- Seed data for Hyperliquid Trading Bot POC
-- User passwords: "password123" (BCrypt hashed)
-- Strategy passwords: "Admin@9090" (BCrypt hashed)

-- Insert Users
INSERT INTO users (username, email, password, role, hyperliquid_private_key, hyperliquid_address, active, is_testnet, created_at, updated_at)
VALUES
    ('admin', 'admin@tradingbot.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'ADMIN', '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef', '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('trader001', 'trader001@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER', '0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890', '0x8626f6940E2eb28930eFb4CeF49B2d1F2C9C1199', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('trader002', 'trader002@example.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'USER', '0x9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba', '0xdD2FD4581271e230360230F9337D5c0430Bf44C0', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Configs
INSERT INTO configs (name, asset, asset_id, lot_size, sl_percent, tp_percent, leverage, order_type, time_in_force, created_at, updated_at)
VALUES
    ('ETH Scalping Config', 'ETH', 1, 0.1, 2.00, 5.00, 5, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BTC Conservative Config', 'BTC', 0, 0.01, 1.50, 3.00, 2, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOL Aggressive Config', 'SOL', 2, 5.0, 3.00, 10.00, 10, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AVAX Swing Trade Config', 'AVAX', 3, 2.5, 2.50, 7.50, 3, 'LIMIT', 'Gtc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Strategies (password is BCrypt hashed "Admin@9090")
-- Testing all 4 modes: pyramid and inverse combinations
INSERT INTO strategies (name, strategy_id, password, config_id, user_id, active, inverse, pyramid, description, created_at, updated_at)
VALUES
    -- MODE 1: pyramid=false, inverse=false (Most Restrictive)
    ('MODE1-ETH-Scalping', '66e858a5-ca3c-4c2c-909c-34c605b3e5c7', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 1, 2, true, false, false, 'MODE 1: Only ONE position allowed (pyramid=false, inverse=false)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MODE 2: pyramid=false, inverse=true (Allow Reversals)
    ('MODE2-BTC-Reversal', 'f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 2, 2, true, true, false, 'MODE 2: Position reversals allowed (pyramid=false, inverse=true)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MODE 3: pyramid=true, inverse=false (Pyramiding Only)
    ('MODE3-SOL-Pyramid', 'a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 3, 3, true, false, true, 'MODE 3: Pyramiding same direction only (pyramid=true, inverse=false)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- MODE 4: pyramid=true, inverse=true (Maximum Flexibility)
    ('MODE4-AVAX-Flexible', 'b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9', '$2a$10$.4xIxq7OtoXFaBuxa23.9ewCFety09oCsofyb8AltpGNtB.Y64P7a', 4, 3, true, true, true, 'MODE 4: Maximum flexibility (pyramid=true, inverse=true)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
