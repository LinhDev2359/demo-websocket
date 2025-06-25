-- =====================================================
-- V3: Create Junction Tables - Refactoring @OneToMany
-- =====================================================
-- 
-- Mục đích: Thay thế @OneToMany relationships bằng Junction Tables
-- 
-- Lý do:
-- 1. ✅ Tránh N+1 query problems
-- 2. ✅ Better performance với large datasets  
-- 3. ✅ Flexible hơn cho complex relationships
-- 4. ✅ Easier testing và maintenance
-- 5. ✅ Better control over loading strategy

-- =====================================================
-- 1. USER_WALLETS Junction Table
-- =====================================================
-- Thay thế: User.wallets (@OneToMany)
-- Mục đích: Liên kết User với Wallet + metadata

CREATE TABLE user_wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    wallet_id BIGINT NOT NULL,
    
    -- Metadata về relationship
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    wallet_type ENUM('EOS') NOT NULL DEFAULT 'EOS',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_APPROVAL') NOT NULL DEFAULT 'ACTIVE',
    permission_level INT NOT NULL DEFAULT 5 CHECK (permission_level BETWEEN 1 AND 5),
    
    -- Audit fields
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(50),
    notes VARCHAR(500),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE KEY uk_user_wallet (user_id, wallet_id),
    UNIQUE KEY uk_user_primary_wallet (user_id, is_primary, wallet_type),
    
    -- Indexes cho performance
    INDEX idx_user_wallet_user_id (user_id),
    INDEX idx_user_wallet_wallet_id (wallet_id),
    INDEX idx_user_wallet_status (status),
    INDEX idx_user_wallet_primary (is_primary),
    INDEX idx_user_wallet_created (created_at),
    
    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

-- =====================================================
-- 2. WALLET_BALANCES Junction Table  
-- =====================================================
-- Thay thế: Wallet.balances (@OneToMany)
-- Mục đích: Liên kết Wallet với Balance + cached data

CREATE TABLE wallet_balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    balance_id BIGINT NOT NULL,
    
    -- Cached information từ Balance entity (để tránh JOIN)
    token_type ENUM('A', 'ram', 'rams', 'wram') NOT NULL,
    token_symbol VARCHAR(20) NOT NULL,
    token_contract VARCHAR(256),
    
    -- Current & previous amounts (để track changes)
    current_amount DECIMAL(30,8) NOT NULL DEFAULT 0.00000000,
    previous_amount DECIMAL(30,8) DEFAULT 0.00000000,
    usd_value DECIMAL(20,8) DEFAULT 0.00000000,
    
    -- Metadata về relationship
    status ENUM('ACTIVE', 'INACTIVE', 'HIDDEN', 'FROZEN') NOT NULL DEFAULT 'ACTIVE',
    sync_status ENUM('PENDING', 'SYNCED', 'FAILED', 'STALE') NOT NULL DEFAULT 'PENDING',
    is_primary_balance BOOLEAN NOT NULL DEFAULT FALSE,
    display_threshold DECIMAL(30,8) DEFAULT 0.00000000,
    
    -- Sync information
    first_detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP,
    notes VARCHAR(500),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE KEY uk_wallet_balance (wallet_id, balance_id),
    UNIQUE KEY uk_wallet_token_type (wallet_id, token_type, token_contract),
    
    -- Indexes cho performance  
    INDEX idx_wallet_balance_wallet_id (wallet_id),
    INDEX idx_wallet_balance_balance_id (balance_id),
    INDEX idx_wallet_balance_token (token_type),
    INDEX idx_wallet_balance_status (status),
    INDEX idx_wallet_balance_updated (last_synced_at),
    INDEX idx_wallet_balance_amount (current_amount),
    
    -- Foreign keys
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    FOREIGN KEY (balance_id) REFERENCES balances(id) ON DELETE CASCADE
);

-- =====================================================
-- 3. Data Migration - Populate Junction Tables
-- =====================================================
-- Migrate existing relationships sang Junction Tables

