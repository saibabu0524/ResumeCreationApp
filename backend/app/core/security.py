"""JWT token creation/verification and password hashing utilities.

Rules
-----
- Never import this module from the domain/model layer.
- Always use ``verify_password`` — never compare plain-text passwords directly.
- Access tokens are short-lived (15 min).  Refresh tokens are long-lived (7 days)
  and are stored as a SHA-256 hash in the DB, never as plain text.
"""

from __future__ import annotations

import hashlib
import secrets
from datetime import UTC, datetime, timedelta
from typing import Any

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.core.config import get_settings

_pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# ── Password helpers ──────────────────────────────────────────────────────────


def hash_password(plain: str) -> str:
    """Return a bcrypt hash of *plain*."""
    return _pwd_context.hash(plain)


def verify_password(plain: str, hashed: str) -> bool:
    """Return ``True`` if *plain* matches *hashed*."""
    return _pwd_context.verify(plain, hashed)


# ── JWT helpers ───────────────────────────────────────────────────────────────


def create_access_token(subject: str | Any, expires_delta: timedelta | None = None) -> str:
    """Create a signed JWT access token.

    Parameters
    ----------
    subject:
        Typically a user ID string.  Stored in the ``sub`` claim.
    expires_delta:
        Override the default expiry from settings.
    """
    settings = get_settings()
    expire = datetime.now(UTC) + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    payload: dict[str, Any] = {"sub": str(subject), "exp": expire, "type": "access"}
    return jwt.encode(payload, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def decode_access_token(token: str) -> dict[str, Any]:
    """Decode and verify *token*.

    Returns the raw payload dict on success.

    Raises
    ------
    JWTError
        When the token is expired, tampered with, or otherwise invalid.
    """
    settings = get_settings()
    return jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])


# ── Refresh token helpers ─────────────────────────────────────────────────────


def generate_refresh_token() -> str:
    """Return a cryptographically secure random URL-safe string."""
    return secrets.token_urlsafe(64)


def hash_refresh_token(raw_token: str) -> str:
    """Return the SHA-256 hex digest of *raw_token* for DB storage."""
    return hashlib.sha256(raw_token.encode()).hexdigest()
