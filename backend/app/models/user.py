"""User SQLModel table model.

SQLModel acts as both the ORM entity (``table=True``) and the Pydantic
validation schema for responses.  Avoid leaking ``hashed_password`` in
API responses — use ``UserPublic`` from ``schemas/auth.py`` instead.

Note: ``from __future__ import annotations`` is intentionally omitted.
With SQLModel 0.0.37 + SQLAlchemy 2.x, that import converts
``list["RefreshToken"]`` to the string ``"list['RefreshToken']"`` which the
ORM mapper cannot resolve as a relationship target.
"""

import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.token import RefreshToken


def _utcnow() -> datetime:
    return datetime.now(UTC)


class UserBase(SQLModel):
    """Shared fields used for input validation and the ORM row."""

    email: str = Field(unique=True, index=True, max_length=254)
    is_active: bool = Field(default=True)
    is_superuser: bool = Field(default=False)


class User(UserBase, table=True):
    """Database table model for a registered user."""

    __tablename__ = "users"

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        primary_key=True,
        index=True,
        nullable=False,
    )
    hashed_password: str
    created_at: datetime = Field(default_factory=_utcnow, nullable=False)
    updated_at: datetime = Field(default_factory=_utcnow, nullable=False)

    # One user → many refresh tokens
    refresh_tokens: list["RefreshToken"] = Relationship(back_populates="user")
