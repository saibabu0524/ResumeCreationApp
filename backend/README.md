# ⚡ FastAPI Production Template

A rock-solid, production-aware FastAPI starter with async SQLite, JWT auth, file uploads, background tasks, rate limiting, and full Docker support. Clone → configure → ship.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Core Components](#-core-components)
- [Authentication](#-authentication--jwt)
- [Database & Migrations](#-database--migrations)
- [Rate Limiting](#-rate-limiting)
- [File Uploads](#-file-uploads)
- [Background Tasks](#-background-tasks--arq)
- [Testing](#-testing)
- [Linting & Formatting](#-linting--formatting)
- [Docker](#-docker)
- [API Reference](#-api-reference)
- [Dependencies](#-dependencies)

---

## 🛠 Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Framework | FastAPI + Uvicorn | Async-native, fast, auto-docs |
| ORM / Schemas | SQLModel | Single model for ORM + Pydantic validation |
| Database | SQLite (async via aiosqlite) | Zero-config, file-based, async-ready |
| Migrations | Alembic | Schema versioning without manual SQL |
| Auth | JWT (python-jose) + bcrypt (passlib) | Stateless access + hashed refresh tokens |
| Rate Limiting | slowapi | FastAPI-compatible, per-route or global |
| Background Tasks | ARQ + Redis | Async-native, lightweight alternative to Celery |
| File Uploads | python-multipart | Streaming multipart support |
| Testing | pytest + pytest-asyncio + httpx | Async-aware, in-memory test DB |
| Linting | Ruff + Black | Fast, unified linting + opinionated formatting |

---

## 📁 Project Structure

```
project/
├── app/
│   ├── api/
│   │   ├── v1/
│   │   │   ├── routes/
│   │   │   │   ├── auth.py          # Register, login, refresh, logout
│   │   │   │   ├── users.py         # User profile, admin endpoints
│   │   │   │   └── uploads.py       # File upload + metadata
│   │   │   └── router.py            # v1 router aggregator
│   │   └── deps.py                  # get_db, get_current_user, get_arq_pool
│   ├── core/
│   │   ├── config.py                # pydantic-settings Settings class
│   │   ├── security.py              # JWT create/verify, password hashing
│   │   └── limiter.py               # slowapi limiter instance
│   ├── db/
│   │   ├── session.py               # Async engine + AsyncSession factory
│   │   └── init_db.py               # DB init / optional seed script
│   ├── models/                      # SQLModel table models (ORM + schema in one)
│   │   ├── user.py                  # User table model
│   │   └── token.py                 # RefreshToken table model
│   ├── schemas/                     # Extra Pydantic schemas (non-table)
│   │   ├── auth.py                  # LoginRequest, TokenResponse, etc.
│   │   └── common.py                # Shared response envelopes, pagination
│   ├── crud/                        # Async DB operations (Create/Read/Update/Delete)
│   │   ├── user.py
│   │   └── token.py
│   ├── tasks/                       # ARQ task definitions
│   │   ├── email.py                 # send_welcome_email, send_reset_email
│   │   └── cleanup.py               # Stale token / file cleanup tasks
│   ├── workers/
│   │   └── arq_worker.py            # WorkerSettings: registered tasks, Redis pool, retries
│   └── main.py                      # App factory, middleware, lifespan, router mount
├── uploads/                         # Stored uploaded files (gitignored, auto-created)
├── tests/
│   ├── conftest.py                  # Fixtures: test client, isolated in-memory DB
│   ├── test_auth.py
│   ├── test_users.py
│   ├── test_uploads.py
│   └── test_tasks.py
├── alembic/
│   ├── env.py                       # Async-aware Alembic env using SQLModel metadata
│   └── versions/
├── .env                             # Local secrets (gitignored)
├── .env.example                     # Safe template committed to repo
├── alembic.ini
├── pyproject.toml                   # Ruff + Black config, project metadata
├── Makefile                         # Dev shortcuts: lint, test, migrate, run
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## 🚀 Getting Started

### Local (without Docker)

```bash
# 1. Clone and enter the project
git clone <repo-url> && cd <project>

# 2. Create a virtual environment
python -m venv .venv && source .venv/bin/activate

# 3. Install dependencies
pip install -e ".[dev]"

# 4. Copy and configure environment
cp .env.example .env

# 5. Run DB migrations
alembic upgrade head

# 6. Start the API server
uvicorn app.main:app --reload

# 7. (Optional) Start the ARQ worker in a separate terminal
arq app.workers.arq_worker.WorkerSettings
```

The API will be available at `http://localhost:8000`.  
Swagger docs: `http://localhost:8000/docs`  
ReDoc: `http://localhost:8000/redoc`

### With Docker

```bash
cp .env.example .env
docker-compose up --build
```

All three services (API, ARQ worker, Redis) start automatically.

---

## 🔧 Environment Variables

All settings live in a single `Settings` class (`core/config.py`) powered by `pydantic-settings`. Copy `.env.example` to `.env` and fill in your values.

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `sqlite+aiosqlite:///./db.sqlite3` | Async SQLite connection string |
| `SECRET_KEY` | — | Random 32+ char string for JWT signing |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | `15` | Short-lived access token lifetime |
| `REFRESH_TOKEN_EXPIRE_DAYS` | `7` | Long-lived refresh token lifetime |
| `ALLOWED_ORIGINS` | `["http://localhost:3000"]` | CORS allowed origins (comma-separated) |
| `UPLOAD_DIR` | `uploads/` | Path to store uploaded files |
| `MAX_UPLOAD_SIZE_MB` | `10` | Maximum allowed upload file size |
| `REDIS_URL` | `redis://localhost:6379` | Redis connection string (ARQ broker) |
| `RATE_LIMIT_DEFAULT` | `100/minute` | Global rate limit per IP |
| `ENVIRONMENT` | `development` | `development` or `production` |
| `LOG_LEVEL` | `info` | Uvicorn log level |

> **Tip:** Never commit your `.env`. It is gitignored. Only `.env.example` is committed.

---

## 🔩 Core Components

### App Factory (`main.py`)

The FastAPI instance is created in a factory pattern. On startup (via the `lifespan` context manager) it:
- Runs `init_db()` to ensure the `uploads/` directory exists
- Connects to the ARQ Redis pool and stores it on `app.state`
- Registers all routers under `/api/v1`
- Attaches middleware: CORS, slowapi rate limiter, and a global 429 exception handler

### Config (`core/config.py`)

A single `Settings` class inheriting from `BaseSettings`. All config is read from environment variables or `.env`. Accessed throughout the app via a cached `get_settings()` dependency — no global state.

### Database Session (`db/session.py`)

Creates an async SQLite engine using `aiosqlite` and provides an `AsyncSession` factory. The `get_db` dependency in `deps.py` yields a session per request and handles commit/rollback/close automatically.

---

## 🔐 Authentication — JWT

### Flow

1. **Register** — Creates a user, hashes the password with bcrypt.
2. **Login** — Verifies credentials, issues an access token (short-lived) and a refresh token (long-lived). The refresh token is hashed and stored in the `refresh_tokens` table.
3. **Refresh** — Client sends the refresh token. The server validates it against the DB hash, then rotates it (deletes old, issues new pair). This prevents token reuse attacks.
4. **Logout** — Marks the refresh token as revoked in the DB.
5. **Protected routes** — Use the `get_current_user` dependency in `deps.py`, which validates the Bearer token and returns the current user model.

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Create a new user account |
| POST | `/api/v1/auth/login` | Public | Get access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Refresh token | Rotate tokens |
| POST | `/api/v1/auth/logout` | Refresh token | Revoke refresh token |
| GET | `/api/v1/users/me` | Bearer | Get current user profile |

---

## 🗄 Database & Migrations

### SQLModel

SQLModel is used as the single source of truth for both the ORM layer and Pydantic request/response validation. Model classes annotated with `table=True` map directly to DB tables. No need to maintain a separate SQLAlchemy model and a Pydantic schema for the same entity — SQLModel handles both.

### Models

**User**

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `email` | str | Unique, indexed |
| `hashed_password` | str | bcrypt hash |
| `is_active` | bool | Default `True` |
| `is_superuser` | bool | Default `False` |
| `created_at` | datetime | Auto-set on insert |
| `updated_at` | datetime | Auto-updated |

**RefreshToken**

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | FK → users.id |
| `token_hash` | str | SHA-256 hash of the raw token |
| `expires_at` | datetime | Expiry timestamp |
| `revoked` | bool | Set to `True` on logout or rotation |
| `created_at` | datetime | Auto-set on insert |

### Alembic Migrations

Alembic is configured for async SQLite using SQLModel's metadata. The `env.py` is set up to use the async engine.

```bash
# Apply all migrations (run this on every new clone)
alembic upgrade head

# Create a new migration after changing a model
alembic revision --autogenerate -m "add column X to users"

# Downgrade one step
alembic downgrade -1

# Check current revision
alembic current
```

> **Never manually ALTER the SQLite file.** Always go through Alembic so the revision history stays clean.

---

## 🚦 Rate Limiting

Uses `slowapi`, a FastAPI-compatible wrapper around the `limits` library.

- A global default limit (e.g. `100/minute` per IP) is set in `core/limiter.py` and attached in `main.py`.
- Individual routes can override the limit with a decorator.
- A custom exception handler returns a consistent `429 Too Many Requests` JSON response instead of the default HTML error page.
- The rate limit key defaults to the client's IP address. This can be customized (e.g. by user ID for authenticated routes).

---

## 📂 File Uploads

The `/api/v1/uploads` endpoint accepts `multipart/form-data` with the following validations before writing to disk:

- **File type** — Checked against an allowlist of permitted MIME types (e.g. `image/png`, `image/jpeg`, `application/pdf`). Requests with disallowed types are rejected with `415 Unsupported Media Type`.
- **File size** — Validated against `MAX_UPLOAD_SIZE_MB` from config. Oversized files are rejected with `413 Request Entity Too Large`.
- **Filename** — The original filename is preserved in metadata but the file is saved with a UUID-based name to prevent path traversal and collisions.
- **Storage** — Files are saved to `UPLOAD_DIR`. The directory is gitignored but is created automatically on app startup.
- **Metadata** — Upload records (original filename, stored filename, MIME type, size, uploader user ID, timestamp) can be optionally persisted to the DB for per-project use.

---

## ⚙️ Background Tasks — ARQ

ARQ is chosen over Celery because it is async-native, has minimal boilerplate, and integrates naturally with an async FastAPI app. Redis is the task broker.

### Architecture

- **Task definitions** live in `app/tasks/`. Each task is a plain async function.
- **`arq_worker.py`** defines `WorkerSettings`: which tasks are registered, the Redis pool config, max retries, and job timeout.
- **Enqueueing** a task from any route: get the ARQ pool from `app.state` (stored on startup) and call `await pool.enqueue_job("task_name", arg1, arg2)`.
- The **worker process** is separate from the API process (see `docker-compose.yml`).

### Included Example Tasks

| Task | Description |
|---|---|
| `send_welcome_email` | Fires after user registration |
| `send_password_reset_email` | Sends reset link |
| `cleanup_expired_tokens` | Purges expired/revoked refresh tokens from DB |
| `cleanup_orphaned_uploads` | Removes upload files with no DB record |

---

## 🧪 Testing

Tests use `pytest` with `pytest-asyncio` and `httpx`'s `AsyncClient`.

### Strategy

- `conftest.py` creates a **separate in-memory SQLite database** for tests and overrides the `get_db` dependency — no real data is touched.
- Each test module gets a **function-scoped fixture** that resets the DB to a clean state, so tests are fully isolated and order-independent.
- A `TestClient` fixture wraps the FastAPI app via `httpx.AsyncClient`.

### Coverage

| File | What's tested |
|---|---|
| `test_auth.py` | Registration, login, token refresh, logout, duplicate email, bad credentials |
| `test_users.py` | Get profile, update profile, protected route requires valid token |
| `test_uploads.py` | Valid upload, oversized file, disallowed MIME type, unauthenticated upload |
| `test_tasks.py` | Task enqueue succeeds, task function logic (mocked Redis) |

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage report
pytest --cov=app --cov-report=term-missing

# Run a single file
pytest tests/test_auth.py -v
```

---

## 🧹 Linting & Formatting

Both tools are configured in `pyproject.toml`.

| Tool | Role |
|---|---|
| **Ruff** | Linting (replaces flake8, isort, pyupgrade, and more) |
| **Black** | Code formatting |

### Makefile Shortcuts

```bash
make lint       # Run Ruff + Black check (no changes)
make format     # Auto-fix with Ruff + Black
make test       # Run pytest
make migrate    # alembic upgrade head
make run        # uvicorn with --reload
make worker     # Start ARQ worker
```

---

## 🐳 Docker

### Dockerfile

Multi-stage build for a lean production image:

1. **Stage 1 (builder)** — Installs all dependencies into a virtual environment.
2. **Stage 2 (runtime)** — Copies only the venv and app code. Runs `uvicorn` on port `8000`.

### docker-compose.yml

Three services:

| Service | Description |
|---|---|
| `api` | FastAPI app — port `8000`, volume-mounted for hot-reload in dev |
| `worker` | ARQ worker — runs `arq app.workers.arq_worker.WorkerSettings` |
| `redis` | Official Redis image — broker for ARQ |

All services share a network and load from the same `.env` file.

```bash
# Start everything
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs for a specific service
docker-compose logs -f api

# Run migrations inside the container
docker-compose exec api alembic upgrade head

# Stop everything
docker-compose down
```

---

## 📡 API Reference

Interactive docs are auto-generated by FastAPI:

- **Swagger UI** → `http://localhost:8000/docs`
- **ReDoc** → `http://localhost:8000/redoc`
- **OpenAPI JSON** → `http://localhost:8000/openapi.json`

### Base URL

```
http://localhost:8000/api/v1
```

### Response Envelope

All responses follow a consistent envelope defined in `schemas/common.py`:

```
{
  "data": { ... },       # Payload
  "message": "...",      # Human-readable status
  "success": true|false
}
```

Errors follow FastAPI's default format with an added `detail` field.

---

## 📦 Dependencies

### Runtime

| Purpose | Package |
|---|---|
| Web framework | `fastapi`, `uvicorn[standard]` |
| ORM + Schemas | `sqlmodel` |
| Async SQLite driver | `aiosqlite` |
| Migrations | `alembic` |
| JWT | `python-jose[cryptography]` |
| Password hashing | `passlib[bcrypt]` |
| Config | `pydantic-settings` |
| Rate limiting | `slowapi` |
| Background tasks | `arq` |
| File uploads | `python-multipart` |

### Dev / Test

| Purpose | Package |
|---|---|
| Testing framework | `pytest`, `pytest-asyncio` |
| HTTP test client | `httpx` |
| Coverage | `pytest-cov` |
| Linting | `ruff` |
| Formatting | `black` |

---

## 🗺 Roadmap / Extending This Template

Some things intentionally left out so you can add per-project:

- Email sending integration (SendGrid, Resend, SMTP) — hook into the `send_welcome_email` task
- S3 / R2 file storage — swap the local file write in `uploads.py` for a boto3/s3 client call
- WebSocket support — add a `ws/` route module and register in `router.py`
- Admin panel — integrate `SQLAdmin` which works natively with SQLModel
- Observability — add `opentelemetry-sdk` instrumentation and export to your preferred backend
- CI/CD — add a GitHub Actions workflow running `make lint` and `make test` on PRs

---

## 📄 License

MIT — do whatever you want, just don't blame us.
