"""Async CRUD operations for the RefreshToken table.

Token rotation strategy
-----------------------
1. On login: create a new token.
2. On refresh: validate the existing token, revoke it, create a new pair.
3. On logout: revoke the supplied token.
4. Periodically (via a background task): hard-delete rows that are both
   expired AND revoked to keep the table small.
"""

from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta

from sqlalchemy import delete, select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.config import get_settings
from app.core.security import hash_refresh_token
from app.models.token import RefreshToken


async def create_refresh_token(
    session: AsyncSession,
    user_id: uuid.UUID,
    raw_token: str,
) -> RefreshToken:
    """Hash *raw_token* and persist a new RefreshToken row."""
    settings = get_settings()
    token = RefreshToken(
        user_id=user_id,
        token_hash=hash_refresh_token(raw_token),
        expires_at=datetime.now(UTC) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
    )
    session.add(token)
    await session.flush()
    return token


async def get_valid_token(session: AsyncSession, raw_token: str) -> RefreshToken | None:
    """Return the RefreshToken for *raw_token* if it is active and not expired."""
    token_hash = hash_refresh_token(raw_token)
    result = await session.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked.is_(False),
            RefreshToken.expires_at > datetime.now(UTC),
        )
    )
    return result.scalars().first()


async def revoke_token(session: AsyncSession, token: RefreshToken) -> None:
    """Mark *token* as revoked without deleting it (audit trail)."""
    token.revoked = True
    session.add(token)
    await session.flush()


async def revoke_all_user_tokens(session: AsyncSession, user_id: uuid.UUID) -> None:
    """Revoke every active token for *user_id* (e.g. on password change)."""
    result = await session.execute(
        select(RefreshToken).where(
            RefreshToken.user_id == user_id,
            RefreshToken.revoked.is_(False),
        )
    )
    for token in result.scalars().all():
        token.revoked = True
        session.add(token)
    await session.flush()


async def delete_expired_tokens(session: AsyncSession) -> int:
    """Hard-delete rows that are both expired and revoked.  Returns count deleted."""
    result = await session.execute(
        delete(RefreshToken).where(
            RefreshToken.revoked.is_(True),
            RefreshToken.expires_at < datetime.now(UTC),
        )
    )
    return result.rowcount
