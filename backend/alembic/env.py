"""Alembic environment configuration with async SQLite support.

This env.py is set up to work with SQLModel and aiosqlite.  It reads the
``DATABASE_URL`` from the app's ``Settings`` class (pydantic-settings) so
there is a single source of truth for the database connection string.

Running migrations
------------------

    alembic upgrade head          # Apply all pending migrations
    alembic revision --autogenerate -m "describe change"  # Generate a new migration
    alembic downgrade -1          # Roll back one step
"""

from __future__ import annotations

import asyncio
from logging.config import fileConfig

from alembic import context
from sqlalchemy.ext.asyncio import create_async_engine
from sqlmodel import SQLModel

# Import all SQLModel *table* models so their metadata is registered
# before Alembic generates or compares the schema.
import app.models.user  # noqa: F401
import app.models.token  # noqa: F401

from app.core.config import get_settings

# ── Alembic Config ────────────────────────────────────────────────────────────

config = context.config
if config.config_file_name:
    fileConfig(config.config_file_name)

target_metadata = SQLModel.metadata


def get_url() -> str:
    return get_settings().DATABASE_URL


# ── Offline migrations (no live DB connection) ────────────────────────────────


def run_migrations_offline() -> None:
    url = get_url()
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        compare_type=True,
    )
    with context.begin_transaction():
        context.run_migrations()


# ── Online migrations (async engine) ─────────────────────────────────────────


def do_run_migrations(connection):  # type: ignore[no-untyped-def]
    context.configure(
        connection=connection,
        target_metadata=target_metadata,
        compare_type=True,
    )
    with context.begin_transaction():
        context.run_migrations()


async def run_async_migrations() -> None:
    engine = create_async_engine(get_url())
    async with engine.connect() as connection:
        await connection.run_sync(do_run_migrations)
    await engine.dispose()


def run_migrations_online() -> None:
    asyncio.run(run_async_migrations())


# ── Entry point ───────────────────────────────────────────────────────────────

if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
