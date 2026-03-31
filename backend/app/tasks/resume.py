"""ARQ background task — resume tailoring pipeline.

This task is enqueued by ``POST /resume/tailor`` and executes the full
LLM → LaTeX → pdflatex pipeline in the worker process, completely outside
the HTTP request/response cycle.  The API endpoint returns a ``job_id``
(the TailoredResume UUID) immediately, and the client polls
``GET /resume/jobs/{job_id}`` for progress.
"""

from __future__ import annotations

import logging
import shutil
import tempfile
import uuid
from pathlib import Path

from sqlmodel import select

from app.core.config import get_settings
from app.db.session import AsyncSessionLocal
from app.models.resume import TailoredResume
from app.services.resume import tailor_resume_pipeline

logger = logging.getLogger(__name__)


async def process_resume_tailor(
    ctx: dict,
    *,
    tailored_resume_id: str,
    pdf_input_path: str,
    job_description: str,
    provider: str,
) -> dict:
    """Run the full resume tailoring pipeline as a background ARQ task.

    Parameters
    ----------
    ctx:
        ARQ worker context (not used directly here).
    tailored_resume_id:
        UUID string of the ``TailoredResume`` DB record created by the route
        handler.  Used to update status throughout the job lifecycle.
    pdf_input_path:
        Absolute path to the original PDF saved by the route handler when the
        job was enqueued.
    job_description:
        Target job description text provided by the user.
    provider:
        LLM provider to use: ``"gemini"``, ``"ollama"``, or ``"cloud"``.
    """
    settings = get_settings()
    request_id = tailored_resume_id

    # ── Mark job as processing ────────────────────────────────────────────────
    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(TailoredResume).where(
                TailoredResume.id == uuid.UUID(tailored_resume_id)
            )
        )
        record = result.scalar_one_or_none()
        if record is None:
            logger.error("[%s] TailoredResume record not found — aborting.", request_id)
            return {"status": "failed", "error": "DB record not found"}

        record.status = "processing"
        db.add(record)
        await db.commit()

    # ── Run pipeline in a temporary working directory ─────────────────────────
    work_dir = tempfile.mkdtemp(prefix=f"resume_{request_id}_")
    try:
        pdf_bytes = Path(pdf_input_path).read_bytes()

        await tailor_resume_pipeline(
            pdf_bytes=pdf_bytes,
            job_description=job_description,
            provider=provider,
            work_dir=work_dir,
            request_id=request_id,
        )

        pdf_path = Path(work_dir) / "resume.pdf"
        if not pdf_path.exists():
            raise RuntimeError("PDF not found after compilation.")

        # Persist the tailored PDF.
        stored_filename = f"tailored_{uuid.uuid4()}.pdf"
        dest = Path(settings.UPLOAD_DIR) / stored_filename
        dest.write_bytes(pdf_path.read_bytes())

        # ── Mark job as completed ─────────────────────────────────────────────
        async with AsyncSessionLocal() as db:
            result = await db.execute(
                select(TailoredResume).where(
                    TailoredResume.id == uuid.UUID(tailored_resume_id)
                )
            )
            record = result.scalar_one_or_none()
            if record:
                record.status = "completed"
                record.stored_filename = stored_filename
                db.add(record)
                await db.commit()

        logger.info("[%s] Resume tailoring completed — %s", request_id, stored_filename)
        return {"status": "completed", "stored_filename": stored_filename}

    except Exception as exc:  # noqa: BLE001
        error_msg = str(exc)[:500]
        logger.error("[%s] Resume pipeline failed: %s", request_id, exc, exc_info=True)

        # ── Mark job as failed ────────────────────────────────────────────────
        async with AsyncSessionLocal() as db:
            result = await db.execute(
                select(TailoredResume).where(
                    TailoredResume.id == uuid.UUID(tailored_resume_id)
                )
            )
            record = result.scalar_one_or_none()
            if record:
                record.status = "failed"
                record.error_message = error_msg
                db.add(record)
                await db.commit()

        return {"status": "failed", "error": error_msg}

    finally:
        shutil.rmtree(work_dir, ignore_errors=True)
