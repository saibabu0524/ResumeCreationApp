"""Resume tailoring route.

POST /resume/tailor
    Upload a PDF resume + job description.
    Returns a tailored PDF compiled from AI-generated LaTeX.

Flow
----
1. Validate the uploaded file is a PDF.
2. Delegate to ``tailor_resume_pipeline`` (services/resume.py).
3. Stream the compiled PDF back to the client.
4. Clean up the temporary working directory via a background task.
"""

from __future__ import annotations

import io
import os
import re
import shutil
import tempfile
import uuid
from pathlib import Path

from fastapi import APIRouter, BackgroundTasks, File, Form, HTTPException, Request, UploadFile, status
from fastapi.responses import StreamingResponse
from sqlmodel import select

from app.api.deps import CurrentUser, DbSession
from app.core.config import get_settings
from app.core.limiter import limiter
from app.services.resume import tailor_resume_pipeline
from app.models.resume import TailoredResume
from app.schemas.common import ApiResponse

router = APIRouter(prefix="/resume", tags=["resume"])


def _cleanup_temp_dir(dir_path: str) -> None:
    """Remove a temporary directory; silently ignores errors."""
    try:
        shutil.rmtree(dir_path)
    except Exception:  # noqa: BLE001
        pass


@router.post(
    "/tailor",
    summary="Tailor a PDF resume to a job description",
    status_code=status.HTTP_200_OK,
)
@limiter.limit("10/minute")
async def tailor_resume(
    request: Request,
    background_tasks: BackgroundTasks,
    current_user: CurrentUser,
    db: DbSession,
    resume: UploadFile = File(..., description="PDF resume to tailor"),
    job_description: str = Form(..., description="Target job description text"),
    provider: str = Form("gemini", description="LLM provider: gemini | ollama | cloud"),
) -> StreamingResponse:
    """Tailor a PDF resume to a specific job description.

    1. Extracts text from the uploaded PDF.
    2. Runs Stage A — structures the text into a LaTeX template via LLM.
    3. Runs Stage B — tailors the LaTeX to match the job description via LLM.
    4. Compiles the final LaTeX with ``pdflatex`` and streams back the PDF.

    Each LLM stage retries up to ``LLM_RETRY_ATTEMPTS`` times, feeding the
    compiler error log back to the model for self-correction on failure.
    """
    settings = get_settings()
    filename = resume.filename or ""
    if not filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail="Upload must be a PDF file.",
        )

    request_id = str(uuid.uuid4())
    work_dir = tempfile.mkdtemp(prefix=f"resume_{request_id}_")
    background_tasks.add_task(_cleanup_temp_dir, work_dir)

    try:
        pdf_bytes = await resume.read()

        await tailor_resume_pipeline(
            pdf_bytes=pdf_bytes,
            job_description=job_description,
            provider=provider,
            work_dir=work_dir,
            request_id=request_id,
        )

        pdf_path = os.path.join(work_dir, "resume.pdf")
        if not os.path.exists(pdf_path):
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="PDF not found after compilation.",
            )

        with open(pdf_path, "rb") as fh:
            pdf_data = fh.read()

        # Save original PDF to permanent uploads directory
        uploaded_stored_filename = f"original_{uuid.uuid4()}.pdf"
        uploaded_dest = Path(settings.UPLOAD_DIR) / uploaded_stored_filename
        uploaded_dest.write_bytes(pdf_bytes)

        # Save tailored PDF to permanent uploads directory
        stored_filename = f"tailored_{uuid.uuid4()}.pdf"
        dest = Path(settings.UPLOAD_DIR) / stored_filename
        dest.write_bytes(pdf_data)

        # Store metadata in DB
        tailored_resume = TailoredResume(
            user_id=current_user.id,
            job_description=job_description,
            provider=provider,
            original_filename=filename,
            stored_filename=stored_filename,
            uploaded_stored_filename=uploaded_stored_filename,
        )
        db.add(tailored_resume)
        await db.commit()
        await db.refresh(tailored_resume)

        safe_name = re.sub(r"[^\w.\-]", "_", filename)
        return StreamingResponse(
            io.BytesIO(pdf_data),
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="tailored_{safe_name}"'},
        )

    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001
        import logging
        logging.getLogger(__name__).error(
            "[%s] Unexpected error: %s", request_id, exc, exc_info=True
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error during processing.",
        ) from exc


@router.get(
    "/history",
    summary="Get user's tailored resumes history",
    response_model=ApiResponse[list[dict]],
)
async def get_history(
    current_user: CurrentUser,
    db: DbSession,
) -> ApiResponse[list[dict]]:
    """Get the history of tailored resumes for the current user."""
    result = await db.execute(
        select(TailoredResume).where(TailoredResume.user_id == current_user.id).order_by(TailoredResume.created_at.desc())
    )
    resumes = result.scalars().all()

    data = [
        {
            "id": r.id,
            "job_description": r.job_description,
            "provider": r.provider,
            "original_filename": r.original_filename,
            "stored_filename": r.stored_filename,
            "uploaded_stored_filename": r.uploaded_stored_filename,
            "created_at": r.created_at.isoformat(),
        }
        for r in resumes
    ]

    return ApiResponse(
        data=data,
        message="History retrieved successfully."
    )

