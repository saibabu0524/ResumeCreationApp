"""User profile routes.

GET  /users/me      — Return the authenticated user's profile.
PATCH /users/me     — Update email and/or password.
DELETE /users/me    — Deactivate (soft-delete) the account.
GET  /users/        — (Superuser only) List all users.
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status
from sqlalchemy import select

from app.api.deps import CurrentUser, DbSession, SuperUser
from app.crud.token import revoke_all_user_tokens
from app.crud.user import deactivate_user, get_user_by_email, update_user
from app.models.user import User
from app.schemas.auth import UserPublic, UserUpdateRequest
from app.schemas.common import ApiResponse, MessageResponse

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=ApiResponse[UserPublic])
async def get_me(current_user: CurrentUser) -> ApiResponse[UserPublic]:
    """Return the currently authenticated user's public profile."""
    return ApiResponse(data=UserPublic.model_validate(current_user))


@router.patch("/me", response_model=ApiResponse[UserPublic])
async def update_me(
    payload: UserUpdateRequest,
    current_user: CurrentUser,
    db: DbSession,
) -> ApiResponse[UserPublic]:
    """Update the authenticated user's email and/or password.

    If the new email is already taken by another account, returns ``409``.
    If the password is changed, all existing refresh tokens are revoked to
    force re-authentication on other devices.
    """
    if payload.email is not None and payload.email.lower() != current_user.email:
        conflict = await get_user_by_email(db, payload.email)
        if conflict is not None:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Email already in use.",
            )

    updated = await update_user(db, current_user, payload)

    if payload.password is not None:
        await revoke_all_user_tokens(db, current_user.id)

    return ApiResponse(
        data=UserPublic.model_validate(updated),
        message="Profile updated.",
    )


@router.delete("/me", response_model=MessageResponse)
async def delete_me(
    current_user: CurrentUser,
    db: DbSession,
) -> MessageResponse:
    """Deactivate (soft-delete) the authenticated user's account.

    All refresh tokens are revoked immediately.
    """
    await revoke_all_user_tokens(db, current_user.id)
    await deactivate_user(db, current_user)
    return MessageResponse(message="Account deactivated.")


@router.get("/", response_model=ApiResponse[list[UserPublic]])
async def list_users(
    _: SuperUser,
    db: DbSession,
    page: int = 1,
    page_size: int = 20,
) -> ApiResponse[list[UserPublic]]:
    """List all users (superuser only).

    Supports simple offset-based pagination via ``page`` and ``page_size``.
    """
    offset = (page - 1) * page_size
    result = await db.execute(
        select(User).offset(offset).limit(page_size).order_by(User.created_at.desc())
    )
    users = result.scalars().all()
    return ApiResponse(
        data=[UserPublic.model_validate(u) for u in users],
        message=f"Page {page}.",
    )
