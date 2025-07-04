-- =====================================================
-- WALLET SYSTEM - READ REPLICAS & PERFORMANCE OPTIMIZATION
-- Version: 2.0
-- Support: Master-Slave replication cho millions users
-- =====================================================

-- =====================================================
-- DATABASE CONFIGURATION cho READ REPLICAS
-- =====================================================

-- Master database configuration
-- Thêm vào MySQL configuration file [mysqld] section:
-- 
-- # Master configuration
-- server-id = 1
-- log-bin = mysql-bin
-- binlog-format = ROW
-- binlog-do-db = wallet_system
-- 
-- # Performance optimization
-- innodb_buffer_pool_size = 4G
-- innodb_log_file_size = 512M
-- innodb_flush_log_at_trx_commit = 2
-- max_connections = 2000
-- table_open_cache = 8000
-- query_cache_type = 0
-- 
-- # Partitioning optimization
-- partition_balance_threshold = 10000

-- =====================================================
-- ADDITIONAL INDEXES cho PERFORMANCE OPTIMIZATION
-- =====================================================

-- Covering indexes cho common read queries
ALTER TABLE users 
ADD INDEX idx_users_covering_profile (user_id, username, email, first_name, last_name, status, created_at);

ALTER TABLE wallets 
ADD INDEX idx_wallets_covering_user (user_id, wallet_address, wallet_name, is_primary, status, created_at);

ALTER TABLE balances 
ADD INDEX idx_balances_covering_portfolio (wallet_address, token_type, balance, usd_value, last_updated);

-- Partial indexes cho better performance
ALTER TABLE balances 
ADD INDEX idx_balances_nonzero (wallet_address, token_type, balance) 
WHERE balance > 0;

ALTER TABLE users 
ADD INDEX idx_users_active_verified (user_id, email, created_at) 
WHERE status = 'ACTIVE' AND email_verified = TRUE;

-- =====================================================
-- MATERIALIZED VIEWS cho READ PERFORMANCE
-- =====================================================

