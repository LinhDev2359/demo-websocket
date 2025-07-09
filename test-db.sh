#!/bin/bash

echo "🔍 Testing Database Connection"

# Check if MySQL container is running
if ! docker ps | grep -q wallet-mysql-dev; then
    echo "❌ MySQL container is not running"
    echo "Run: ./reset-dev.sh"
    exit 1
fi

echo "📊 MySQL Container Status:"
docker ps | grep wallet-mysql-dev

echo ""
echo "🔑 Testing root connection..."
if docker exec wallet-mysql-dev mysql -u root -proot -e "SELECT 'Root connection successful' as status;"; then
    echo "✅ Root connection works"
else
    echo "❌ Root connection failed"
    exit 1
fi

echo ""
echo "👤 Checking users:"
docker exec wallet-mysql-dev mysql -u root -proot -e "SELECT User, Host FROM mysql.user WHERE User IN ('root', 'wallet_user');"

echo ""
echo "🗄️ Checking databases:"
docker exec wallet-mysql-dev mysql -u root -proot -e "SHOW DATABASES;"

echo ""
echo "🔑 Testing wallet_user connection..."
if docker exec wallet-mysql-dev mysql -u wallet_user -pwallet_pass -e "SELECT 'wallet_user connection successful' as status;" wallet_system; then
    echo "✅ wallet_user connection works"
else
    echo "❌ wallet_user connection failed"
    echo ""
    echo "🔧 Trying to fix permissions..."
    docker exec wallet-mysql-dev mysql -u root -proot << 'EOF'
CREATE USER IF NOT EXISTS 'wallet_user'@'localhost' IDENTIFIED BY 'wallet_pass';
CREATE USER IF NOT EXISTS 'wallet_user'@'%' IDENTIFIED BY 'wallet_pass';
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_user'@'localhost';
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_user'@'%';
FLUSH PRIVILEGES;
EOF
    
    echo "✅ Permissions updated. Testing again..."
    if docker exec wallet-mysql-dev mysql -u wallet_user -pwallet_pass -e "SELECT 'wallet_user connection successful' as status;" wallet_system; then
        echo "✅ wallet_user connection now works!"
    else
        echo "❌ wallet_user connection still failed"
    fi
fi

echo ""
echo "📋 Testing from host (port 3307):"
if command -v mysql &> /dev/null; then
    mysql -h localhost -P 3307 -u wallet_user -pwallet_pass -e "SELECT 'External connection successful' as status;" wallet_system 2>/dev/null && echo "✅ External connection works" || echo "❌ External connection failed (this is OK if mysql client not installed)"
else
    echo "ℹ️ MySQL client not installed on host (this is OK)"
fi

echo ""
echo "🎯 Ready to test Spring Boot application!"
echo "Run: ./run-local.sh"