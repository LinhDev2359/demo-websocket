#!/bin/bash

echo "🔍 Testing MySQL connection..."

# Test connection với credentials của ứng dụng
echo "Testing connection with application credentials..."

docker-compose -f docker-compose.dev.yml exec mysql mysql \
    -h localhost \
    -P 3306 \
    -u wallet_dev_user \
    -pdev_wallet_pass_2024 \
    -D wallet_system \
    -e "SELECT 'Connection successful!' as status, USER() as current_user, DATABASE() as current_database;"

if [ $? -eq 0 ]; then
    echo "✅ MySQL connection test successful!"
else
    echo "❌ MySQL connection test failed!"
    echo ""
    echo "Debugging information:"
    echo "1. Check if container is running:"
    docker-compose -f docker-compose.dev.yml ps mysql
    echo ""
    echo "2. Check MySQL users:"
    docker-compose -f docker-compose.dev.yml exec mysql mysql -u root -pdev_root_pass_2024 -e "SELECT User, Host FROM mysql.user WHERE User LIKE '%wallet%';"
    echo ""
    echo "3. Try to fix user authentication:"
    echo "   ./fix-mysql-user.sh"
fi