"""Database initialisation helpers.

``init_db()`` is called once on application startup (via the lifespan hook
in ``main.py``).  It creates all SQLModel tables that don't already exist and
ensures the uploads directory is present on disk.

For schema *changes* after initial deployment, always use Alembic migrations —
never rely on ``SQLModel.metadata.create_all`` alone.
"""

from __future__ import annotations

import os

from sqlmodel import SQLModel

from app.core.config import get_settings
from app.db.session import engine

# Import all models so their metadata is registered before create_all runs.
# This must stay in sync with every ``table=True`` SQLModel class.
import app.models.user  # noqa: F401
import app.models.token  # noqa: F401


async def init_db() -> None:
    """Create tables and required directories on first startup."""
    settings = get_settings()

    # Ensure the uploads directory exists.
    os.makedirs(settings.UPLOAD_DIR, exist_ok=True)

    # Create any missing tables.  In production, rely on Alembic instead
    # and comment out the create_all call to be safe.
    async with engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.create_all)
