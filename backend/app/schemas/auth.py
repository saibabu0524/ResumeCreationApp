"""Pydantic schemas for authentication request/response bodies.

These are *not* table models — they exist purely for API I/O validation.
Never expose ``hashed_password`` in any of these schemas.
"""

from __future__ import annotations

import uuid

from pydantic import BaseModel, EmailStr, Field


# ── Request bodies ────────────────────────────────────────────────────────────


class RegisterRequest(BaseModel):
    """Payload expected by ``POST /auth/register``."""

    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    """Payload expected by ``POST /auth/login``."""

    email: EmailStr
    password: str


class RefreshRequest(BaseModel):
    """Payload expected by ``POST /auth/refresh``."""

    refresh_token: str


class LogoutRequest(BaseModel):
    """Payload expected by ``POST /auth/logout``."""

    refresh_token: str


# ── Response bodies ───────────────────────────────────────────────────────────


class TokenResponse(BaseModel):
    """Returned after a successful login or token rotation."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class UserPublic(BaseModel):
    """Safe user representation — never includes hashed_password."""

    id: uuid.UUID
    email: str
    is_active: bool
    is_superuser: bool

    model_config = {"from_attributes": True}


class UserUpdateRequest(BaseModel):
    """Partial update payload for ``PATCH /users/me``."""

    email: EmailStr | None = None
    password: str | None = Field(default=None, min_length=8, max_length=128)
