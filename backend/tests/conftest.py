"""Pytest fixtures shared across all test modules.

Key design decisions
--------------------
- A separate **in-memory SQLite** database is used for every test session.
  The ``get_db`` dependency is overridden to point at this DB.
- Each test function receives a *clean* DB via the ``db`` fixture, which
  wraps every test in a transaction that is rolled back afterward.
- The ``client`` fixture is function-scoped, so tests are fully isolated.
"""

from __future__ import annotations

import asyncio
import os
from collections.abc import AsyncGenerator
from typing import Any

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlmodel import SQLModel

# Import all models so metadata contains all tables before create_all.
import app.models.user  # noqa: F401
import app.models.token  # noqa: F401
import app.models.resume  # noqa: F401
import app.models.stored_resume  # noqa: F401

from app.core.limiter import limiter
from app.db.session import get_db
from app.main import create_app

# ── In-memory test database ───────────────────────────────────────────────────

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
TestSessionLocal = async_sessionmaker(
    bind=test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
    autocommit=False,
)


# ── Session-scoped table creation ─────────────────────────────────────────────


@pytest.fixture(scope="session", autouse=True)
def ensure_upload_dir() -> None:
    """Create the uploads directory that the file-upload route writes to."""
    os.makedirs("uploads", exist_ok=True)


@pytest.fixture(autouse=True)
def reset_rate_limiter() -> None:
    """Reset in-memory rate-limiter storage before every test.

    Prevents limit counts from bleeding across test functions when all tests
    originate from the same IP (127.0.0.1 in the ASGI test transport).
    """
    limiter._storage.reset()


@pytest_asyncio.fixture(scope="session")
async def create_tables() -> AsyncGenerator[None, None]:
    """Create all tables once per test session."""
    async with test_engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.create_all)
    yield
    async with test_engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.drop_all)


# ── Function-scoped DB session ────────────────────────────────────────────────


@pytest_asyncio.fixture
async def db(create_tables: None) -> AsyncGenerator[AsyncSession, None]:
    """Yield an ``AsyncSession`` scoped to a single test.

    All writes are rolled back after the test finishes to ensure isolation.
    """
    async with TestSessionLocal() as session:
        yield session
        await session.rollback()


# ── FastAPI test client ───────────────────────────────────────────────────────


@pytest_asyncio.fixture
async def client(db: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Yield an ``AsyncClient`` wired up to the FastAPI app with the test DB."""

    async def _override_get_db() -> AsyncGenerator[AsyncSession, None]:
        yield db

    application = create_app()
    application.dependency_overrides[get_db] = _override_get_db

    transport = ASGITransport(app=application)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    application.dependency_overrides.clear()


# ── Helpers ───────────────────────────────────────────────────────────────────


async def register_and_login(client: AsyncClient, email: str, password: str) -> dict[str, str]:
    """Register a user and return their token pair.  Utility for tests that
    need an authenticated client without repeating login boilerplate."""
    await client.post("/api/v1/auth/register", json={"email": email, "password": password})
    resp = await client.post("/api/v1/auth/login", json={"email": email, "password": password})
    return resp.json()["data"]
