#!/bin/bash

# FashionStore Deployment Script
# Supports zero-downtime deployment with blue-green strategy

set -e

# Configuration
ENVIRONMENT=${1:-production}
REGION=${2:-us-east-1}
CLUSTER_NAME="fashionstore-${ENVIRONMENT}"
SERVICE_NAME="fashionstore-app"
TASK_DEF_FAMILY="fashionstore-${ENVIRONMENT}"
IMAGE_TAG=${3:-latest}
HEALTH_CHECK_TIMEOUT=300
ROLLBACK_ON_FAILURE=true

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        error "AWS CLI is not installed"
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed"
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        error "AWS credentials are not configured"
    fi
    
    # Check if cluster exists
    if ! aws ecs describe-clusters --cluster "$CLUSTER_NAME" &> /dev/null; then
        error "ECS cluster '$CLUSTER_NAME' does not exist"
    fi
    
    success "Prerequisites check passed"
}

# Build and push Docker image
build_and_push_image() {
    log "Building Docker image..."
    
    # Build image
    docker build -t "fashionstore:$IMAGE_TAG" -f devops/Dockerfile .
    
    # Tag for ECR
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    ECR_REGISTRY="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
    FULL_IMAGE_NAME="$ECR_REGISTRY/fashionstore:$IMAGE_TAG"
    
    # Login to ECR
    aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
    
    # Tag and push
    docker tag "fashionstore:$IMAGE_TAG" "$FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"
    
    success "Docker image built and pushed: $FULL_IMAGE_NAME"
}

# Register new task definition
register_task_definition() {
    log "Registering new task definition..."
    
    # Get current task definition
    CURRENT_TASK_DEF=$(aws ecs describe-services \
        --cluster "$CLUSTER_NAME" \
        --services "$SERVICE_NAME" \
        --query 'services[0].taskDefinition' \
        --output text)
    
    # Get current task definition details
    aws ecs describe-task-definition \
        --task-definition "$CURRENT_TASK_DEF" \
        --query 'taskDefinition' > current-task-def.json
    
    # Update image in task definition
    sed -i "s|\"image\": \".*\"|\"image\": \"$FULL_IMAGE_NAME\"|g" current-task-def.json
    
    # Register new task definition
    NEW_TASK_DEF=$(aws ecs register-task-definition \
        --cli-input-json file://current-task-def.json \
        --query 'taskDefinition.taskDefinitionArn' \
        --output text)
    
    # Clean up
    rm current-task-def.json
    
    success "New task definition registered: $NEW_TASK_DEF"
    echo "$NEW_TASK_DEF"
}

# Update service with new task definition
update_service() {
    local task_def_arn=$1
    log "Updating ECS service..."
    
    # Get current desired count
    DESIRED_COUNT=$(aws ecs describe-services \
        --cluster "$CLUSTER_NAME" \
        --services "$SERVICE_NAME" \
        --query 'services[0].desiredCount' \
        --output text)
    
    # Update service
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$task_def_arn" \
        --force-new-deployment \
        --desired-count "$DESIRED_COUNT"
    
    success "Service update initiated"
}

# Wait for deployment to complete
wait_for_deployment() {
    log "Waiting for deployment to complete..."
    
    local start_time=$(date +%s)
    local timeout=$HEALTH_CHECK_TIMEOUT
    
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [ $elapsed -gt $timeout ]; then
            error "Deployment timeout after ${timeout}s"
        fi
        
        # Check service status
        local service_status=$(aws ecs describe-services \
            --cluster "$CLUSTER_NAME" \
            --services "$SERVICE_NAME" \
            --query 'services[0].status' \
            --output text)
        
        if [ "$service_status" != "ACTIVE" ]; then
            warning "Service status: $service_status"
            sleep 10
            continue
        fi
        
        # Check deployment status
        local deployments=$(aws ecs describe-services \
            --cluster "$CLUSTER_NAME" \
            --services "$SERVICE_NAME" \
            --query 'services[0].deployments' \
            --output json)
        
        local primary_deployment=$(echo "$deployments" | jq -r '.[] | select(.status == "PRIMARY")')
        local running_count=$(echo "$primary_deployment" | jq -r '.runningCount')
        local desired_count=$(echo "$primary_deployment" | jq -r '.desiredCount')
        
        log "Deployment progress: $running_count/$desired_count tasks running"
        
        if [ "$running_count" -eq "$desired_count" ]; then
            success "Deployment completed successfully"
            break
        fi
        
        sleep 15
    done
}

