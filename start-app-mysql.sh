#!/bin/bash

echo "🚀 Starting EOS Wallet System with MySQL..."

# Kiểm tra xem MySQL có đang chạy không
echo "🔍 Checking MySQL connection..."
if ! docker-compose -f docker-compose.dev.yml exec mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
    echo "❌ MySQL is not running!"
    echo "Please start MySQL first with: ./start-mysql.sh"
    exit 1
fi

echo "✅ MySQL is running!"

# Set profile to dev
export SPRING_PROFILES_ACTIVE=dev

# Khởi động ứng dụng
echo "🌱 Starting Spring Boot application..."
echo "Profile: $SPRING_PROFILES_ACTIVE"
echo "Database: MySQL (localhost:3307/wallet_system)"
echo "Config: Using application-dev.yml"
echo ""

# Clean và khởi động với profile dev
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev