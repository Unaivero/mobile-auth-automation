#!/bin/bash

# Mobile Auth Automation - Complete Environment Setup Script
# This script sets up the entire testing environment with all services

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"
TIMEOUT=300 # 5 minutes timeout for service startup

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Mobile Auth Automation Setup${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to check if Docker is running
check_docker() {
    echo -e "${YELLOW}Checking Docker installation...${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Docker is not installed. Please install Docker Desktop first.${NC}"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}Docker is not running. Please start Docker Desktop.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker is running${NC}"
}

# Function to check if Docker Compose is available
check_docker_compose() {
    echo -e "${YELLOW}Checking Docker Compose...${NC}"
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo -e "${RED}Docker Compose is not available. Please install Docker Compose.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker Compose is available${NC}"
}

# Function to create environment file
create_env_file() {
    if [ ! -f "$ENV_FILE" ]; then
        echo -e "${YELLOW}Creating environment file...${NC}"
        cat > "$ENV_FILE" << EOF
# Database Configuration
POSTGRES_DB=mobile_auth_testing
POSTGRES_USER=test_user
POSTGRES_PASSWORD=test_password

# Redis Configuration
REDIS_PASSWORD=

# ZAP Configuration
ZAP_API_KEY=mobile-auth-zap-key

# Mock API Configuration
JWT_SECRET=mobile-auth-jwt-secret-key-for-testing
MAX_LOGIN_ATTEMPTS=5
CAPTCHA_THRESHOLD=3

# Test Configuration
TEST_ENVIRONMENT=docker
APPIUM_SERVER_URL=http://appium-server:4723
MOCK_API_URL=http://mock-api:8081

# Grafana Configuration
GF_SECURITY_ADMIN_PASSWORD=admin

# InfluxDB Configuration
INFLUXDB_ADMIN_PASSWORD=adminpassword
INFLUXDB_USER_PASSWORD=test_password
EOF
        echo -e "${GREEN}✓ Environment file created${NC}"
    else
        echo -e "${GREEN}✓ Environment file already exists${NC}"
    fi
}

# Function to pull required Docker images
pull_images() {
    echo -e "${YELLOW}Pulling required Docker images...${NC}"
    
    images=(
        "postgres:15-alpine"
        "redis:7-alpine"
        "owasp/zap2docker-stable"
        "grafana/grafana:latest"
        "influxdb:2.0"
        "python:3.9-slim"
        "maven:3.8-openjdk-11"
        "ubuntu:20.04"
    )
    
    for image in "${images[@]}"; do
        echo -e "Pulling ${image}..."
        docker pull "$image"
    done
    
    echo -e "${GREEN}✓ All images pulled successfully${NC}"
}

# Function to build custom images
build_images() {
    echo -e "${YELLOW}Building custom Docker images...${NC}"
    
    # Build in the correct order to handle dependencies
    docker-compose build mock-api
    docker-compose build appium-server
    docker-compose build android-emulator
    docker-compose build test-runner
    
    echo -e "${GREEN}✓ Custom images built successfully${NC}"
}

# Function to start services
start_services() {
    echo -e "${YELLOW}Starting all services...${NC}"
    
    # Start infrastructure services first
    echo -e "Starting infrastructure services..."
    docker-compose up -d postgres-db redis influxdb
    
    # Wait for database to be ready
    echo -e "Waiting for database to be ready..."
    timeout=60
    while ! docker-compose exec -T postgres-db pg_isready -U test_user -d mobile_auth_testing &> /dev/null; do
        if [ $timeout -le 0 ]; then
            echo -e "${RED}Database failed to start within timeout${NC}"
            exit 1
        fi
        echo -e "Waiting for database... ($timeout seconds remaining)"
        sleep 2
        timeout=$((timeout - 2))
    done
    echo -e "${GREEN}✓ Database is ready${NC}"
    
    # Start application services
    echo -e "Starting application services..."
    docker-compose up -d mock-api zap
    
    # Wait for mock API to be ready
    echo -e "Waiting for mock API to be ready..."
    timeout=60
    while ! curl -f http://localhost:8081/health &> /dev/null; do
        if [ $timeout -le 0 ]; then
            echo -e "${RED}Mock API failed to start within timeout${NC}"
            exit 1
        fi
        echo -e "Waiting for mock API... ($timeout seconds remaining)"
        sleep 2
        timeout=$((timeout - 2))
    done
    echo -e "${GREEN}✓ Mock API is ready${NC}"
    
    # Start testing services
    echo -e "Starting testing services..."
    docker-compose up -d appium-server android-emulator
    
    # Wait for Appium to be ready
    echo -e "Waiting for Appium server to be ready..."
    timeout=120
    while ! curl -f http://localhost:4723/wd/hub/status &> /dev/null; do
        if [ $timeout -le 0 ]; then
            echo -e "${RED}Appium server failed to start within timeout${NC}"
            exit 1
        fi
        echo -e "Waiting for Appium server... ($timeout seconds remaining)"
        sleep 5
        timeout=$((timeout - 5))
    done
    echo -e "${GREEN}✓ Appium server is ready${NC}"
    
    # Start monitoring services
    echo -e "Starting monitoring services..."
    docker-compose up -d grafana
    
    # Start test runner (but don't execute tests yet)
    echo -e "Starting test runner container..."
    docker-compose up -d test-runner
    
    echo -e "${GREEN}✓ All services started successfully${NC}"
}

