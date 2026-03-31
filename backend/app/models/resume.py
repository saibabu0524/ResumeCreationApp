import uuid
from datetime import UTC, datetime
from typing import Literal

from sqlmodel import Field, SQLModel

# Valid job statuses for resume tailoring jobs.
ResumeJobStatus = Literal["queued", "processing", "completed", "failed"]


def _utcnow() -> datetime:
    return datetime.now(UTC)


class TailoredResumeBase(SQLModel):
    user_id: uuid.UUID = Field(index=True)
    job_description: str = Field(max_length=10000)
    provider: str
    original_filename: str
    # Set to None until the background job completes successfully.
    stored_filename: str | None = Field(default=None)
    uploaded_stored_filename: str | None = None


class TailoredResume(TailoredResumeBase, table=True):
    __tablename__ = "tailored_resumes"

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        primary_key=True,
        index=True,
        nullable=False,
    )
    # Job lifecycle tracking — updated by the ARQ worker task.
    status: str = Field(default="queued")
    error_message: str | None = Field(default=None)
    created_at: datetime = Field(default_factory=_utcnow, nullable=False)
