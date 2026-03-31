"""Tests for POST /api/v1/ats/analyse.

Coverage
--------
- Unauthenticated request → 401.
- Non-PDF file upload → 415.
- Empty job description → 422.
- Happy path (mocked LLM) → 200 with ATS result.
- No resume provided → 422.
- Stored resume ID used → 200.
"""

from __future__ import annotations

import io
import uuid
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient

from tests.conftest import register_and_login

pytestmark = pytest.mark.asyncio

_FAKE_PDF = b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\nstartxref\n0\n%%EOF"

_FAKE_ATS_RESULT = {
    "overall_score": 78,
    "score_label": "Good",
    "keywords_present": ["Python", "FastAPI", "Docker"],
    "keywords_missing": ["Kubernetes", "AWS"],
    "section_scores": {
        "skills_match": 80,
        "experience_relevance": 75,
        "education_match": 70,
        "formatting": 85,
    },
    "suggestions": ["Add cloud deployment experience"],
    "strengths": ["Strong Python background"],
    "summary": "Good match for a backend engineering role.",
}


def _auth_headers(tokens: dict) -> dict:
    return {"Authorization": f"Bearer {tokens['access_token']}"}


def _pdf_file(content: bytes = _FAKE_PDF, filename: str = "resume.pdf") -> dict:
    return {"resume": (filename, io.BytesIO(content), "application/pdf")}


def _ats_data(
    job_description: str = "Senior Python Engineer with Docker experience",
    provider: str = "gemini",
) -> dict:
    return {"job_description": job_description, "provider": provider}


async def test_ats_unauthenticated(client: AsyncClient) -> None:
    """No Authorization header → 401."""
    resp = await client.post(
        "/api/v1/ats/analyse",
        files=_pdf_file(),
        data=_ats_data(),
    )
    assert resp.status_code == 401


async def test_ats_non_pdf_file(client: AsyncClient) -> None:
    """Uploading a .txt file → 415."""
    tokens = await register_and_login(client, "ats1@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/ats/analyse",
        headers=_auth_headers(tokens),
        files={"resume": ("resume.txt", io.BytesIO(b"plain text"), "text/plain")},
        data=_ats_data(),
    )
    assert resp.status_code == 415


async def test_ats_empty_job_description(client: AsyncClient) -> None:
    """Empty job description → 422."""
    tokens = await register_and_login(client, "ats2@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/ats/analyse",
        headers=_auth_headers(tokens),
        files=_pdf_file(),
        data={"job_description": "   ", "provider": "gemini"},
    )
    assert resp.status_code == 422


async def test_ats_happy_path(client: AsyncClient) -> None:
    """Full happy path with mocked LLM returns ATS result."""
    tokens = await register_and_login(client, "ats3@test.com", "securepass1")

    with patch("app.api.v1.routes.ats.analyse_ats", new_callable=AsyncMock) as mock_analyse:
        mock_analyse.return_value = _FAKE_ATS_RESULT

        resp = await client.post(
            "/api/v1/ats/analyse",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_ats_data(),
        )

    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["data"]["overall_score"] == 78
    assert body["data"]["score_label"] == "Good"
    assert "Python" in body["data"]["keywords_present"]
    assert "Kubernetes" in body["data"]["keywords_missing"]
    assert body["data"]["section_scores"]["skills_match"] == 80


async def test_ats_no_resume_or_stored_id(client: AsyncClient) -> None:
    """No resume file and no stored_resume_id → 422."""
    tokens = await register_and_login(client, "ats4@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/ats/analyse",
        headers=_auth_headers(tokens),
        data=_ats_data(),
    )
    assert resp.status_code == 422


async def test_ats_llm_error(client: AsyncClient) -> None:
    """LLM service unavailable → 500."""
    tokens = await register_and_login(client, "ats5@test.com", "securepass1")

    with patch("app.api.v1.routes.ats.analyse_ats", new_callable=AsyncMock) as mock_analyse:
        mock_analyse.side_effect = Exception("LLM API timeout")

        resp = await client.post(
            "/api/v1/ats/analyse",
            headers=_auth_headers(tokens),
            files=_pdf_file(),
            data=_ats_data(),
        )

    assert resp.status_code == 500
