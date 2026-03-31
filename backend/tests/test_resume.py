"""Integration tests for the resume tailoring endpoints.

Coverage
--------
- POST /api/v1/resume/tailor:
  - Unauthenticated request → 401.
  - Non-PDF file upload → 415.
  - Happy path: enqueues a job and returns 202 + job_id.
  - Missing job_description → 422.
- GET /api/v1/resume/jobs/{job_id}:
  - Known job → returns status.
  - Unknown job → 404.
- GET /api/v1/resume/jobs/{job_id}/download:
  - Job not completed → 409.
"""

from __future__ import annotations

import io
import uuid
from unittest.mock import AsyncMock, MagicMock, patch

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


def _mock_arq_pool() -> MagicMock:
    """Create a mock ARQ pool whose enqueue_job returns a fake job."""
    pool = AsyncMock()
    pool.enqueue_job = AsyncMock(return_value=MagicMock(job_id="fake"))
    return pool


# --- Tests — POST /resume/tailor ─────────────────────────────────────────────


async def test_tailor_unauthenticated(client: AsyncClient) -> None:
    """No Authorization header → 401."""
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


async def test_tailor_happy_path_enqueues_job(client: AsyncClient) -> None:
    """Happy path with Gemini provider returns 202 + job_id."""
    tokens = await register_and_login(client, "tailor2@test.com", "securepass1")

    mock_pool = _mock_arq_pool()
    client._transport.app.state.arq_pool = mock_pool  # type: ignore[union-attr]

    resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        data=_tailor_data(provider="gemini"),
    )

    assert resp.status_code == 202
    body = resp.json()
    assert body["data"]["status"] == "queued"
    assert "job_id" in body["data"]
    mock_pool.enqueue_job.assert_awaited_once()


async def test_tailor_happy_path_ollama(client: AsyncClient) -> None:
    """Happy path with Ollama provider returns 202 + job_id."""
    tokens = await register_and_login(client, "tailor3@test.com", "securepass1")

    mock_pool = _mock_arq_pool()
    client._transport.app.state.arq_pool = mock_pool  # type: ignore[union-attr]

    resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        data=_tailor_data(provider="ollama"),
    )

    assert resp.status_code == 202
    body = resp.json()
    assert body["data"]["status"] == "queued"


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


# --- Tests — GET /resume/jobs/{job_id} ────────────────────────────────────────


async def test_job_status_not_found(client: AsyncClient) -> None:
    """Unknown job_id → 404."""
    tokens = await register_and_login(client, "poll1@test.com", "securepass1")
    fake_id = str(uuid.uuid4())
    resp = await client.get(
        f"/api/v1/resume/jobs/{fake_id}",
        headers=_auth_headers(tokens),
    )
    assert resp.status_code == 404


async def test_job_status_after_submit(client: AsyncClient) -> None:
    """After submitting a job, polling returns 'queued' status."""
    tokens = await register_and_login(client, "poll2@test.com", "securepass1")

    mock_pool = _mock_arq_pool()
    client._transport.app.state.arq_pool = mock_pool  # type: ignore[union-attr]

    submit_resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        data=_tailor_data(),
    )
    job_id = submit_resp.json()["data"]["job_id"]

    status_resp = await client.get(
        f"/api/v1/resume/jobs/{job_id}",
        headers=_auth_headers(tokens),
    )
    assert status_resp.status_code == 200
    assert status_resp.json()["data"]["status"] == "queued"


# --- Tests — GET /resume/jobs/{job_id}/download ──────────────────────────────


async def test_download_not_completed(client: AsyncClient) -> None:
    """Downloading before job is completed → 409."""
    tokens = await register_and_login(client, "dl1@test.com", "securepass1")

    mock_pool = _mock_arq_pool()
    client._transport.app.state.arq_pool = mock_pool  # type: ignore[union-attr]

    submit_resp = await client.post(
        "/api/v1/resume/tailor",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        data=_tailor_data(),
    )
    job_id = submit_resp.json()["data"]["job_id"]

    dl_resp = await client.get(
        f"/api/v1/resume/jobs/{job_id}/download",
        headers=_auth_headers(tokens),
    )
    assert dl_resp.status_code == 409
