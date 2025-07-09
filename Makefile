# EOS Wallet System - Development Makefile

.PHONY: help dev-up dev-down dev-logs build test clean docker-build docker-run

# Default target
help:
	@echo "🚀 EOS Wallet System - Available Commands:"
	@echo ""
	@echo "  dev-up       - Start development environment (MySQL + Redis)"
	@echo "  dev-down     - Stop development environment"
	@echo "  dev-logs     - Show development logs"
	@echo "  build        - Build Java application"
	@echo "  test         - Run tests"
	@echo "  clean        - Clean build artifacts"
	@echo "  docker-build - Build Docker image"
	@echo "  docker-run   - Run application in Docker"
	@echo ""

# Development environment
dev-up:
	@echo "🐳 Starting development environment..."
	docker-compose -f docker-compose.dev.yml up -d
	@echo "✅ Development environment started!"
	@echo "   MySQL: localhost:3306 (wallet_user/wallet_pass)"
	@echo "   Redis: localhost:6379"

dev-down:
	@echo "🛑 Stopping development environment..."
	docker-compose -f docker-compose.dev.yml down
	@echo "✅ Development environment stopped!"

dev-logs:
	docker-compose -f docker-compose.dev.yml logs -f

# Java application
build:
	@echo "🔨 Building Java application..."
	./mvnw clean compile
	@echo "✅ Build completed!"

test:
	@echo "🧪 Running tests..."
	./mvnw test
	@echo "✅ Tests completed!"

clean:
	@echo "🧹 Cleaning build artifacts..."
	./mvnw clean
	@echo "✅ Clean completed!"

# Docker commands
docker-build:
	@echo "🐳 Building Docker image..."
	docker build -t eos-wallet-system .
	@echo "✅ Docker image built!"

docker-run:
	@echo "🐳 Running application in Docker..."
	docker run -p 8080:8080 \
		-e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/wallet_system \
		-e REDIS_HOST=host.docker.internal \
		eos-wallet-system

# Quick development workflow
dev: dev-up build
	@echo "🚀 Development environment ready!"
	@echo "   Run: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"