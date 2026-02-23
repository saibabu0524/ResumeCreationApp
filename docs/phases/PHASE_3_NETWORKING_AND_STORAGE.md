# Phase 3 — Networking, Storage & Data Layer

**Goal**: Build the data infrastructure — networking stack, local database, data stores, and the data layer that orchestrates between them. After this phase, repository implementations can fetch from the network, cache locally, and return typed results.

**Implementation Order Steps**: 7–10

---

## Table of Contents

- [Overview](#overview)
- [core:data Module](#coredata-module)
- [core:network Module](#corenetwork-module)
- [core:database Module](#coredatabase-module)
- [core:datastore Module](#coredatastore-module)
- [Dependency Graph After Phase 3](#dependency-graph-after-phase-3)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

This phase builds four tightly related modules. The `:core:data` module coordinates between `:core:network`, `:core:database`, and `:core:datastore`, implementing the repository interfaces defined in `:core:domain` (Phase 2). Data flows only downward — the domain layer never imports anything from these modules.

---

## core:data Module

**Plugin**: `AndroidLibraryConventionPlugin`
**Depends on**: `:core:domain`, `:core:network`, `:core:database`, `:core:datastore`

### Repository Implementations

Implement the interfaces defined in `:core:domain`. Each repository decides its own caching strategy:

| Strategy | When to Use |
|----------|------------|
| **Network-first** | Fresh, frequently changing data (e.g., user profile) |
| **Cache-first** | Mostly static data, offline support (e.g., settings) |
| **Cache-then-network** | Show stale data immediately, update in background (e.g., feeds) |

### Data Mappers

Three model types are **always kept separate and never mixed**:

```
DTO (Network) ──mapper──► Domain Model ◄──mapper── Entity (Database)
```

- **DTOs** — mirror the API response shape, live in `:core:network`
- **Entities** — mirror the database table shape, live in `:core:database`
- **Domain Models** — clean business objects, live in `:core:domain`

Mapper functions convert between these three types and live in `:core:data`.

### safeApiCall Wrapper

```kotlin
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T
): Result<T> {
    return withContext(dispatcher) {
        try {
            Result.Success(apiCall())
        } catch (throwable: Throwable) {
            Result.Error(ExceptionMapper.map(throwable))
        }
    }
}
```

### ExceptionMapper

Converts raw exceptions into typed `AppException` variants:

| Exception | Maps To |
|-----------|---------|
| `HttpException` (401) | `AppException.Unauthorized` |
| `HttpException` (403) | `AppException.Forbidden` |
| `HttpException` (404) | `AppException.NotFound` |
| `HttpException` (5xx) | `AppException.ServerError` |
| `IOException` | `AppException.NetworkError` |
| `SocketTimeoutException` | `AppException.Timeout` |
| `SerializationException` | `AppException.ParseError` |
| Other | `AppException.Unknown` |

---

## core:network Module

**Plugin**: `AndroidLibraryConventionPlugin`

All networking infrastructure is isolated here. No other module imports Retrofit or OkHttp directly.

### OkHttp Client

| Interceptor | Purpose |
|-------------|---------|
| `AuthInterceptor` | Attaches bearer token to every outgoing request |
| `ConnectivityInterceptor` | Throws typed `NetworkError` immediately when offline (instead of waiting for timeout) |
| `HttpLoggingInterceptor` | Request/response logging — **debug builds only** |

### SSL & Security

- Certificate pinning via `res/xml/network_security_config.xml` (ready-to-fill template)
- OpenSSL command for pin extraction documented inline
- Cleartext traffic **disabled globally** by default

### Retrofit Configuration

- **Kotlin Serialization** converter (compile-time safe, no reflection)
- `ApiResultCall` adapter wraps every response in `Result<T>` automatically — call sites never write try-catch

### ApiResultCall Adapter

```kotlin
// Wraps Retrofit Call<T> to return Result<T>
// Handles HTTP error codes, network failures, serialization errors
// Maps all exceptions through ExceptionMapper
```

### MultipartHelper

Utility for file and image upload operations. Included because multipart uploads are painful to add retroactively and nearly every production app needs them.

### NetworkMonitor

```kotlin
class NetworkMonitor(connectivityManager: ConnectivityManager) {
    val isOnline: Flow<Boolean>  // Observes connectivity state
}
```

Used by:
- UI layer — react to connectivity changes (offline banner)
- SyncWorker — delay work until connected

---

## core:database Module

**Plugin**: `AndroidRoomConventionPlugin`

### Room Database

- Abstract database class listing all DAOs
- Schema exported to `schemas/` directory (committed to version control — auditable)

### Migrations

| Type | Usage |
|------|-------|
| `@AutoMigration` | Simple schema changes (column additions) |
| Manual `Migration` objects | Complex changes — written and tested explicitly |

### Safety Rules

| Rule | Scope |
|------|-------|
| `fallbackToDestructiveMigration` | **Debug builds only** |
| Lint check | Fails release build if destructive migration flag is present |
| `withTransaction` | All multi-table write operations — prevents partial writes |

### Out-of-the-Box Type Converters

- Date/time converters
- Enum converters
- Common collection type converters

### DAOs

Define data access for each entity — standard CRUD + any custom queries needed by repositories.

---

## core:datastore Module

**Plugin**: `AndroidLibraryConventionPlugin`

### Preferences DataStore

For lightweight key-value storage:
- Onboarding state
- Selected theme
- Notification preferences

### Proto DataStore

For structured typed preferences:
- Schema defined in `.proto` file and enforced at compile time
- Use for complex preference objects

### Encapsulation Rule

Raw `Preferences` keys are **never exposed** outside this module. All access goes through an abstract repository interface, making the storage mechanism replaceable without touching feature code.

### Encrypted Storage

- Access tokens and refresh tokens → `EncryptedSharedPreferences`
- Backed by `MasterKey` with AES256-GCM encryption
- Encapsulated entirely inside the DataStore DI module
- Never exposed as plain preferences keys

---

## Dependency Graph After Phase 3

```
:core:data
 ├── :core:domain
 ├── :core:network
 ├── :core:database
 └── :core:datastore

:core:domain
 └── :core:common

:core:network     (depends on :core:common)
:core:database    (depends on :core:common)
:core:datastore   (depends on :core:common)
```

---

## Deliverables Checklist

- [ ] `:core:network` — OkHttp client, interceptors (Auth, Connectivity, Logging), Retrofit setup, `ApiResultCall`, `ExceptionMapper`, `MultipartHelper`, `NetworkMonitor`, network security config
- [ ] `:core:database` — Room database, DAOs, entities, type converters, migration setup, schema export
- [ ] `:core:datastore` — Preferences DataStore, Proto DataStore, `EncryptedSharedPreferences` for tokens
- [ ] `:core:data` — Repository implementations, data mappers (DTO ↔ Domain ↔ Entity), `safeApiCall` wrapper, caching strategies
- [ ] DI modules: `NetworkModule`, `DatabaseModule`, `DataStoreModule`, `RepositoryModule`, `DispatcherModule`
- [ ] Verify no raw exceptions leak past the data layer
- [ ] Verify domain layer has no imports from data/network/database modules
