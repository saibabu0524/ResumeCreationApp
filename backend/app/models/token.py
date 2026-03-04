"""RefreshToken SQLModel table model.

Refresh tokens are never stored in plain text.  Only the SHA-256 hash of
the raw token is persisted here.  The raw token is sent to the client once
and never stored server-side.

Token rotation is enforced: every ``/auth/refresh`` call revokes the current
token and issues a brand-new pair.  This prevents token reuse attacks.

Note: ``from __future__ import annotations`` is intentionally omitted.
See app/models/user.py for explanation.
"""

import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.user import User


def _utcnow() -> datetime:
    return datetime.now(UTC)


class RefreshToken(SQLModel, table=True):
    """Stores a hashed refresh token for a user."""

    __tablename__ = "refresh_tokens"

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        primary_key=True,
        index=True,
        nullable=False,
    )
    user_id: uuid.UUID = Field(foreign_key="users.id", nullable=False, index=True)
    token_hash: str = Field(unique=True, index=True)  # SHA-256 hex digest
    expires_at: datetime = Field(nullable=False)
    revoked: bool = Field(default=False)
    created_at: datetime = Field(default_factory=_utcnow, nullable=False)

    # Many refresh tokens → one user
    user: "User" = Relationship(back_populates="refresh_tokens")
