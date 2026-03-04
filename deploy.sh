#!/bin/bash

# Ensure script fails on any error
set -e

echo "🚀 Starting backend-only deployment process..."

# 1. Pull latest code from the current branch
BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "📦 Pulling latest changes from git branch: $BRANCH..."
git pull origin "$BRANCH"

# 2. Change directory to where the script is located (the project root folder)
echo "📂 Changing to the project root directory..."
cd "$(dirname "$0")" || exit 1

# 3. Stop existing containers to free up memory for the build
echo "🛑 Stopping existing containers..."
docker compose down

# 4. Build Docker containers (backend only, frontend disabled)
echo "🏭 Building Docker containers..."
docker compose build api

# 5. Start the newly built containers
echo "🔄 Starting new containers..."
docker compose up -d

# 6. Clean up dangling images to save disk space
echo "🧹 Cleaning up old Docker images..."
docker image prune -f

echo "✅ Backend deployment completed successfully!"
