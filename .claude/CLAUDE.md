# CLAUDE.md — Project Intelligence File

This file is read by Claude at the start of every session. Follow every instruction here before writing, editing, or reviewing any code.

---

## 🧠 First Thing You Do — Always

Before touching **any** code, you **must** read the best practices folder:

```
.claude/best-practices/
```

Read every `.md` file inside it. These files contain:
- Common AI coding mistakes to avoid in this project
- Project-specific patterns and conventions
- What has been tried before and failed
- Approved approaches for recurring problems

**Do not skip this step, even for small edits.** If the folder is empty or missing, tell the user before proceeding.

---

## 📁 Project Overview

A reusable **Android Jetpack Compose** multi-module template. Every app spawned from this template shares the same structure, patterns, and conventions. Changes to patterns must be reflected in `best-practices/` so future sessions stay consistent.

This repo also contains a companion **FastAPI backend** under `backend/` — a production-ready Python API template. See `backend/README.md` for its own conventions. The Android rules in `best-practices/` below are Android-first; backend-specific rules live in the files prefixed `backend-` inside `best-practices/`:

- `backend-architecture.md` — layer map, banned imports, new-endpoint/task checklists
- `backend-api-patterns.md` — route handlers, response envelopes, auth deps, rate limiting
- `backend-testing.md` — async test setup, fixtures, coverage rules
- `backend-security.md` — token rules, password handling, upload security, production checklist

**Android Stack:**
- Kotlin 100% — no Java files ever
- Jetpack Compose (no XML layouts, ever)
- MVVM + Clean Architecture (strict layer separation)
- Hilt (Dagger-Hilt) for dependency injection
- Coroutines + Flow (no RxJava, no LiveData)
- Retrofit + OkHttp + Kotlin Serialization for networking
- Room for local persistence
- Proto DataStore / Preferences DataStore
- WorkManager for background work
- Firebase (Crashlytics, Analytics, Remote Config)
- Navigation Compose with type-safe `@Serializable` routes
- Gradle Version Catalog + Convention Plugins in `build-logic/`
- Detekt + ktlint for static analysis

---

## 🗂️ Module Map

```
root/
├── app/                        → Entry point, DI wiring, NavHost, Application class
├── build-logic/                → Convention plugins (Gradle, no buildSrc)
├── core/
│   ├── common/                 → Extensions, utils, base classes, Result<T>
│   ├── ui/                     → Compose theme, components, design system
│   ├── domain/                 → UseCases, Repository interfaces, domain models
│   ├── data/                   → Repository implementations, mappers, data sources
│   ├── network/                → Retrofit, OkHttp, interceptors, NetworkMonitor
│   ├── database/               → Room DB, DAOs, Entities, TypeConverters
│   ├── datastore/              → Proto DataStore, Preferences DataStore
│   ├── work/                   → WorkManager workers, WorkerFactory
│   ├── analytics/              → Analytics abstraction + Firebase impl
│   ├── feature-flags/          → Feature flag abstraction + Remote Config impl
│   ├── notifications/          → Push notification handling
│   └── testing/                → Fakes, test rules, MainDispatcherRule
├── feature/
│   ├── auth/                   → Login, Register, Onboarding
│   ├── home/                   → Home screen
│   ├── settings/               → Settings screen
│   └── profile/                → User profile screen
└── backend/                    → FastAPI companion backend (see backend/README.md)
```

---

## 📐 Architecture Rules

### Layer Separation (NEVER break these)
- `core:domain` is pure Kotlin — zero Android imports
- Domain defines Repository **interfaces** only — never implementations
- `core:data` implements domain interfaces — never references presentation layer
- Feature modules NEVER depend on other feature modules
- `:app` is the ONLY module that knows about all feature modules
- `core:testing` is testImplementation only — never implementation/api

### Naming Conventions
- Files: `PascalCase.kt` (Kotlin standard)
- Screen composables: `XxxScreen.kt`
- ViewModels: `XxxViewModel.kt`
- UiState: `XxxUiState.kt` — always a flat `data class`
- User intents (actions): `XxxUserIntent.kt` — `sealed interface`
- One-shot UI events: `XxxUiEvent.kt` — `sealed interface`
- Domain models: plain `data class` — no suffix
- Room entities: `XxxEntity.kt`
- Network DTOs: `XxxDto.kt`
- Mappers: `XxxMapper.kt`
- UseCases: `VerbNounUseCase.kt` — one `operator fun invoke()` only
- Hilt modules: `XxxModule.kt`
- Navigation: `XxxNavigation.kt` per feature

### ViewModel & State
- State: `MutableStateFlow` → exposed as `StateFlow` — never mutable public
- One-shot events: `Channel<XxxUiEvent>(Channel.BUFFERED)` → `receiveAsFlow()`
- User actions: single `fun onEvent(intent: XxxUserIntent)` entry point
- UiState is always a flat data class (never sealed Loading/Success/Error)
- Never use `LiveData` — Flow only
- Never reference Android context inside ViewModel

