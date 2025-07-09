# 🚀 EOS Wallet System - Development Setup Guide

## 📋 Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.6+**
- **Docker Desktop** with WSL2 integration enabled
- **Git**

### ⚠️ WSL2 Docker Setup
If you see "docker-compose command not found" in WSL2:

1. **Install Docker Desktop** on Windows
2. **Enable WSL2 Integration**:
   - Open Docker Desktop
   - Go to Settings → Resources → WSL Integration
   - Enable integration with your WSL2 distro
   - Click "Apply & Restart"

3. **Restart WSL2**:
   ```bash
   # In Windows PowerShell (as Administrator)
   wsl --shutdown
   wsl
   ```

## 🛠️ Quick Start

### 1. Clone Repository
```bash
git clone <repository-url>
cd demo-websocket
```

### 2. Start Development Environment
```bash
# Start MySQL and Redis services
./start-dev.sh

# Or manually:
docker-compose -f docker-compose.dev.yml up -d
```

### 3. Build and Run Application
```bash
# Build project
mvn clean compile

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Verify Setup
- **Application**: http://localhost:8080/api/actuator/health
- **MySQL**: localhost:3306 (wallet_user/wallet_pass)
- **Redis**: localhost:6379

## 🐳 Docker Setup

### Development (Local)
```bash
# Start services only
docker-compose -f docker-compose.dev.yml up -d

# Stop services
./stop-dev.sh
```

### Production (Full Stack)
```bash
# Build and start all services
docker-compose up -d

# Stop all services
docker-compose down
```

## 🗄️ Database

### Database Schema
- **Database**: `wallet_system`
- **Tables**: users, wallets, balances, sync_status, transaction_history, user_sessions
- **Partitioning**: Hash partitioning for scalability

### Database Access
```bash
# Connect to MySQL
docker exec -it wallet-mysql-dev mysql -u wallet_user -p wallet_system

# Connect to Redis
docker exec -it wallet-redis-dev redis-cli
```

## 🔧 Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:mysql://localhost:3306/wallet_system
DATABASE_USERNAME=wallet_user
DATABASE_PASSWORD=wallet_pass

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-secret-key

# EOS API
EOS_API_URL=https://eos.greymass.com
```

### Application Profiles
- **dev**: Development with debug logging
- **prod**: Production optimized
- **test**: Testing with H2 database

## 📊 Monitoring

### Development
- **Health Check**: http://localhost:8080/api/actuator/health
- **Metrics**: http://localhost:8080/api/actuator/metrics

### Production
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=WalletServiceTest

# Integration tests with TestContainers
mvn test -Dtest=*IntegrationTest
```

## 📦 Build & Deploy

### Local Build
```bash
# Build JAR
mvn clean package

# Build Docker image
docker build -t eos-wallet-system .
```

### Production Deploy
```bash
# Build and deploy
docker-compose up -d --build

# Scale application
docker-compose up -d --scale app=3
```

## 🔍 Troubleshooting

### Common Issues

**1. Port conflicts**
```bash
# Check port usage
netstat -tulpn | grep :8080
netstat -tulpn | grep :3306
netstat -tulpn | grep :6379
```

**2. Database connection**
```bash
# Test MySQL connection
docker exec wallet-mysql-dev mysqladmin ping -h localhost -u root -proot

# Check MySQL logs
docker logs wallet-mysql-dev
```

**3. Memory issues**
```bash
# Increase Docker memory limit (minimum 4GB recommended)
# Docker Desktop > Settings > Resources > Advanced
```

### Logs
```bash
# Application logs
docker logs eos-wallet-system-app-1

# Database logs
docker logs wallet-mysql-dev

# Redis logs
docker logs wallet-redis-dev
```

## 📚 Development Notes

### Database Design
- **Sharding Strategy**: Hash partitioning by user_id and wallet_address
- **Read Replicas**: Master-slave setup for read scaling
- **Connection Pooling**: HikariCP with optimized settings

### Performance Optimizations
- **JPA Batch Processing**: Batch size 50 for bulk operations
- **Redis Caching**: 5-minute TTL for portfolio data
- **Database Indexes**: Optimized for millions of users

### Security Features
- **JWT Authentication**: Stateless authentication
- **Rate Limiting**: 1000 requests/minute per user
- **Input Validation**: Comprehensive validation on all endpoints

## 📞 Support

For issues or questions:
1. Check this documentation
2. Review application logs
3. Check Docker container status
4. Consult project README.md

---

**Happy Coding! 🎉**