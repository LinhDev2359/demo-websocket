#!/bin/bash

echo "🚀 Starting Complete EOS Wallet System Setup..."
echo ""

# Bước 1: Khởi động MySQL
echo "📊 Step 1: Starting MySQL..."
./start-mysql.sh

if [ $? -ne 0 ]; then
    echo "❌ Failed to start MySQL"
    exit 1
fi

echo ""
echo "⏳ Waiting 5 seconds for database to be fully ready..."
sleep 5

# Bước 2: Test kết nối
echo "🔍 Step 2: Testing database connection..."
./test-mysql-connection.sh

if [ $? -ne 0 ]; then
    echo "❌ Database connection failed"
    echo "Attempting to fix user authentication..."
    ./fix-mysql-user.sh
fi

echo ""
echo "⏳ Waiting 3 seconds before starting application..."
sleep 3

# Bước 3: Khởi động ứng dụng
echo "🌱 Step 3: Starting Spring Boot Application..."
echo "============================================="
./start-app-mysql.sh