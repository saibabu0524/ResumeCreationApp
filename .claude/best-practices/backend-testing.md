# backend-testing.md — FastAPI Testing Patterns

> **Full details:** `backend/README.md` §Testing

---

## Test Setup (Quick Reference)

```python
# ✅ Correct: use the function-scoped AsyncClient fixture from conftest.py
async def test_something(client: AsyncClient) -> None:
    resp = await client.post("/api/v1/auth/register", json={...})
    assert resp.status_code == 201

# ✅ Use register_and_login helper for authenticated tests
from tests.conftest import register_and_login

async def test_protected(client: AsyncClient) -> None:
    tokens = await register_and_login(client, "user@test.com", "securepass1")
    resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
    )
    assert resp.status_code == 200
```

---

## Rules

- All test functions are `async def` — `asyncio_mode = "auto"` in `pyproject.toml` handles this.
- Never use a shared global DB state between tests — the `db` fixture rolls back after each test.
- Never import `app` directly in tests — always use the `client` fixture which applies the DB override.
- Never hit a real database in CI — the in-memory SQLite in `conftest.py` is always used.
- Cover at minimum: happy path + one error/edge case per endpoint.

---

## Testing Background Tasks

Task functions are plain async functions — test them directly without ARQ:

```python
async def test_my_task() -> None:
    ctx: dict = {}  # ARQ context — empty dict is fine for unit tests
    await my_task(ctx, kwarg="value")  # Must not raise
```

For tasks that write to the DB, use the `db` fixture from conftest and pass the session to the task directly.

---

## Test File Layout

| File | What to test |
|---|---|
| `test_auth.py` | Register, login, refresh, logout, duplicates, bad creds, replay attacks |
| `test_users.py` | GET/PATCH/DELETE /users/me, superuser-only endpoints |
| `test_uploads.py` | Valid upload, bad MIME type, oversized file, unauthenticated |
| `test_tasks.py` | Task functions — no real Redis needed |

---

## Running Tests

```bash
make test          # Run all tests
make cov           # Tests + coverage report (must stay ≥ 80%)
pytest tests/test_auth.py -v  # Run a single file
```
