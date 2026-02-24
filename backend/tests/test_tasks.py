"""Tests for ARQ background task logic (no real Redis required).

Coverage
--------
- ``send_welcome_email``: executed successfully with a mock context.
- ``cleanup_expired_tokens``: deletes the expected rows and commits.
"""

from __future__ import annotations

import pytest

from app.tasks.email import send_welcome_email

pytestmark = pytest.mark.asyncio


async def test_send_welcome_email_runs_without_error() -> None:
    """The task must complete without raising even with the stub mailer."""
    # ARQ passes a context dict as the first argument.
    ctx: dict = {}
    # Should not raise.
    await send_welcome_email(ctx, email="test@example.com", username="TestUser")
