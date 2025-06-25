-- Development Database Initialization Template
-- This file will be processed by envsubst to replace variables

-- Create application database if not exists
CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};
USE ${MYSQL_DATABASE};

-- Create application user with proper privileges for both localhost and remote connections
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';

-- Grant all privileges on database
GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'%';

-- Grant additional privileges for development
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, RELOAD on *.* TO '${MYSQL_USER}'@'localhost';
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, RELOAD on *.* TO '${MYSQL_USER}'@'%';

FLUSH PRIVILEGES;

-- Simple Users Table (no partitioning for dev)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Simple Wallets Table
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    wallet_address VARCHAR(255) UNIQUE NOT NULL,
    wallet_type ENUM('EOS') DEFAULT 'EOS',
    wallet_name VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    status ENUM('ACTIVE', 'INACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_wallet_address (wallet_address),
    
    CONSTRAINT fk_wallets_user_id 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Simple Balances Table
CREATE TABLE IF NOT EXISTS balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    token_type ENUM('A', 'ram', 'rams', 'wram') NOT NULL,
    balance DECIMAL(20,8) NOT NULL DEFAULT 0.00000000,
    usd_value DECIMAL(15,2) DEFAULT 0.00,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    
    UNIQUE KEY uk_wallet_token (wallet_address, token_type),
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_token_type (token_type),
    
    CONSTRAINT fk_balances_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sync Status Table
CREATE TABLE IF NOT EXISTS sync_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    last_sync_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sync_status ENUM('SUCCESS', 'FAILED', 'IN_PROGRESS') DEFAULT 'SUCCESS',
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_wallet_address (wallet_address),
    INDEX idx_last_sync_at (last_sync_at),
    INDEX idx_sync_status (sync_status),
    
    CONSTRAINT fk_sync_status_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert test data
INSERT IGNORE INTO users (user_id, username, email, password_hash, first_name, last_name, email_verified) VALUES
('user_000001', 'testuser1', 'test1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz', 'Test', 'User 1', TRUE),
('user_000002', 'testuser2', 'test2@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz', 'Test', 'User 2', TRUE);

-- Insert test wallets
INSERT IGNORE INTO wallets (user_id, wallet_address, wallet_name, is_primary) VALUES
('user_000001', 'eosio.test1234', 'Primary Wallet', TRUE),
('user_000002', 'eosio.test2345', 'Primary Wallet', TRUE);

-- Insert test balances
INSERT IGNORE INTO balances (wallet_address, token_type, balance, usd_value) VALUES
('eosio.test1234', 'A', 1000.50000000, 1000.50),
('eosio.test1234', 'ram', 2048.00000000, 10.24),
('eosio.test2345', 'A', 2500.75000000, 2500.75),
('eosio.test2345', 'ram', 4096.00000000, 20.48);

-- Insert sync status
INSERT IGNORE INTO sync_status (wallet_address, sync_status) VALUES
('eosio.test1234', 'SUCCESS'),
('eosio.test2345', 'SUCCESS');