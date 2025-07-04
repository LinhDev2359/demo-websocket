# 🚀 WALLET SYSTEM SPRINT - EOS BLOCKCHAIN PORTFOLIO MANAGEMENT

## 📋 TỔNG QUAN DỰ ÁN

**Mục tiêu**: Xây dựng hệ thống quản lý portfolio ví blockchain EOS với WebSocket realtime và đồng bộ balance tự động.

**Công nghệ**: Java Spring Boot, MySQL, WebSocket (Socket.IO), Redis

**Quy mô**: Hỗ trợ hàng triệu user, mỗi user không giới hạn số lượng ví

---

## 🏃‍♂️ SPRINT PLAN - 2 TUẦN

### 📅 **TUẦN 1: FOUNDATION & CORE FEATURES**

#### **DAY 1-2: PROJECT SETUP & SECURITY**

##### ✅ **TASK 1.1: Project Initialization** (4h) - **COMPLETED ✅**
```bash
# Checklist
✅ Tạo Spring Boot project với dependencies cần thiết
✅ Cấu hình MySQL connection với HikariCP pool
✅ Setup Redis connection cho caching và session
✅ Cấu hình application.yml cho multiple environments
✅ Setup Dockerfile và docker-compose.yml với MySQL
✅ Cấu hình database sharding strategy cho millions users
```

**Dependencies cần thêm:**
```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

##### ✅ **TASK 1.2: Security Implementation** (6h) - **COMPLETED ✅**
```bash
# Security Tasks
✅ Implement JWT authentication service
✅ Create JwtTokenUtil class
✅ Setup Security Configuration với rate limiting
✅ Implement password encryption với BCrypt
✅ Create custom authentication entry point
✅ Setup CORS configuration
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/security/JwtTokenUtil.java`
- ✅ `src/main/java/com/wallet/security/SecurityConfig.java`
- ✅ `src/main/java/com/wallet/security/JwtAuthenticationEntryPoint.java`
- ✅ `src/main/java/com/wallet/security/JwtRequestFilter.java`
- ✅ `src/main/java/com/wallet/service/CustomUserDetailsService.java`
- ✅ `src/main/java/com/wallet/service/AuthenticationService.java`
- ✅ `src/main/java/com/wallet/controller/AuthController.java`
- ✅ `src/main/java/com/wallet/dto/LoginRequest.java`
- ✅ `src/main/java/com/wallet/dto/RefreshTokenRequest.java`

#### **DAY 3-4: DATABASE DESIGN & ENTITIES**

##### ✅ **TASK 2.1: Database Schema Design** (4h) - **COMPLETED ✅**
```bash
# Database Tasks
✅ Thiết kế MySQL tables với proper normalization
✅ Tạo database indexes cho performance (millions records)
✅ Setup database partitioning strategy cho users table
✅ Create database migration scripts với Flyway
✅ Design read replicas configuration
✅ Setup database sharding cho horizontal scaling
```

**Files đã tạo:**
- ✅ `src/main/resources/db/migration/V1__Create_initial_tables.sql`
- ✅ `src/main/resources/db/migration/V2__Add_read_replicas_config.sql`
- ✅ `src/main/java/com/wallet/config/DatabaseShardingConfig.java`
- ✅ `src/main/java/com/wallet/config/ReadReplicaConfig.java`
- ✅ Updated `application.yml` với Flyway configuration

##### ✅ **TASK 2.2: Entity Classes** (4h) - **COMPLETED ✅**
```bash
# Entity Development
✅ Create User entity với JPA annotations
✅ Create Wallet entity với proper relationships
✅ Create Balance entity với optimistic locking
✅ Implement BaseEntity với audit fields
✅ Setup entity relationships với proper fetch strategies
✅ Add database constraints cho data integrity
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/entity/User.java`
- ✅ `src/main/java/com/wallet/entity/Wallet.java`
- ✅ `src/main/java/com/wallet/entity/Balance.java`
- ✅ `src/main/java/com/wallet/entity/BaseEntity.java`
- ✅ `src/main/java/com/wallet/entity/TokenType.java`

## 🗄️ **MYSQL DATABASE DESIGN CHO HÀNG TRIỆU USERS**

### **Database Schema**

```sql
-- Users Table (Partitioned by user_id hash)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at)
) PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;

