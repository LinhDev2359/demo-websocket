#!/bin/bash

echo "🔐 Setting up Environment Variables for EOS Wallet System"

# Function to generate random password
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Function to generate JWT secret
generate_jwt_secret() {
    openssl rand -base64 64 | tr -d "=+/" | cut -c1-64
}

# Check if .env already exists
if [ -f .env ]; then
    echo "⚠️  .env file already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborted. Keeping existing .env file."
        exit 0
    fi
fi

echo "🎲 Generating secure random passwords..."

# Generate secure passwords
MYSQL_ROOT_PASSWORD=$(generate_password)
MYSQL_PASSWORD=$(generate_password)
REDIS_PASSWORD=$(generate_password)
JWT_SECRET=$(generate_jwt_secret)
GRAFANA_PASSWORD=$(generate_password)

# Ask for basic configuration
echo ""
echo "📝 Please provide basic configuration:"
read -p "Database name [wallet_system]: " DB_NAME
DB_NAME=${DB_NAME:-wallet_system}

read -p "Database username [wallet_user]: " DB_USER
DB_USER=${DB_USER:-wallet_user}

read -p "Environment (dev/staging/prod) [dev]: " ENV
ENV=${ENV:-dev}

read -p "Server port [8080]: " PORT
PORT=${PORT:-8080}

echo ""
echo "🔧 Creating .env file..."

# Create .env file
cat > .env << EOF
# =================================
# EOS WALLET SYSTEM - ENVIRONMENT VARIABLES
# Generated on: $(date)
# =================================
# IMPORTANT: Never commit this file to version control!

# Database Configuration
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
MYSQL_DATABASE=${DB_NAME}
MYSQL_USER=${DB_USER}
MYSQL_PASSWORD=${MYSQL_PASSWORD}

# Application Database Connection
DATABASE_URL=jdbc:mysql://localhost:3307/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=${DB_USER}
DATABASE_PASSWORD=${MYSQL_PASSWORD}

# Redis Configuration
REDIS_PASSWORD=${REDIS_PASSWORD}

# JWT Security (64 characters minimum)
JWT_SECRET=${JWT_SECRET}

# EOS Blockchain API
EOS_API_URL=https://eos.greymass.com

# Application Environment
ENVIRONMENT=${ENV}
SERVER_PORT=${PORT}

# Docker Configuration
COMPOSE_PROJECT_NAME=wallet-system-${ENV}

# Monitoring
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
EOF

# Create processed init-dev.sql
echo "🗄️ Creating database initialization script..."
envsubst < mysql/init-dev-template.sql > mysql/init-dev.sql

echo ""
echo "✅ Environment setup complete!"
echo ""
echo "📋 Generated credentials:"
echo "   MySQL Root: ${MYSQL_ROOT_PASSWORD}"
echo "   MySQL User: ${DB_USER}"
echo "   MySQL Pass: ${MYSQL_PASSWORD}"
echo "   Redis Pass: ${REDIS_PASSWORD}"
echo "   Grafana Pass: ${GRAFANA_PASSWORD}"
echo ""
echo "🔒 IMPORTANT SECURITY NOTES:"
echo "   1. .env file has been added to .gitignore"
echo "   2. NEVER commit .env file to version control"
echo "   3. Share credentials securely (encrypted channels only)"
echo "   4. Rotate passwords regularly in production"
echo ""
echo "🚀 Ready to start development:"
echo "   ./reset-dev.sh"
echo "   ./run-local.sh"