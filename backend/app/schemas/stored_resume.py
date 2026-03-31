"""Pydantic schemas for stored resume endpoints."""

from __future__ import annotations

import uuid
from datetime import datetime

from pydantic import BaseModel


class StoredResumePublic(BaseModel):
    """Safe response schema for stored resumes."""

    id: uuid.UUID
    original_filename: str
    file_size_bytes: int
    created_at: datetime

    model_config = {"from_attributes": True}
