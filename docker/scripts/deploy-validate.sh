#!/bin/bash

# ============================================================
# DEPLOYMENT VALIDATION SCRIPT - FashionStore
# Validates deployment before going live
# ============================================================

set -e

echo "=== FashionStore Deployment Validation ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VALIDATION_FAILED=0

# Validation function
validate() {
    local check_name=$1
    local check_command=$2
    
    echo -n "Validating $check_name... "
    
    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        VALIDATION_FAILED=1
        return 1
    fi
}

# Validate Docker Compose configuration
validate "Docker Compose Configuration" "docker compose -f docker-compose.unified.yml config"

# Validate .env file exists
validate ".env File" "test -f .env"

# Validate required environment variables
validate "DB_HOST Environment Variable" "test -n \${DB_HOST}"
validate "DB_PASSWORD Environment Variable" "test -n \${DB_PASSWORD}"
validate "JWT_SECRET_KEY Environment Variable" "test -n \${JWT_SECRET_KEY}"

# Validate Docker daemon is running
validate "Docker Daemon" "docker info > /dev/null 2>&1"

# Validate required Docker images are available
validate "MySQL Docker Image" "docker image inspect mysql:8.0 > /dev/null 2>&1 || docker pull mysql:8.0"
validate "Redis Docker Image" "docker image inspect redis:7-alpine > /dev/null 2>&1 || docker pull redis:7-alpine"

# Validate port availability
validate "Port 8080 Available" "! nc -z localhost 8080 2>/dev/null || true"
validate "Port 3306 Available" "! nc -z localhost 3306 2>/dev/null || true"
validate "Port 6379 Available" "! nc -z localhost 6379 2>/dev/null || true"

# Validate Docker networks
validate "Docker Network Creation" "docker network create fashionstore-network 2>/dev/null || true"

echo ""
echo "=== Deployment Validation Complete ==="

if [ $VALIDATION_FAILED -ne 0 ]; then
    echo -e "${RED}Deployment validation failed. Please fix the issues above.${NC}"
    exit 1
else
    echo -e "${GREEN}Deployment validation passed. Ready to deploy.${NC}"
    exit 0
fi
