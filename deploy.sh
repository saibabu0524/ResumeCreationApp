#!/bin/bash

# Ensure script fails on any error
set -e

echo "🚀 Starting full stack deployment process..."

# 1. Pull latest code from the current branch
BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "📦 Pulling latest changes from git branch: $BRANCH..."
git pull origin "$BRANCH"

# 2. Change directory to where the script is located (the project root folder)
echo "📂 Changing to the project root directory..."
cd "$(dirname "$0")" || exit 1

# 3. Build and restart Docker containers for both frontend and backend
echo "🏭 Building and restarting all Docker containers..."
# This uses the root docker-compose.yml which includes frontend, api, nginx, worker, and redis
docker compose up -d --build

# 4. Clean up dangling images to save disk space
echo "🧹 Cleaning up old Docker images..."
docker image prune -f

echo "✅ Full deployment completed successfully!"
