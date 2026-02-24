"""Email background tasks.

These are plain ``async`` functions — ARQ calls them in the worker process.
They are NOT FastAPI routes.

To enqueue from a route handler::

    await request.app.state.arq_pool.enqueue_job(
        "send_welcome_email", email="user@example.com", username="Alice"
    )

For production, replace the ``_send_email`` stub with SendGrid, Resend, or
any SMTP client.
"""

from __future__ import annotations

import logging

logger = logging.getLogger(__name__)


async def send_welcome_email(ctx: dict, *, email: str, username: str) -> None:
    """Send a welcome email after successful registration.

    Parameters
    ----------
    ctx:
        ARQ context dict (contains the Redis pool, retry count, etc.).
    email:
        Recipient email address.
    username:
        Display name for the email body.
    """
    logger.info("Sending welcome email to %s", email)
    # TODO: Replace with real email client (SendGrid, Resend, SMTP).
    await _send_email(
        to=email,
        subject="Welcome!",
        body=f"Hi {username}, welcome aboard!",
    )
    logger.info("Welcome email sent to %s", email)


async def send_password_reset_email(
    ctx: dict,
    *,
    email: str,
    reset_link: str,
) -> None:
    """Send a password-reset link to *email*.

    Parameters
    ----------
    ctx:
        ARQ context dict.
    email:
        Recipient email address.
    reset_link:
        The fully-qualified URL the user should visit to reset their password.
    """
    logger.info("Sending password reset email to %s", email)
    await _send_email(
        to=email,
        subject="Reset your password",
        body=f"Click the link to reset your password: {reset_link}",
    )
    logger.info("Password reset email sent to %s", email)


# ── Private helpers ───────────────────────────────────────────────────────────


async def _send_email(*, to: str, subject: str, body: str) -> None:
    """Stub function — replace with a real email provider SDK."""
    # In production plug in your email provider here.
    logger.debug("(stub) to=%s subject=%s body=%s", to, subject, body)
