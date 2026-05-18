#!/bin/bash

# ============================================================
# ROLLBACK SCRIPT - FashionStore
# Safely rolls back to previous deployment
# ============================================================

set -e

echo "=== FashionStore Rollback Script ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.unified.yml}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
MAX_BACKUPS="${MAX_BACKUPS:-5}"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Function to create backup before rollback
create_backup() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="$BACKUP_DIR/docker-compose.backup.$timestamp.yml"
    
    echo "Creating backup: $backup_file"
    cp "$COMPOSE_FILE" "$backup_file"
    
    # Keep only last N backups
    ls -t "$BACKUP_DIR"/docker-compose.backup.*.yml 2>/dev/null | tail -n +$((MAX_BACKUPS + 1)) | xargs -r rm
    
    echo "Backup created successfully"
}

# Function to rollback to previous version
rollback_to_version() {
    local version=$1
    
    echo "Rolling back to version: $version"
    
    # Pull previous Docker images
    echo "Pulling previous Docker images..."
    docker compose -f "$COMPOSE_FILE" pull
    
    # Restart services with previous images
    echo "Restarting services with previous images..."
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d
    
    # Wait for services to be healthy
    echo "Waiting for services to be healthy..."
    sleep 30
    
    # Run health check
    if ./docker/scripts/health-check.sh; then
        echo -e "${GREEN}Rollback successful${NC}"
        return 0
    else
        echo -e "${RED}Rollback failed - services not healthy${NC}"
        return 1
    fi
}

# Main rollback logic
if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo ""
    echo "Available rollback options:"
    echo "  - previous: Rollback to previous deployment"
    echo "  - <version>: Rollback to specific version tag"
    echo ""
    exit 1
fi

# Create backup before rollback
create_backup

# Perform rollback
if [ "$1" == "previous" ]; then
    # Rollback to previous deployment
    echo "Rolling back to previous deployment..."
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d --force-recreate
    
    # Wait for services to be healthy
    echo "Waiting for services to be healthy..."
    sleep 30
    
    # Run health check
    if ./docker/scripts/health-check.sh; then
        echo -e "${GREEN}Rollback to previous deployment successful${NC}"
        exit 0
    else
        echo -e "${RED}Rollback failed - services not healthy${NC}"
        exit 1
    fi
else
    # Rollback to specific version
    rollback_to_version "$1"
fi

echo ""
echo "=== Rollback Complete ==="
