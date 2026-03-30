#!/bin/bash
# deploy-frontend.sh — Deploy Next.js frontend to Vercel
# Run this from the repo root on your local machine or CI.
# Prerequisites: Node.js, Vercel CLI (`npm i -g vercel`), project linked via `vercel link`

set -e

echo "🚀 Starting frontend deployment to Vercel..."

# ── 1. Change to frontend directory ───────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/frontend" || exit 1

# ── 2. Verify Vercel CLI is available ─────────────────────────────────────
if ! command -v vercel &> /dev/null; then
  echo "❌ Vercel CLI not found. Install it with: npm i -g vercel"
  exit 1
fi

# ── 3. Ensure env var is configured on Vercel (first-time setup hint) ─────
#    Run once manually if not already done:
#    vercel env add NEXT_PUBLIC_API_BASE_URL production
#    Value: https://resumetailor.in/api/v1
echo "ℹ️  Ensure NEXT_PUBLIC_API_BASE_URL is set in Vercel dashboard / env."
echo "   Expected value: https://resumetailor.in/api/v1"

# ── 4. Deploy to production ────────────────────────────────────────────────
echo "🏭 Deploying to Vercel (production)..."
vercel --prod --yes

echo "✅ Frontend deployment complete!"
echo "   Live at: https://app.resumetailor.in"
