"""Async CRUD operations for the User table.

Rules
-----
- All functions are ``async`` and accept an ``AsyncSession`` parameter.
- Business logic does NOT belong here — move it to the API layer or a
  dedicated service function.
- Passwords are never compared here; use ``core.security.verify_password``.
"""

from __future__ import annotations

import uuid
from datetime import UTC, datetime

from sqlalchemy import select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.security import hash_password
from app.models.user import User
from app.schemas.auth import RegisterRequest, UserUpdateRequest


async def get_user_by_id(session: AsyncSession, user_id: uuid.UUID) -> User | None:
    """Return a user by primary key, or ``None`` if not found."""
    return await session.get(User, user_id)


async def get_user_by_email(session: AsyncSession, email: str) -> User | None:
    """Return a user by email address (case-insensitive), or ``None``."""
    result = await session.execute(
        select(User).where(User.email == email.lower().strip())
    )
    return result.scalars().first()


async def create_user(session: AsyncSession, payload: RegisterRequest) -> User:
    """Create and persist a new user.  Raises ``IntegrityError`` on duplicate email."""
    user = User(
        email=payload.email.lower().strip(),
        hashed_password=hash_password(payload.password),
    )
    session.add(user)
    await session.flush()  # Populate ``user.id`` before the caller needs it.
    return user


async def update_user(
    session: AsyncSession,
    user: User,
    payload: UserUpdateRequest,
) -> User:
    """Apply partial updates to *user* and persist."""
    if payload.email is not None:
        user.email = payload.email.lower().strip()
    if payload.password is not None:
        user.hashed_password = hash_password(payload.password)
    user.updated_at = datetime.now(UTC)
    session.add(user)
    await session.flush()
    return user


async def deactivate_user(session: AsyncSession, user: User) -> User:
    """Set ``is_active = False`` without deleting the row."""
    user.is_active = False
    user.updated_at = datetime.now(UTC)
    session.add(user)
    await session.flush()
    return user
