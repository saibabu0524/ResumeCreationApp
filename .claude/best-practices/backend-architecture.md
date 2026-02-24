# backend-architecture.md — FastAPI Backend Architecture

> **Full stack details:** `backend/README.md`
> **This file:** Lean AI-session hints — patterns, banned combinations, checklists.

---

## Layer Map (Quick Reference)

```
app/
├── core/          → config, security, limiter  (pure Python, no DB/HTTP imports)
├── db/            → engine, session factory, init_db
├── models/        → SQLModel table=True classes (ORM + Pydantic in one)
├── schemas/       → Pydantic-only I/O schemas (request/response bodies that are NOT tables)
├── crud/          → async DB operations (read/write only, no business logic)
├── api/
│   ├── deps.py    → FastAPI Depends() helpers (get_db, get_current_user, ...)
│   └── v1/routes/ → thin route handlers (validate → delegate → return envelope)
├── tasks/         → ARQ async task functions (plain async def, no FastAPI imports)
├── workers/       → ARQ WorkerSettings (task registry, Redis config, cron jobs)
└── main.py        → app factory, middleware, lifespan, router mount
```

---

## Dependency Direction — Banned Combinations

| Banned | Why |
|---|---|
| `core/` imports from `api/`, `crud/`, `models/` | core is a pure utility layer |
| `crud/` contains business logic | crud is DB I/O only; logic goes in routes or a service layer |
| `models/` imports from `schemas/` | models are the source; schemas derive from them |
| `tasks/` imports FastAPI `Request` or `Depends` | tasks run in the worker process, not the API process |
| Route handler calls `engine` directly | always go through `get_db` dependency |
| Route handler imports `AsyncSessionLocal` | reserved for scripts and test conftest only |

---

## Adding a New Endpoint — Checklist

1. Define request/response schemas in `app/schemas/` if they don't already exist.
2. Add CRUD function(s) in `app/crud/` for any new DB operations.
3. Write the route function in `app/api/v1/routes/<module>.py`.
4. Register the router in `app/api/v1/router.py` if it's a new module.
5. Write a test in `tests/test_<module>.py`; cover happy path + at least one error case.
6. Apply `@limiter.limit(...)` on any mutation or auth endpoint.

---

## Adding a New Background Task — Checklist

1. Write `async def my_task(ctx: dict, *, ...)` in `app/tasks/`.
2. Add it to `WorkerSettings.functions` in `app/workers/arq_worker.py`.
3. For scheduled tasks, add a `cron(...)` entry to `WorkerSettings.cron_jobs`.
4. Enqueue from a route with `await request.app.state.arq_pool.enqueue_job("my_task", ...)`.
5. Test the task logic in `tests/test_tasks.py` using a plain context dict `{}`.

---

## AI-Specific Pitfalls

**Pitfall 1 — Putting business logic in the CRUD layer**
CRUD functions are DB I/O only.  Never add validation, branching, or error-raising logic
(other than database exceptions) inside `crud/`.  Move it to the route handler or a
dedicated service module.

**Pitfall 2 — Using sync SQLAlchemy in an async context**
Never call `session.execute(...)` without `await`.  Never use `create_engine` — use
`create_async_engine` from `sqlalchemy.ext.asyncio`.

**Pitfall 3 — Creating a new `Settings()` instance in every function**
`Settings()` reads `.env` from disk on every construction.  Always use `get_settings()`
which is `@lru_cache`-wrapped.

**Pitfall 4 — Exposing `hashed_password` in API responses**
`User` model has a `hashed_password` field.  Always return `UserPublic` from `schemas/auth.py`
in response bodies — never the raw `User` ORM object.

**Pitfall 5 — Comparing passwords directly**
Always use `verify_password(plain, hashed)` from `core/security.py`.  Never compare strings.

**Pitfall 6 — Storing raw refresh tokens in the DB**
Only the SHA-256 hash of a refresh token is stored.  Use `hash_refresh_token(raw)` before
any DB write.  The raw token is sent to the client once and never persisted server-side.
