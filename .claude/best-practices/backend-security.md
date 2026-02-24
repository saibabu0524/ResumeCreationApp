# backend-security.md — Security Patterns for the FastAPI Backend

> **Full details:** `backend/README.md` §Authentication, §Rate Limiting, §File Uploads

---

## Token Security

| Rule | Implementation |
|---|---|
| Access tokens are short-lived | `ACCESS_TOKEN_EXPIRE_MINUTES=15` (default) |
| Refresh tokens stored as a hash only | `hash_refresh_token(raw)` → SHA-256 hex stored in DB |
| Token rotation on every refresh | Old token revoked, new pair issued atomically |
| Logout revokes the refresh token immediately | `revoke_token(db, token)` |
| Password change revokes ALL tokens for the user | `revoke_all_user_tokens(db, user_id)` |

**Never:**
- Store a raw refresh token in the database.
- Issue a new access token from a token with `"type": "refresh"` — validate `payload["type"] == "access"`.
- Return `hashed_password` in any response schema.

---

## Password Rules

```python
# ✅ Always hash with bcrypt via core/security.py
hashed = hash_password(plain_password)

# ✅ Always verify with constant-time compare
is_valid = verify_password(plain_password, stored_hash)

# ❌ Never compare passwords directly
if user.hashed_password == plain_password:  # FORBIDDEN
```

Minimum password length enforced at the schema level: `Field(min_length=8)`.

---

## Rate Limiting

- Auth endpoints (`/register`, `/login`): `20/minute` per IP.
- Refresh / logout: `30/minute` per IP.
- Global default: `100/minute` per IP (applied automatically by SlowAPI middleware).
- In production, consider switching the key function from IP address to user ID for
  authenticated endpoints so shared NAT/proxy IPs don't unfairly trigger limits.

---

## File Upload Security

1. **MIME type allowlist** — check `content_type` against `settings.ALLOWED_MIME_TYPES`.  Do NOT rely on the file extension alone.
2. **Size limit** — read the full content, then compare `len(contents) > settings.max_upload_bytes`.
3. **UUID filename** — stored filename is always `uuid4() + original_suffix` to prevent path traversal and name collisions.
4. **Path traversal prevention** — when serving files, always do `Path(filename).name` to strip any directory components before constructing the full path.

---

## Production Hardening Checklist

- [ ] `SECRET_KEY` is a random 32+ byte hex string (use `openssl rand -hex 32`).
- [ ] `ENVIRONMENT=production` disables `/docs`, `/redoc`, `/openapi.json`.
- [ ] `ALLOWED_ORIGINS` contains only your actual frontend domain(s).
- [ ] `DEBUG=false` so SQLAlchemy does not echo queries.
- [ ] The API runs as a non-root user inside Docker (see `Dockerfile`).
- [ ] `.env` is in `.gitignore` and never committed.
- [ ] Alembic migrations are run via `alembic upgrade head` before the app starts, not via `SQLModel.metadata.create_all`.
