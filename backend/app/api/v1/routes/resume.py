"""Resume tailoring route — async job-queue pattern.

POST /resume/tailor
    Validate input, save the PDF, enqueue an ARQ background job, and return
    202 Accepted + job_id immediately.  The heavy LLM → pdflatex pipeline
    runs in the separate ``worker`` container so no HTTP timeout can occur.

GET /resume/jobs/{job_id}
    Poll for the status of a previously submitted tailoring job.
    Returns queued | processing | completed | failed.

GET /resume/jobs/{job_id}/download
    Stream the tailored PDF once the job has reached ``completed`` status.

GET /resume/history
    Return the list of all tailoring jobs submitted by the current user.
"""

from __future__ import annotations

import uuid
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile, status
from fastapi.responses import StreamingResponse
from sqlmodel import select

from app.api.deps import CurrentUser, DbSession
from app.core.config import get_settings
from app.core.limiter import limiter
from app.crud.stored_resume import get_stored_resume
from app.models.resume import TailoredResume
from app.schemas.common import ApiResponse

router = APIRouter(prefix="/resume", tags=["resume"])


# ── POST /resume/tailor ───────────────────────────────────────────────────────

@router.post(
    "/tailor",
    summary="Submit a resume-tailoring job",
    status_code=status.HTTP_202_ACCEPTED,
)
@limiter.limit("10/minute")
async def tailor_resume(
    request: Request,
    current_user: CurrentUser,
    db: DbSession,
    resume: Optional[UploadFile] = File(None, description="PDF resume to tailor"),
    job_description: str = Form(..., description="Target job description text"),
    provider: str = Form("gemini", description="LLM provider: gemini | ollama | cloud"),
    stored_resume_id: Optional[str] = Form(None, description="ID of a previously stored resume"),
) -> ApiResponse[dict]:
    """Enqueue a resume-tailoring job and return its ID immediately.

    The tailoring pipeline (LLM calls + pdflatex compilation) runs in the
    background worker.  Poll ``GET /resume/jobs/{job_id}`` for progress, then
    fetch the result from ``GET /resume/jobs/{job_id}/download``.
    """
    settings = get_settings()

    # ── Resolve PDF bytes ─────────────────────────────────────────────────────
    if stored_resume_id:
        stored = await get_stored_resume(db, uuid.UUID(stored_resume_id), current_user.id)
        if stored is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Stored resume not found.")
        stored_path = Path(settings.UPLOAD_DIR) / stored.stored_filename
        if not stored_path.exists():
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Stored resume file missing from server.")
        pdf_bytes = stored_path.read_bytes()
        filename = stored.original_filename
    elif resume is not None:
        filename = resume.filename or ""
        if not filename.lower().endswith(".pdf"):
            raise HTTPException(
                status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
                detail="Upload must be a PDF file.",
            )
        pdf_bytes = await resume.read()
    else:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Provide either a resume file or stored_resume_id.",
        )

    # ── Persist the original PDF so the worker can access it ─────────────────
    uploaded_stored_filename = f"original_{uuid.uuid4()}.pdf"
    uploaded_dest = Path(settings.UPLOAD_DIR) / uploaded_stored_filename
    uploaded_dest.write_bytes(pdf_bytes)

    # ── Create DB record (status = queued) ─────────────────────────────────────
    tailored_resume = TailoredResume(
        user_id=current_user.id,
        job_description=job_description,
        provider=provider,
        original_filename=filename,
        uploaded_stored_filename=uploaded_stored_filename,
        status="queued",
    )
    db.add(tailored_resume)
    await db.commit()
    await db.refresh(tailored_resume)

    # ── Enqueue ARQ background job ────────────────────────────────────────────
    arq_pool = request.app.state.arq_pool
    await arq_pool.enqueue_job(
        "process_resume_tailor",
        tailored_resume_id=str(tailored_resume.id),
        pdf_input_path=str(uploaded_dest),
        job_description=job_description,
        provider=provider,
        _job_id=str(tailored_resume.id),  # use the DB record ID as the ARQ job ID
    )

    return ApiResponse(
        data={
            "job_id": str(tailored_resume.id),
            "status": "queued",
        },
        message="Resume tailoring job queued. Poll /resume/jobs/{job_id} for status.",
    )


