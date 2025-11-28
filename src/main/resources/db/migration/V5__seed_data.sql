-- ============================================
-- V5: Admin User for Production (Flyway)
-- ============================================
-- Creates only the admin user for production.
-- Users, configs, and strategies should be created via API.

-- ==========================================
-- ADMIN USER
-- ==========================================
-- Password: password123 (BCrypt hashed)
-- Change this password after first login!

INSERT INTO users (username, email, password, role, hyperliquid_private_key, hyperliquid_address, api_wallet_private_key, api_wallet_address, is_testnet, active, created_at, updated_at) VALUES
('admin', 'admin@tradingbot.com', '$2a$10$XkkgBRL6GUCwFSXz88OuRu/3VPI6cuaLKLNFseGvrPJ7ehFiHVl6G', 'ADMIN',
 NULL, NULL, NULL, NULL,
 true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
