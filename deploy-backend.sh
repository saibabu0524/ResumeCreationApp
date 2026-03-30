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

# ── 4. Free ports 80 / 443 on the host before Docker tries to bind them ──────
# On a fresh EC2 Ubuntu instance, system nginx or apache2 may already own :80.
# fuser only covers IPv4; we use ss+kill to catch IPv6 (:::80) as well.
echo "🔌 Freeing ports 80 and 443 on the host..."
sudo systemctl stop nginx    2>/dev/null || true
sudo systemctl disable nginx 2>/dev/null || true
sudo systemctl stop apache2  2>/dev/null || true
sudo systemctl disable apache2 2>/dev/null || true

# Kill every PID that ss reports as listening on :80 or :443 (IPv4 + IPv6)
for PORT in 80 443; do
  PIDS=$(sudo ss -tlnp "sport = :$PORT" 2>/dev/null \
         | grep -oP '(?<=pid=)\d+' | sort -u)
  if [ -n "$PIDS" ]; then
    echo "   Killing PIDs on port $PORT: $PIDS"
    echo "$PIDS" | xargs -r sudo kill -9
  fi
done

# Extra safety net via fuser (IPv4 only, but harmless)
sudo fuser -k 80/tcp  2>/dev/null || true
sudo fuser -k 443/tcp 2>/dev/null || true

# Brief pause to let the kernel release the sockets
sleep 1

# ── 5. Stop old containers (free memory before build on t3.micro) ──────────
echo "🛑 Stopping existing containers..."
docker compose down

# ── 6. Build backend image ─────────────────────────────────────────────────
echo "🏭 Building backend image..."
docker compose build api

# ── 7. Start all backend services ─────────────────────────────────────────
echo "🔄 Starting containers (nginx + api + worker + redis)..."
docker compose up -d

# ── 8. Run database migrations ────────────────────────────────────────────
echo "🗄️  Running Alembic migrations..."
docker compose exec api alembic upgrade head

# ── 9. Prune dangling images ───────────────────────────────────────────────
echo "🧹 Cleaning up old images..."
docker image prune -f

echo "✅ Backend deployment complete!"
echo "   API: https://resumetailor.in/api/v1"
echo "   Docs: https://resumetailor.in/api/v1/docs"
