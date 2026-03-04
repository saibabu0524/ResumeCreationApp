"""File upload routes.

POST   /uploads/          — Upload a file (authenticated users only).
GET    /uploads/{filename} — Download a previously uploaded file.

Security
--------
- MIME type is checked against an allowlist, NOT just the file extension.
- File size is checked before writing to disk.
- The stored filename is UUID-based to prevent path traversal.
"""

from __future__ import annotations

import uuid
from pathlib import Path

from fastapi import APIRouter, File, HTTPException, UploadFile, status
from fastapi.responses import FileResponse

from app.api.deps import CurrentUser
from app.core.config import get_settings
from app.schemas.common import ApiResponse, MessageResponse

router = APIRouter(prefix="/uploads", tags=["uploads"])

_UPLOAD_METADATA: dict[str, dict] = {}  # In-memory for demo; replace with DB table.


@router.post("/", response_model=ApiResponse[dict], status_code=status.HTTP_201_CREATED)
async def upload_file(
    current_user: CurrentUser,
    file: UploadFile = File(...),
) -> ApiResponse[dict]:
    """Upload a file.

    Validates content type and size, then saves to ``UPLOAD_DIR`` under a
    UUID-based filename to prevent path traversal and name collisions.
    """
    settings = get_settings()

    # ── Validate MIME type ────────────────────────────────────────────────────
    if file.content_type not in settings.ALLOWED_MIME_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"File type '{file.content_type}' is not allowed.",
        )

    # ── Read & validate size ──────────────────────────────────────────────────
    contents = await file.read()
    if len(contents) > settings.max_upload_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"File exceeds the {settings.MAX_UPLOAD_SIZE_MB} MB limit.",
        )

    # ── Derive a safe stored filename ─────────────────────────────────────────
    suffix = Path(file.filename or "upload").suffix or ""
    stored_name = f"{uuid.uuid4()}{suffix}"
    dest = Path(settings.UPLOAD_DIR) / stored_name
    dest.write_bytes(contents)

    # Persist metadata (extend this to write to the DB for your project).
    _UPLOAD_METADATA[stored_name] = {
        "original_filename": file.filename,
        "stored_filename": stored_name,
        "content_type": file.content_type,
        "size_bytes": len(contents),
        "uploaded_by": str(current_user.id),
    }

    return ApiResponse(
        data=_UPLOAD_METADATA[stored_name],
        message="File uploaded successfully.",
    )


@router.get("/{filename}")
async def download_file(
    filename: str,
    current_user: CurrentUser,
) -> FileResponse:
    """Download an uploaded file by its stored filename.

    Only authenticated users may download files; extend this to add
    ownership checks if needed.
    """
    settings = get_settings()
    # Prevent path traversal: strip any directory components.
    safe_name = Path(filename).name
    path = Path(settings.UPLOAD_DIR) / safe_name
    if not path.exists() or not path.is_file():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found.")

    meta = _UPLOAD_METADATA.get(safe_name, {})
    media_type = meta.get("content_type", "application/octet-stream")
    return FileResponse(path=str(path), media_type=media_type, filename=safe_name)
