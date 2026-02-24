"""Async SQLAlchemy engine and session factory.

Rules
-----
- Never create an engine outside this module.
- Always use ``get_db`` (the FastAPI dependency) to obtain a session
  inside route handlers.  Direct use of ``AsyncSessionLocal`` is reserved
  for scripts and Alembic env.py only.
"""

from __future__ import annotations

from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlmodel import SQLModel

from app.core.config import get_settings

_settings = get_settings()

engine = create_async_engine(
    _settings.DATABASE_URL,
    echo=_settings.DEBUG,
    # SQLite-specific: allow connections across threads (required by aiosqlite).
    connect_args={"check_same_thread": False} if "sqlite" in _settings.DATABASE_URL else {},
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
    autocommit=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency that yields a database session per request.

    Commits on success; rolls back and closes on any exception.
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
