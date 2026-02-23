# Phase 6 — Background Work, Notifications & Security

**Goal**: Add WorkManager infrastructure for offline-first sync, push notification handling, and all security hardening. After this phase, the app supports background operations, receives push notifications, and has production-grade security in place.

**Implementation Order Steps**: 11, 15

---

## Table of Contents

- [Overview](#overview)
- [core:work Module](#corework-module)
- [core:notifications Module](#corenotifications-module)
- [Security](#security)
- [Commonly Missed Critical Details](#commonly-missed-critical-details)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

This phase covers three areas that are often deferred and painful to add later: background work, push notifications, and security. Having these in the template from the start means feature teams can use them immediately without infrastructure work.

---

## core:work Module

**Plugin**: `AndroidWorkConventionPlugin`
**Depends on**: `:core:data`, `:core:domain`

### HiltWorkerFactory

- Registered as the `WorkerFactory` via `Configuration.Provider` on the `Application` class
- Workers receive injected dependencies through `@AssistedInject`

### Base CoroutineWorker

Provides standard error reporting, retry logic, and progress notification so concrete workers focus on their task:

```kotlin
abstract class BaseCoroutineWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    // Standard error reporting
    // Retry logic with exponential backoff
    // Progress notification support
}
```

### Reference Workers

| Worker | Pattern | Details |
|--------|---------|---------|
| `SyncWorker` | Periodic sync | Network connectivity constraint, offline-first sync |
| `UploadWorker` | Upload queue | Exponential backoff, pending upload management |

### WorkManagerObserver

Maps `WorkInfo` state streams into `Result<T>` — UI layer observes background work with the same patterns used for any other data stream.

### Offline-First Pattern

1. Write locally first
2. Return success to the user immediately
3. Sync to server as background operation
4. Conflict resolution strategy — documented as placeholder for consuming project

---

## core:notifications Module

**Plugin**: `AndroidLibraryConventionPlugin`
**Depends on**: `:core:datastore`, `:core:common`

No feature module imports Firebase Messaging directly.

### FcmService

Extends `FirebaseMessagingService`:

| Event | Handling |
|-------|---------|
| **Token refresh** | Write new token to DataStore, schedule backend sync via WorkManager |
| **Incoming message** | Route by message type to appropriate `NotificationBuilder` method |

### NotificationChannels

Single object declaring all channel IDs, names, and importance levels in one place:
- Created at app startup via **Jetpack App Startup** (not in `Application.onCreate`)
- Keeps startup time minimal

### NotificationBuilder

| Responsibility | Details |
|---------------|---------|
| Construct `Notification` objects | Title, body, channel, priority |
| `PendingIntent` construction | Deep links using `@Serializable` route definitions |
| Post notifications | Correct notification ID management |

Deep links from notification payloads use the **same route definitions** as in-app navigation — notifications always land on the correct screen state.

---

## Security

### Token Storage

| Item | Storage |
|------|---------|
| Access token | `EncryptedSharedPreferences` (AES256-GCM) |
| Refresh token | `EncryptedSharedPreferences` (AES256-GCM) |
| Other credentials | **Never** plain DataStore or SharedPreferences |

### Certificate Pinning

- `res/xml/network_security_config.xml` — ready-to-fill
- OpenSSL command for SHA-256 pin extraction documented inline
- Cleartext traffic disabled globally

### ProGuard / R8 Rules

Pre-configured for:
- Retrofit
- Kotlin Serialization
- Hilt
- Room
- Coroutines
- Compose

Rules file is annotated so adding new library rules is an obvious task.

### No Logging in Release

- Timber initialized with `DebugTree` only when `BuildConfig.DEBUG == true`
- No `android.util.Log` calls in the codebase — Timber only
- No-op in release builds

### Biometric Authentication

Handled by `BiometricPromptManager` in `:core:common` (built in Phase 2), demonstrated in `:feature:profile` (Phase 5).

### Device Security Checker

`DeviceSecurityChecker` in `:core:common` (built in Phase 2) — enforcement policy is a configuration decision.

---

## Commonly Missed Critical Details

These are included in the template because they are painful to add retroactively:

| Feature | Details |
|---------|---------|
| **Splash Screen** | Official Jetpack `SplashScreen` API; `postSplashScreenTheme` set correctly |
| **App Startup** | Jetpack App Startup for lazy library initialization with explicit dependency ordering |
| **In-App Updates** | Google Play In-App Update API — flexible (non-critical) and immediate (critical) modes |
| **App Review** | Google Play In-App Review API — `ReviewManager` wrapper; trigger policy is configurable |
| **App Shortcuts** | `ShortcutHelper` in `:app` for static (`shortcuts.xml`) and dynamic shortcuts |
| **Crash Reporting** | Firebase Crashlytics behind user consent flag and `FeatureFlag` check (GDPR) |

---

## Deliverables Checklist

- [ ] `:core:work` — `HiltWorkerFactory`, base `CoroutineWorker`, `SyncWorker`, `UploadWorker`, `WorkManagerObserver`
- [ ] `:core:notifications` — `FcmService`, `NotificationChannels`, `NotificationBuilder`, deep link routing
- [ ] Security — encrypted token storage, certificate pinning config, ProGuard/R8 rules, Timber debug-only
- [ ] Splash Screen — Jetpack `SplashScreen` API integration
- [ ] App Startup — lazy library initialization
- [ ] In-App Updates — flexible and immediate update support
- [ ] In-App Review — `ReviewManager` wrapper
- [ ] App Shortcuts — static and dynamic shortcut support
- [ ] Crash Reporting — Crashlytics with consent and feature flag gating
- [ ] Offline-first sync pattern documented and implemented
