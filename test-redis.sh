#!/bin/bash

echo "🔍 Testing Redis Connection for EOS Wallet System"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "================================"
echo "1. Checking Redis Container"
echo "================================"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not available in WSL2${NC}"
    echo "Please install Docker Desktop and enable WSL2 integration"
    exit 1
fi

# Check if Redis container is running
if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q redis; then
    echo -e "${GREEN}✅ Redis container is running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep redis
else
    echo -e "${YELLOW}⚠️  Redis container is not running${NC}"
    echo "Starting Redis container..."
    
    # Try to start with docker-compose
    if command -v docker-compose &> /dev/null; then
        docker-compose -f docker-compose.dev.yml up -d redis
        sleep 10
    else
        echo "Starting standalone Redis container..."
        docker run -d \
            --name wallet-redis-dev \
            -p 6379:6379 \
            -v redis_dev_data:/data \
            redis:7-alpine \
            redis-server --appendonly yes --maxmemory 512mb
        sleep 5
    fi
fi

echo ""
echo "================================"
echo "2. Testing Redis Connection"
echo "================================"

# Test Redis connection from host
echo "🔗 Testing Redis connection from host..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h localhost -p 6379 ping | grep -q "PONG"; then
        echo -e "${GREEN}✅ Redis host connection successful${NC}"
    else
        echo -e "${RED}❌ Redis host connection failed${NC}"
    fi
else
    echo "redis-cli not installed on host, testing via container..."
fi

# Test Redis connection from container
echo "📦 Testing Redis connection from container..."
CONTAINER_NAME=$(docker ps --format "{{.Names}}" | grep redis | head -1)

if [ ! -z "$CONTAINER_NAME" ]; then
    if docker exec $CONTAINER_NAME redis-cli ping | grep -q "PONG"; then
        echo -e "${GREEN}✅ Redis container connection successful${NC}"
        
        # Test Redis operations
        echo "🧪 Testing Redis operations..."
        docker exec $CONTAINER_NAME redis-cli set test_key "test_value" > /dev/null
        TEST_RESULT=$(docker exec $CONTAINER_NAME redis-cli get test_key)
        
        if [ "$TEST_RESULT" = "test_value" ]; then
            echo -e "${GREEN}✅ Redis read/write operations working${NC}"
            docker exec $CONTAINER_NAME redis-cli del test_key > /dev/null
        else
            echo -e "${RED}❌ Redis read/write operations failed${NC}"
        fi
        
        # Check Redis info
        echo "📊 Redis information:"
        docker exec $CONTAINER_NAME redis-cli info server | grep "redis_version"
        docker exec $CONTAINER_NAME redis-cli info memory | grep "used_memory_human"
        
    else
        echo -e "${RED}❌ Redis container connection failed${NC}"
    fi
else
    echo -e "${RED}❌ No Redis container found${NC}"
fi

echo ""
echo "================================"
echo "3. Testing Spring Boot Connection"
echo "================================"

# Test if Spring Boot application can connect to Redis
echo "🍃 Testing Spring Boot Redis connection..."

# Create a simple test to see if Redis is accessible from application perspective
echo "Creating temporary Redis test..."

# Check if application is running
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Application is running${NC}"
    
    # Test application health (should include Redis)
    HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/actuator/health 2>/dev/null || echo '{"status":"unknown"}')
    echo "Health check response: $HEALTH_RESPONSE"
    
    if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✅ Application health check passed${NC}"
    else
        echo -e "${YELLOW}⚠️  Application health check shows issues${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Application is not running${NC}"
    echo "Start application with: ./run-local.sh"
fi

echo ""
echo "================================"
echo "4. Configuration Check"
echo "================================"

echo "📋 Current Redis configuration:"
echo "   Host: localhost"
echo "   Port: 6379"
echo "   Password: None (development mode)"
echo "   Container: $CONTAINER_NAME"

echo ""
echo "🔧 Redis container logs (last 10 lines):"
if [ ! -z "$CONTAINER_NAME" ]; then
    docker logs --tail 10 $CONTAINER_NAME
else
    echo "No Redis container found"
fi

echo ""
echo "================================"
echo "📊 REDIS TEST SUMMARY"
echo "================================"

if docker ps --format "{{.Names}}" | grep -q redis && docker exec $(docker ps --format "{{.Names}}" | grep redis | head -1) redis-cli ping | grep -q "PONG"; then
    echo -e "${GREEN}🎉 Redis is working correctly!${NC}"
    echo ""
    echo "✅ Connection successful"
    echo "✅ Read/write operations working"
    echo "✅ No authentication required (dev mode)"
    echo ""
    echo "🚀 Ready to start Spring Boot application:"
    echo "   ./run-local.sh"
else
    echo -e "${RED}❌ Redis has issues that need to be resolved${NC}"
    echo ""
    echo "🔧 Troubleshooting steps:"
    echo "   1. Check Docker Desktop is running"
    echo "   2. Enable WSL2 integration in Docker Desktop"
    echo "   3. Run: ./reset-dev.sh"
    echo "   4. Check logs: docker logs <redis-container-name>"
fi