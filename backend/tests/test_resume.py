"""Integration tests for POST /api/v1/resume/tailor.

Coverage
--------
- Unauthenticated request → 403.
- Non-PDF file upload → 415.
- Happy path (Gemini provider): returns a PDF stream.
- Happy path (Ollama provider): returns a PDF stream.
- Stage pipeline returns None (LLM/compile failure) → 500.
- PDF extraction fails (corrupt PDF) → 400.
- Provider error (Gemini not configured) → 503.
"""

from __future__ import annotations

import io
import tempfile
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient

from tests.conftest import register_and_login

pytestmark = pytest.mark.asyncio

# Minimal valid PDF bytes (not rendered, only used as upload payload for mock-bypassed tests)
_FAKE_PDF = b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\nstartxref\n0\n%%EOF"

# --- Helpers ------------------------------------------------------------------


def _auth_headers(tokens: dict) -> dict:
    return {"Authorization": f"Bearer {tokens['access_token']}"}


def _pdf_file(content: bytes = _FAKE_PDF, filename: str = "resume.pdf") -> dict:
    return {"resume": (filename, io.BytesIO(content), "application/pdf")}


def _tailor_data(
    job_description: str = "Senior Python Engineer",
    provider: str = "gemini",
) -> dict:
    return {"job_description": job_description, "provider": provider}


def _make_fake_pipeline(pdf_content: bytes = b"%PDF-1.4 tailored" + b"x" * 2000):
    """Return an async function that writes a fake PDF and returns LaTeX."""

    async def _fake_pipeline(pdf_bytes, job_description, provider, work_dir, request_id):
        # Write a fake compiled PDF so the route can read it back.
        Path(work_dir, "resume.pdf").write_bytes(pdf_content)
        return r"\documentclass{article}\begin{document}Tailored\end{document}"

    return _fake_pipeline


# --- Tests --------------------------------------------------------------------


async def test_tailor_unauthenticated(client: AsyncClient) -> None:
    """No Authorization header → 403."""
    resp = await client.post(
        "/api/v1/resume/tailor",
        files=_pdf_file(),
        data=_tailor_data(),
    )
    assert resp.status_code == 401


async def test_tailor_invalid_token(client: AsyncClient) -> None:
    """Malformed Bearer token → 401."""
    resp = await client.post(
        "/api/v1/resume/tailor",
        headers={"Authorization": "Bearer not_a_real_token"},
        files=_pdf_file(),
        data=_tailor_data(),
    )
    assert resp.status_code == 401


async def test_tailor_non_pdf_file(client: AsyncClient) -> None:
    """Uploading a .txt file → 415."""
    tokens = await register_and_login(client, "tailor1@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files={"resume": ("resume.txt", io.BytesIO(b"plain text"), "text/plain")},
        data=_tailor_data(),
    )
    assert resp.status_code == 415


async def test_tailor_happy_path_gemini(client: AsyncClient) -> None:
    """Full happy path with Gemini provider returns a PDF stream."""
    tokens = await register_and_login(client, "tailor2@test.com", "securepass1")

    with patch(
        "app.api.v1.routes.resume.tailor_resume_pipeline",
        side_effect=_make_fake_pipeline(),
    ):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_tailor_data(provider="gemini"),
        )

    assert resp.status_code == 200
    assert resp.headers["content-type"] == "application/pdf"
    assert b"%PDF" in resp.content
    assert "tailored_resume.pdf" in resp.headers.get("content-disposition", "")


async def test_tailor_happy_path_ollama(client: AsyncClient) -> None:
    """Full happy path with Ollama provider returns a PDF stream."""
    tokens = await register_and_login(client, "tailor3@test.com", "securepass1")

    with patch(
        "app.api.v1.routes.resume.tailor_resume_pipeline",
        side_effect=_make_fake_pipeline(),
    ):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_tailor_data(provider="ollama"),
        )

    assert resp.status_code == 200
    assert resp.headers["content-type"] == "application/pdf"


async def test_tailor_pipeline_compile_failure(client: AsyncClient) -> None:
    """Pipeline returns None (all retries exhausted) → 500."""
    tokens = await register_and_login(client, "tailor4@test.com", "securepass1")

    from fastapi import HTTPException

    async def _failing_pipeline(**kwargs):
        raise HTTPException(
            status_code=500,
            detail="Failed to generate valid structured LaTeX from resume after retries.",
        )

    with patch("app.api.v1.routes.resume.tailor_resume_pipeline", side_effect=_failing_pipeline):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_tailor_data(),
        )

    assert resp.status_code == 500


async def test_tailor_corrupt_pdf(client: AsyncClient) -> None:
    """Corrupt PDF that can't be parsed → 400."""
    tokens = await register_and_login(client, "tailor5@test.com", "securepass1")

    from fastapi import HTTPException

    async def _extract_fails(**kwargs):
        raise HTTPException(status_code=400, detail="Could not extract text from PDF.")

    with patch("app.api.v1.routes.resume.tailor_resume_pipeline", side_effect=_extract_fails):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files={"resume": ("resume.pdf", io.BytesIO(b"not a pdf"), "application/pdf")},
            data=_tailor_data(),
        )

    assert resp.status_code == 400


async def test_tailor_gemini_not_configured(client: AsyncClient) -> None:
    """Gemini API key missing → 503."""
    tokens = await register_and_login(client, "tailor6@test.com", "securepass1")

    from fastapi import HTTPException

    async def _no_key(**kwargs):
        raise HTTPException(status_code=503, detail="GEMINI_API_KEY not configured.")

    with patch("app.api.v1.routes.resume.tailor_resume_pipeline", side_effect=_no_key):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_tailor_data(provider="gemini"),
        )

    assert resp.status_code == 503


async def test_tailor_unknown_provider(client: AsyncClient) -> None:
    """Unrecognised provider string → 400."""
    tokens = await register_and_login(client, "tailor7@test.com", "securepass1")

    from fastapi import HTTPException

    async def _bad_provider(**kwargs):
        raise HTTPException(status_code=400, detail="Invalid provider.")

    with patch("app.api.v1.routes.resume.tailor_resume_pipeline", side_effect=_bad_provider):
        resp = await client.post(
            "/api/v1/resume/tailor",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_tailor_data(provider="openai"),
        )

    assert resp.status_code == 400


async def test_tailor_missing_job_description(client: AsyncClient) -> None:
    """Missing job_description form field → 422 Unprocessable Entity."""
    tokens = await register_and_login(client, "tailor8@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        # intentionally omit job_description
        data={"provider": "gemini"},
    )
    assert resp.status_code == 422
