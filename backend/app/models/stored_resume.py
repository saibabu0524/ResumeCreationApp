"""Stored resume model — user-uploaded base resumes for reuse."""

from __future__ import annotations

import uuid
from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


def _utcnow() -> datetime:
    return datetime.now(UTC)


class StoredResume(SQLModel, table=True):
    __tablename__ = "stored_resumes"

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        primary_key=True,
        index=True,
        nullable=False,
    )
    user_id: uuid.UUID = Field(index=True)
    original_filename: str
    stored_filename: str
    file_size_bytes: int = 0
    created_at: datetime = Field(default_factory=_utcnow, nullable=False)
