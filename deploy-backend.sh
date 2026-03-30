#!/bin/bash
# deploy-backend.sh — Deploy FastAPI backend to EC2
# Run this from the repo root on your EC2 instance.
# Prerequisites: Docker, Docker Compose, git

set -e

echo "🚀 Starting backend deployment to EC2..."

# ── 1. Pull latest code ────────────────────────────────────────────────────
BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "📦 Pulling latest changes from branch: $BRANCH..."
git pull origin "$BRANCH"

# ── 2. Change to repo root ─────────────────────────────────────────────────
cd "$(dirname "$0")" || exit 1

# ── 3. Verify required env vars ────────────────────────────────────────────
if [ -z "$GEMINI_API_KEY" ]; then
  echo "❌ GEMINI_API_KEY is not set. Export it or add to your shell profile."
  exit 1
fi

# ── 4. Stop old containers (free memory before build on t3.micro) ──────────
echo "🛑 Stopping existing containers..."
docker compose down

# ── 5. Build backend image ─────────────────────────────────────────────────
echo "🏭 Building backend image..."
docker compose build api

# ── 6. Start all backend services ─────────────────────────────────────────
echo "🔄 Starting containers (nginx + api + worker + redis)..."
docker compose up -d

# ── 7. Run database migrations ────────────────────────────────────────────
echo "🗄️  Running Alembic migrations..."
docker compose exec api alembic upgrade head

# ── 8. Prune dangling images ───────────────────────────────────────────────
echo "🧹 Cleaning up old images..."
docker image prune -f

echo "✅ Backend deployment complete!"
echo "   API: https://resumetailor.in/api/v1"
echo "   Docs: https://resumetailor.in/api/v1/docs"
