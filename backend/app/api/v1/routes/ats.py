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

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile, status

from app.api.deps import CurrentUser, DbSession
from app.core.limiter import limiter
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
    resume: UploadFile = File(..., description="PDF resume to analyse"),
    job_description: str = Form(..., description="Target job description text"),
    provider: str = Form("gemini", description="LLM provider: gemini | ollama | cloud"),
) -> ApiResponse[AtsResult]:
    """Analyse how well a PDF resume matches a job description for ATS systems.

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
    filename = resume.filename or ""
    if not filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail="Upload must be a PDF file.",
        )

    if not job_description.strip():
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Job description cannot be empty.",
        )

    try:
        pdf_bytes = await resume.read()
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
