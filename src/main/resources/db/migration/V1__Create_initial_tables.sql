-- =====================================================
-- EOS WALLET SYSTEM - DATABASE SCHEMA
-- Optimized for millions of users with sharding strategy
-- =====================================================

-- Users Table with Hash Partitioning
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'External user identifier',
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    -- Indexes for performance
    INDEX idx_user_id (user_id),
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_last_login (last_login_at)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Users table partitioned for millions of users'
  PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;

-- Wallets Table with Hash Partitioning by user_id
CREATE TABLE wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    wallet_address VARCHAR(255) UNIQUE NOT NULL COMMENT 'EOS wallet address',
    wallet_type ENUM('EOS') DEFAULT 'EOS',
    wallet_name VARCHAR(100) COMMENT 'User-defined wallet name',
    is_primary BOOLEAN DEFAULT FALSE COMMENT 'Primary wallet flag',
    status ENUM('ACTIVE', 'INACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_user_id (user_id),
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_wallet_type (wallet_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_is_primary (is_primary),
    
    -- Foreign key constraint
    CONSTRAINT fk_wallets_user_id 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Wallets table partitioned by user_id'
  PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;

-- Balances Table with Hash Partitioning by wallet_address
CREATE TABLE balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    token_type ENUM('A', 'ram', 'rams', 'wram') NOT NULL COMMENT 'EOS token types',
    balance DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT 'Token balance with 8 decimal precision',
    usd_value DECIMAL(15,2) DEFAULT 0.00 COMMENT 'USD equivalent value',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0 COMMENT 'Optimistic locking version',
    
    -- Unique constraint for wallet-token combination
    UNIQUE KEY uk_wallet_token (wallet_address, token_type),
    
    -- Indexes for performance
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_token_type (token_type),
    INDEX idx_last_updated (last_updated),
    INDEX idx_balance (balance),
    INDEX idx_usd_value (usd_value),
    
    -- Foreign key constraint
    CONSTRAINT fk_balances_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Balances table partitioned by wallet_address with more partitions'
  PARTITION BY HASH(CRC32(wallet_address)) PARTITIONS 32;

-- Sync Status Table for tracking balance synchronization
CREATE TABLE sync_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    last_sync_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sync_status ENUM('SUCCESS', 'FAILED', 'IN_PROGRESS') DEFAULT 'SUCCESS',
    error_message TEXT COMMENT 'Error details if sync failed',
    retry_count INT DEFAULT 0 COMMENT 'Number of retry attempts',
    sync_duration_ms INT DEFAULT 0 COMMENT 'Sync duration in milliseconds',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Unique constraint for wallet address
    UNIQUE KEY uk_wallet_address (wallet_address),
    
    -- Indexes for monitoring and performance
    INDEX idx_last_sync_at (last_sync_at),
    INDEX idx_sync_status (sync_status),
    INDEX idx_retry_count (retry_count),
    INDEX idx_created_at (created_at),
    
    -- Foreign key constraint
    CONSTRAINT fk_sync_status_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Sync status tracking table';

-- Transaction History Table for audit trail
CREATE TABLE transaction_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    transaction_type ENUM('BALANCE_UPDATE', 'SYNC', 'MANUAL_ADJUSTMENT') NOT NULL,
    old_balance DECIMAL(20,8),
    new_balance DECIMAL(20,8),
    token_type ENUM('A', 'ram', 'rams', 'wram') NOT NULL,
    change_amount DECIMAL(20,8) NOT NULL DEFAULT 0.00000000,
    reason VARCHAR(500) COMMENT 'Reason for the change',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    
    -- Indexes for performance
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_token_type (token_type),
    INDEX idx_created_at (created_at),
    INDEX idx_created_by (created_by),
    
    -- Composite index for wallet + time range queries
    INDEX idx_wallet_created (wallet_address, created_at),
    
    -- Foreign key constraint
    CONSTRAINT fk_transaction_history_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Transaction history for audit trail'
  PARTITION BY RANGE (UNIX_TIMESTAMP(created_at)) (
    PARTITION p202401 VALUES LESS THAN (UNIX_TIMESTAMP('2024-02-01 00:00:00')),
    PARTITION p202402 VALUES LESS THAN (UNIX_TIMESTAMP('2024-03-01 00:00:00')),
    PARTITION p202403 VALUES LESS THAN (UNIX_TIMESTAMP('2024-04-01 00:00:00')),
    PARTITION p202404 VALUES LESS THAN (UNIX_TIMESTAMP('2024-05-01 00:00:00')),
    PARTITION p202405 VALUES LESS THAN (UNIX_TIMESTAMP('2024-06-01 00:00:00')),
    PARTITION p202406 VALUES LESS THAN (UNIX_TIMESTAMP('2024-07-01 00:00:00')),
    PARTITION p202407 VALUES LESS THAN (UNIX_TIMESTAMP('2024-08-01 00:00:00')),
    PARTITION p202408 VALUES LESS THAN (UNIX_TIMESTAMP('2024-09-01 00:00:00')),
    PARTITION p202409 VALUES LESS THAN (UNIX_TIMESTAMP('2024-10-01 00:00:00')),
    PARTITION p202410 VALUES LESS THAN (UNIX_TIMESTAMP('2024-11-01 00:00:00')),
    PARTITION p202411 VALUES LESS THAN (UNIX_TIMESTAMP('2024-12-01 00:00:00')),
    PARTITION p202412 VALUES LESS THAN (UNIX_TIMESTAMP('2025-01-01 00:00:00')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
  );

-- User Sessions Table for JWT token management
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    session_token VARCHAR(512) NOT NULL,
    refresh_token VARCHAR(512),
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Indexes for performance
    INDEX idx_user_id (user_id),
    INDEX idx_session_token (session_token),
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_expires_at (expires_at),
    INDEX idx_is_active (is_active),
    INDEX idx_last_activity (last_activity),
    
    -- Foreign key constraint
    CONSTRAINT fk_user_sessions_user_id 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='User sessions for JWT token management'
  PARTITION BY HASH(CRC32(user_id)) PARTITIONS 8;

-- Create triggers for transaction history
DELIMITER $$

CREATE TRIGGER tr_balances_update_history
AFTER UPDATE ON balances
FOR EACH ROW
BEGIN
    IF OLD.balance != NEW.balance THEN
        INSERT INTO transaction_history (
            wallet_address, 
            transaction_type, 
            old_balance, 
            new_balance, 
            token_type, 
            change_amount,
            reason
        ) VALUES (
            NEW.wallet_address,
            'BALANCE_UPDATE',
            OLD.balance,
            NEW.balance,
            NEW.token_type,
            NEW.balance - OLD.balance,
            'Automated balance sync'
        );
    END IF;
END$$

DELIMITER ;

-- Create stored procedures for performance optimization
DELIMITER $$

-- Procedure to get user portfolio efficiently
CREATE PROCEDURE GetUserPortfolio(IN p_user_id VARCHAR(50))
READS SQL DATA
DETERMINISTIC
BEGIN
    SELECT 
        w.wallet_address,
        w.wallet_name,
        w.is_primary,
        b.token_type,
        b.balance,
        b.usd_value,
        b.last_updated,
        ss.last_sync_at,
        ss.sync_status
    FROM wallets w
    LEFT JOIN balances b ON w.wallet_address = b.wallet_address
    LEFT JOIN sync_status ss ON w.wallet_address = ss.wallet_address
    WHERE w.user_id = p_user_id 
      AND w.status = 'ACTIVE'
    ORDER BY w.is_primary DESC, w.created_at ASC, b.token_type ASC;
END$$

-- Procedure to cleanup old sessions
CREATE PROCEDURE CleanupExpiredSessions()
MODIFIES SQL DATA
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < NOW() 
       OR (is_active = FALSE AND last_activity < DATE_SUB(NOW(), INTERVAL 7 DAY));
END$$

DELIMITER ;

-- Create views for common queries
CREATE VIEW v_user_portfolio_summary AS
SELECT 
    u.user_id,
    u.username,
    u.email,
    COUNT(DISTINCT w.id) as total_wallets,
    COUNT(DISTINCT CASE WHEN w.status = 'ACTIVE' THEN w.id END) as active_wallets,
    SUM(CASE WHEN b.token_type = 'A' THEN b.balance ELSE 0 END) as total_a_balance,
    SUM(CASE WHEN b.token_type = 'A' THEN b.usd_value ELSE 0 END) as total_a_usd_value,
    MAX(b.last_updated) as last_balance_update
FROM users u
LEFT JOIN wallets w ON u.user_id = w.user_id
LEFT JOIN balances b ON w.wallet_address = b.wallet_address
WHERE u.status = 'ACTIVE'
GROUP BY u.user_id, u.username, u.email;

-- Insert default data for testing
INSERT INTO users (user_id, username, email, password_hash, first_name, last_name, email_verified) VALUES
('user_000001', 'testuser1', 'test1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz', 'Test', 'User 1', TRUE),
('user_000002', 'testuser2', 'test2@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz', 'Test', 'User 2', TRUE);

-- Performance optimization hints
-- Add these to MySQL configuration for production:
-- innodb_buffer_pool_size = 70% of available RAM
-- innodb_log_file_size = 256M
-- innodb_flush_log_at_trx_commit = 2
-- query_cache_type = 0 (disable for high write workload)
-- max_connections = 1000+
-- table_open_cache = 4000+