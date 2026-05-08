# FashionStore DevOps Deployment Guide

## Overview

This comprehensive guide covers the complete DevOps setup for FashionStore, including Docker containerization, CI/CD pipelines, cloud deployment, monitoring, and production optimization.

## 🐳 Docker Containerization

### Container Architecture
FashionStore uses a multi-stage Docker build process for optimized production images:

```dockerfile
# Multi-stage build for production optimization
FROM maven:3.9-openjdk-17 AS build
# Build stage with Maven

FROM openjdk:17-jre-slim AS runtime
# Runtime stage with optimized JRE
```

### Key Features
- **Multi-stage builds** for smaller production images
- **Non-root user** for security
- **Health checks** for container monitoring
- **Optimized JVM settings** for production

### Quick Start
```bash
# Build and run locally
docker-compose up -d

# Build specific environment
docker-compose -f docker-compose.prod.yml up -d
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow
The CI/CD pipeline includes:

1. **Quality Checks**
   - Unit tests with JUnit
   - SonarCloud code analysis
   - OWASP dependency scanning
   - Security vulnerability scanning

2. **Build & Test**
   - Docker image building
   - Integration tests
   - Performance testing with k6

3. **Deployment**
   - Staging deployment (develop branch)
   - Production deployment (main branch)
   - Blue-green deployment strategy

### Pipeline Triggers
- **Push to develop**: Automatic staging deployment
- **Push to main**: Production deployment with approval
- **Pull requests**: Full testing suite

### Environment Variables
Set up in GitHub repository secrets:
```bash
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
SONAR_TOKEN
SLACK_WEBHOOK_URL
DB_PASSWORD
REDIS_PASSWORD
```

## ☁️ Cloud Deployment Architecture

### AWS Infrastructure

#### Core Components
- **VPC**: Isolated network environment
- **ECS Fargate**: Container orchestration
- **ALB**: Application Load Balancer
- **RDS**: MySQL database
- **ElastiCache**: Redis cache
- **S3**: Static assets storage
- **CloudFront**: CDN distribution

#### Security Setup
- **Security Groups**: Network-level security
- **IAM Roles**: Least privilege access
- **Encryption**: Data at rest and in transit
- **SSL/TLS**: HTTPS enforcement

#### Auto Scaling
- **Horizontal scaling**: Based on CPU/memory
- **Load balancing**: Even distribution
- **Health checks**: Automatic replacement
- **Rolling updates**: Zero downtime

### Deployment Environments

#### Development
```bash
# Local development
docker-compose up -d

# Development environment
./devops/scripts/deploy.sh development us-east-1 dev-branch
```

#### Staging
```bash
# Staging deployment (automatic)
git push develop
```

#### Production
```bash
# Production deployment (manual approval)
git push main
```

## 📊 Monitoring & Logging

### Prometheus Metrics
- **Application metrics**: Custom business metrics
- **JVM metrics**: Memory, GC, threads
- **System metrics**: CPU, memory, disk
- **Database metrics**: Connection pool, query performance
- **Redis metrics**: Cache hit rate, memory usage

### Grafana Dashboards
- **Application Overview**: Health and performance
- **Infrastructure**: Resource utilization
- **Business Metrics**: Orders, users, revenue
- **Alerting**: Real-time notifications

### Logging Stack
- **Elasticsearch**: Log storage and search
- **Logstash**: Log processing and transformation
- **Kibana**: Log visualization and analysis

### Key Metrics to Monitor
```yaml
# Application Health
http_requests_total
http_request_duration_seconds
application_health_status

# JVM Performance
jvm_memory_used_bytes
jvm_gc_pause_seconds
jvm_threads_live_threads

# Database Performance
mysql_connections_active
mysql_slow_queries_total
mysql_query_duration_seconds

# Business Metrics
orders_total
revenue_total
active_users_total
```

## 🔧 Environment Configuration

### Environment Variables
Configure per environment:

#### Development
```bash
APP_ENV=development
APP_DEBUG=true
DB_HOST=localhost
REDIS_HOST=localhost
```

#### Staging
```bash
APP_ENV=staging
APP_DEBUG=false
DB_HOST=staging-db.example.com
REDIS_HOST=staging-redis.example.com
```

#### Production
```bash
APP_ENV=production
APP_DEBUG=false
DB_HOST=prod-db.example.com
REDIS_HOST=prod-redis.example.com
```

### Configuration Management
- **Environment-specific configs**: Separate per environment
- **Secrets management**: AWS Secrets Manager
- **Parameter Store**: Configuration values
- **Encryption**: Sensitive data protection

## 🛡️ Security Implementation

### SSL/HTTPS Setup
```bash
# Automatic SSL certificate management
certbot --nginx -d fashionstore.com -d www.fashionstore.com

# AWS Certificate Manager
aws acm request-certificate --domain-name fashionstore.com
```

### Security Headers
```nginx
# Security headers configuration
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Frame-Options DENY always;
add_header X-Content-Type-Options nosniff always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'" always;
```

### Network Security
- **VPC isolation**: Private network segments
- **Security groups**: Restricted access
- **NACLs**: Network-level filtering
- **WAF**: Web application firewall

## 🔄 Zero-Downtime Deployment

### Blue-Green Strategy
1. **Blue environment**: Current production
2. **Green environment**: New version
3. **Health checks**: Verify green environment
4. **Traffic switch**: Route to green environment
5. **Blue cleanup**: Remove old version

### Deployment Script
```bash
# Zero-downtime deployment
./devops/scripts/deploy.sh production us-east-1 latest

