# lessons.md — Running Log of Mistakes Made and How They Were Fixed

> Add a new entry every time a non-obvious mistake is discovered and fixed in this project.
> Format: date, what went wrong, root cause, fix applied.
> This file helps avoid repeating the same mistakes in future sessions.

---

## Entry Template

```
## [YYYY-MM-DD] — Short title of the mistake
**What happened:** ...
**Root cause:** ...
**Fix:** ...
**Rule updated:** (which best-practices file was updated, if any)
```

---

## [2026-02-24] — CLAUDE.md was describing FastAPI rules, not Android rules

**What happened:** The `.claude/CLAUDE.md` file contained FastAPI/Python project rules
(SQLAlchemy, Alembic, ARQ, Ruff, Black, Docker) even though this is an Android Jetpack
Compose multi-module template project.

**Root cause:** The CLAUDE.md was initially bootstrapped from a backend project template
and was never updated to reflect the actual Android codebase.

**Fix:** Rewrote CLAUDE.md entirely for Android, covering:
- Kotlin/Compose/MVVM/Clean Architecture stack
- Module map (app, build-logic, core/*, feature/*)
- Layer separation rules
- Naming conventions
- ViewModel/State/Event patterns
- Testing rules
- Code style (Kotlin, 120 chars, Timber, no !!)

Also created the entire `.claude/best-practices/` folder with:
- `architecture.md` — module boundaries, convention plugins, layer responsibilities
- `compose-ui.md` — recomposition, state hoisting, side effects, navigation routes
- `viewmodel-state.md` — UDF pattern, StateFlow, Channel events, flat UiState
- `testing.md` — FakeXxx pattern, MainDispatcherRule, UseCase tests, screenshot tests
- `network-data.md` — Retrofit, Result<T>, offline-first, mappers, DataStore

**Rule updated:** All files created fresh from scratch in this session.

---

## [2026-02-24] — best-practices files duplicated content already in docs/ai/

**What happened:** The initial best-practices files were written as standalone references,
copying full code examples and explanations that already existed verbatim in:
- `docs/ai/PROJECT_MEMORY.md` (module map, ViewModel/UiState/Intent/Event templates, Result<T>)
- `docs/ai/CODING_STANDARDS.md` (mapper, repository, Flow, StateFlow, Hilt, Compose standards)
- `docs/ai/ARCHITECTURE_DECISIONS.md` (all 10 pitfalls with root cause + fix)

**Root cause:** Best-practices files were authored without first reading what was already in docs/ai/.

**Fix:** Trimmed all 5 files to be lean AI session hints:
- Removed any section fully covered in docs/ai — replaced with a `> ref:` pointer
- Kept only content that is NOT in docs/ai (checklists, UseCase test template,
  ApiResultCallAdapterFactory, OkHttp setup, DataStore singleton pattern,
  screenshot test commands, Side effects table, spacing token table)
- Added `> Full details: docs/ai/XxxFile.md §section` headers at top of each file

**Rule added:** Before expanding a best-practices file, grep docs/ai/ first.

---

## [2025-07-11] — Backend ↔ Frontend API Contract Drift (13 mismatches)

**What happened:** After the backend FastAPI template was fully built, a full audit of
the Android Retrofit layer revealed 13 contract mismatches covering paths, verbs,
payloads, error codes, and auth flow.

**Root cause:** Backend and frontend were developed independently without a shared
contract document, and no incremental sync was done as backend routes evolved.

**Key mismatches fixed:**
1. `AuthApi` login: `@FormUrlEncoded @POST("login")` with `username` field
   → `@Body LoginRequestDto` (JSON) with `email` field, path `"auth/login"`
2. All `AuthApi` paths were missing the `auth/` prefix (`"register"` → `"auth/register"`)
3. `TokenResponseDto` was missing `refreshToken` — only `accessToken` was saved to storage
4. `UserResponseDto.id` was typed `Int` instead of `String` (UUID)
5. No `ApiResponseDto<T>` envelope — all service functions returned raw DTOs instead of
   the `{data, message, success}` wrapper the backend sends on every response
6. `UserApi` used `GET/PUT users/{userId}` — backend only exposes `GET/PATCH users/me`
7. `Dispatchers.IO` was hardcoded in `AuthRepositoryImpl`, `ResumeRepository`
   → inject `@IoDispatcher CoroutineDispatcher`
8. `AuthRepositoryImpl` used Kotlin stdlib `Result` API (`.isSuccess`, `.getOrNull()`)
   instead of the project's sealed `DomainResult` type
9. No `logout()` or `refreshToken()` in `AuthRepository` interface or implementation
10. `ExceptionMapper` was missing HTTP 400, 409, 413, 415, 422, 429 mappings
11. `ResumeApi` path was `"tailor"` → should be `"resume/tailor"` (missing resource prefix)
12. `UserDto` had non-nullable `displayName: String`, `createdAt: Long`, `updatedAt: Long`
    — server can omit all three; crash on deserialization
13. `NetworkModule` was not providing `ResumeApi` or `UploadsApi` into the DI graph

**Fixes applied:**
- Created `ApiResponseDto.kt`, `UploadsApi.kt`, `UserUpdateRequestDto.kt`
- Added 5 new `AppException` subtypes: `BadRequest`, `Conflict`, `FileTooLarge`,
  `UnsupportedMediaType`, `RateLimited`
- Fully rewrote `AuthApi.kt`, `AuthRepositoryImpl.kt`, `UserApi.kt`
- Extended `AuthRepository.kt` with `logout()` and `refreshToken()`
- Made `UserDto` fields nullable with safe defaults in `UserMapper` (`?: ""`, `?: 0L`)
- Added `provideResumeApi()` and `provideUploadsApi()` in `NetworkModule.kt`

**Rule added:** After any backend route change, run a diff of all Retrofit service
interfaces against the OpenAPI spec or route file before the PR is merged.

---

## [YYYY-MM-DD] — Add new entries below as mistakes are discovered


## [2026-02-24] — Backend FastAPI template created from scratch

**What happened:** The `backend/` directory only contained a `README.md` describing the
intended production FastAPI template.  No actual Python code existed.

**Fix:** Built the complete production FastAPI template matching the README spec:
- `app/core/` — `config.py` (pydantic-settings), `security.py` (JWT + bcrypt), `limiter.py` (slowapi)
- `app/db/` — async SQLAlchemy engine, session factory, `init_db` with table creation
- `app/models/` — `User` and `RefreshToken` SQLModel table classes
- `app/schemas/` — `auth.py` (request/response Pydantic schemas), `common.py` (ApiResponse envelope)
- `app/crud/` — async CRUD for users and refresh tokens; no business logic inside
- `app/api/v1/routes/` — `auth.py`, `users.py`, `uploads.py`; thin handlers delegating to crud
- `app/api/deps.py` — `get_current_user`, `get_current_active_user`, `get_current_superuser` with typed aliases
- `app/tasks/` — `email.py`, `cleanup.py` (plain async ARQ task functions)
- `app/workers/arq_worker.py` — `WorkerSettings` with registered functions, cron jobs, Redis config
- `app/main.py` — `create_app()` factory, lifespan (init_db + ARQ pool), CORS, rate limiting
- `tests/` — `conftest.py` (in-memory SQLite, DB override, AsyncClient fixture), `test_auth.py`, `test_users.py`, `test_uploads.py`, `test_tasks.py`
- `alembic/env.py`, `alembic/versions/0001_initial.py` — async-aware Alembic setup
- `pyproject.toml` — Ruff, Black, mypy, pytest config
- `Makefile`, `Dockerfile` (multi-stage), `docker-compose.yml` (api + worker + redis), `.env.example`, `.gitignore`

Also added backend-specific best-practices files:
- `backend-architecture.md`, `backend-api-patterns.md`, `backend-testing.md`, `backend-security.md`

Updated `CLAUDE.md` to reference the four new backend best-practices files.

**Rule added:** Backend rules now live in `best-practices/backend-*.md`, not just in `backend/README.md`.

