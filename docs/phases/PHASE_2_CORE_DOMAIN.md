# Phase 2 — Core Domain & Common Infrastructure

**Goal**: Build the pure-Kotlin domain layer and the shared common utilities. These modules form the architectural foundation that every other module in the project depends on. Getting these right means the entire app's dependency direction is correct from the start.

**Implementation Order Steps**: 3–6

---

## Table of Contents

- [Overview](#overview)
- [core:common Module](#corecommon-module)
- [core:domain Module](#coredomain-module)
- [core:analytics Module](#coreanalytics-module)
- [core:feature-flags Module](#corefeature-flags-module)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

This phase builds four core modules. Critically, `:core:domain` is a **pure Kotlin JVM module** — it must have zero Android SDK imports. This is enforced by using the `JvmLibraryConventionPlugin` from Phase 1. If the domain layer ever requires an Android import, that's a design mistake to correct.

---

## core:common Module

**Plugin**: `AndroidLibraryConventionPlugin`

Contains cross-cutting utilities shared across the entire codebase.

### Contents

| Component | Description |
|-----------|-------------|
| **Extension Functions** | Kotlin extension functions for common operations (String, Context, Flow, etc.) |
| **Base Classes** | Base ViewModel, base use case, or any shared abstractions |
| **`DeviceSecurityChecker`** | Interface + default implementation that checks for root indicators, developer options, debuggable state |
| **`BiometricPromptManager`** | Handles all edge cases of the Biometric API — checks `canAuthenticate()` for strong vs weak biometrics, handles all result codes |
| **Utility Classes** | Date/time helpers, formatting utilities, etc. |

### DeviceSecurityChecker Details

```kotlin
interface DeviceSecurityChecker {
    fun isDeviceRooted(): Boolean
    fun isDeveloperOptionsEnabled(): Boolean
    fun isDebuggable(): Boolean
}
```

Enforcement policy (block user vs. log finding) is a configuration decision for the consuming project.

### BiometricPromptManager Details

- Checks `BiometricManager.canAuthenticate()` correctly for strong vs weak biometrics
- Handles all authentication result codes
- Provides a clean interface to feature code
- Used as reference implementation in `:feature:profile`

---

## core:domain Module

**Plugin**: `JvmLibraryConventionPlugin` (pure Kotlin JVM — **no Android SDK**)

This is the heart of the application.

### Result<T> Sealed Class

The universal return type for any operation that can fail:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

### Repository Interfaces

Declare what data operations are available **without specifying how** they are performed. Example:

```kotlin
interface UserRepository {
    fun getUser(id: String): Flow<Result<User>>
    suspend fun updateUser(user: User): Result<Unit>
}
```

### Use Case Base Classes

Encapsulate single units of business logic. Depend only on repository interfaces.

```kotlin
abstract class UseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {
    suspend operator fun invoke(parameters: P): Result<R> {
        return withContext(dispatcher) {
            execute(parameters)
        }
    }
    protected abstract suspend fun execute(parameters: P): Result<R>
}

abstract class FlowUseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> {
        return execute(parameters).flowOn(dispatcher)
    }
    protected abstract fun execute(parameters: P): Flow<Result<R>>
}
```

### Domain Models

- Pure Kotlin data classes — **no Android imports**
- Annotated with `@Immutable` or `@Stable` where they'll be used as Composable parameters
- Separate from DTOs (network) and Entities (database) — the three model types are never mixed

### Dependency Rule

`:core:domain` depends only on `:core:common`. It never imports anything from data, network, or any other layer.

---

## core:analytics Module

**Plugin**: `AndroidLibraryConventionPlugin` (leaf module — no internal dependencies)

Features import only this module — **never a Firebase SDK directly**.

### AnalyticsTracker Interface

```kotlin
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
```

### AnalyticsEvent Sealed Class

```kotlin
sealed class AnalyticsEvent(val name: String) {
    data class ScreenView(val screenName: String) : AnalyticsEvent("screen_view")
    data class ButtonClick(val buttonId: String, val screenName: String) : AnalyticsEvent("button_click")
    // One subclass per event type with typed parameters
}
```

### Implementations

| Implementation | Usage |
|---------------|-------|
| `NoOpAnalyticsTracker` | Tests and local development builds — events are silently discarded |
| `FirebaseAnalyticsTracker` | Production — sends events to Firebase Analytics |

Swapping the analytics backend is a **single DI binding change** — no feature code is touched.

---

## core:feature-flags Module

**Plugin**: `AndroidLibraryConventionPlugin` (leaf module — no internal dependencies)

### Feature Enum

```kotlin
enum class Feature(val defaultValue: Boolean) {
    DARK_MODE(true),
    BIOMETRIC_LOGIN(false),
    NEW_HOME_LAYOUT(false),
    // Each flag has a safe default so the app works with no config service
}
```

### FeatureFlags Interface

```kotlin
interface FeatureFlags {
    fun isEnabled(feature: Feature): Boolean
}
```

### Implementations

| Implementation | Usage |
|---------------|-------|
| `LocalFeatureFlags` | Default — backed by a hard-coded map; app works with no remote service |
| `RemoteConfigFeatureFlags` | Optional — backed by Firebase Remote Config; swapped in via DI |

---

## Deliverables Checklist

- [ ] `:core:common` module with extension functions, base classes, `DeviceSecurityChecker`, `BiometricPromptManager`
- [ ] `:core:domain` module as pure Kotlin JVM — `Result<T>`, repository interfaces, use case bases, domain models
- [ ] `:core:analytics` module with `AnalyticsTracker`, `AnalyticsEvent`, `NoOpAnalyticsTracker`, `FirebaseAnalyticsTracker`
- [ ] `:core:feature-flags` module with `Feature` enum, `FeatureFlags` interface, `LocalFeatureFlags`, `RemoteConfigFeatureFlags`
- [ ] Verify `:core:domain` has **zero Android SDK imports** (enforced by `JvmLibraryConventionPlugin`)
- [ ] All modules compile and the dependency graph is correct