# Rollback if needed
./devops/scripts/deploy.sh rollback production us-east-1
```

### Health Checks
```bash
# Application health endpoint
GET /FashionStore/health

# Database connectivity
GET /FashionStore/actuator/health/db

# External dependencies
GET /FashionStore/actuator/health/redis
```

## 📈 Performance Optimization

### Application Optimization
- **JVM tuning**: G1GC, heap sizing
- **Connection pooling**: Database and Redis
- **Caching**: Multi-layer caching strategy
- **Async processing**: Non-blocking operations

### Infrastructure Optimization
- **Auto scaling**: Dynamic resource allocation
- **Load balancing**: Even traffic distribution
- **CDN**: Static asset delivery
- **Database**: Read replicas, indexing

### Monitoring Performance
```bash
# Performance testing
k6 run --out json=results.json devops/performance/load-test.js

# Benchmarking
ab -n 1000 -c 10 https://fashionstore.com/
```

## 💾 Backup & Disaster Recovery

### Database Backups
```bash
# Automated daily backups
aws rds create-db-snapshot \
    --db-instance-identifier fashionstore-prod \
    --db-snapshot-identifier fashionstore-backup-$(date +%Y%m%d)

# Point-in-time recovery
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier fashionstore-recovered \
    --db-snapshot-identifier fashionstore-backup-20231201
```

### Asset Backups
```bash
# S3 backup with versioning
aws s3 sync s3://fashionstore-assets s3://fashionstore-backups/$(date +%Y%m%d)

# Cross-region replication
aws s3api put-bucket-replication \
    --bucket fashionstore-assets \
    --replication-configuration file://replication-config.json
```

### Disaster Recovery Plan
1. **RTO**: 4 hours (Recovery Time Objective)
2. **RPO**: 1 hour (Recovery Point Objective)
3. **Multi-region**: Active-active setup
4. **Failover testing**: Monthly drills

## 🚨 Error Tracking & Alerting

### Sentry Integration
```java
// Error tracking setup
Sentry.init("{SENTRY_DSN}");
```

### Alerting Rules
```yaml
# High CPU usage
- alert: HighCPUUsage
  expr: cpu_usage_percent > 80
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High CPU usage detected"

# Memory pressure
- alert: HighMemoryUsage
  expr: memory_usage_percent > 85
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "High memory usage detected"

# Application errors
- alert: ApplicationErrors
  expr: error_rate > 5
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "High error rate detected"
```

### Notification Channels
- **Slack**: Real-time alerts
- **Email**: Critical notifications
- **SMS**: Emergency alerts
- **PagerDuty**: On-call rotations

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] Code review completed
- [ ] All tests passing
- [ ] Security scan passed
- [ ] Performance tests passed
- [ ] Documentation updated
- [ ] Backup strategy verified

### Post-Deployment
- [ ] Health checks passing
- [ ] Smoke tests successful
- [ ] Monitoring alerts configured
- [ ] Rollback plan ready
- [ ] Team notified
- [ ] Documentation updated

### Production Readiness
- [ ] SSL certificates valid
- [ ] Domain names configured
- [ ] DNS records updated
- [ ] Load balancer configured
- [ ] Auto scaling enabled
- [ ] Monitoring active

## 🔧 Troubleshooting

### Common Issues

#### Container Won't Start
```bash
# Check logs
docker logs fashionstore-app

# Debug container
docker exec -it fashionstore-app bash

# Check resource usage
docker stats
```

#### Database Connection Issues
```bash
# Test connectivity
telnet db-host 3306

# Check connection pool
curl http://localhost:8080/FashionStore/actuator/health/db

# Review logs
grep "database" /opt/fashionstore/logs/application.log
```

#### Performance Issues
```bash
# Check JVM metrics
curl http://localhost:8080/FashionStore/actuator/metrics/jvm.memory.used

# Monitor GC
jstat -gc <pid>

# Profile application
jvisualvm
```

### Emergency Procedures

#### Service Down
1. **Check health endpoints**
2. **Review logs**
3. **Verify dependencies**
4. **Rollback if needed**
5. **Notify team**

#### Database Issues
1. **Check connectivity**
2. **Verify credentials**
3. **Review query performance**
4. **Check connection pool**
5. **Scale database if needed**

#### High Traffic
1. **Check auto scaling**
2. **Monitor resource usage**
3. **Review CDN performance**
4. **Enable caching**
5. **Scale infrastructure**

## 📚 Additional Resources

### Documentation
- **API Documentation**: `/docs/api`
- **Architecture Guide**: `/docs/architecture`
- **Security Guide**: `/docs/security`
- **Performance Guide**: `/docs/performance`

### Tools & Scripts
- **Deployment Script**: `/devops/scripts/deploy.sh`
- **Backup Script**: `/devops/scripts/backup.sh`
- **Monitoring Setup**: `/devops/monitoring/`
- **Performance Tests**: `/devops/performance/`

### Support
- **DevOps Team**: devops@fashionstore.com
- **Emergency Contact**: +1-555-DEVOPS
- **Documentation**: https://docs.fashionstore.com
- **Status Page**: https://status.fashionstore.com

---

**Last Updated**: May 7, 2026  
**Version**: 2.0  
**Maintained by**: DevOps Team  
**Review Date**: Monthly
