#!/bin/bash

# ============================================================
# MONITORING HOOKS SCRIPT - FashionStore
# Collects and sends metrics to monitoring systems
# ============================================================

set -e

echo "=== FashionStore Monitoring Hooks ==="
echo ""

# Configuration
METRICS_ENDPOINT="${METRICS_ENDPOINT:-}"
METRICS_API_KEY="${METRICS_API_KEY:-}"
ENABLE_MONITORING="${ENABLE_MONITORING:-false}"

# If monitoring is disabled, exit gracefully
if [ "$ENABLE_MONITORING" != "true" ]; then
    echo "Monitoring is disabled. Set ENABLE_MONITORING=true to enable."
    exit 0
fi

# Function to collect container metrics
collect_container_metrics() {
    local container_name=$1
    
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" "$container_name" 2>/dev/null || true
}

# Function to collect application metrics
collect_app_metrics() {
    local endpoint=$1
    
    curl -s "$endpoint/metrics" 2>/dev/null || echo "Metrics endpoint not available"
}

# Function to send metrics to monitoring service
send_metrics() {
    local metrics=$1
    
    if [ -n "$METRICS_ENDPOINT" ] && [ -n "$METRICS_API_KEY" ]; then
        curl -X POST "$METRICS_ENDPOINT" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $METRICS_API_KEY" \
            -d "$metrics" \
            2>/dev/null || echo "Failed to send metrics"
    fi
}

# Collect metrics from all containers
echo "Collecting container metrics..."
for container in fashionstore-backend fashionstore-admin fashionstore-mysql fashionstore-redis; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "Metrics for $container:"
        collect_container_metrics "$container"
        echo ""
    fi
done

# Collect application metrics
echo "Collecting application metrics..."
BACKEND_METRICS=$(collect_app_metrics "http://localhost:8080")
echo "Backend metrics: $BACKEND_METRICS"

# Send metrics to monitoring service
if [ -n "$BACKEND_METRICS" ]; then
    send_metrics "$BACKEND_METRICS"
fi

echo "=== Monitoring Hooks Complete ==="