# Function to verify service health
verify_services() {
    echo -e "${YELLOW}Verifying service health...${NC}"
    
    services=(
        "postgres-db:5432"
        "redis:6379"  
        "mock-api:8081"
        "zap:8080"
        "appium-server:4723"
        "grafana:3000"
        "influxdb:8086"
    )
    
    for service in "${services[@]}"; do
        service_name="${service%:*}"
        port="${service#*:}"
        
        if docker-compose ps "$service_name" | grep -q "Up"; then
            echo -e "${GREEN}✓ $service_name is running${NC}"
        else
            echo -e "${RED}✗ $service_name is not running${NC}"
        fi
    done
}

# Function to show service URLs
show_service_urls() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE} Service URLs${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}Mock API Health:${NC} http://localhost:8081/health"
    echo -e "${GREEN}Grafana Dashboard:${NC} http://localhost:3000 (admin/admin)"
    echo -e "${GREEN}ZAP Proxy:${NC} http://localhost:8080"
    echo -e "${GREEN}Appium Hub:${NC} http://localhost:4723/wd/hub/status"
    echo -e "${GREEN}InfluxDB:${NC} http://localhost:8086"
    echo -e "${GREEN}PostgreSQL:${NC} localhost:5432 (test_user/test_password)"
    echo -e "${GREEN}Redis:${NC} localhost:6379"
}

# Function to show next steps
show_next_steps() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE} Next Steps${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}1. Run tests:${NC}"
    echo -e "   ./run-security-tests.sh"
    echo -e ""
    echo -e "${YELLOW}2. Run specific test categories:${NC}"
    echo -e "   mvn clean test -Dgroups=biometric"
    echo -e "   mvn clean test -Dgroups=api-security"
    echo -e "   mvn clean test -Dgroups=ui-security"
    echo -e ""
    echo -e "${YELLOW}3. Execute tests in Docker:${NC}"
    echo -e "   docker-compose exec test-runner mvn clean test"
    echo -e ""
    echo -e "${YELLOW}4. View logs:${NC}"
    echo -e "   docker-compose logs -f mock-api"
    echo -e "   docker-compose logs -f appium-server"
    echo -e ""
    echo -e "${YELLOW}5. Stop all services:${NC}"
    echo -e "   docker-compose down"
    echo -e ""
    echo -e "${YELLOW}6. Stop and remove volumes:${NC}"
    echo -e "   docker-compose down -v"
}

# Function to handle cleanup on exit
cleanup() {
    echo -e "\n${YELLOW}Cleaning up...${NC}"
    # Add any cleanup logic here if needed
}

# Trap cleanup function on script exit
trap cleanup EXIT

# Main execution
main() {
    echo -e "${BLUE}Starting Mobile Auth Automation setup...${NC}"
    
    check_docker
    check_docker_compose
    create_env_file
    
    # Ask user if they want to pull images (can be time-consuming)
    read -p "Do you want to pull the latest Docker images? This may take several minutes. (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        pull_images
    fi
    
    build_images
    start_services
    verify_services
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN} Setup Complete! 🎉${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    show_service_urls
    show_next_steps
}

# Check if help was requested
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Mobile Auth Automation Setup Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help     Show this help message"
    echo "  --clean        Stop and remove all containers and volumes"
    echo "  --logs         Show logs from all services"
    echo "  --status       Show status of all services"
    echo ""
    echo "Examples:"
    echo "  $0                  # Full setup"
    echo "  $0 --clean          # Clean environment"
    echo "  $0 --status         # Check service status"
    exit 0
fi

# Handle cleanup option
if [[ "$1" == "--clean" ]]; then
    echo -e "${YELLOW}Cleaning up environment...${NC}"
    docker-compose down -v --remove-orphans
    docker system prune -f
    echo -e "${GREEN}✓ Environment cleaned${NC}"
    exit 0
fi

# Handle logs option
if [[ "$1" == "--logs" ]]; then
    docker-compose logs -f
    exit 0
fi

# Handle status option
if [[ "$1" == "--status" ]]; then
    echo -e "${YELLOW}Service Status:${NC}"
    docker-compose ps
    echo -e "\n${YELLOW}Service Health:${NC}"
    verify_services
    exit 0
fi

# Run main setup
main "$@"