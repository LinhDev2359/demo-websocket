#!/bin/bash

# EOS Wallet System Development Stop Script

echo "🛑 Stopping EOS Wallet System Development Environment..."

# Stop all services
docker-compose -f docker-compose.dev.yml down

echo "✅ All services stopped successfully!"
echo ""
echo "💾 Data volumes preserved:"
echo "   - mysql_dev_data"
echo "   - redis_dev_data"
echo ""
echo "🗑️ To remove all data volumes:"
echo "   docker-compose -f docker-compose.dev.yml down -v"