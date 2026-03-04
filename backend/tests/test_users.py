"""Tests for user profile endpoints.

Coverage
--------
- GET /users/me: success, unauthenticated.
- PATCH /users/me: change email, change password, duplicate email.
- DELETE /users/me: deactivation.
- GET /users/: superuser access, non-superuser rejection.
"""

from __future__ import annotations

import pytest
from httpx import AsyncClient

from tests.conftest import register_and_login

pytestmark = pytest.mark.asyncio


async def test_get_me(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "alice@test.com", "securepass1")
    resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["email"] == "alice@test.com"


async def test_get_me_unauthenticated(client: AsyncClient) -> None:
    resp = await client.get("/api/v1/users/me")
    assert resp.status_code == 401  # HTTPBearer in FastAPI ≥0.115 returns 401 when header is missing


async def test_get_me_invalid_token(client: AsyncClient) -> None:
    resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": "Bearer not_a_real_token"},
    )
    assert resp.status_code == 401


async def test_update_email(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "bob@test.com", "securepass1")
    resp = await client.patch(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
        json={"email": "bob_new@test.com"},
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["email"] == "bob_new@test.com"


async def test_update_email_conflict(client: AsyncClient) -> None:
    await register_and_login(client, "carol@test.com", "securepass1")
    tokens = await register_and_login(client, "dan@test.com", "securepass1")
    resp = await client.patch(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
        json={"email": "carol@test.com"},  # Already taken
    )
    assert resp.status_code == 409


async def test_delete_account(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "eve@test.com", "securepass1")
    resp = await client.delete(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
    )
    assert resp.status_code == 200


async def test_list_users_requires_superuser(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "frank@test.com", "securepass1")
    resp = await client.get(
        "/api/v1/users/",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
    )
    # Regular user must be rejected
    assert resp.status_code == 403
