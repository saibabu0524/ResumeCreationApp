"""Async CRUD operations for the StoredResume table."""

from __future__ import annotations

import uuid

from sqlalchemy import delete, select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.models.stored_resume import StoredResume


async def create_stored_resume(
    session: AsyncSession,
    *,
    user_id: uuid.UUID,
    original_filename: str,
    stored_filename: str,
    file_size_bytes: int,
) -> StoredResume:
    """Persist a new stored resume record."""
    resume = StoredResume(
        user_id=user_id,
        original_filename=original_filename,
        stored_filename=stored_filename,
        file_size_bytes=file_size_bytes,
    )
    session.add(resume)
    await session.flush()
    return resume


async def get_user_resumes(
    session: AsyncSession,
    user_id: uuid.UUID,
) -> list[StoredResume]:
    """Return all stored resumes for a user, newest first."""
    result = await session.execute(
        select(StoredResume)
        .where(StoredResume.user_id == user_id)
        .order_by(StoredResume.created_at.desc())
    )
    return list(result.scalars().all())


async def get_stored_resume(
    session: AsyncSession,
    resume_id: uuid.UUID,
    user_id: uuid.UUID,
) -> StoredResume | None:
    """Return a specific stored resume owned by the user, or None."""
    result = await session.execute(
        select(StoredResume).where(
            StoredResume.id == resume_id,
            StoredResume.user_id == user_id,
        )
    )
    return result.scalars().first()


async def delete_stored_resume(
    session: AsyncSession,
    resume_id: uuid.UUID,
    user_id: uuid.UUID,
) -> bool:
    """Delete a stored resume. Returns True if a row was deleted."""
    result = await session.execute(
        delete(StoredResume).where(
            StoredResume.id == resume_id,
            StoredResume.user_id == user_id,
        )
    )
    return result.rowcount > 0
