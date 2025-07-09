#!/bin/bash

echo "🚀 Running EOS Wallet System Locally"

# Check if databases are running
echo "🔍 Checking database connectivity..."

# Start databases if not running
if ! docker ps | grep -q wallet-mysql-dev; then
    echo "📊 Starting databases..."
    docker-compose -f docker-compose.dev.yml up -d
    echo "⏳ Waiting for databases..."
    sleep 30
fi

# Test MySQL connection
if docker exec wallet-mysql-dev mysqladmin ping -h localhost -u root -proot > /dev/null 2>&1; then
    echo "✅ MySQL is ready"
else
    echo "❌ MySQL is not ready. Starting databases..."
    docker-compose -f docker-compose.dev.yml restart
    sleep 30
fi

# Test Redis connection  
REDIS_CONTAINER=$(docker ps --format "{{.Names}}" | grep redis | head -1)
if [ ! -z "$REDIS_CONTAINER" ] && docker exec $REDIS_CONTAINER redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
    echo "Run: ./test-redis.sh to diagnose Redis issues"
fi

echo ""
echo "🌐 Database connections:"
echo "   MySQL: localhost:3307 (wallet_user/wallet_pass)"
echo "   Redis: localhost:6379"
echo ""
echo "🔨 Building application..."
./mvnw clean compile

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🚀 Starting application..."
    echo "   Profile: dev"
    echo "   URL: http://localhost:8080/api/health"
    echo ""
    
    # Run the application
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
else
    echo "❌ Build failed!"
    exit 1
fi