-- 3.1 Migrate User-Wallet relationships
INSERT INTO user_wallets (user_id, wallet_id, is_primary, wallet_type, status, permission_level, assigned_at)
SELECT 
    w.user_id,
    w.id as wallet_id,
    w.is_primary,
    w.wallet_type,
    CASE 
        WHEN w.status = 'ACTIVE' THEN 'ACTIVE'
        WHEN w.status = 'INACTIVE' THEN 'INACTIVE' 
        ELSE 'SUSPENDED'
    END as status,
    5 as permission_level, -- Default full access
    w.created_at as assigned_at
FROM wallets w
WHERE w.user_id IS NOT NULL;

-- 3.2 Migrate Wallet-Balance relationships  
INSERT INTO wallet_balances (
    wallet_id, balance_id, token_type, token_symbol, token_contract,
    current_amount, usd_value, status, first_detected_at, last_synced_at
)
SELECT 
    b.wallet_id,
    b.id as balance_id,
    b.token_type,
    b.token_symbol,
    b.token_contract,
    b.balance as current_amount,
    COALESCE(b.usd_value, 0) as usd_value,
    CASE 
        WHEN b.status = 'ACTIVE' THEN 'ACTIVE'
        WHEN b.status = 'INACTIVE' THEN 'INACTIVE'
        ELSE 'HIDDEN'
    END as status,
    b.created_at as first_detected_at,
    b.last_updated as last_synced_at
FROM balances b
WHERE b.wallet_id IS NOT NULL;

-- =====================================================
-- 4. Update Existing Tables Schema
-- =====================================================
-- Remove foreign key constraints từ existing tables
-- (Sẽ drop columns sau khi test Junction Tables)

-- 4.1 Add comment cho old columns
ALTER TABLE wallets 
    MODIFY COLUMN user_id VARCHAR(50) 
    COMMENT 'DEPRECATED: Use user_wallets junction table instead';

ALTER TABLE balances 
    MODIFY COLUMN wallet_id BIGINT 
    COMMENT 'DEPRECATED: Use wallet_balances junction table instead';

-- =====================================================
-- 5. Performance Optimization Views
-- =====================================================
-- Tạo views để dễ dàng query thông qua Junction Tables

-- 5.1 User Wallets View - Thay thế cho @OneToMany navigation
CREATE VIEW v_user_wallets AS
SELECT 
    uw.user_id,
    uw.wallet_id,
    w.wallet_address,
    w.wallet_name,
    uw.wallet_type,
    uw.is_primary,
    uw.status as user_wallet_status,
    w.status as wallet_status,
    uw.permission_level,
    uw.assigned_at,
    w.created_at as wallet_created_at,
    w.total_balance
FROM user_wallets uw
JOIN wallets w ON uw.wallet_id = w.id
WHERE uw.status = 'ACTIVE' AND w.status = 'ACTIVE';

-- 5.2 Wallet Balances View - Thay thế cho @OneToMany navigation  
CREATE VIEW v_wallet_balances AS
SELECT 
    wb.wallet_id,
    wb.balance_id,
    wb.token_type,
    wb.token_symbol,
    wb.token_contract,
    wb.current_amount,
    wb.previous_amount,
    wb.usd_value,
    wb.status as wallet_balance_status,
    b.status as balance_status,
    wb.last_synced_at,
    wb.sync_status,
    wb.is_primary_balance
FROM wallet_balances wb
JOIN balances b ON wb.balance_id = b.id
WHERE wb.status = 'ACTIVE' AND b.status = 'ACTIVE';

-- 5.3 Portfolio Summary View - Optimized cho portfolio queries
CREATE VIEW v_portfolio_summary AS
SELECT 
    uw.user_id,
    COUNT(DISTINCT uw.wallet_id) as total_wallets,
    COUNT(DISTINCT CASE WHEN uw.is_primary THEN uw.wallet_id END) as primary_wallets,
    SUM(CASE WHEN wb.token_type = 'A' THEN wb.current_amount ELSE 0 END) as total_eos,
    SUM(CASE WHEN wb.token_type = 'ram' THEN wb.current_amount ELSE 0 END) as total_ram,
    SUM(CASE WHEN wb.token_type = 'rams' THEN wb.current_amount ELSE 0 END) as total_rams,
    SUM(CASE WHEN wb.token_type = 'wram' THEN wb.current_amount ELSE 0 END) as total_wram,
    SUM(wb.usd_value) as total_usd_value,
    MAX(wb.last_synced_at) as last_updated
