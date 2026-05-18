#!/bin/bash

# ============================================================
# HEALTH CHECK SCRIPT - FashionStore
# Checks health of all services
# ============================================================

set -e

echo "=== FashionStore Health Check ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Service endpoints
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
ADMIN_URL="${ADMIN_URL:-http://localhost:8081}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"

# Check function
check_service() {
    local service_name=$1
    local check_command=$2
    
    echo -n "Checking $service_name... "
    
    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Check Backend Health
check_service "Backend Health" "curl -f -s ${BACKEND_URL}/health"

# Check Admin Health
check_service "Admin Health" "curl -f -s ${ADMIN_URL}/health"

# Check MySQL Connection
check_service "MySQL Connection" "mysqladmin ping -h ${MYSQL_HOST} -P ${MYSQL_PORT}"

# Check Redis Connection
check_service "Redis Connection" "redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping"

echo ""
echo "=== Health Check Complete ==="

# Exit with error if any check failed
if [ $? -ne 0 ]; then
    echo -e "${RED}One or more health checks failed${NC}"
    exit 1
else
    echo -e "${GREEN}All health checks passed${NC}"
    exit 0
fi
