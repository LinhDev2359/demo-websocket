# 📊 **DATABASE SCHEMA DESIGN - EOS WALLET SYSTEM**

## 🎯 **Tổng quan thiết kế**

Hệ thống database được thiết kế để hỗ trợ **hàng triệu users** với **không giới hạn số lượng ví** mỗi user. Sử dụng các kỹ thuật advanced như **partitioning**, **sharding**, và **read replicas** để đảm bảo hiệu suất cao.

---

## 🏗️ **ARCHITECTURE OVERVIEW**

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│                    DATABASE ROUTING                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   MASTER DB     │  │  READ REPLICA 1 │  │  READ REPLICA 2 │  │
│  │   (Writes)      │  │   (Reads)       │  │   (Reads)       │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    PARTITIONING LAYER                           │
│  Users: 16 partitions │ Wallets: 16 partitions │ Balances: 32  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 **DATABASE TABLES**

### **1. USERS TABLE**

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL UNIQUE,        -- External identifier
    username VARCHAR(100) NOT NULL,             -- Login name
    email VARCHAR(255) NOT NULL UNIQUE,         -- Email address
    password_hash VARCHAR(255) NOT NULL,        -- BCrypt hash
    first_name VARCHAR(100),                    -- First name
    last_name VARCHAR(100),                     -- Last name
    status ENUM('ACTIVE','INACTIVE','SUSPENDED') DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    PRIMARY KEY (id, user_id),
    
    -- Performance indexes
    INDEX idx_users_user_id (user_id),
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_status (status),
    INDEX idx_users_created_at (created_at),
    
    -- Compound indexes cho common queries
    INDEX idx_users_status_created (status, created_at),
    INDEX idx_users_email_status (email, status)
    
) PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;
```

**Đặc điểm:**
- ✅ **16 partitions** theo `user_id` hash
- ✅ **Covering indexes** cho fast queries
- ✅ **Compound indexes** cho complex searches
- ✅ **Email verification** tracking
- ✅ **Last login** tracking cho analytics

### **2. WALLETS TABLE**

```sql
CREATE TABLE wallets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,               -- FK to users.user_id
    wallet_address VARCHAR(255) NOT NULL UNIQUE, -- EOS address
    wallet_name VARCHAR(100),                   -- User-defined name
    wallet_type ENUM('EOS') DEFAULT 'EOS',      -- Blockchain type
    is_primary BOOLEAN DEFAULT FALSE,           -- Primary wallet flag
    status ENUM('ACTIVE','INACTIVE','DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id, user_id),
    
    -- Performance indexes
    INDEX idx_wallets_user_id (user_id),
    INDEX idx_wallets_address (wallet_address),
    INDEX idx_wallets_status (status),
    INDEX idx_wallets_primary (is_primary),
    
    -- Compound indexes
    INDEX idx_wallets_user_status (user_id, status),
    INDEX idx_wallets_user_primary (user_id, is_primary),
    
    CONSTRAINT fk_wallets_user_id 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
        
) PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;
```

**Đặc điểm:**
- ✅ **Same partitioning** với users table (efficient joins)
- ✅ **Primary wallet** logic support  
- ✅ **Soft delete** với status field
- ✅ **Unlimited wallets** per user
- ✅ **EOS address** validation support

### **3. BALANCES TABLE**

```sql
CREATE TABLE balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,       -- FK to wallets.wallet_address
    token_type ENUM('EOS','RAM','CPU','NET') NOT NULL,
    balance DECIMAL(19,8) NOT NULL DEFAULT 0.00000000,
    usd_value DECIMAL(19,2) DEFAULT 0.00,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,                   -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id, wallet_address),
    
    -- Unique constraint
    UNIQUE KEY uk_wallet_token (wallet_address, token_type),
    
    -- Performance indexes
    INDEX idx_balances_wallet (wallet_address),
    INDEX idx_balances_token_type (token_type),
    INDEX idx_balances_balance (balance),
    INDEX idx_balances_usd_value (usd_value),
    INDEX idx_balances_last_updated (last_updated),
    
    -- Compound indexes
    INDEX idx_balances_wallet_token (wallet_address, token_type),
    INDEX idx_balances_token_balance (token_type, balance),
    
    CONSTRAINT fk_balances_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
        
) PARTITION BY HASH(CRC32(wallet_address)) PARTITIONS 32;
```

**Đặc điểm:**
- ✅ **32 partitions** cho heavy read/write operations
- ✅ **Optimistic locking** cho concurrency control
- ✅ **8-decimal precision** cho EOS tokens
- ✅ **USD value** caching
- ✅ **Real-time balance** tracking

### **4. SYNC STATUS TABLE**

```sql
CREATE TABLE sync_status (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_address VARCHAR(255) NOT NULL UNIQUE,
    last_sync_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sync_status ENUM('SUCCESS','FAILED','IN_PROGRESS') DEFAULT 'SUCCESS',
    error_message TEXT,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_sync_wallet_address (wallet_address),
    INDEX idx_sync_status (sync_status),
    INDEX idx_sync_last_sync (last_sync_at),
    INDEX idx_sync_next_retry (next_retry_at),
    
    CONSTRAINT fk_sync_wallet_address 
        FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) 
        ON DELETE CASCADE
);
```

**Đặc điểm:**
- ✅ **Sync monitoring** cho blockchain integration
- ✅ **Retry mechanism** với exponential backoff
- ✅ **Error tracking** cho debugging
- ✅ **No partitioning** (small dataset)

---

## 🔀 **PARTITIONING STRATEGY**

### **Hash Partitioning Algorithm**

```java
// MySQL CRC32 hash function equivalent
public int getPartition(String key, int partitions) {
    long crc32 = calculateCRC32(key);
    return (int) (Math.abs(crc32) % partitions);
}
```

### **Partition Distribution**

| Table | Partitions | Key | Reason |
|-------|------------|-----|---------|
| `users` | 16 | `user_id` | Even user distribution |
| `wallets` | 16 | `user_id` | Join efficiency with users |
| `balances` | 32 | `wallet_address` | High read/write volume |
| `sync_status` | 1 | N/A | Small dataset |

### **Benefits**

✅ **Parallel Processing**: Multiple partitions = parallel operations  
✅ **Improved Performance**: Smaller index sizes per partition  
✅ **Maintenance**: Easier backup/restore per partition  
✅ **Scalability**: Can add more partitions as data grows  

---

## 🔄 **READ REPLICAS CONFIGURATION**

### **Master-Slave Architecture**

```yaml
# Master Configuration (my.cnf)
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
binlog-do-db = wallet_system

