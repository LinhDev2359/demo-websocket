#!/bin/bash

echo "🧪 Testing Docker Setup for EOS Wallet System"

# Step 1: Start databases only first
echo "📊 Step 1: Starting databases..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for databases
echo "⏳ Waiting for databases to be ready..."
sleep 30

# Check MySQL
echo "🔍 Testing MySQL connection..."
if docker exec wallet-mysql-dev mysqladmin ping -h localhost -u root -proot; then
    echo "✅ MySQL is ready!"
else
    echo "❌ MySQL failed to start"
    docker logs wallet-mysql-dev
    exit 1
fi

# Check Redis
echo "🔍 Testing Redis connection..."
if docker exec wallet-redis-dev redis-cli ping; then
    echo "✅ Redis is ready!"
else
    echo "❌ Redis failed to start"
    docker logs wallet-redis-dev
    exit 1
fi

# Step 2: Build application JAR first
echo "🔨 Step 2: Building application JAR..."
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ JAR build successful!"
else
    echo "❌ JAR build failed"
    exit 1
fi

# Step 3: Build Docker image
echo "🐳 Step 3: Building Docker image..."
docker build -t eos-wallet-system .

if [ $? -eq 0 ]; then
    echo "✅ Docker image build successful!"
else
    echo "❌ Docker image build failed"
    exit 1
fi

# Step 4: Run application in Docker
echo "🚀 Step 4: Running application in Docker..."
docker run -d \
    --name wallet-app-test \
    --network demo-websocket_default \
    -p 8080:8080 \
    -e DATABASE_URL=jdbc:mysql://wallet-mysql-dev:3306/wallet_system \
    -e DATABASE_USERNAME=wallet_user \
    -e DATABASE_PASSWORD=wallet_pass \
    -e REDIS_HOST=wallet-redis-dev \
    -e ENVIRONMENT=dev \
    eos-wallet-system

# Wait for app to start
echo "⏳ Waiting for application to start..."
sleep 45

# Test health endpoint
echo "🔍 Testing application health..."
if curl -f http://localhost:8080/api/health; then
    echo ""
    echo "✅ Application is running successfully!"
    echo "🌐 Health endpoint: http://localhost:8080/api/health"
else
    echo ""
    echo "❌ Application health check failed"
    echo "📋 Application logs:"
    docker logs wallet-app-test
fi

echo ""
echo "🧹 Cleanup commands:"
echo "  docker stop wallet-app-test && docker rm wallet-app-test"
echo "  docker-compose -f docker-compose.dev.yml down"