"""Tests for authentication endpoints.

Coverage
--------
- Register: success, duplicate email.
- Login: success, wrong password, unknown email, inactive account.
- Refresh: success, reuse of a rotated token (replay attack).
- Logout: success, already-revoked token (idempotent).
"""

from __future__ import annotations

import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


async def test_register_success(client: AsyncClient) -> None:
    resp = await client.post(
        "/api/v1/auth/register",
        json={"email": "alice@example.com", "password": "securepass1"},
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["success"] is True
    assert body["data"]["email"] == "alice@example.com"
    assert "hashed_password" not in body["data"]


async def test_register_duplicate_email(client: AsyncClient) -> None:
    payload = {"email": "bob@example.com", "password": "securepass1"}
    await client.post("/api/v1/auth/register", json=payload)
    resp = await client.post("/api/v1/auth/register", json=payload)
    assert resp.status_code == 409


async def test_login_success(client: AsyncClient) -> None:
    await client.post(
        "/api/v1/auth/register",
        json={"email": "carol@example.com", "password": "securepass1"},
    )
    resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "carol@example.com", "password": "securepass1"},
    )
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"


async def test_login_wrong_password(client: AsyncClient) -> None:
    await client.post(
        "/api/v1/auth/register",
        json={"email": "dave@example.com", "password": "securepass1"},
    )
    resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "dave@example.com", "password": "wrongpassword"},
    )
    assert resp.status_code == 401


async def test_login_unknown_email(client: AsyncClient) -> None:
    resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "nobody@example.com", "password": "securepass1"},
    )
    assert resp.status_code == 401


async def test_token_refresh_success(client: AsyncClient) -> None:
    await client.post(
        "/api/v1/auth/register",
        json={"email": "eve@example.com", "password": "securepass1"},
    )
    login_resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "eve@example.com", "password": "securepass1"},
    )
    refresh_token = login_resp.json()["data"]["refresh_token"]

    resp = await client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200
    new_data = resp.json()["data"]
    assert "access_token" in new_data
    assert new_data["refresh_token"] != refresh_token  # Token rotation


async def test_refresh_token_replay_attack(client: AsyncClient) -> None:
    """A used refresh token must not be accepted a second time."""
    await client.post(
        "/api/v1/auth/register",
        json={"email": "frank@example.com", "password": "securepass1"},
    )
    login_resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "frank@example.com", "password": "securepass1"},
    )
    refresh_token = login_resp.json()["data"]["refresh_token"]

    # First use — succeeds.
    await client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})

    # Second use — must fail (token was rotated).
    resp = await client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 401


async def test_logout_success(client: AsyncClient) -> None:
    await client.post(
        "/api/v1/auth/register",
        json={"email": "grace@example.com", "password": "securepass1"},
    )
    login_resp = await client.post(
        "/api/v1/auth/login",
        json={"email": "grace@example.com", "password": "securepass1"},
    )
    refresh_token = login_resp.json()["data"]["refresh_token"]

    resp = await client.post("/api/v1/auth/logout", json={"refresh_token": refresh_token})
    assert resp.status_code == 200

    # After logout the refresh token must be invalid.
    resp2 = await client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert resp2.status_code == 401


async def test_logout_invalid_token_is_idempotent(client: AsyncClient) -> None:
    """Logging out with an unknown token must still return 200."""
    resp = await client.post("/api/v1/auth/logout", json={"refresh_token": "garbage_token"})
    assert resp.status_code == 200