-- Wallets Table (Partitioned by user_id hash)
CREATE TABLE wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    wallet_address VARCHAR(255) UNIQUE NOT NULL,
    wallet_type ENUM('EOS') DEFAULT 'EOS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_created_at (created_at),
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) PARTITION BY HASH(CRC32(user_id)) PARTITIONS 16;

-- Balances Table (Partitioned by wallet_address hash)
CREATE TABLE balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    token_type ENUM('A', 'ram', 'rams', 'wram') NOT NULL,
    balance DECIMAL(20,8) NOT NULL DEFAULT 0.00000000,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0, -- For optimistic locking
    
    UNIQUE KEY uk_wallet_token (wallet_address, token_type),
    INDEX idx_wallet_address (wallet_address),
    INDEX idx_token_type (token_type),
    INDEX idx_last_updated (last_updated),
    
    FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) ON DELETE CASCADE
) PARTITION BY HASH(CRC32(wallet_address)) PARTITIONS 32;

-- Sync Status Table (for tracking balance sync)
CREATE TABLE sync_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(255) NOT NULL,
    last_sync_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sync_status ENUM('SUCCESS', 'FAILED', 'IN_PROGRESS') DEFAULT 'SUCCESS',
    error_message TEXT,
    retry_count INT DEFAULT 0,
    
    UNIQUE KEY uk_wallet_address (wallet_address),
    INDEX idx_last_sync_at (last_sync_at),
    INDEX idx_sync_status (sync_status)
);
```

### **Sharding Strategy**
- **Users & Wallets**: Partition theo user_id hash (16 partitions)
- **Balances**: Partition theo wallet_address hash (32 partitions)  
- **Read Replicas**: Master-Slave replication cho read scaling
- **Connection Pooling**: HikariCP với 50 connections per instance

##### ✅ **TASK 2.3: Repository Layer** (3h) - **COMPLETED ✅**
```bash
# Repository Tasks
✅ Create UserRepository với JPA custom queries
✅ Create WalletRepository với pagination support
✅ Create BalanceRepository với bulk operations
✅ Implement custom repository methods với native queries
✅ Add database indexes optimization
✅ Setup read/write repository separation
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/repository/UserRepository.java`
- ✅ `src/main/java/com/wallet/repository/WalletRepository.java`
- ✅ `src/main/java/com/wallet/repository/BalanceRepository.java`
- ✅ `src/main/java/com/wallet/repository/base/ReadOnlyRepository.java`
- ✅ `src/main/java/com/wallet/repository/base/WriteOnlyRepository.java`
- ✅ `src/main/java/com/wallet/repository/read/UserReadRepository.java`
- ✅ `src/main/java/com/wallet/repository/read/WalletReadRepository.java`
- ✅ `src/main/java/com/wallet/repository/read/BalanceReadRepository.java`
- ✅ `src/main/java/com/wallet/repository/write/UserWriteRepository.java`
- ✅ `src/main/java/com/wallet/repository/write/WalletWriteRepository.java`
- ✅ `src/main/java/com/wallet/repository/write/BalanceWriteRepository.java`
- ✅ `src/main/java/com/wallet/config/RepositoryConfig.java`
- ✅ Updated `application.yml` với read/write separation configuration

#### **DAY 5: REST API - WALLET MANAGEMENT**

##### ✅ **TASK 3.1: Wallet Service Layer** (4h) - **COMPLETED ✅**
```bash
# Service Layer
✅ Implement WalletService với business logic
✅ Add validation cho EOS wallet address format
✅ Implement pagination cho unlimited wallets per user
✅ Create wallet existence checking với caching
✅ Add error handling và logging
✅ Implement database sharding logic
```

##### ✅ **TASK 3.2: Wallet REST Controller** (4h) - **COMPLETED ✅**
```bash
# Controller Tasks
✅ Create WalletController với CRUD endpoints
✅ Implement POST /api/wallets endpoint
✅ Add request/response DTOs
✅ Implement input validation với @Valid
✅ Add API documentation với Swagger
✅ Create integration tests
```

**Files đã tạo:**
- ✅ `src/main/java/com/eoswallet/controller/WalletController.java`
- ✅ `src/main/java/com/eoswallet/dto/WalletCreateRequest.java`
- ✅ `src/main/java/com/eoswallet/dto/WalletCreateResponse.java`
- ✅ `src/main/java/com/eoswallet/dto/WalletResponse.java`
- ✅ `src/main/java/com/eoswallet/dto/WalletUpdateRequest.java`
- ✅ `src/test/java/com/eoswallet/controller/WalletControllerTest.java`

---

### 📅 **TUẦN 2: WEBSOCKET & SYNC FEATURES**

#### **DAY 6-7: WEBSOCKET IMPLEMENTATION**

##### ✅ **TASK 4.1: WebSocket Configuration** (3h) - **COMPLETED ✅**
```bash
# WebSocket Setup
✅ Configure Spring WebSocket với STOMP
✅ Setup message broker configuration
✅ Implement JWT authentication cho WebSocket
✅ Configure CORS cho WebSocket connections
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/config/WebSocketConfig.java`
- ✅ `src/main/java/com/wallet/websocket/WebSocketJwtAuthInterceptor.java`
- ✅ `src/main/java/com/wallet/websocket/WebSocketUserPrincipal.java`
- ✅ `src/main/java/com/wallet/config/WebSocketSecurityConfig.java`
- ✅ `src/main/java/com/wallet/websocket/WebSocketEventListener.java`
- ✅ `src/main/java/com/wallet/websocket/WebSocketTestController.java`
- ✅ `WEBSOCKET_CLIENT_GUIDE.md`

##### ✅ **TASK 4.2: Portfolio WebSocket Service** (5h) - **COMPLETED ✅**
```bash
# WebSocket Service
✅ Create PortfolioWebSocketController
✅ Implement real-time portfolio data streaming
✅ Add JWT token validation cho WebSocket messages
✅ Create portfolio response DTOs
✅ Implement user-specific message routing
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/dto/PortfolioResponse.java`
- ✅ `src/main/java/com/wallet/dto/PortfolioUpdateRequest.java`
- ✅ `src/main/java/com/wallet/dto/WebSocketErrorResponse.java`
- ✅ `src/main/java/com/wallet/controller/PortfolioWebSocketController.java`
- ✅ `src/main/java/com/wallet/service/PortfolioService.java`
- ✅ `src/main/java/com/wallet/service/impl/PortfolioServiceImpl.java`
- ✅ `src/main/java/com/wallet/websocket/PortfolioWebSocketEventListener.java`
- ✅ `PORTFOLIO_WEBSOCKET_CLIENT_GUIDE.md`

##### ✅ **TASK 4.3: Portfolio Business Logic** (4h) - **COMPLETED ✅**
```bash
# Portfolio Logic
✅ Implement PortfolioService với getUserPortfolio
✅ Aggregate wallet data với balance information
✅ Add caching layer với Redis
✅ Implement cache invalidation strategy
✅ Add performance optimization
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/service/impl/PortfolioServiceV2Impl.java`
- ✅ `src/test/java/com/wallet/service/PortfolioServiceTest.java`
- ✅ `src/test/java/com/wallet/integration/PortfolioWebSocketIntegrationTest.java`
- ✅ Junction Tables implementation with Redis caching (5-minute TTL)
- ✅ Performance optimization with batch queries and N+1 elimination

#### **DAY 8-9: EOS BLOCKCHAIN INTEGRATION**

##### ✅ **TASK 5.1: EOS Client Service** (4h) - **COMPLETED ✅**
```bash
# EOS Integration
✅ Create EOSClientService với RestTemplate
✅ Implement getWalletBalance method
✅ Add timeout và retry mechanism
✅ Create EOS API request/response DTOs
✅ Implement error handling cho API failures
```

##### ✅ **TASK 5.2: Circuit Breaker Pattern** (3h) - **COMPLETED ✅**
```bash
# Resilience Pattern
✅ Implement Circuit Breaker cho EOS API calls
✅ Add Retry pattern với exponential backoff
✅ Create fallback mechanisms
✅ Add monitoring và alerting
```

##### ✅ **TASK 5.3: Balance Sync Scheduler** (5h) - **COMPLETED ✅**
```bash
# Sync Service
✅ Create BalanceSyncService với @Scheduled
✅ Implement syncAllBalances method (mỗi 5 phút)
✅ Add individual wallet sync logic
✅ Implement database update logic
✅ Add sync status tracking và error logging
```

**Files đã tạo:**
- ✅ `src/main/java/com/wallet/service/EOSClientService.java`
- ✅ `src/main/java/com/wallet/service/EOSCircuitBreakerService.java`
- ✅ `src/main/java/com/wallet/service/BalanceSyncService.java`
- ✅ `src/main/java/com/wallet/dto/EOSBalanceRequest.java`
- ✅ `src/main/java/com/wallet/dto/EOSBalanceResponse.java`
- ✅ `src/main/java/com/wallet/dto/BalanceUpdateMessage.java`
- ✅ `src/main/java/com/wallet/dto/SyncStatistics.java`
- ✅ `src/main/java/com/wallet/config/EOSConfig.java`
- ✅ `src/main/java/com/wallet/config/Resilience4jConfig.java`
- ✅ `src/main/java/com/wallet/config/SchedulingConfig.java`
- ✅ `src/main/java/com/wallet/controller/BalanceSyncController.java`

#### **DAY 10: TESTING & OPTIMIZATION**

##### ✅ **TASK 6.1: Unit Tests** (4h) - **COMPLETED ✅**
```bash
# Testing Tasks
✅ Write unit tests cho WalletService
✅ Write unit tests cho PortfolioService
✅ Write unit tests cho EOSClientService
✅ Write unit tests cho BalanceSyncService
✅ Create mock tests cho external API calls
✅ Add test coverage analysis
```

##### ✅ **TASK 6.2: Integration Tests** (4h) - **COMPLETED ✅**
```bash
# Integration Testing
✅ Create WebSocket integration tests
✅ Test REST API endpoints với MockMvc
✅ Test database operations với TestContainers
✅ Test authentication flows
✅ Performance testing với concurrent users
```

**Files đã tạo:**
- ✅ `src/test/java/com/wallet/service/WalletServiceTest.java`
- ✅ `src/test/java/com/wallet/service/EOSClientServiceTest.java`
- ✅ `src/test/java/com/wallet/service/BalanceSyncServiceTest.java`
- ✅ `src/test/java/com/wallet/integration/WebSocketIntegrationTest.java`
- ✅ `src/test/java/com/wallet/integration/WalletControllerIntegrationTest.java`
- ✅ `src/test/java/com/wallet/integration/DatabaseIntegrationTest.java`

---

## 🔧 **CONFIGURATION FILES**

### **application.yml**
```yaml
spring:
  profiles:
    active: ${ENVIRONMENT:dev}
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/wallet_system?useSSL=false&serverTimezone=UTC}
    username: ${DATABASE_USERNAME:wallet_user}
    password: ${DATABASE_PASSWORD:wallet_pass}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 900000
      leak-detection-threshold: 60000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
  