FROM user_wallets uw
LEFT JOIN wallet_balances wb ON uw.wallet_id = wb.wallet_id AND wb.status = 'ACTIVE'
WHERE uw.status = 'ACTIVE'
GROUP BY uw.user_id;

-- =====================================================
-- 6. Indexes for Junction Table Performance
-- =====================================================
-- Additional composite indexes cho common query patterns

-- User Wallets composite indexes
CREATE INDEX idx_user_wallets_user_status_primary ON user_wallets(user_id, status, is_primary);
CREATE INDEX idx_user_wallets_wallet_type_status ON user_wallets(wallet_type, status);
CREATE INDEX idx_user_wallets_assigned_date ON user_wallets(assigned_at DESC);

-- Wallet Balances composite indexes  
CREATE INDEX idx_wallet_balances_wallet_token ON wallet_balances(wallet_id, token_type, status);
CREATE INDEX idx_wallet_balances_amount_desc ON wallet_balances(current_amount DESC) WHERE current_amount > 0;
CREATE INDEX idx_wallet_balances_usd_desc ON wallet_balances(usd_value DESC) WHERE usd_value > 0;
CREATE INDEX idx_wallet_balances_sync_status ON wallet_balances(sync_status, last_synced_at);

-- =====================================================
-- 7. Stored Procedures cho Common Operations
-- =====================================================

-- 7.1 Get User Portfolio Procedure
DELIMITER //
CREATE PROCEDURE sp_get_user_portfolio(IN p_user_id VARCHAR(50))
BEGIN
    -- Get user basic info + wallet summary
    SELECT 
        u.user_id,
        u.username,
        ps.total_wallets,
        ps.total_eos,
        ps.total_ram,
        ps.total_rams,
        ps.total_wram,
        ps.total_usd_value,
        ps.last_updated
    FROM users u
    LEFT JOIN v_portfolio_summary ps ON u.user_id = ps.user_id
    WHERE u.user_id = p_user_id;
    
    -- Get detailed wallet info
    SELECT * FROM v_user_wallets 
    WHERE user_id = p_user_id 
    ORDER BY is_primary DESC, assigned_at ASC;
    
    -- Get detailed balance info
    SELECT wb.* 
    FROM v_wallet_balances wb
    JOIN user_wallets uw ON wb.wallet_id = uw.wallet_id
    WHERE uw.user_id = p_user_id AND uw.status = 'ACTIVE'
    ORDER BY wb.wallet_id, wb.usd_value DESC;
END //
DELIMITER ;

-- 7.2 Update Balance Procedure
DELIMITER //
CREATE PROCEDURE sp_update_wallet_balance(
    IN p_wallet_id BIGINT,
    IN p_token_type VARCHAR(20),
    IN p_new_amount DECIMAL(30,8),
    IN p_usd_value DECIMAL(20,8)
)
BEGIN
    UPDATE wallet_balances 
    SET 
        previous_amount = current_amount,
        current_amount = p_new_amount,
        usd_value = p_usd_value,
        last_synced_at = CURRENT_TIMESTAMP,
        sync_status = 'SYNCED'
    WHERE wallet_id = p_wallet_id 
        AND token_type = p_token_type 
        AND status = 'ACTIVE';
        
    -- Update wallet total balance
    UPDATE wallets 
    SET total_balance = (
        SELECT COALESCE(SUM(current_amount), 0) 
        FROM wallet_balances 
        WHERE wallet_id = p_wallet_id AND status = 'ACTIVE'
    )
    WHERE id = p_wallet_id;
END //
DELIMITER ;

-- =====================================================
-- Migration completed! 
-- =====================================================
-- Next steps:
-- 1. Test Junction Table approach thoroughly
-- 2. Update application code to use new repositories  
-- 3. Monitor performance improvements
-- 4. Drop old foreign key columns after confirmation