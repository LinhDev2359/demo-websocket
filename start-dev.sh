#!/bin/bash

# EOS Wallet System Development Startup Script

echo "🚀 Starting EOS Wallet System Development Environment..."

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not available in WSL2"
    echo "Please install Docker Desktop and enable WSL2 integration"
    exit 1
fi

# Start MySQL and Redis
echo "📊 Starting MySQL and Redis..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for databases to be ready
echo "⏳ Waiting for databases to start..."
sleep 30

# Check database connectivity
echo "🔍 Checking database connectivity..."
if docker exec wallet-mysql-dev mysqladmin ping -h localhost -u root -proot; then
    echo "✅ MySQL is ready!"
else
    echo "❌ MySQL is not ready. Check the logs:"
    echo "   docker logs wallet-mysql-dev"
    exit 1
fi

if docker exec wallet-redis-dev redis-cli ping; then
    echo "✅ Redis is ready!"
else
    echo "❌ Redis is not ready. Check the logs:"
    echo "   docker logs wallet-redis-dev"
    exit 1
fi

echo ""
echo "🌐 Services running:"
echo "   MySQL: localhost:3306"
echo "   Redis: localhost:6379"
echo ""
echo "📝 Database credentials:"
echo "   Database: wallet_system"
echo "   Username: wallet_user"
echo "   Password: wallet_pass"
echo ""
echo "🛠️ Available commands:"
echo "   make build     - Build the application"
echo "   make test      - Run tests"
echo "   make dev-down  - Stop services"
echo ""
echo "🚀 To run the application:"
echo "   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"