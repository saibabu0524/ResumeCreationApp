"""Stored resumes routes — manage user's base resume library.

POST   /resumes/upload       — Upload a PDF to the resume library.
GET    /resumes/             — List user's stored resumes.
DELETE /resumes/{resume_id}  — Remove a stored resume.
"""

from __future__ import annotations

import uuid
from pathlib import Path

from fastapi import APIRouter, File, HTTPException, Request, UploadFile, status

from app.api.deps import CurrentUser, DbSession
from app.core.config import get_settings
from app.core.limiter import limiter
from app.crud.stored_resume import (
    create_stored_resume,
    delete_stored_resume,
    get_user_resumes,
)
from app.schemas.common import ApiResponse, MessageResponse
from app.schemas.stored_resume import StoredResumePublic

router = APIRouter(prefix="/resumes", tags=["resumes"])


@router.post(
    "/upload",
    response_model=ApiResponse[StoredResumePublic],
    status_code=status.HTTP_201_CREATED,
    summary="Upload a PDF resume to the library",
)
@limiter.limit("20/minute")
async def upload_resume(
    request: Request,
    current_user: CurrentUser,
    db: DbSession,
    file: UploadFile = File(..., description="PDF resume file"),
) -> ApiResponse[StoredResumePublic]:
    """Upload a base resume for later reuse in tailoring and ATS analysis."""
    settings = get_settings()
    filename = file.filename or ""

    if not filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail="Upload must be a PDF file.",
        )

    contents = await file.read()
    if len(contents) > settings.max_upload_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"File exceeds the {settings.MAX_UPLOAD_SIZE_MB} MB limit.",
        )

    stored_name = f"stored_{uuid.uuid4()}.pdf"
    dest = Path(settings.UPLOAD_DIR) / stored_name
    dest.write_bytes(contents)

    resume = await create_stored_resume(
        db,
        user_id=current_user.id,
        original_filename=filename,
        stored_filename=stored_name,
        file_size_bytes=len(contents),
    )
    await db.commit()
    await db.refresh(resume)

    return ApiResponse(
        data=StoredResumePublic.model_validate(resume),
        message="Resume uploaded successfully.",
    )


@router.get(
    "/",
    response_model=ApiResponse[list[StoredResumePublic]],
    summary="List stored resumes",
)
async def list_resumes(
    current_user: CurrentUser,
    db: DbSession,
) -> ApiResponse[list[StoredResumePublic]]:
    """Return all resumes in the user's library, newest first."""
    resumes = await get_user_resumes(db, current_user.id)
    return ApiResponse(
        data=[StoredResumePublic.model_validate(r) for r in resumes],
        message=f"Found {len(resumes)} resume(s).",
    )


@router.delete(
    "/{resume_id}",
    response_model=MessageResponse,
    summary="Delete a stored resume",
)
async def remove_resume(
    resume_id: uuid.UUID,
    current_user: CurrentUser,
    db: DbSession,
) -> MessageResponse:
    """Remove a resume from the user's library and delete the file."""
    from app.crud.stored_resume import get_stored_resume as get_resume

    resume = await get_resume(db, resume_id, current_user.id)
    if resume is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Resume not found.",
        )

    # Delete the file from disk
    settings = get_settings()
    file_path = Path(settings.UPLOAD_DIR) / resume.stored_filename
    if file_path.exists():
        file_path.unlink()

    deleted = await delete_stored_resume(db, resume_id, current_user.id)
    await db.commit()

    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Resume not found.",
        )

    return MessageResponse(message="Resume deleted successfully.")
