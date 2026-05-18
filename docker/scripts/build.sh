#!/bin/bash

set -e

echo "Building FashionStore Docker images..."

# Build backend WAR file
echo "Building backend WAR file..."
cd backend
mvn clean package -DskipTests
cd ..

# Build Docker images
echo "Building Docker images with docker compose..."
docker compose build

echo "Build complete!"
echo "Run 'docker compose up' to start the services."
