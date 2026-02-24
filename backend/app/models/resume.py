import uuid
from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


def _utcnow() -> datetime:
    return datetime.now(UTC)


class TailoredResumeBase(SQLModel):
    user_id: uuid.UUID = Field(index=True)
    job_description: str = Field(max_length=10000)
    provider: str
    original_filename: str
    stored_filename: str
    uploaded_stored_filename: str | None = None


class TailoredResume(TailoredResumeBase, table=True):
    __tablename__ = "tailored_resumes"

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        primary_key=True,
        index=True,
        nullable=False,
    )
    created_at: datetime = Field(default_factory=_utcnow, nullable=False)
