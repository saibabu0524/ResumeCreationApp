"""Shared FastAPI dependencies.

Import these into route handlers via ``Depends()``.

Example::

    @router.get("/users/me")
    async def me(current_user: User = Depends(get_current_active_user)):
        ...
"""

from __future__ import annotations

from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.security import decode_access_token
from app.crud.user import get_user_by_id
from app.db.session import get_db
from app.models.user import User

import uuid

_bearer = HTTPBearer(auto_error=True)

# ── Database ──────────────────────────────────────────────────────────────────

DbSession = Annotated[AsyncSession, Depends(get_db)]

# ── Authentication ────────────────────────────────────────────────────────────


async def get_current_user(
    db: DbSession,
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> User:
    """Decode the Bearer token and return the corresponding User.

    Raises ``401`` if the token is missing, invalid, or expired.
    Raises ``404`` if the user ID in the token no longer exists in the DB.
    """
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = decode_access_token(credentials.credentials)
        user_id_str: str | None = payload.get("sub")
        if user_id_str is None:
            raise credentials_exception
        token_type: str | None = payload.get("type")
        if token_type != "access":
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    try:
        user_id = uuid.UUID(user_id_str)
    except ValueError:
        raise credentials_exception

    user = await get_user_by_id(db, user_id)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found.",
        )
    return user


async def get_current_active_user(
    current_user: User = Depends(get_current_user),
) -> User:
    """Extend ``get_current_user`` to also enforce ``is_active``."""
    if not current_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Inactive user account.",
        )
    return current_user


async def get_current_superuser(
    current_user: User = Depends(get_current_active_user),
) -> User:
    """Only allow superusers through.  Use for admin-only endpoints."""
    if not current_user.is_superuser:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions.",
        )
    return current_user


# ── Convenience type aliases (use in route signatures) ────────────────────────

CurrentUser = Annotated[User, Depends(get_current_active_user)]
SuperUser = Annotated[User, Depends(get_current_superuser)]