### Compose UI Rules
- Collect state with `collectAsStateWithLifecycle()` — never `collectAsState()`
- Stateless composables accept data + lambda callbacks — never ViewModel directly
- Use `key = { it.id }` in `items {}` to prevent recomposition storms
- Never create lambdas inline inside `items {}` — hoist or `remember` them
- Use `Modifier` as the first optional parameter in every composable
- Never use hardcoded dimensions — use `MaterialTheme.spacing` or `dimensionResource`

### Coroutines & Async
- Inject `CoroutineDispatcher` via `@IoDispatcher` / `@MainDispatcher` / `@DefaultDispatcher`
- Never write `Dispatchers.IO` directly in production code
- Use `viewModelScope.launch` in ViewModels only
- Repository and UseCase functions are `suspend` — not `Flow` unless streaming

### Dependency Injection (Hilt)
- All Hilt modules live in a `di/` subpackage of their module
- `@Singleton` for app-wide dependencies, `@ViewModelScoped` for ViewModel deps
- Never use `ServiceLocator` or manual DI — always Hilt

### Navigation
- Routes are `@Serializable` data classes/objects — never string routes
- Navigation callbacks bubble up from feature screen → `:app` NavHost
- Features expose navigation as lambda parameters — never navigate internally

### Build Logic
- All shared Gradle config lives in `build-logic/convention/` as convention plugins
- Never use `buildSrc/`
- All dependency versions are in `gradle/libs.versions.toml` — never hardcoded
- A new module always applies the correct convention plugin(s) — never duplicates config

---

## 🧪 Testing Rules

- Every ViewModel must have a `XxxViewModelTest.kt`
- Use `FakeXxx` classes from `core:testing` — never Mockito/MockK for repositories
- MockK is acceptable only for leaf dependencies (DAO, Retrofit service)
- `MainDispatcherRule` must be applied in every ViewModel test
- Pass `UnconfinedTestDispatcher()` wherever a `CoroutineDispatcher` is needed in tests
- Tests must cover: happy path + at least one error/edge case

---

## ✍️ Code Style

- Language: **Kotlin** — 100%, no Java
- Max line length: **120 characters**
- Indentation: **4 spaces** (no tabs)
- Trailing commas: always in multi-line expressions
- `val` over `var` — use `var` only with explicit justification
- No force-unwrap (`!!`) in production code — ever
- No magic numbers — use named constants
- No `println()` / `Log.d()` — use **Timber** (`Timber.d`, `Timber.e`)
- No commented-out dead code — delete it
- All public functions and classes must have KDoc
- Explicit return types on all public and internal functions

---

## ❌ Things You Must Never Do

| Never | Instead |
|---|---|
| Use XML layouts | Jetpack Compose only |
| Use LiveData | StateFlow + `collectAsStateWithLifecycle()` |
| Use `Dispatchers.IO` directly | Inject `@IoDispatcher` |
| Force-unwrap with `!!` | Safe call `?.` or `?: error(...)` |
| Put business logic in a Composable | Move to ViewModel or UseCase |
| Put DB queries in a ViewModel | Repository → UseCase → ViewModel |
| Import another feature module from a feature | Route through `:app` NavHost |
| Use string-based navigation routes | `@Serializable` data class routes |
| Use `collectAsState()` | `collectAsStateWithLifecycle()` |
| Create lambdas inline in `items {}` | Hoist or `remember` stable references |
| Put shared Gradle config in each module | Convention plugin in `build-logic/` |
| Use `buildSrc/` | `build-logic/convention/` plugins |
| Hardcode a version in `build.gradle.kts` | Use `libs.versions.toml` catalog |
| Use MockK for repository in tests | Use `FakeXxx` from `core:testing` |
| Use `Log.d` / `println` | Use `Timber` |

---

## 🗂️ Best Practices Folder Layout

Keep `.claude/best-practices/` updated as the project evolves:

```
.claude/
└── best-practices/
    ├── architecture.md       # Layer rules, module boundaries, dependency graph
    ├── compose-ui.md         # Compose patterns, recomposition, state hoisting
    ├── viewmodel-state.md    # UDF pattern, StateFlow, Channel events, intents
    ├── network-data.md       # Retrofit, Result<T>, offline-first, mappers
    ├── testing.md            # Fakes, dispatchers, ViewModel tests, screenshot tests
    └── lessons.md            # Running log of mistakes made and how they were fixed
```

After completing a task, if a new mistake was discovered or a new pattern was established, **update the relevant file** in `.claude/best-practices/` before closing the session.

---

## 🔁 Workflow for Every Task

1. Read all files in `.claude/best-practices/`
2. Understand the full scope of the change before writing anything
3. Identify which module the change belongs to — never blur layer boundaries
4. Check if existing components in `core:ui`, `core:domain`, or `core:testing` can be reused
5. Write or edit code following all rules above
6. Add or update `XxxViewModelTest.kt` / `XxxScreenTest.kt`
7. If a new Gradle dependency is needed: add to `libs.versions.toml` first
8. If a new pattern or mistake was found: update `.claude/best-practices/`

---

## 💬 Communication Style

- Be concise — no unnecessary preamble
- If a requirement is ambiguous, ask **one clarifying question** before proceeding
- If a rule in this file conflicts with a user request, flag the conflict and explain the tradeoff — don't silently break the rules
- If something in `best-practices/` is outdated or wrong, say so and suggest an update
