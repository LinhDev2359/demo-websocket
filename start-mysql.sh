#!/bin/bash

echo "🚀 Starting MySQL for EOS Wallet System..."

# Kiểm tra xem có file docker-compose không
if [ ! -f "docker-compose.dev.yml" ]; then
    echo "❌ docker-compose.dev.yml not found!"
    exit 1
fi

# Kiểm tra xem Docker có chạy không
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not running"
    echo "Please install Docker Desktop and make sure it's running"
    exit 1
fi

# Dừng các container cũ nếu có
echo "🧹 Cleaning up old containers..."
docker-compose -f docker-compose.dev.yml down

# Khởi động MySQL
echo "📊 Starting MySQL database..."
docker-compose -f docker-compose.dev.yml up -d mysql

# Chờ MySQL khởi động
echo "⏳ Waiting for MySQL to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if docker-compose -f docker-compose.dev.yml exec mysql mysqladmin ping -h localhost --silent; then
        echo "✅ MySQL is ready!"
        break
    fi
    
    echo "Attempt $attempt/$max_attempts: MySQL not ready yet..."
    sleep 2
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ MySQL failed to start after $max_attempts attempts"
    echo "Check logs with: docker-compose -f docker-compose.dev.yml logs mysql"
    exit 1
fi

# Tự động fix user authentication
echo "🔧 Setting up database user..."
sleep 2

# Tạo user nếu chưa có
docker-compose -f docker-compose.dev.yml exec mysql mysql -u root -pdev_root_pass_2024 -e "
CREATE USER IF NOT EXISTS 'wallet_dev_user'@'%' IDENTIFIED BY 'dev_wallet_pass_2024';
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_dev_user'@'%';
FLUSH PRIVILEGES;
" > /dev/null 2>&1

# Test connection
echo "🔍 Testing database connection..."
if docker-compose -f docker-compose.dev.yml exec mysql mysql -u wallet_dev_user -pdev_wallet_pass_2024 -D wallet_system -e "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database connection test successful!"
else
    echo "⚠️  Database connection test failed. Running user fix..."
    ./fix-mysql-user.sh
fi

# Hiển thị thông tin kết nối
echo ""
echo "🎉 MySQL is running successfully!"
echo ""
echo "📝 Connection details:"
echo "   Host: localhost"
echo "   Port: 3307"
echo "   Database: wallet_system"
echo "   Username: wallet_dev_user"
echo "   Password: dev_wallet_pass_2024"
echo ""
echo "🔧 Useful commands:"
echo "   Test connection: ./test-mysql-connection.sh"
echo "   Fix user issues: ./fix-mysql-user.sh"
echo "   Check status: docker-compose -f docker-compose.dev.yml ps"
echo "   View logs: docker-compose -f docker-compose.dev.yml logs mysql"
echo "   Stop: docker-compose -f docker-compose.dev.yml down"
echo ""
echo "🚀 You can now start your Spring Boot application!"