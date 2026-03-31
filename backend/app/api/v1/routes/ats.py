"""ATS analysis route.

POST /ats/analyse
    Upload a PDF resume + job description.
    Returns an ATS compatibility score, keywords analysis, and improvement suggestions.

Flow
----
1. Validate uploaded file is a PDF.
2. Delegate to ``analyse_ats`` (services/ats.py).
3. Return structured JSON result wrapped in ApiResponse.
"""

from __future__ import annotations

import uuid
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile, status

from app.api.deps import CurrentUser, DbSession
from app.core.config import get_settings
from app.core.limiter import limiter
from app.crud.stored_resume import get_stored_resume
from app.schemas.common import ApiResponse
from app.schemas.ats import AtsResult
from app.services.ats import analyse_ats

router = APIRouter(prefix="/ats", tags=["ats"])


@router.post(
    "/analyse",
    summary="Analyse resume ATS compatibility against a job description",
    response_model=ApiResponse[AtsResult],
    status_code=status.HTTP_200_OK,
)
@limiter.limit("20/minute")
async def analyse_resume_ats(
    request: Request,
    current_user: CurrentUser,
    db: DbSession,
    resume: Optional[UploadFile] = File(None, description="PDF resume to analyse"),
    job_description: str = Form(..., description="Target job description text"),
    provider: str = Form("gemini", description="LLM provider: gemini | ollama | cloud"),
    stored_resume_id: Optional[str] = Form(None, description="ID of a previously stored resume"),
) -> ApiResponse[AtsResult]:
    """Analyse how well a PDF resume matches a job description for ATS systems.

    Supply either a ``resume`` file upload OR a ``stored_resume_id`` referencing
    a previously uploaded resume from the user's library.

    Returns:
    - Overall ATS score (0-100)
    - Score label: Excellent / Good / Fair / Poor
    - Keywords present in resume that match JD
    - Keywords missing from resume that are in JD
    - Per-section scores: skills_match, experience_relevance, education_match, formatting
    - Actionable improvement suggestions
    - Strengths identified
    - Summary assessment
    """
    if not job_description.strip():
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Job description cannot be empty.",
        )

    # Resolve PDF bytes from either upload or stored resume
    settings = get_settings()
    if stored_resume_id:
        stored = await get_stored_resume(db, uuid.UUID(stored_resume_id), current_user.id)
        if stored is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Stored resume not found.",
            )
        stored_path = Path(settings.UPLOAD_DIR) / stored.stored_filename
        if not stored_path.exists():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Stored resume file missing from server.",
            )
        pdf_bytes = stored_path.read_bytes()
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

    try:
        result = await analyse_ats(
            pdf_bytes=pdf_bytes,
            job_description=job_description,
            provider=provider,
        )

        ats_result = AtsResult(**result)
        return ApiResponse(
            data=ats_result,
            message=f"ATS analysis complete. Score: {ats_result.overall_score}/100 ({ats_result.score_label})",
        )

    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001
        import logging
        logging.getLogger(__name__).error("ATS analysis error: %s", exc, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error during ATS analysis.",
        ) from exc