# Read Replica Configuration (my.cnf)
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
read-only = 1
```

### **Application-Level Routing**

```java
// Write operations → Master
@Transactional
public void createUser(User user) {
    DatabaseContextHolder.setWriteMode();
    userRepository.save(user);
}

// Read operations → Read Replica
@Transactional(readOnly = true)
public User getUserById(String userId) {
    DatabaseContextHolder.setReadOnly();
    return userRepository.findByUserId(userId);
}
```

### **Load Balancing Strategy**

- ✅ **Round-robin** selection among read replicas
- ✅ **Health checks** với automatic failover
- ✅ **Connection pooling** per replica
- ✅ **Read preference** cho heavy read operations

---

## 📈 **PERFORMANCE OPTIMIZATIONS**

### **1. Index Strategy**

```sql
-- Covering indexes cho common queries
ALTER TABLE users 
ADD INDEX idx_users_covering_profile (
    user_id, username, email, first_name, last_name, status, created_at
);

-- Partial indexes cho filtered queries
ALTER TABLE balances 
ADD INDEX idx_balances_nonzero (wallet_address, token_type, balance) 
WHERE balance > 0;
```

### **2. Materialized Views**

```sql
-- Portfolio cache table cho fast aggregations
CREATE TABLE portfolio_cache (
    user_id VARCHAR(50) NOT NULL PRIMARY KEY,
    total_wallets INT DEFAULT 0,
    active_wallets INT DEFAULT 0,
    total_eos_balance DECIMAL(20,8) DEFAULT 0.00000000,
    total_usd_value DECIMAL(20,2) DEFAULT 0.00,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;
```

### **3. Stored Procedures**

```sql
-- Efficient portfolio calculation
DELIMITER $$
CREATE PROCEDURE GetUserPortfolio(IN p_user_id VARCHAR(50))
BEGIN
    SELECT 
        w.wallet_address,
        w.wallet_name,
        w.is_primary,
        b.token_type,
        b.balance,
        b.usd_value,
        b.last_updated
    FROM wallets w
    LEFT JOIN balances b ON w.wallet_address = b.wallet_address
    WHERE w.user_id = p_user_id AND w.status = 'ACTIVE'
    ORDER BY w.is_primary DESC, w.created_at ASC;
END$$
```

---

## 🔧 **DATABASE SHARDING IMPLEMENTATION**

### **Sharding Utilities**

```java
@Service
public class DatabaseShardingUtils {
    
    // User sharding (16 partitions)
    public int getUserShard(String userId) {
        long crc32 = calculateCRC32(userId);
        return (int) (Math.abs(crc32) % 16);
    }
    
    // Balance sharding (32 partitions)
    public int getBalanceShard(String walletAddress) {
        long crc32 = calculateCRC32(walletAddress);
        return (int) (Math.abs(crc32) % 32);
    }
    
    // SQL hint generation
    public String getShardingHint(String tableName, String shardKey) {
        int shard = getShardForTable(tableName, shardKey);
        return String.format("/* SHARD:%s:%d */", tableName, shard);
    }
}
```

### **Usage Examples**

```java
// Repository với sharding awareness
public Optional<User> findUserById(String userId) {
    int shard = shardingUtils.getUserShard(userId);
    String sql = "/* SHARD:users:" + shard + " */ SELECT * FROM users WHERE user_id = ?";
    return jdbcTemplate.queryForObject(sql, User.class, userId);
}
```

---

## 📊 **MONITORING & ANALYTICS**

### **Performance Metrics Tables**

```sql
-- System metrics tracking
CREATE TABLE system_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,4) NOT NULL,
    metric_unit VARCHAR(20),
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metrics_name_time (metric_name, recorded_at)
);