# Health check
health_check() {
    log "Performing health check..."
    
    # Get load balancer DNS name
    local lb_dns=$(aws elbv2 describe-load-balancers \
        --names "fashionstore-${ENVIRONMENT}-alb" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    # Wait for DNS propagation
    sleep 30
    
    # Perform health checks
    local health_url="http://$lb_dns/FashionStore/health"
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$health_url" > /dev/null; then
            success "Health check passed"
            return 0
        fi
        
        warning "Health check attempt $attempt/$max_attempts failed"
        sleep 10
        ((attempt++))
    done
    
    error "Health check failed after $max_attempts attempts"
}

# Rollback function
rollback() {
    if [ "$ROLLBACK_ON_FAILURE" = "true" ]; then
        log "Initiating rollback..."
        
        # Get previous task definition
        local previous_task_def=$(aws ecs describe-services \
            --cluster "$CLUSTER_NAME" \
            --services "$SERVICE_NAME" \
            --query 'services[0].taskDefinition' \
            --output text)
        
        # Update service with previous task definition
        aws ecs update-service \
            --cluster "$CLUSTER_NAME" \
            --service "$SERVICE_NAME" \
            --task-definition "$previous_task_def" \
            --force-new-deployment
        
        warning "Rollback initiated with task definition: $previous_task_def"
    fi
}

# Smoke tests
run_smoke_tests() {
    log "Running smoke tests..."
    
    # Get load balancer DNS name
    local lb_dns=$(aws elbv2 describe-load-balancers \
        --names "fashionstore-${ENVIRONMENT}-alb" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    # Test endpoints
    local endpoints=(
        "/FashionStore/"
        "/FashionStore/products"
        "/FashionStore/health"
        "/FashionStore/actuator/info"
    )
    
    for endpoint in "${endpoints[@]}"; do
        local url="http://$lb_dns$endpoint"
        if curl -f -s "$url" > /dev/null; then
            success "Smoke test passed: $endpoint"
        else
            error "Smoke test failed: $endpoint"
        fi
    done
    
    success "All smoke tests passed"
}

# Cleanup old task definitions
cleanup_old_task_definitions() {
    log "Cleaning up old task definitions..."
    
    # Keep only last 10 task definitions
    aws ecs list-task-definitions \
        --family "$TASK_DEF_FAMILY" \
        --sort DESC \
        --query 'taskDefinitionArns[10:]' \
        --output text | \
    while read -r task_def; do
        if [ -n "$task_def" ]; then
            aws ecs deregister-task-definition --task-definition "$task_def"
            log "Deregistered old task definition: $task_def"
        fi
    done
}

# Send notification
send_notification() {
    local status=$1
    local message=$2
    
    if [ -n "$SLACK_WEBHOOK_URL" ]; then
        local color="good"
        if [ "$status" = "failed" ]; then
            color="danger"
        fi
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"$message\", \"attachments\": [{\"color\":\"$color\"}]}" \
            "$SLACK_WEBHOOK_URL"
    fi
}

# Main deployment function
deploy() {
    log "Starting FashionStore deployment to $ENVIRONMENT environment"
    
    # Check prerequisites
    check_prerequisites
    
    # Build and push image
    build_and_push_image
    
    # Register new task definition
    local new_task_def
    new_task_def=$(register_task_definition)
    
    # Update service
    update_service "$new_task_def"
    
    # Wait for deployment
    wait_for_deployment
    
    # Health check
    health_check
    
    # Run smoke tests
    run_smoke_tests
    
    # Cleanup
    cleanup_old_task_definitions
    
    success "Deployment completed successfully!"
    send_notification "success" "FashionStore deployment to $ENVIRONMENT completed successfully! 🚀"
}

# Rollback function
rollback_deployment() {
    log "Rolling back deployment..."
    rollback
    wait_for_deployment
    health_check
    success "Rollback completed"
    send_notification "warning" "FashionStore deployment to $ENVIRONMENT was rolled back"
}

# Handle script arguments
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    rollback)
        rollback_deployment
        ;;
    health-check)
        health_check
        ;;
    smoke-tests)
        run_smoke_tests
        ;;
    *)
        echo "Usage: $0 {deploy|rollback|health-check|smoke-tests} [environment] [region] [image-tag]"
        echo "Example: $0 deploy production us-east-1 latest"
        exit 1
        ;;
esac
