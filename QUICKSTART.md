# 🚀 Quick Start - EOS Wallet System

## ⚡ 30-Second Setup

### Option 1: Secure Setup (Recommended)
```bash
# 1. Setup secure environment variables
./setup-env.sh

# 2. Check security configuration
./check-security.sh

# 3. Start development environment
./reset-dev.sh

# 4. Run application
./run-local.sh

# 5. Test health check
curl http://localhost:8080/api/health
```

### Option 2: If Having Database Issues
```bash
# 1. Reset development environment
./reset-dev.sh

# 2. Test database connection
./test-db.sh

# 3. Run application
./run-local.sh
```

### Option 3: Manual Steps
```bash
# 1. Start databases
./start-dev.sh

# 2. Build and run application (in another terminal)
./mvnw clean compile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Test health check  
curl http://localhost:8080/api/health
```

## 🐳 Docker Issues?

If you see "docker-compose command not found":

1. **Install Docker Desktop** on Windows
2. **Enable WSL2 Integration**: Docker Desktop → Settings → Resources → WSL Integration
3. **Restart WSL2**: In PowerShell as Admin: `wsl --shutdown` then `wsl`

## 🗄️ Database Access

```bash
# Connect to MySQL
docker exec-it wallet-mysql-dev mysql -u wallet_user -p
# Password: wallet_pass

# Test data already inserted:
# - 2 test users
# - 2 test wallets 
# - Test balances
```

## 🛠️ Development Commands  

```bash
make help         # Show all commands
make dev-up       # Start databases
make dev-down     # Stop databases  
make build        # Build application
make test         # Run tests
```

## 🔍 Troubleshooting

**Redis connection failed?**
```bash
./test-redis.sh  # Comprehensive Redis diagnosis
# Or manually:
docker ps | grep redis
docker exec <redis-container> redis-cli ping
```

**Port 3307 already in use?**
```bash
sudo lsof -i :3307
# Kill existing MySQL or change port in docker-compose.dev.yml
```

**Build errors?**
```bash
./mvnw clean compile -X  # Verbose output
```

**Docker issues in WSL2?**
```bash
# Enable WSL2 integration in Docker Desktop
# Restart WSL2: wsl --shutdown (in PowerShell as Admin)
```

---

✅ **TASK 1.1 Complete!** Ready for TASK 1.2: Security Implementation