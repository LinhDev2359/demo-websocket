#!/bin/bash

echo "🔧 Fixing MySQL user authentication..."

# Kiểm tra xem MySQL container có đang chạy không
if ! docker-compose -f docker-compose.dev.yml ps mysql | grep -q "Up"; then
    echo "❌ MySQL container is not running!"
    echo "Please start MySQL first with: ./start-mysql.sh"
    exit 1
fi

echo "📊 MySQL container is running. Fixing user authentication..."

# Connect to MySQL và tạo user với quyền từ mọi IP
docker-compose -f docker-compose.dev.yml exec mysql mysql -u root -pdev_root_pass_2024 -e "
-- Drop existing users if they exist
DROP USER IF EXISTS 'wallet_dev_user'@'localhost';
DROP USER IF EXISTS 'wallet_dev_user'@'%';

-- Create users with proper authentication
CREATE USER 'wallet_dev_user'@'localhost' IDENTIFIED BY 'dev_wallet_pass_2024';
CREATE USER 'wallet_dev_user'@'%' IDENTIFIED BY 'dev_wallet_pass_2024';

-- Grant all privileges on wallet_system database
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_dev_user'@'localhost';
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_dev_user'@'%';

-- Grant additional privileges for development
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, RELOAD on *.* TO 'wallet_dev_user'@'localhost';
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, RELOAD on *.* TO 'wallet_dev_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Show created users
SELECT User, Host FROM mysql.user WHERE User = 'wallet_dev_user';
"

if [ $? -eq 0 ]; then
    echo "✅ MySQL user authentication fixed successfully!"
    echo ""
    echo "📝 Test connection:"
    echo "   Host: localhost:3307"
    echo "   Database: wallet_system"
    echo "   Username: wallet_dev_user"
    echo "   Password: dev_wallet_pass_2024"
    echo ""
    echo "🚀 You can now start the application with: ./start-app-mysql.sh"
else
    echo "❌ Failed to fix MySQL user authentication"
    echo "Check MySQL container logs: docker-compose -f docker-compose.dev.yml logs mysql"
fi