server:
  port: 8080

# EOS Configuration
eos:
  api:
    url: ${EOS_API_URL:https://eos-mainnet.example.com}
    timeout: 30000

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400

# Rate Limiting (per million users)
rate-limit:
  requests-per-minute: 1000

# Caching Configuration
cache:
  portfolio:
    ttl: 300 # 5 minutes
  balance:
    ttl: 60 # 1 minute

logging:
  level:
    com.wallet: INFO
    org.hibernate.SQL: WARN
```

### **docker-compose.yml**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:mysql://mysql-master:3306/wallet_system
      - DATABASE_USERNAME=wallet_user
      - DATABASE_PASSWORD=wallet_pass
      - REDIS_HOST=redis
    depends_on:
      - mysql-master
      - mysql-slave
      - redis

  mysql-master:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=wallet_system
      - MYSQL_USER=wallet_user
      - MYSQL_PASSWORD=wallet_pass
    volumes:
      - mysql_master_data:/var/lib/mysql
      - ./mysql/master.cnf:/etc/mysql/conf.d/master.cnf
    command: --server-id=1 --log-bin=mysql-bin --binlog-format=ROW

  mysql-slave:
    image: mysql:8.0
    ports:
      - "3307:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=wallet_system
      - MYSQL_USER=wallet_user
      - MYSQL_PASSWORD=wallet_pass
    volumes:
      - mysql_slave_data:/var/lib/mysql
      - ./mysql/slave.cnf:/etc/mysql/conf.d/slave.cnf
    command: --server-id=2 --relay-log=mysql-relay-bin
    depends_on:
      - mysql-master

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 2gb --maxmemory-policy allkeys-lru

volumes:
  mysql_master_data:
  mysql_slave_data:
  redis_data:
```

---

## 🧪 **TESTING STRATEGY**

### **Unit Testing Checklist**
```bash
□ Service layer logic testing
□ Controller input validation testing
□ Repository query testing
□ JWT token utility testing
□ EOS client service mocking
□ Balance calculation testing
```

### **Integration Testing Checklist**
```bash
□ End-to-end API flow testing
□ WebSocket connection testing với load balancing
□ Database transaction testing với master-slave setup
□ Authentication flow testing với millions users simulation
□ Error handling testing
□ Performance testing với 1,000,000 concurrent users
□ Database sharding testing
□ Cache performance testing
□ Master-slave replication testing
```

---

## 📊 **MONITORING & METRICS**

### **Application Metrics**
```bash
□ Wallet creation rate (per second)
□ Portfolio request count (millions per day)
□ Balance sync duration (distributed)
□ WebSocket connection count (concurrent millions)
□ API response times (P95, P99)
□ Error rates by endpoint
□ Database query performance
□ Cache hit/miss ratios
□ Master-slave replication lag
□ Connection pool utilization
```

### **Infrastructure Metrics**
```bash
□ Database connection pool usage (per instance)
□ Redis cache hit/miss ratio (>95% target)
□ Memory và CPU usage (per node)
□ Network latency (cross-region)
□ EOS API response times
□ Load balancer distribution
□ Database partition performance
□ Disk I/O usage
□ Auto-scaling triggers
```

---

## 🚀 **DEPLOYMENT CHECKLIST**

### **Pre-deployment**
```bash
□ Code review hoàn thành
□ All tests passed
□ Security scan passed
□ Performance testing completed
□ Documentation updated
```

### **Deployment Steps**
```bash
□ Build Docker image
□ Deploy to staging environment
□ Run smoke tests
□ Deploy to production
□ Monitor system health
□ Verify all features working
```

---

## 🔍 **TROUBLESHOOTING GUIDE**

### **Common Issues**
1. **WebSocket Connection Failed**
   - Check CORS configuration
   - Verify JWT token validity
   - Check network connectivity

2. **EOS API Timeout**
   - Verify EOS node status
   - Check circuit breaker status
   - Review retry configuration

3. **Database Connection Issues**
   - Check MongoDB service status
   - Verify connection string
   - Review connection pool settings

4. **High Memory Usage**
   - Monitor cache size
   - Check for memory leaks
   - Review batch processing

---

## 📈 **PERFORMANCE TARGETS**

- **API Response Time**: < 100ms (95th percentile)
- **WebSocket Latency**: < 50ms
- **Database Query Time**: < 20ms
- **Concurrent Users**: 1,000,000+
- **Balance Sync Time**: < 5 minutes cho tất cả ví (distributed)
- **Uptime**: 99.99%
- **Cache Hit Ratio**: > 95%
- **Database Connection Pool**: 50 connections per instance

---

## 📚 **LEARNING RESOURCES**

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Redis Caching Strategies](https://redis.io/docs/manual/patterns/)
- [Database Sharding Patterns](https://docs.microsoft.com/en-us/azure/architecture/patterns/sharding)

---

**🎯 Sprint Goal**: Hoàn thành 100% core features với quality code, comprehensive tests, và production-ready deployment.

**👥 Team**: 1 Developer
**⏱️ Duration**: 10 working days
**🔄 Daily Standup**: 9:00 AM
**📋 Sprint Review**: End of Day 10