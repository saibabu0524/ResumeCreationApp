"""Maintenance / cleanup background tasks.

Scheduled via ARQ's ``cron_jobs`` feature (see ``arq_worker.py``).
"""

from __future__ import annotations

import logging
import os
from pathlib import Path

from app.core.config import get_settings
from app.crud.token import delete_expired_tokens
from app.db.session import AsyncSessionLocal

logger = logging.getLogger(__name__)


async def cleanup_expired_tokens(ctx: dict) -> None:
    """Delete refresh tokens that are both revoked and past their expiry date.

    Keeps the ``refresh_tokens`` table small.  Safe to run daily.
    """
    async with AsyncSessionLocal() as session:
        try:
            count = await delete_expired_tokens(session)
            await session.commit()
            logger.info("cleanup_expired_tokens: deleted %d rows", count)
        except Exception:
            await session.rollback()
            logger.exception("cleanup_expired_tokens: failed")
            raise


async def cleanup_orphaned_uploads(ctx: dict) -> None:
    """Remove upload files on disk that have no corresponding DB record.

    In this template the 'DB record' is the in-memory ``_UPLOAD_METADATA``
    dict in ``uploads.py``.  Replace with a proper DB query in your project.
    """
    settings = get_settings()
    upload_dir = Path(settings.UPLOAD_DIR)

    if not upload_dir.exists():
        return

    removed = 0
    for path in upload_dir.iterdir():
        if path.is_file():
            # TODO: Replace with a real DB existence check.
            # if not await upload_exists_in_db(path.name):
            #     path.unlink()
            #     removed += 1
            pass

    logger.info("cleanup_orphaned_uploads: removed %d orphaned files", removed)
