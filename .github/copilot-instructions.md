# GitHub Copilot Instructions

## Repo Overview
Monorepo with four components running under the domain `resumetailor.in`:
| Component | Path | Stack |
|---|---|---|
| Android app | `app/`, `core/`, `feature/`, `build-logic/` | Kotlin, Jetpack Compose, MVVM + Clean Arch |
| FastAPI backend | `backend/` | Python 3.12, FastAPI, SQLite (async), ARQ+Redis |
| Next.js frontend | `frontend/` | Next.js 16, React 19, TypeScript, Tailwind v4 |
| Play Store automation | `playstore-production-automation/` | Python, Fastlane |

**Before any session:** read every file in `.claude/best-practices/` — it contains project-specific patterns, banned combinations, and lessons learned.

---

## Architecture — Android

Strict multi-module Clean Architecture. Dependency direction is **one-way only**:

```
feature → core:domain ← core:data → core:network / core:database
    ↑                                       ↑
   :app (only module that sees all features)
```

- `core:domain` — **pure Kotlin JVM**, zero Android SDK imports, holds interfaces + UseCases + `Result<T>`
- `core:data` — implements domain interfaces; mappers live here (`XxxMapper.kt`)
- Feature modules **never import each other** — navigation callbacks bubble up to `:app` NavHost
- Every module applies a **convention plugin** from `build-logic/convention/`; never duplicate Gradle config

**Adding a feature module checklist:** `build.gradle.kts` with `AndroidFeaturePlugin` → `settings.gradle.kts` include → `:app` dependency → `@Serializable` route → `AppNavHost` composable → `XxxScreen/ViewModel/UiState/UserIntent/UiEvent` files → `XxxViewModelTest`.

### ViewModel / State pattern
```kotlin
private val _uiState = MutableStateFlow(HomeUiState())        // flat data class, NEVER sealed
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED) // Channel, NOT SharedFlow
val uiEvent = _uiEvent.receiveAsFlow()
fun onEvent(intent: HomeUserIntent) { ... }                   // single entry point
```
State updates: always `_uiState.update { it.copy(...) }` — never `.value = `.

### Hard rules
- `collectAsStateWithLifecycle()` — **never** `collectAsState()`
- Inject `@IoDispatcher` / `@MainDispatcher` — **never** `Dispatchers.IO` directly
- No XML layouts, no LiveData, no RxJava, no `!!`, no `Log.d` (use `Timber`)
- All versions in `gradle/libs.versions.toml` — never hardcode in `.gradle.kts`
- Use `build-logic/convention/` — **never** `buildSrc/`

---

## Architecture — FastAPI Backend

```
core/      → config (get_settings() is @lru_cache), security, limiter — no DB/HTTP imports
db/        → async engine + AsyncSession factory
models/    → SQLModel table=True (ORM + Pydantic in one)
schemas/   → Pydantic-only I/O shapes (never expose User ORM directly)
crud/      → DB I/O only — no business logic, no branching
api/v1/routes/ → thin handlers: validate → crud/service → ApiResponse[T]
tasks/     → ARQ async functions (no FastAPI imports)
workers/   → WorkerSettings: task registry + Redis config
```

### Route handler pattern
```python
@router.post("/resource", response_model=ApiResponse[ResourcePublic], status_code=201)
@limiter.limit("20/minute")
async def create_resource(request: Request, payload: CreateRequest,
                          db: DbSession, current_user: CurrentUser) -> ApiResponse[ResourcePublic]:
    item = await crud.create_item(db, payload, owner_id=current_user.id)
    return ApiResponse(data=ResourcePublic.model_validate(item), message="Created.")
```
Every response is wrapped in `ApiResponse[T]` from `schemas/common.py`. Use `MessageResponse` for no-data replies. `request: Request` must be first param when using `@limiter.limit`.

### Key rules
- Always `get_settings()` (LRU cached) — never `Settings()` in a function
- Never return a raw ORM object — always `XxxPublic` schema
- Refresh tokens stored as **SHA-256 hash only** — never raw
- Rate limit every auth + mutation endpoint: `@limiter.limit("20/minute")`

---

## Architecture — Next.js Frontend

- API base URL: `NEXT_PUBLIC_API_BASE_URL` (build arg + env var) → `https://resumetailor.in/api/v1`
- `next.config.ts` has `output: "standalone"` — required for Docker; do not remove
- Routes: `app/(auth)/` for public pages, `app/(dashboard)/` for authenticated pages
- Global state via Zustand; server state via TanStack Query; forms via React Hook Form + Zod

---

## Deployment & Infrastructure

**Traffic routing (nginx):**
- `/api/*` → FastAPI on port 8000
- `/*` → Next.js on port 3000

**Run full-stack locally:**
```bash
# From repo root
docker compose up --build
```

**Deploy to production (repo root):**
```bash
./deploy.sh   # pulls git, builds api + frontend images, docker compose up -d
```

**Backend-only deploy (from backend/):**
```bash
./deploy.sh   # uses backend/docker-compose.prod.yml
```

**Backend local dev (from backend/):**
```bash
make install && make migrate && make run   # API on :8000
make worker                                # ARQ worker (separate terminal)
```

**Frontend local dev:**
```bash
cd frontend && npm install && npm run dev   # :3000 → proxies /api to :8000
```

**Key env files:**
- `local.properties` (Android) — copied from `.env.example` at repo root
- `backend/.env` — copied from `backend/.env.example`; `GEMINI_API_KEY` required for resume tailoring
- `frontend/.env.local` — copied from `frontend/.env.local.example`

---

## Cross-Component Communication

- Android app calls `PROD_BASE_URL` (set in `local.properties`) via Retrofit in `core:network`
- Frontend calls `NEXT_PUBLIC_API_BASE_URL` via Axios
- Both authenticate with short-lived JWT access tokens (15 min) + refresh tokens (7 days)
- Background resume processing is enqueued via ARQ: route handler → `arq_pool.enqueue_job("process_resume")` → `tasks/` function runs in `worker` container with Gemini API

---

## Testing

**Android:** `XxxViewModelTest` with `MainDispatcherRule` + `UnconfinedTestDispatcher`. Use `FakeXxx` from `core:testing` for repositories — MockK only for DAOs/Retrofit services.

**Backend:** `pytest` with `pytest-asyncio`; in-memory async SQLite for tests; run `make test` or `make cov`.
