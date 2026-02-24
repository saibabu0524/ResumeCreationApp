"""ARQ worker settings.

Start the worker process::

    arq app.workers.arq_worker.WorkerSettings

The worker is separate from the API process.  See ``docker-compose.yml`` for
how they run side by side.

Adding a new task
-----------------
1. Define an ``async def my_task(ctx, *, ...)`` function in ``app/tasks/``.
2. Add it to the ``functions`` list in ``WorkerSettings`` below.
3. Enqueue it from any route with::

       await request.app.state.arq_pool.enqueue_job("my_task", kwarg=value)
"""

from __future__ import annotations

from arq import cron
from arq.connections import RedisSettings

from app.core.config import get_settings
from app.tasks.cleanup import cleanup_expired_tokens, cleanup_orphaned_uploads
from app.tasks.email import send_password_reset_email, send_welcome_email


async def startup(ctx: dict) -> None:
    """Runs once when the worker process starts."""
    pass


async def shutdown(ctx: dict) -> None:
    """Runs once when the worker process shuts down cleanly."""
    pass


_settings = get_settings()


class WorkerSettings:
    """ARQ WorkerSettings class — discovered by the ``arq`` CLI."""

    # ── Registered task functions ─────────────────────────────────────────────
    functions = [
        send_welcome_email,
        send_password_reset_email,
        cleanup_expired_tokens,
        cleanup_orphaned_uploads,
    ]

    # ── Scheduled (cron) tasks ────────────────────────────────────────────────
    cron_jobs = [
        # Run token cleanup every day at 03:00 UTC.
        cron(cleanup_expired_tokens, hour=3, minute=0),
        # Run orphaned-upload cleanup every day at 04:00 UTC.
        cron(cleanup_orphaned_uploads, hour=4, minute=0),
    ]

    # ── Redis ─────────────────────────────────────────────────────────────────
    redis_settings = RedisSettings.from_dsn(_settings.REDIS_URL)

    # ── Retry / timeout ───────────────────────────────────────────────────────
    max_tries = 3
    job_timeout = 300  # seconds
    keep_result = 3600  # seconds to retain job result in Redis

    # ── Lifecycle hooks ───────────────────────────────────────────────────────
    on_startup = startup
    on_shutdown = shutdown