# ── GET /resume/jobs/{job_id} ─────────────────────────────────────────────────

@router.get(
    "/jobs/{job_id}",
    summary="Poll the status of a tailoring job",
)
async def get_resume_job_status(
    job_id: uuid.UUID,
    request: Request,
    current_user: CurrentUser,
    db: DbSession,
) -> ApiResponse[dict]:
    """Return the current status of a resume tailoring job.

    ``status`` is one of: ``queued``, ``processing``, ``completed``, ``failed``.

    When ``status == "completed"`` the response includes a ``download_url``
    pointing to ``GET /resume/jobs/{job_id}/download``.
    """
    result = await db.execute(
        select(TailoredResume).where(
            TailoredResume.id == job_id,
            TailoredResume.user_id == current_user.id,
        )
    )
    record = result.scalar_one_or_none()
    if record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found.")

    data: dict = {
        "job_id": str(record.id),
        "status": record.status,
        "provider": record.provider,
        "original_filename": record.original_filename,
        "created_at": record.created_at.isoformat(),
    }

    if record.status == "completed":
        data["download_url"] = f"/api/v1/resume/jobs/{record.id}/download"

    if record.status == "failed":
        data["error"] = record.error_message

    return ApiResponse(data=data, message=f"Job status: {record.status}.")


# ── GET /resume/jobs/{job_id}/download ────────────────────────────────────────

@router.get(
    "/jobs/{job_id}/download",
    summary="Download a completed tailored resume PDF",
)
async def download_tailored_resume(
    job_id: uuid.UUID,
    current_user: CurrentUser,
    db: DbSession,
) -> StreamingResponse:
    """Stream the tailored PDF for a completed job.

    Returns ``404`` if the job is not found or ``409`` if the job has not yet
    reached ``completed`` status.
    """
    settings = get_settings()

    result = await db.execute(
        select(TailoredResume).where(
            TailoredResume.id == job_id,
            TailoredResume.user_id == current_user.id,
        )
    )
    record = result.scalar_one_or_none()
    if record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found.")

    if record.status != "completed":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Job is not completed yet (current status: {record.status}).",
        )

    if not record.stored_filename:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="PDF path missing.")

    pdf_path = Path(settings.UPLOAD_DIR) / record.stored_filename
    if not pdf_path.exists():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="PDF file missing from server.")

    pdf_data = pdf_path.read_bytes()
    safe_name = record.original_filename.replace(" ", "_")
    return StreamingResponse(
        iter([pdf_data]),
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="tailored_{safe_name}"'},
    )


# ── GET /resume/history ───────────────────────────────────────────────────────

@router.get(
    "/history",
    summary="Get resume tailoring job history",
    response_model=ApiResponse[list[dict]],
)
@limiter.limit("30/minute")
async def get_resume_history(
    request: Request,
    current_user: CurrentUser,
    db: DbSession,
) -> ApiResponse[list[dict]]:
    """Get the history of tailored resumes for the current user."""
    result = await db.execute(
        select(TailoredResume)
        .where(TailoredResume.user_id == current_user.id)
        .order_by(TailoredResume.created_at.desc())
    )
    resumes = result.scalars().all()

    data = [
        {
            "id": str(r.id),
            "status": r.status,
            "job_description": r.job_description,
            "provider": r.provider,
            "original_filename": r.original_filename,
            "stored_filename": r.stored_filename,
            "uploaded_stored_filename": r.uploaded_stored_filename,
            "created_at": r.created_at.isoformat(),
            "download_url": f"/api/v1/resume/jobs/{r.id}/download" if r.status == "completed" else None,
            "error": r.error_message if r.status == "failed" else None,
        }
        for r in resumes
    ]

    return ApiResponse(data=data, message="History retrieved successfully.")

