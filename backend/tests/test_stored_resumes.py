"""Tests for stored resume endpoints.

Coverage
--------
- Upload a PDF resume → 201.
- Upload non-PDF → 415.
- List resumes → 200 with items.
- Delete resume → 200.
- Delete nonexistent → 404.
- Unauthenticated → 401.
"""

from __future__ import annotations

import io
import uuid

import pytest
from httpx import AsyncClient

from tests.conftest import register_and_login

pytestmark = pytest.mark.asyncio

_FAKE_PDF = b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\nstartxref\n0\n%%EOF"


def _auth_headers(tokens: dict) -> dict:
    return {"Authorization": f"Bearer {tokens['access_token']}"}


async def test_upload_resume(client: AsyncClient) -> None:
    """Upload a valid PDF → 201 with resume metadata."""
    tokens = await register_and_login(client, "stored1@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/resumes/upload",
        headers=_auth_headers(tokens),
        files={"file": ("my_resume.pdf", io.BytesIO(_FAKE_PDF), "application/pdf")},
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["success"] is True
    assert body["data"]["original_filename"] == "my_resume.pdf"
    assert "id" in body["data"]


async def test_upload_non_pdf(client: AsyncClient) -> None:
    """Upload a .txt file → 415."""
    tokens = await register_and_login(client, "stored2@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/resumes/upload",
        headers=_auth_headers(tokens),
        files={"file": ("resume.txt", io.BytesIO(b"text"), "text/plain")},
    )
    assert resp.status_code == 415


async def test_list_resumes(client: AsyncClient) -> None:
    """List resumes returns uploaded items."""
    tokens = await register_and_login(client, "stored3@test.com", "securepass1")

    # Upload one first
    await client.post(
        "/api/v1/resumes/upload",
        headers=_auth_headers(tokens),
        files={"file": ("test.pdf", io.BytesIO(_FAKE_PDF), "application/pdf")},
    )

    resp = await client.get(
        "/api/v1/resumes/",
        headers=_auth_headers(tokens),
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert len(body["data"]) >= 1


async def test_delete_resume(client: AsyncClient) -> None:
    """Delete an existing resume → 200."""
    tokens = await register_and_login(client, "stored4@test.com", "securepass1")

    # Upload first
    upload_resp = await client.post(
        "/api/v1/resumes/upload",
        headers=_auth_headers(tokens),
        files={"file": ("del.pdf", io.BytesIO(_FAKE_PDF), "application/pdf")},
    )
    resume_id = upload_resp.json()["data"]["id"]

    resp = await client.delete(
        f"/api/v1/resumes/{resume_id}",
        headers=_auth_headers(tokens),
    )
    assert resp.status_code == 200
    assert resp.json()["success"] is True


async def test_delete_nonexistent(client: AsyncClient) -> None:
    """Delete a resume that doesn't exist → 404."""
    tokens = await register_and_login(client, "stored5@test.com", "securepass1")
    fake_id = str(uuid.uuid4())
    resp = await client.delete(
        f"/api/v1/resumes/{fake_id}",
        headers=_auth_headers(tokens),
    )
    assert resp.status_code == 404


async def test_upload_unauthenticated(client: AsyncClient) -> None:
    """No auth header → 401."""
    resp = await client.post(
        "/api/v1/resumes/upload",
        files={"file": ("resume.pdf", io.BytesIO(_FAKE_PDF), "application/pdf")},
    )
    assert resp.status_code == 401


async def test_list_unauthenticated(client: AsyncClient) -> None:
    """No auth header on list → 401."""
    resp = await client.get("/api/v1/resumes/")
    assert resp.status_code == 401
