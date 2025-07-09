#!/bin/bash

echo "🔄 Resetting Development Environment"

# Stop and remove containers and volumes
echo "🛑 Stopping and removing containers..."
docker-compose -f docker-compose.dev.yml down -v

# Remove any existing containers
echo "🧹 Cleaning up existing containers..."
docker rm -f wallet-mysql-dev wallet-redis-dev 2>/dev/null || true

# Remove volumes to start fresh
echo "🗑️ Removing volumes..."
docker volume rm demo-websocket_mysql_dev_data demo-websocket_redis_dev_data 2>/dev/null || true

# Start fresh containers
echo "🚀 Starting fresh development environment..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for containers to start
echo "⏳ Waiting for containers to initialize..."
sleep 45

# Check MySQL status
echo "🔍 Checking MySQL status..."
if docker exec wallet-mysql-dev mysqladmin ping -h localhost -u root -proot; then
    echo "✅ MySQL is running!"
    
    # Test wallet_user connection
    echo "🔑 Testing wallet_user connection..."
    if docker exec wallet-mysql-dev mysql -u wallet_user -pwallet_pass -e "SELECT 1;" wallet_system; then
        echo "✅ wallet_user can connect successfully!"
    else
        echo "❌ wallet_user connection failed"
        echo "📋 Checking user privileges..."
        docker exec wallet-mysql-dev mysql -u root -proot -e "SELECT User, Host FROM mysql.user WHERE User='wallet_user';"
    fi
else
    echo "❌ MySQL failed to start"
    echo "📋 Container logs:"
    docker logs wallet-mysql-dev
fi

# Check Redis status
echo "🔍 Checking Redis status..."
if docker exec wallet-redis-dev redis-cli ping; then
    echo "✅ Redis is running!"
else
    echo "❌ Redis failed to start"
    docker logs wallet-redis-dev
fi

echo ""
echo "🌐 Services:"
echo "   MySQL: localhost:3307 (wallet_user/wallet_pass)"
echo "   Redis: localhost:6379"
echo ""
echo "🚀 Ready to run application:"
echo "   ./run-local.sh"