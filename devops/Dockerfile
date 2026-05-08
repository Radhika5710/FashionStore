# FashionStore Dockerfile
# Multi-stage build for production optimization

# Stage 1: Build stage with Maven
FROM maven:3.9-openjdk-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with OpenJDK
FROM openjdk:17-jre-slim

# Install necessary packages
RUN apt-get update && \
    apt-get install -y \
    curl \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r fashionstore && \
    useradd -r -g fashionstore fashionstore

# Set working directory
WORKDIR /opt/fashionstore

# Copy WAR file from build stage
COPY --from=build /app/target/FashionStore.war ./FashionStore.war

# Copy configuration files
COPY docker/server.xml ./conf/
COPY docker/context.xml ./conf/
COPY docker/web.xml ./conf/
COPY docker/logging.properties ./conf/
COPY docker/keystore.jks ./conf/

# Create necessary directories
RUN mkdir -p /opt/fashionstore/logs \
    /opt/fashionstore/temp \
    /opt/fashionstore/work \
    /opt/fashionstore/webapps \
    /opt/fashionstore/conf \
    && chown -R fashionstore:fashionstore /opt/fashionstore

# Switch to non-root user
USER fashionstore

# Expose ports
EXPOSE 8080 8443

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/FashionStore/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV CATALINA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=production

# Start command
CMD ["java", "-jar", "/opt/fashionstore/FashionStore.war"]