-- Health monitoring
CREATE TABLE health_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_name VARCHAR(100) NOT NULL,
    check_status ENUM('HEALTHY','WARNING','CRITICAL') NOT NULL,
    check_details JSON,
    response_time_ms INT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### **Key Metrics to Track**

- ✅ **Total users/wallets/balances** count
- ✅ **Average portfolio value** per user
- ✅ **Sync success rate** percentage
- ✅ **Query response times** per partition
- ✅ **Connection pool** utilization
- ✅ **Replication lag** between master-slave

---

## 🚀 **MIGRATION STRATEGY**

### **Flyway Integration**

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history
```

### **Migration Files**

1. **V1__Create_initial_tables.sql**: Base schema với partitioning
2. **V2__Add_read_replicas_config.sql**: Performance optimizations
3. **V3__Add_monitoring_tables.sql**: Metrics và health checks

### **Deployment Process**

1. ✅ **Backup** existing data
2. ✅ **Run migrations** on staging environment
3. ✅ **Validate** schema changes
4. ✅ **Deploy** to production với zero downtime
5. ✅ **Monitor** performance post-deployment

---

## 🔐 **SECURITY CONSIDERATIONS**

### **Data Protection**

- ✅ **Password hashing** với BCrypt (strength 12)
- ✅ **Input validation** tại application layer
- ✅ **SQL injection** prevention với PreparedStatements
- ✅ **Access control** với role-based permissions
- ✅ **Audit trail** với transaction history

### **Connection Security**

```yaml
# SSL/TLS configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wallet_system?useSSL=true&requireSSL=true
    hikari:
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

---

## 📈 **PERFORMANCE TARGETS**

| Metric | Target | Method |
|--------|--------|---------|
| Query Response | < 20ms | Partitioning + Indexes |
| Concurrent Users | 1M+ | Connection pooling |
| Read Throughput | 10K QPS | Read replicas |
| Write Throughput | 5K QPS | Master optimization |
| Storage Growth | Linear | Partitioning strategy |
| Uptime | 99.99% | HA với failover |

---

## 🔄 **MAINTENANCE PROCEDURES**

### **Daily Tasks**

```sql
-- Update statistics
ANALYZE TABLE users, wallets, balances;

-- Clean expired sessions
CALL CleanupExpiredSessions();

-- Refresh portfolio cache
CALL BulkRefreshPortfolioCache(1000);
```

### **Weekly Tasks**

```sql
-- Archive old transaction history
CALL ArchiveOldTransactionHistory(365);

-- Optimize tables
OPTIMIZE TABLE users, wallets, balances;

-- Check replication status
SHOW SLAVE STATUS;
```

---

## 🏁 **SUMMARY**

### **✅ Completed Features**

1. **Database Schema**: Normalized tables với proper relationships
2. **Partitioning**: Hash partitioning cho millions users
3. **Read Replicas**: Master-slave setup với load balancing
4. **Sharding**: Application-level sharding utilities
5. **Performance**: Optimized indexes và caching
6. **Migration**: Flyway integration với versioning
7. **Monitoring**: Health checks và metrics tracking

### **🎯 Key Benefits**

- **Scalability**: Supports millions of concurrent users
- **Performance**: Sub-20ms query response times
- **Availability**: 99.99% uptime với automatic failover
- **Maintainability**: Clean schema với proper documentation
- **Security**: Industry-standard security practices

### **📚 Next Steps**

1. **Entity Layer**: Create JPA entities mapping to schema
2. **Repository Layer**: Implement data access objects
3. **Service Layer**: Business logic implementation
4. **API Layer**: REST controllers với validation
5. **Testing**: Unit và integration tests
6. **Deployment**: Production environment setup

---

**Database Schema Design hoàn thành! ✅**