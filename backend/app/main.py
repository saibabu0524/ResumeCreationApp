"""FastAPI application factory.

Startup sequence
----------------
1. ``lifespan`` runs ``init_db()`` → tables are created and upload dir is ensured.
2. ARQ pool is created and stored on ``app.state.arq_pool``.
3. Routes are mounted at ``/api/v1``.
4. Middleware is added: CORS, slowapi rate limiting, 429 handler.

Never import ``app`` directly in tests — use the ``AsyncClient`` fixture in
``conftest.py`` instead to ensure DB override is applied correctly.
"""

from __future__ import annotations

from contextlib import asynccontextmanager
from typing import AsyncGenerator

import arq
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from app.api.v1.router import v1_router
from app.core.config import get_settings
from app.core.limiter import limiter
from app.db.init_db import init_db


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application lifespan: runs startup logic before yield and teardown after."""
    settings = get_settings()

    # ── Startup ───────────────────────────────────────────────────────────────
    await init_db()

    # Connect ARQ to Redis and store the pool so routes can enqueue jobs.
    arq_pool = await arq.create_pool(arq.connections.RedisSettings.from_dsn(settings.REDIS_URL))
    app.state.arq_pool = arq_pool

    yield  # App runs here.

    # ── Shutdown ──────────────────────────────────────────────────────────────
    await arq_pool.close()


def create_app() -> FastAPI:
    """Create and configure the FastAPI application.

    Keeping construction in a factory function makes it easy to create
    custom variants for testing (override dependencies, skip real Redis, etc.).
    """
    settings = get_settings()

    app = FastAPI(
        title=settings.APP_NAME,
        version="1.0.0",
        docs_url="/docs" if not settings.is_production else None,
        redoc_url="/redoc" if not settings.is_production else None,
        openapi_url="/openapi.json" if not settings.is_production else None,
        lifespan=lifespan,
    )

    # ── Rate limiting ─────────────────────────────────────────────────────────
    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
    app.add_middleware(SlowAPIMiddleware)

    # ── CORS ──────────────────────────────────────────────────────────────────
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.ALLOWED_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # ── Routers ───────────────────────────────────────────────────────────────
    app.include_router(v1_router, prefix="/api/v1")

    # ── Health check ──────────────────────────────────────────────────────────
    @app.get("/health", tags=["health"])
    async def health_check() -> dict:
        return {"status": "ok", "version": app.version}

    return app


# Top-level ``app`` instance used by Uvicorn.
app = create_app()
