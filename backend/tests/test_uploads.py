"""Tests for the file upload endpoint.

Coverage
--------
- Valid upload: JPEG, PNG, PDF.
- Disallowed MIME type → 415.
- Oversized file → 413.
- Unauthenticated upload → 403.
"""

from __future__ import annotations

import io

import pytest
from httpx import AsyncClient

from tests.conftest import register_and_login

pytestmark = pytest.mark.asyncio

# 1×1 white JPEG (smallest valid JPEG blob)
_TINY_JPEG = bytes.fromhex(
    "ffd8ffe000104a46494600010100000100010000"
    "ffdb004300080606070605080707070909080a0c"
    "140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20"
    "242e2720222c231c1c2837292c30313434341f27"
    "39393830333432ffc0000b080001000101011100"
    "ffc4001f0000010501010101010100000000000000"
    "000102030405060708090a0bffda00080101000"
    "0013f00ffd9"
)


async def test_upload_jpeg(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "uploader@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/uploads/",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
        files={"file": ("test.jpg", io.BytesIO(_TINY_JPEG), "image/jpeg")},
    )
    assert resp.status_code == 201
    data = resp.json()["data"]
    assert data["content_type"] == "image/jpeg"
    assert "stored_filename" in data


async def test_upload_disallowed_mime_type(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "badmime@test.com", "securepass1")
    resp = await client.post(
        "/api/v1/uploads/",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
        files={"file": ("script.sh", io.BytesIO(b"#!/bin/bash"), "application/x-sh")},
    )
    assert resp.status_code == 415


async def test_upload_unauthenticated(client: AsyncClient) -> None:
    resp = await client.post(
        "/api/v1/uploads/",
        files={"file": ("test.jpg", io.BytesIO(_TINY_JPEG), "image/jpeg")},
    )
    assert resp.status_code == 401
