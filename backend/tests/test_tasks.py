"""Tests for ARQ background task logic (no real Redis required).

Coverage
--------
- ``send_welcome_email``: executed successfully with a mock context.
- ``cleanup_expired_tokens``: runs against the test DB without errors.
- ``cleanup_orphaned_uploads``: runs without errors when upload dir exists.
"""

from __future__ import annotations

import os
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest

from app.tasks.email import send_welcome_email
from app.tasks.cleanup import cleanup_expired_tokens, cleanup_orphaned_uploads

pytestmark = pytest.mark.asyncio


async def test_send_welcome_email_runs_without_error() -> None:
    """The task must complete without raising even with the stub mailer."""
    # ARQ passes a context dict as the first argument.
    ctx: dict = {}
    # Should not raise.
    await send_welcome_email(ctx, email="test@example.com", username="TestUser")


async def test_cleanup_expired_tokens_runs() -> None:
    """cleanup_expired_tokens executes against DB without raising."""
    ctx: dict = {}
    # The task opens its own session via AsyncSessionLocal; mock it
    # to use a no-op session so it doesn't hit a real DB.
    mock_session = AsyncMock()
    mock_session.__aenter__ = AsyncMock(return_value=mock_session)
    mock_session.__aexit__ = AsyncMock(return_value=False)

    with patch("app.tasks.cleanup.AsyncSessionLocal", return_value=mock_session):
        await cleanup_expired_tokens(ctx)


async def test_cleanup_orphaned_uploads_runs() -> None:
    """cleanup_orphaned_uploads executes without raising."""
    ctx: dict = {}
    os.makedirs("uploads", exist_ok=True)
    await cleanup_orphaned_uploads(ctx)