-- Portfolio aggregation table for faster reads
CREATE TABLE portfolio_cache (
    user_id VARCHAR(50) NOT NULL PRIMARY KEY,
    total_wallets INT DEFAULT 0,
    active_wallets INT DEFAULT 0,
    total_eos_balance DECIMAL(20,8) DEFAULT 0.00000000,
    total_ram_balance DECIMAL(20,8) DEFAULT 0.00000000,
    total_cpu_balance DECIMAL(20,8) DEFAULT 0.00000000,
    total_net_balance DECIMAL(20,8) DEFAULT 0.00000000,
    total_usd_value DECIMAL(20,2) DEFAULT 0.00,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_portfolio_usd_value (total_usd_value),
    INDEX idx_portfolio_last_calculated (last_calculated),
    INDEX idx_portfolio_active_wallets (active_wallets),
    
    CONSTRAINT fk_portfolio_cache_user_id 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Cached portfolio data for fast reads'
  PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;

-- =====================================================
-- STORED PROCEDURES cho CACHE MANAGEMENT
-- =====================================================

DELIMITER $$

-- Procedure để refresh portfolio cache cho specific user
CREATE PROCEDURE RefreshUserPortfolioCache(IN p_user_id VARCHAR(50))
MODIFIES SQL DATA
BEGIN
    DECLARE v_total_wallets INT DEFAULT 0;
    DECLARE v_active_wallets INT DEFAULT 0;
    DECLARE v_total_eos DECIMAL(20,8) DEFAULT 0.00000000;
    DECLARE v_total_ram DECIMAL(20,8) DEFAULT 0.00000000;
    DECLARE v_total_cpu DECIMAL(20,8) DEFAULT 0.00000000;
    DECLARE v_total_net DECIMAL(20,8) DEFAULT 0.00000000;
    DECLARE v_total_usd DECIMAL(20,2) DEFAULT 0.00;
    
    -- Calculate portfolio statistics
    SELECT 
        COUNT(w.id),
        COUNT(CASE WHEN w.status = 'ACTIVE' THEN w.id END),
        COALESCE(SUM(CASE WHEN b.token_type = 'EOS' THEN b.balance END), 0),
        COALESCE(SUM(CASE WHEN b.token_type = 'RAM' THEN b.balance END), 0),
        COALESCE(SUM(CASE WHEN b.token_type = 'CPU' THEN b.balance END), 0),
        COALESCE(SUM(CASE WHEN b.token_type = 'NET' THEN b.balance END), 0),
        COALESCE(SUM(b.usd_value), 0)
    INTO 
        v_total_wallets, v_active_wallets, 
        v_total_eos, v_total_ram, v_total_cpu, v_total_net, 
        v_total_usd
    FROM wallets w
    LEFT JOIN balances b ON w.wallet_address = b.wallet_address
    WHERE w.user_id = p_user_id;
    
    -- Update cache table
    INSERT INTO portfolio_cache (
        user_id, total_wallets, active_wallets,
        total_eos_balance, total_ram_balance, total_cpu_balance, total_net_balance,
        total_usd_value, last_calculated
    ) VALUES (
        p_user_id, v_total_wallets, v_active_wallets,
        v_total_eos, v_total_ram, v_total_cpu, v_total_net,
        v_total_usd, CURRENT_TIMESTAMP
    ) ON DUPLICATE KEY UPDATE
        total_wallets = v_total_wallets,
        active_wallets = v_active_wallets,
        total_eos_balance = v_total_eos,
        total_ram_balance = v_total_ram,
        total_cpu_balance = v_total_cpu,
        total_net_balance = v_total_net,
        total_usd_value = v_total_usd,
        last_calculated = CURRENT_TIMESTAMP;
END$$

-- Procedure để bulk refresh portfolio cache
CREATE PROCEDURE BulkRefreshPortfolioCache(IN p_batch_size INT DEFAULT 1000)
MODIFIES SQL DATA
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_user_id VARCHAR(50);
    DECLARE user_cursor CURSOR FOR 
        SELECT DISTINCT u.user_id 
        FROM users u 
        WHERE u.status = 'ACTIVE'
        LIMIT p_batch_size;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN user_cursor;
    
    read_loop: LOOP
        FETCH user_cursor INTO v_user_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        CALL RefreshUserPortfolioCache(v_user_id);
    END LOOP;
    
    CLOSE user_cursor;
END$$

DELIMITER ;

-- =====================================================
-- TRIGGERS để AUTO-UPDATE CACHE
-- =====================================================

DELIMITER $$

-- Trigger để update portfolio cache khi wallet thay đổi
CREATE TRIGGER tr_wallets_update_cache
AFTER INSERT ON wallets
FOR EACH ROW
BEGIN
    CALL RefreshUserPortfolioCache(NEW.user_id);
END$$

CREATE TRIGGER tr_wallets_update_cache_on_update
AFTER UPDATE ON wallets
FOR EACH ROW
BEGIN
    CALL RefreshUserPortfolioCache(NEW.user_id);
    
    -- Nếu user_id thay đổi, update cả old user
    IF OLD.user_id != NEW.user_id THEN
        CALL RefreshUserPortfolioCache(OLD.user_id);
    END IF;
END$$

-- Trigger để update portfolio cache khi balance thay đổi
CREATE TRIGGER tr_balances_update_cache
AFTER INSERT ON balances
FOR EACH ROW
BEGIN
    -- Get user_id từ wallet_address
    DECLARE v_user_id VARCHAR(50);
    SELECT user_id INTO v_user_id 
    FROM wallets 
    WHERE wallet_address = NEW.wallet_address 
    LIMIT 1;
    
    IF v_user_id IS NOT NULL THEN
        CALL RefreshUserPortfolioCache(v_user_id);
    END IF;
END$$

CREATE TRIGGER tr_balances_update_cache_on_update
AFTER UPDATE ON balances
FOR EACH ROW
BEGIN
    -- Get user_id từ wallet_address
    DECLARE v_user_id VARCHAR(50);
    SELECT user_id INTO v_user_id 
    FROM wallets 
    WHERE wallet_address = NEW.wallet_address 
    LIMIT 1;
    
    IF v_user_id IS NOT NULL THEN
        CALL RefreshUserPortfolioCache(v_user_id);
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- ARCHIVE TABLES cho DATA RETENTION
-- =====================================================

-- Archived transaction history (older than 1 year)
CREATE TABLE transaction_history_archive (
    id BIGINT,
    wallet_address VARCHAR(255),
    transaction_type ENUM('BALANCE_UPDATE', 'SYNC', 'MANUAL_ADJUSTMENT'),
    old_balance DECIMAL(20,8),
    new_balance DECIMAL(20,8),
    token_type ENUM('EOS', 'RAM', 'CPU', 'NET'),
    change_amount DECIMAL(20,8),
    reason VARCHAR(500),
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_archived_wallet_created (wallet_address, created_at),
    INDEX idx_archived_at (archived_at)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Archived transaction history (older than 1 year)';

-- =====================================================
-- HOUSEKEEPING PROCEDURES
-- =====================================================

DELIMITER $$

-- Procedure để archive old transaction history
CREATE PROCEDURE ArchiveOldTransactionHistory(IN p_days_old INT DEFAULT 365)
MODIFIES SQL DATA
BEGIN
    DECLARE v_cutoff_date TIMESTAMP DEFAULT DATE_SUB(NOW(), INTERVAL p_days_old DAY);
    
    -- Move old records to archive table
    INSERT INTO transaction_history_archive (
        id, wallet_address, transaction_type, old_balance, new_balance,
        token_type, change_amount, reason, created_at, created_by
    )
    SELECT 
        id, wallet_address, transaction_type, old_balance, new_balance,
        token_type, change_amount, reason, created_at, created_by
    FROM transaction_history
    WHERE created_at < v_cutoff_date;
    
    -- Delete old records from main table
    DELETE FROM transaction_history 
    WHERE created_at < v_cutoff_date;
    
    -- Log the archiving operation
    SELECT 
        ROW_COUNT() as archived_records,
        v_cutoff_date as cutoff_date,
        NOW() as archived_at;
END$$

-- Procedure để cleanup expired portfolio cache
CREATE PROCEDURE CleanupExpiredPortfolioCache(IN p_hours_old INT DEFAULT 24)
MODIFIES SQL DATA
BEGIN
    DECLARE v_cutoff_time TIMESTAMP DEFAULT DATE_SUB(NOW(), INTERVAL p_hours_old HOUR);
    
    -- Update stale cache entries by recalculating
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_user_id VARCHAR(50);
    DECLARE stale_cursor CURSOR FOR 
        SELECT user_id 
        FROM portfolio_cache 
        WHERE last_calculated < v_cutoff_time;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN stale_cursor;
    
    refresh_loop: LOOP
        FETCH stale_cursor INTO v_user_id;
        IF done THEN
            LEAVE refresh_loop;
        END IF;
        
        CALL RefreshUserPortfolioCache(v_user_id);
    END LOOP;
    
    CLOSE stale_cursor;
END$$

DELIMITER ;

-- =====================================================
-- MONITORING TABLES cho SYSTEM HEALTH
-- =====================================================

-- System metrics table
CREATE TABLE system_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,4) NOT NULL,
    metric_unit VARCHAR(20),
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metrics_name_time (metric_name, recorded_at),
    INDEX idx_metrics_recorded_at (recorded_at)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='System performance metrics'
  PARTITION BY RANGE (UNIX_TIMESTAMP(recorded_at)) (
    PARTITION p_current VALUES LESS THAN (UNIX_TIMESTAMP('2025-01-01 00:00:00')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
  );

-- Database health checks table
CREATE TABLE health_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_name VARCHAR(100) NOT NULL,
    check_status ENUM('HEALTHY', 'WARNING', 'CRITICAL') NOT NULL,
    check_details JSON,
    response_time_ms INT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_health_name_status (check_name, check_status),
    INDEX idx_health_checked_at (checked_at)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Database health monitoring';

-- Insert initial system metrics
INSERT INTO system_metrics (metric_name, metric_value, metric_unit) VALUES
('total_users', 0, 'count'),
('total_wallets', 0, 'count'), 
('total_balances', 0, 'count'),
('avg_portfolio_value', 0.00, 'USD'),
('sync_success_rate', 100.00, 'percentage');

-- =====================================================
-- PERFORMANCE OPTIMIZATION HINTS
-- =====================================================

-- Optimize table statistics
ANALYZE TABLE users, wallets, balances, portfolio_cache;

-- Update optimizer statistics
UPDATE mysql.innodb_table_stats SET n_rows = 1000000 WHERE table_name = 'users';
UPDATE mysql.innodb_table_stats SET n_rows = 5000000 WHERE table_name = 'wallets';
UPDATE mysql.innodb_table_stats SET n_rows = 20000000 WHERE table_name = 'balances';

-- =====================================================
-- END OF MIGRATION V2
-- =====================================================