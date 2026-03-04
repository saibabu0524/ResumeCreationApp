"""Auth routes: register, login, refresh, logout.

All routes apply tighter rate limits than the global default (20/minute
instead of 100/minute) to harden against brute-force attacks.
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request, status

from app.api.deps import DbSession
from app.core.limiter import limiter
from app.core.security import (
    create_access_token,
    generate_refresh_token,
    verify_password,
)
from app.crud.token import (
    create_refresh_token,
    get_valid_token,
    revoke_all_user_tokens,
    revoke_token,
)
from app.crud.user import create_user, get_user_by_email
from app.schemas.auth import (
    LoginRequest,
    LogoutRequest,
    RefreshRequest,
    RegisterRequest,
    TokenResponse,
    UserPublic,
)
from app.schemas.common import ApiResponse, MessageResponse

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=ApiResponse[UserPublic],
    status_code=status.HTTP_201_CREATED,
)
@limiter.limit("20/minute")
async def register(
    request: Request,
    payload: RegisterRequest,
    db: DbSession,
) -> ApiResponse[UserPublic]:
    """Create a new user account.

    Returns ``409`` if the email is already registered.
    """
    existing = await get_user_by_email(db, payload.email)
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists.",
        )
    user = await create_user(db, payload)
    return ApiResponse(
        data=UserPublic.model_validate(user),
        message="Account created successfully.",
    )


@router.post("/login", response_model=ApiResponse[TokenResponse])
@limiter.limit("20/minute")
async def login(
    request: Request,
    payload: LoginRequest,
    db: DbSession,
) -> ApiResponse[TokenResponse]:
    """Authenticate a user and return an access + refresh token pair.

    Returns ``401`` for invalid credentials.  The error message is deliberately
    generic to prevent user enumeration.
    """
    user = await get_user_by_email(db, payload.email)
    if user is None or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password.",
        )
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Account is disabled.",
        )

    access_token = create_access_token(str(user.id))
    raw_refresh = generate_refresh_token()
    await create_refresh_token(db, user.id, raw_refresh)

    return ApiResponse(
        data=TokenResponse(access_token=access_token, refresh_token=raw_refresh),
        message="Login successful.",
    )


@router.post("/refresh", response_model=ApiResponse[TokenResponse])
@limiter.limit("30/minute")
async def refresh(
    request: Request,
    payload: RefreshRequest,
    db: DbSession,
) -> ApiResponse[TokenResponse]:
    """Rotate a refresh token.

    Validates the supplied token, revokes it, and issues a new access + refresh
    pair.  Returns ``401`` if the token is expired, revoked, or unknown.
    """
    token = await get_valid_token(db, payload.refresh_token)
    if token is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired refresh token.",
        )

    # Revoke the old token (rotation).
    await revoke_token(db, token)

    access_token = create_access_token(str(token.user_id))
    raw_refresh = generate_refresh_token()
    await create_refresh_token(db, token.user_id, raw_refresh)

    return ApiResponse(
        data=TokenResponse(access_token=access_token, refresh_token=raw_refresh),
        message="Token refreshed.",
    )


@router.post("/logout", response_model=MessageResponse)
@limiter.limit("30/minute")
async def logout(
    request: Request,
    payload: LogoutRequest,
    db: DbSession,
) -> MessageResponse:
    """Revoke a refresh token.

    Always returns ``200`` regardless of whether the token was found,
    to avoid leaking token validity information.
    """
    token = await get_valid_token(db, payload.refresh_token)
    if token is not None:
        await revoke_token(db, token)
    return MessageResponse(message="Logged out.")
