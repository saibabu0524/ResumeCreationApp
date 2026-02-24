#!/bin/bash

# Ensure script fails on any error
set -e

echo "🚀 Starting deployment process..."

# 1. Pull latest code from the current branch
BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "📦 Pulling latest changes from git branch: $BRANCH..."
git pull origin "$BRANCH"

# 2. Build and restart Docker containers
echo "🏭 Building and restarting Docker containers..."
docker compose -f docker-compose.prod.yml up -d --build

# 3. Clean up dangling images to save disk space
echo "🧹 Cleaning up old Docker images..."
docker image prune -f

echo "✅ Deployment completed successfully!"
