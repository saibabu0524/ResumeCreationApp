# Android Jetpack Compose Template

A production-grade, multi-module Android template built with Jetpack Compose, Clean Architecture, and modern Android development best practices. Use this as a starting point for any serious Android project and skip the first two weeks of boilerplate setup.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-green.svg)](https://developer.android.com/jetpack/compose)

---

## Table of Contents

- [Using This Template](#using-this-template)
- [Module Architecture](#module-architecture)
- [Module Dependency Graph](#module-dependency-graph)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Core Architecture](#core-architecture)
- [Convention Plugins](#convention-plugins)
- [Dependency Management](#dependency-management)
- [Build Variants and Flavors](#build-variants-and-flavors)
- [Networking](#networking)
- [Local Storage](#local-storage)
- [Coroutines and Flows](#coroutines-and-flows)
- [Compose UI Layer](#compose-ui-layer)
- [Navigation](#navigation)
- [Dependency Injection](#dependency-injection)
- [WorkManager](#workmanager)
- [Security](#security)
- [Analytics and Feature Flags](#analytics-and-feature-flags)
- [Notifications](#notifications)
- [Testing Strategy](#testing-strategy)
- [Developer Tooling and Code Quality](#developer-tooling-and-code-quality)
- [CI/CD](#cicd)
- [Performance](#performance)
- [Accessibility and Localization](#accessibility-and-localization)
- [Adaptive Layouts](#adaptive-layouts)
- [Commonly Missed But Critical Details](#commonly-missed-but-critical-details)
- [Implementation Order](#implementation-order)
- [Contributing](#contributing)

---

## Using This Template

### Step 1 — Create from Template

Click **"Use this template"** on GitHub to create a new repository. You will get a clean copy without the fork history. Clone your new repository locally before continuing.

### Step 2 — Run the Setup Script

A `setup.py` script is included at the root of the project. Run it with your package name, app display name, and desired module prefix. The script performs a global find-and-replace across all Gradle files, Android manifests, and Kotlin source files so you only need to rename things once. Review the diff carefully before committing.

### Step 3 — Configure Your Environment

Copy `.env.example` to `local.properties` at the project root and fill in your values. This file holds your base URLs per environment and any API keys required at build time. It is already included in `.gitignore` and must never be committed to version control. Secrets should be passed via CI environment variables in automated pipelines, not stored in the repository.

### Step 4 — Configure Firebase

If you are using Firebase services, place your `google-services.json` files in the flavor-specific source sets under `app/src/dev/`, `app/src/staging/`, and `app/src/prod/`. Each environment gets its own Firebase project so that development traffic never pollutes production data.

---

## Module Architecture

The project is split into many small, focused modules organized around Clean Architecture layers and feature boundaries. This structure keeps build times fast, enforces hard dependency rules between layers, and makes every piece independently testable and replaceable.

### Core Modules

**:core:common** contains extension functions, utility classes, base classes, and cross-cutting helpers like `DeviceSecurityChecker`. Everything that does not belong to a specific layer but is shared across the whole codebase lives here.

**:core:domain** is the heart of the application. It holds repository interfaces, use case base classes and implementations, domain models, and the `Result<T>` sealed class. This is a **pure Kotlin JVM module with zero Android SDK imports**. Keeping it Android-free means it builds faster, stays independently testable with plain JUnit, and enforces the rule that business logic has no platform dependency.

**:core:data** holds repository implementations, data mappers that convert between DTO, Entity, and Domain model types, and the orchestration logic between remote and local data sources. Caching strategy decisions live here.

**:core:network** contains the Retrofit and OkHttp setup, all interceptors (auth, logging, connectivity), the centralized `ApiResultCall` adapter that wraps responses into `Result<T>` automatically, the `ExceptionMapper` that converts raw network exceptions into typed domain errors, and the `NetworkMonitor` flow that observes connectivity state.

**:core:database** holds the Room database class, all DAOs, entities, type converters, and migration objects. The database schema is exported and tracked in version control for auditing.

**:core:datastore** contains both Preferences DataStore for lightweight key-value storage and Proto DataStore for typed structured preferences. Encrypted SharedPreferences for sensitive token storage is also isolated here inside the DI module.

**:core:ui** is the shared Compose component library. It contains the MaterialTheme wrapper, custom color schemes for light and dark mode, typography, shape definitions, spacing composition locals, and all reusable UI components used across features.

**:core:analytics** defines an abstract `AnalyticsTracker` interface and an `AnalyticsEvent` sealed class hierarchy so that features never import Firebase or any analytics SDK directly. A no-op implementation is used in tests and local development. The Firebase implementation lives here and is swapped in via DI.

**:core:feature-flags** defines a `FeatureFlags` interface and a `Feature` enum with default values. A local map implementation is the default so the app works without any remote configuration service. A Remote Config-backed implementation can be swapped in via DI when ready.

**:core:notifications** handles FCM token management, token refresh, notification channel creation and configuration, the `NotificationBuilder` for constructing and posting notifications, and the routing of deep links from notification payloads into the navigation graph.

**:core:work** contains the WorkManager setup, `HiltWorkerFactory` so that workers can have injected dependencies, base worker classes with standard retry and error handling baked in, and reference implementations for periodic sync and upload workers.

**:core:testing** is a shared test module containing fake implementations of all repository interfaces, a `TestDispatcherRule` for coroutine test setup, custom test utilities, and any shared fixtures. Features and other core modules depend on this module in their test source sets only.

### Feature Modules

**:feature:auth** covers login, registration, and onboarding flows.

**:feature:home** is the primary post-login screen and also serves as the reference implementation for Paging 3 list patterns.

**:feature:settings** covers theme selection, notification preferences, and other user-facing app settings.

**:feature:profile** covers the user profile screen and serves as the reference implementation for biometric authentication.

### App and Build Modules

**:app** is the only module that sees everything. It holds the `Application` class, `MainActivity`, the root `AppNavHost` that assembles all feature navigation graphs, top-level DI wiring, and the `NavigationSuiteScaffold` for adaptive layout support.

**:baselineprofile** contains the Macrobenchmark-based Baseline Profile generator that runs on a real device and records the critical user journeys for ahead-of-time compilation by the Play Store.

**:benchmark** contains Macrobenchmark tests for startup time, scroll performance, and other runtime performance measurements.

**build-logic/** contains all Gradle convention plugins. No module-level build file ever repeats configuration.

---

## Module Dependency Graph

```
:app
 ├── :feature:auth
 ├── :feature:home
 ├── :feature:settings
 └── :feature:profile
      └── each feature module depends on
           ├── :core:ui
           ├── :core:domain
           ├── :core:analytics
           ├── :core:feature-flags
           └── :core:common

:core:data
 ├── :core:domain
 ├── :core:network
 ├── :core:database
 └── :core:datastore

:core:domain
 └── :core:common

:core:work
 ├── :core:data
 └── :core:domain

:core:notifications
 ├── :core:datastore
 └── :core:common

:core:analytics     (leaf — no internal dependencies)
:core:feature-flags (leaf — no internal dependencies)
```

Three rules are enforced at all times. Features never depend on other features. Data flows only downward, so the domain layer never imports anything from the data or network layers. And `:app` is the only module that is permitted to see all other modules simultaneously.

---

## Tech Stack

### Language and Async

Kotlin 2.0 is the language throughout. Kotlin Coroutines handle all asynchronous work. Kotlin Flow is the reactive stream primitive used at every layer from data source to ViewModel. Kotlin Serialization is the default JSON serialization library — it is compile-time safe and requires no reflection, making it the correct choice for a Kotlin-first codebase.

### UI and Navigation

Jetpack Compose with the BOM is the UI toolkit. Material3 is the design system. Navigation Compose with type-safe routes via Kotlin Serialization handles all in-app navigation. `NavigationSuiteScaffold` from the `material3-adaptive-navigation-suite` library handles phone, tablet, and foldable navigation patterns automatically based on window size. Coil handles image loading. Paging 3 handles paginated list data.

### Architecture Components

ViewModel with `SavedStateHandle` manages UI state and survives process death. The `collectAsStateWithLifecycle` API from Lifecycle is used throughout to avoid background flow collection. Hilt handles all dependency injection. WorkManager handles deferrable background work.

### Storage

Room is the local SQL database. DataStore (both Preferences and Proto variants) handles typed local preferences. Security Crypto provides `EncryptedSharedPreferences` for token storage.

### Networking

Retrofit is the HTTP client. OkHttp is the HTTP engine and interceptor host. Kotlin Serialization is the converter for JSON.

### Testing

JUnit 4 is the base test framework. JUnit 5 support is added via the `android-junit5` Gradle plugin for unit tests. MockK handles mocking. Turbine handles Flow testing. `kotlinx.coroutines.test` with `UnconfinedTestDispatcher` handles coroutine test setup. Roborazzi or Paparazzi handles screenshot regression testing. Compose UI testing uses `createComposeRule` with semantic matchers.

### Debug Tools

LeakCanary detects memory leaks in debug builds. Chucker provides an in-app HTTP inspector for debug builds. Timber provides structured logging with a `DebugTree` registered only in debug builds — no log statements reach production. Flipper provides database and network inspection for debug builds.

---

## Project Structure

```
├── app/
│   └── src/
│       ├── main/
│       ├── dev/
│       ├── staging/
│       └── prod/
├── build-logic/
│   └── convention/
│       └── src/main/kotlin/
│           ├── AndroidApplicationConventionPlugin.kt
│           ├── AndroidLibraryConventionPlugin.kt
│           ├── AndroidFeatureConventionPlugin.kt
│           ├── AndroidComposeConventionPlugin.kt
│           ├── AndroidHiltConventionPlugin.kt
│           ├── AndroidRoomConventionPlugin.kt
│           ├── AndroidWorkConventionPlugin.kt
│           ├── AndroidTestingConventionPlugin.kt
│           └── JvmLibraryConventionPlugin.kt
├── core/
│   ├── analytics/
│   ├── common/
│   ├── data/
│   ├── database/
│   ├── datastore/
│   ├── domain/
│   ├── feature-flags/
│   ├── network/
│   ├── notifications/
│   ├── testing/
│   ├── ui/
│   └── work/
├── feature/
│   ├── auth/
│   ├── home/
│   ├── profile/
│   └── settings/
├── baselineprofile/
├── benchmark/
├── gradle/
│   └── libs.versions.toml
├── .github/
│   ├── workflows/
│   │   ├── build.yml
│   │   ├── test.yml
│   │   ├── lint.yml
│   │   └── release.yml
│   └── pull_request_template.md
├── gradle.properties
├── setup.py
├── CHANGELOG.md
├── CONTRIBUTING.md
└── README.md
```

---

## Core Architecture

The template implements MVVM combined with Clean Architecture and enforces Unidirectional Data Flow throughout. The flow is always one direction: a user action enters the ViewModel as an intent, the ViewModel delegates to a use case, the use case calls a repository interface, the repository implementation coordinates between remote and local data sources, and the result travels back up as a `Result<T>` wrapped in a Flow. The ViewModel transforms that into a UI state that the Composable observes and reacts to.

### Domain Layer

The domain layer defines what the application does. Repository interfaces declare what data operations are available without specifying how they are performed. Use case classes encapsulate single units of business logic and depend only on repository interfaces. Domain models are pure Kotlin data classes with no Android imports. The `Result<T>` sealed class with `Success`, `Error`, and `Loading` variants is the universal return type for any operation that can fail. This layer is a pure JVM module — if it ever requires an Android import, that is a design mistake to correct.

### Data Layer

The data layer implements the contracts defined by the domain layer. Repository implementations decide the caching strategy — network-first, cache-first, or cache-then-network — for each use case independently based on the data's sensitivity and freshness requirements. Mapper functions convert between the three model types: DTOs from the network, entities from the database, and domain models for the rest of the app. These three model types are always kept separate and never mixed.

### Presentation Layer

Each screen has its own ViewModel, a UI state data class or sealed class representing every possible state the screen can be in, and a set of one-way UI events for things like navigation commands and snackbars that should fire once and not replay. The ViewModel holds a `StateFlow` for the UI state. One-time events are dispatched through a `Channel` exposed as a `Flow` so that they are never replayed to new collectors. Composables observe state with `collectAsStateWithLifecycle` and never call ViewModel methods directly — they send intents describing what the user did, and the ViewModel decides what to do.

---

## Convention Plugins

Convention plugins in `build-logic/` are the solution to copy-pasted Gradle configuration. Every module applies exactly one or two convention plugins and then only declares its own specific dependencies on top of what the plugin provides. No version strings, no plugin IDs, and no repeated configuration blocks ever appear in module-level build files.

The **android-application** plugin applies to `:app` only and includes the Application plugin, signing configuration, product flavor setup, and build type configuration. The **android-library** plugin applies to all Android library modules and provides the common configuration every library needs. The **jvm-library** plugin is specifically for `:core:domain` and any other pure Kotlin modules — it applies the Java Library and Kotlin JVM plugins only, with no Android Gradle Plugin, which keeps those modules faster to build and architecturally correct. The **android-feature** plugin is a convenience plugin for all feature modules that combines the library plugin with Hilt, Compose, ViewModel, and the standard feature testing dependencies. The **android-compose** plugin configures the Compose compiler options and adds a Gradle task to generate Compose compiler reports and metrics for performance analysis. The **android-hilt**, **android-room**, and **android-work** plugins handle the respective KSP and plugin setups for those libraries. The **android-testing** plugin applies to `:core:testing` and sets up the complete test dependency stack.

Note that JUnit 5 support via the `android-junit5` plugin requires that plugin to be declared as a dependency of the `build-logic` module itself, not just the modules that use it. This is a common point of confusion documented in the build-logic setup notes.

---

## Dependency Management

The `gradle/libs.versions.toml` Version Catalog is the single source of truth for every library version, alias, and bundle across the entire project. No module ever declares a version string directly in its build file. Libraries that are commonly used together are grouped into bundles so that a module imports one bundle alias rather than listing five individual dependencies. Compose libraries are imported via the BOM so all Compose artifact versions remain consistent without manual synchronization.

---

## Build Variants and Flavors

The template defines three product flavors: `dev`, `staging`, and `prod`. Each flavor has its own `BASE_URL` sourced from `local.properties`, its own `google-services.json`, and its own package name suffix so all three can be installed side by side on the same device. Dev and staging builds use a tinted launcher icon so they are visually distinct at a glance from the production build.

API keys and secrets are sourced from `local.properties` and injected as `BuildConfig` fields at build time. They are never hardcoded in source files. In CI, they are provided as encrypted environment secrets and injected into the Gradle build using the same property names that `local.properties` would provide locally.

`StrictMode` is enabled in the `Application` class for debug builds, covering both thread policy (disk and network reads on the main thread) and VM policy (leaked activities, closeable objects, and cursors). This catches common performance and resource mistakes before they reach production or code review.

---

## Networking

All networking infrastructure lives in `:core:network`.

The OkHttp client is configured with an `AuthInterceptor` that attaches the bearer token to every outgoing request, a `ConnectivityInterceptor` that throws a typed domain-level `NetworkError` immediately when the device has no internet connection rather than letting the request hang until timeout, and an `HttpLoggingInterceptor` added only in debug builds. SSL certificate pinning configuration is included in `res/xml/network_security_config.xml` as a ready-to-fill template with the exact OpenSSL command needed to extract the correct pin from your server documented inline. Cleartext traffic is disabled globally by default.

Retrofit is configured with a Kotlin Serialization converter as the default. Kotlin Serialization is preferred over Gson or Moshi because it is compile-time safe, requires no reflection, and is the idiomatic choice for a Kotlin-first project.

The `ApiResultCall` adapter wraps every Retrofit response in `Result<T>` automatically so that call sites never write try-catch blocks. The `ExceptionMapper` converts `HttpException`, `IOException`, `SocketTimeoutException`, `SerializationException`, and others into typed `AppException` variants that the UI layer can handle without knowing anything about network internals.

A `MultipartHelper` utility is included for file and image upload operations, which come up in nearly every production app and are painful to add retroactively.

The `NetworkMonitor` class exposes a `Flow<Boolean>` that observes connectivity state using `ConnectivityManager`, allowing the UI to react to connectivity changes and the sync worker to delay work until a connection is available.

---

## Local Storage

### Room Database

The Room database class is abstract and lists all DAOs in one place. The schema is exported to a `schemas/` directory committed to version control so every schema change is auditable. `@AutoMigration` annotations handle simple schema changes like column additions without requiring hand-written SQL. For complex migrations, dedicated `Migration` objects are written and tested explicitly.

`fallbackToDestructiveMigration` is enabled only in debug builds. A lint check is configured to fail the release build if this flag is still present, so it cannot accidentally ship to production. All multi-table write operations use `withTransaction` to prevent partial writes from leaving the database in an inconsistent state. Type converters for dates, enums, and common collection types are provided out of the box.

### DataStore

Preferences DataStore handles lightweight key-value storage for things like onboarding state, selected theme, and notification preferences. Proto DataStore handles structured typed preferences where the schema is defined in a `.proto` file and enforced at compile time. Raw `Preferences` keys are never exposed outside the DataStore module — all access goes through an abstract repository interface so that the storage mechanism is replaceable without touching feature code.

### Encrypted Storage

Access tokens and refresh tokens are stored in `EncryptedSharedPreferences` backed by the AES256-GCM master key scheme. This is encapsulated entirely inside the DataStore DI module and never exposed as plain preferences keys to the rest of the application.

---

## Coroutines and Flows

### Dispatcher Strategy

`CoroutineDispatcher` instances are always injected via Hilt using named `@Qualifier` annotations — `@IoDispatcher`, `@DefaultDispatcher`, and `@MainDispatcher`. Dispatchers are never hardcoded directly in any class that has other dependencies. This makes every class that does async work fully testable with `UnconfinedTestDispatcher` without needing to set test dispatchers globally. `Dispatchers.IO` is used for network and database operations. `Dispatchers.Default` is used for CPU-heavy transformations. `Dispatchers.Main.immediate` is used only where UI-thread immediacy is genuinely required.

### ViewModel State Pattern

ViewModels expose UI state as a `StateFlow` created with `stateIn` using `SharingStarted.WhileSubscribed(5_000)`. The 5-second window is intentional — it matches the ANR timeout, meaning that a configuration change like rotation does not restart the upstream flow, but the flow does stop if the user genuinely navigates away. This is the correct default for all ViewModels in the template. One-time events like navigation commands and snackbar messages use a `Channel` exposed as a `Flow` so they are consumed exactly once and never replayed.

### Shared Mutable State

Any shared mutable state that could be accessed concurrently uses a `Mutex` to prevent race conditions. The template demonstrates this pattern in the caching layer of the home repository.

### Flow Operator Reference

The home feature serves as a reference for common flow operator patterns including `combine` for merging multiple state streams, `flatMapLatest` for search and filter operations that cancel previous in-flight work, and `debounce` for search input throttling.

### Error Handling

The `safeApiCall` suspend function wraps every network operation and passes any throwable through `ExceptionMapper`. Raw exceptions from the network or database layers never reach the ViewModel. The ViewModel handles only typed `AppException` variants and maps them to user-visible error states.

---

## Compose UI Layer

### Theme System

`:core:ui` contains the full Material3 theme wrapper with custom `ColorScheme` definitions for both light and dark modes. Dynamic color (Android 12+) is supported with a graceful fallback to custom colors on older API levels. Typography uses a defined type scale. Shape tokens are centralized. Spacing and dimension values are provided through `CompositionLocal` so that screens never hardcode numeric margin or padding values.

### Component Library

All reusable Compose components live in `:core:ui`. Every component in this library has accessibility handled from the start — content descriptions, semantic roles, and minimum touch target sizes of 48dp are enforced at the base component level so individual screens cannot accidentally produce inaccessible UI.

The library includes `AppButton` with primary, secondary, text, and loading state variants; `AppTextField` with error state, label, and icon slots; `AppTopBar` with configurable back navigation and action support; `AppBottomBar` with navigation item configuration; a `NavigationSuiteScaffold` wrapper for adaptive navigation; `AppScaffold` that handles standard loading, error, and empty state delegation; `LoadingIndicator`, `EmptyState`, and `ErrorState` screens; `AppDialog` and `AppBottomSheet` wrappers; and `NetworkImage` as a Coil wrapper with loading and error placeholder support.

### Compose Compiler Reports

The `android-compose` convention plugin includes a Gradle task to generate Compose compiler reports and metrics. These reports show which composables are skippable and which parameters are stable or unstable. Running this before release builds is required — unstable classes cause unnecessary recompositions in list-heavy screens and are a leading cause of Compose performance issues that are invisible until profiled.

### Stability Annotations

Domain and UI model classes used as Composable parameters are annotated with `@Immutable` or `@Stable` as appropriate. The template documents the policy for when each annotation should be used and why, because getting stability wrong silently degrades Compose performance without obvious symptoms.

### Shared Element Transitions

The template includes a reference implementation of shared element transitions between a list item and a detail screen using the stable Compose 1.7+ shared element transition APIs. This is one of the most frequently requested patterns and is significantly harder to retrofit into an existing navigation structure than to include from the start.

---

## Navigation

Navigation uses Navigation Compose with type-safe routes defined using `@Serializable` Kotlin data classes, which gives compile-time safety for route arguments. Each feature module defines and exposes its own navigation graph as an extension function on `NavGraphBuilder`. The root `AppNavHost` in `:app` assembles all feature graphs in one place.

Features never navigate directly to other features. Navigation events are sent to the ViewModel as intents, the ViewModel emits a navigation event through the events channel, and the `AppNavHost` handles the actual navigation call. This keeps feature modules free of cross-feature dependencies.

Deep links are configured from the beginning. Every screen reachable from a notification or external link has its deep link defined alongside its route. Back stack state is saved and restored correctly — `rememberSaveable` is used in Composables for transient UI state, and `SavedStateHandle` is used in ViewModels for state that must survive process death.

---

## Dependency Injection

Hilt is the DI framework throughout. The `Application` class is annotated with `@HiltAndroidApp`. `MainActivity` is annotated with `@AndroidEntryPoint`. Every ViewModel is annotated with `@HiltViewModel`. Custom `@Qualifier` annotations are defined for dispatcher injection so the three dispatchers can be distinguished at injection sites.

DI modules are organized by concern. `NetworkModule` provides the OkHttp client, Retrofit instance, and all interceptors. `DatabaseModule` provides the Room database singleton and all DAOs. `DataStoreModule` provides DataStore instances and the encrypted preferences instance. `RepositoryModule` binds repository interfaces to their implementations. `DispatcherModule` provides the named coroutine dispatcher qualifiers. `AnalyticsModule` binds the `AnalyticsTracker` interface to the appropriate implementation per build variant. `FeatureFlagModule` binds the `FeatureFlags` interface to the local or remote implementation.

---

## WorkManager

`:core:work` provides the complete WorkManager infrastructure. `HiltWorkerFactory` is registered as the `WorkerFactory` via the `Configuration.Provider` interface on the `Application` class, so workers can receive injected dependencies through `@AssistedInject`.

A base `CoroutineWorker` class handles standard error reporting, retry logic, and progress notification so that concrete workers focus only on their specific task.

The template includes two reference workers. `SyncWorker` runs on a periodic schedule with a network connectivity constraint and demonstrates the offline-first synchronization pattern. `UploadWorker` manages a queue of pending uploads with exponential backoff and demonstrates the background upload queue pattern.

A `WorkManagerObserver` utility maps `WorkInfo` state streams into the same `Result<T>` type used throughout the codebase, so the UI layer can observe background work progress with the same patterns used for any other data stream.

---

## Security

**Token Storage** — Access and refresh tokens are stored only in `EncryptedSharedPreferences` backed by a `MasterKey` with AES256-GCM encryption. Plain DataStore and SharedPreferences are never used for credentials or sensitive data.

**Certificate Pinning** — `res/xml/network_security_config.xml` provides a ready-to-fill pinning configuration. The exact OpenSSL command for extracting the SHA-256 pin from your server certificate is documented as an inline comment. Cleartext traffic is disabled globally.

**ProGuard and R8** — Rules are pre-configured for Retrofit, Kotlin Serialization, Hilt, Room, Coroutines, and Compose. The rules file is annotated so that adding a new library that requires reflection rules is an obvious and prompted task.

**No Logging in Release** — Timber is initialized with `DebugTree` only when `BuildConfig.DEBUG` is true. No `android.util.Log` calls exist in the codebase — only Timber calls, which are no-ops in release builds.

**Biometric Authentication** — A `BiometricPromptManager` in `:core:common` handles all edge cases of the Biometric API including checking `BiometricManager.canAuthenticate()` correctly for strong versus weak biometrics, handling all authentication result codes, and providing a clean interface to feature code. The profile feature serves as the reference implementation.

**Device Security Checker** — `DeviceSecurityChecker` in `:core:common` is an interface with a default implementation that checks for root indicators, developer options, and debuggable state. Enforcement policy — whether to block the user or merely log the finding — is left as a configuration decision for the consuming project.

---

## Analytics and Feature Flags

### Analytics

Features import only `:core:analytics` — never a Firebase SDK directly. The `AnalyticsTracker` interface defines a single `track(event: AnalyticsEvent)` method. `AnalyticsEvent` is a sealed class hierarchy with one subclass per event type containing only typed parameters. This means renaming or restructuring events is a compile-time operation and swapping the analytics backend is a single DI binding change with no feature code touched.

A `NoOpAnalyticsTracker` is used in tests and in local development builds so that events are not accidentally sent during development or automated testing.

### Feature Flags

The `FeatureFlags` interface and `Feature` enum live in `:core:feature-flags`. The `Feature` enum declares each flag with its safe default value so the app works correctly with no configuration service at all. A `LocalFeatureFlags` implementation backed by a hard-coded map is the default binding. A `RemoteConfigFeatureFlags` implementation backed by Firebase Remote Config can be swapped in via a single DI binding change when remote flag management is needed.

---

## Notifications

`:core:notifications` handles all push and local notification concerns so that no feature module ever imports Firebase Messaging directly.

The `FcmService` extends `FirebaseMessagingService` and handles both token refresh and incoming message routing. On token refresh, the new token is written to DataStore and a token sync with the backend is scheduled via WorkManager. Incoming messages are routed by a message type field to the appropriate `NotificationBuilder` method.

`NotificationChannels` is a single object that declares all notification channel IDs, names, and importance levels in one place. Channels are created at app startup via Jetpack App Startup rather than in `Application.onCreate` to keep startup time minimal.

`NotificationBuilder` handles constructing `Notification` objects and posting them, including `PendingIntent` construction for deep links. Deep links from notification payloads use the same `@Serializable` route definitions used for in-app navigation, so notification deep links always land on the correct screen state without any special handling.

---

## Testing Strategy

### Philosophy

Fakes are preferred over mocks at repository and data source boundaries. All fakes live in `:core:testing` alongside the interfaces they implement. Using fakes makes tests resilient to refactoring — changing the internal implementation of a repository without changing its interface does not break any test. MockK is used for verifying specific interactions where that is the actual intent of the test, such as confirming that an analytics event was sent with the correct parameters.

### Unit Tests

Every ViewModel is testable in isolation because dispatchers are injected and replaced with `UnconfinedTestDispatcher` in tests. Every use case is testable in isolation because its repository dependencies are injected interfaces implemented by fakes. Every repository is testable with fake remote and local data sources. Turbine is used to test every `Flow` emission in sequence, asserting on exact values emitted in exact order. A `TestDispatcherRule` JUnit rule in `:core:testing` handles coroutine test scope setup automatically for every test class.

JUnit 5 support for unit tests uses the `android-junit5` Gradle plugin. This plugin must be declared as a dependency of the `build-logic` module itself — this is a common source of confusion and is documented explicitly in the build-logic README.

### Integration Tests

Room is tested using an in-memory database so tests are isolated and fast. DataStore is tested with an in-memory implementation. WorkManager workers are tested with `TestDriver`, which allows immediate constraint satisfaction and work request triggering without waiting for real scheduling.

### UI Tests

Compose UI tests use `createComposeRule` with semantic matchers. Components in `:core:ui` have their own Compose tests that verify correct rendering in every variant, catching regressions before any feature uses the component. Screenshot regression tests using Roborazzi or Paparazzi run in CI and fail the build on any visual difference, acting as a safety net for accidental UI changes.

---

## Developer Tooling and Code Quality

### Linting and Formatting

Ktlint enforces code formatting with a pre-commit Git hook so that unformatted code cannot be committed. Detekt enforces Clean Architecture rules, including that domain layer classes do not import Android SDK classes and that feature modules do not import each other. Spotless wraps both tools and allows auto-formatting with a single Gradle task. All three tools run in the CI lint workflow on every pull request.

### Dependency Guard

Dependency Guard locks the project's full resolved dependency tree to a baseline file committed to the repository. If any dependency changes unexpectedly — including version bumps from indirect transitive dependencies — the CI build fails. This prevents silent dependency upgrades that can introduce breaking API changes or security vulnerabilities.

### Git Conventions

Conventional Commits are enforced via `commitlint`. Branch naming conventions and PR expectations are documented in `CONTRIBUTING.md`. A PR template with a pre-flight checklist is included in `.github/pull_request_template.md`.

---

## CI/CD

Four GitHub Actions workflows are included and ready to use.

`build.yml` runs on every pull request and builds the debug APK to verify the project compiles. `test.yml` runs on every pull request and executes all unit tests across all modules. `lint.yml` runs Ktlint, Detekt, and Dependency Guard checks on every pull request and fails if any check does not pass. `release.yml` triggers on version tag pushes matching the semver pattern, builds the signed release AAB, and uploads it as a workflow artifact.

The keystore for release signing is stored as encrypted GitHub repository secrets. The signing configuration reads from `local.properties` locally and from environment secrets in CI using identical Gradle property names in both contexts.

---

## Performance

### Baseline Profiles

The `:baselineprofile` module generates a Baseline Profile by running Macrobenchmark on a connected device and recording the critical user journeys: cold app startup, home screen initial load, and common scroll interactions. The generated profile is committed to `app/src/main/baseline-prof.txt` and is included automatically in release builds by the Android Gradle Plugin. Without a Baseline Profile, Compose apps have a noticeably slower cold start on first launch because the JIT compiler has not yet optimized the hot code paths. The Play Store uses the committed profile to perform ahead-of-time compilation during app install.

### Gradle Build Performance

`gradle.properties` is pre-configured with parallel module execution, build caching, configuration caching, file system watching, and an increased JVM heap. `nonTransitiveRClass=true` is enabled — this prevents all modules from accessing resources from all their transitive dependencies and meaningfully reduces incremental build times in large multi-module projects. Kotlin incremental compilation is enabled.

---

## Accessibility and Localization

### Accessibility

Content descriptions are required on all image components and are enforced at the base `NetworkImage` and icon button component level. The base `AppButton` component enforces a minimum touch target size of 48dp × 48dp regardless of how small the visual content is. Custom interactive components include semantics blocks that describe their role and state to accessibility services. The Compose UI tests in `:core:ui` verify that accessibility properties are correctly set for every component variant.

### Localization

`strings.xml` contains every user-facing string from the first commit. No string literals exist anywhere in the Kotlin source. Plural strings use the `<plurals>` resource type. RTL layout support is verified using layout direction overrides in Compose Previews. The `setup.py` script flags any hardcoded strings it finds during the project renaming step.

---

## Adaptive Layouts

`WindowSizeClass` is the primary mechanism for adapting layouts to different screen sizes. `NavigationSuiteScaffold` in `:app` reads the window size class and automatically presents a bottom navigation bar on compact screens, a navigation rail on medium screens, and a navigation drawer on expanded screens. No conditional logic for this is required in individual feature screens.

Feature screens that show a list-detail pattern use a two-pane layout on expanded screens and single-pane navigation on compact screens. Foldable posture is handled through `WindowInfoTracker` where relevant. Edge-to-edge display is enabled by default and `WindowInsets` are applied correctly via `AppScaffold` so features do not need to handle insets individually.

---

## Commonly Missed But Critical Details

**Splash Screen** — The official Jetpack `SplashScreen` API is used. There is no custom launcher activity workaround. The `postSplashScreenTheme` attribute is set correctly so the app theme applies correctly after the splash exits.

**App Startup** — Jetpack App Startup initializes all libraries lazily rather than doing everything in `Application.onCreate`. Each initializer declares its dependencies explicitly, so initialization order is enforced and the cold start time is kept as short as possible.

**In-App Updates** — The Google Play In-App Update API is wired in. Flexible updates are used for non-critical updates. Immediate updates are used for critical ones that must be applied before the user continues. The update check runs on app foreground.

**App Review** — The Google Play In-App Review API is included as a `ReviewManager` wrapper. The policy for when to prompt for a review — after a certain number of sessions, after the user completes a meaningful action — is left as a configuration decision for the consuming project.

**App Shortcuts** — A `ShortcutHelper` in `:app` manages static and dynamic shortcuts. Static shortcuts are declared in `res/xml/shortcuts.xml`. Dynamic shortcuts are added and updated based on user activity.

**Offline First** — `SyncWorker` documents and implements an offline-first synchronization strategy. The pattern is: write locally first, return success to the user immediately, then sync to the server as a background operation. Conflict resolution strategy is documented as a placeholder that the consuming project must define based on its own data semantics.

**Crash Reporting** — Firebase Crashlytics is initialized behind a user consent flag and behind a `FeatureFlag` check. Crash reporting is not enabled until the user has accepted the privacy policy. This is required for GDPR and similar regulations in most target markets.

**Pagination** — The home feature includes a complete Paging 3 implementation as a reference covering both a `RemoteMediator` for network-plus-database pagination and the correct Compose integration with `collectAsLazyPagingItems`.

**Locale and RTL Testing** — A documented manual step and Compose Preview variant are included for verifying RTL layout correctness. Many layout bugs are only visible in RTL and are cheapest to catch early.

**Memory Leak Verification** — LeakCanary is included and configured, but the tool only surfaces leaks — teams must act on them. A step in `CONTRIBUTING.md` requires that LeakCanary is checked manually on a physical device whenever a new screen is added to the project.

**Gradle Remote Build Cache** — Remote build caching is configured as a commented-out placeholder in `settings.gradle.kts` with a placeholder for the cache server URL. Local build caching is enabled by default.

---

## Implementation Order

Build the foundation before building any feature. Skipping steps in the order below results in refactoring work as soon as the first feature is built.

1. `build-logic` convention plugins and `libs.versions.toml` version catalog
2. Product flavors and build type configuration in the application plugin
3. `:core:common` — utilities, base classes, `DeviceSecurityChecker`
4. `:core:domain` — use case bases, repository interfaces, domain models, `Result<T>`
5. `:core:analytics` — tracker interface, event hierarchy, no-op and Firebase implementations
6. `:core:feature-flags` — flags interface, feature enum, local implementation
7. `:core:data` — repository implementations, mappers, `safeApiCall` wrapper, `ExceptionMapper`
8. `:core:network` — OkHttp, Retrofit, interceptors, `NetworkMonitor`
9. `:core:database` — Room, DAOs, migrations, transaction utilities
10. `:core:datastore` — DataStore setup, encrypted preferences
11. `:core:notifications` — FCM service, notification builder, channel setup
12. `:core:ui` — theme, component library, accessibility enforcement
13. `:feature:auth` — first feature end-to-end, validates all core modules work together
14. `:app` navigation — `AppNavHost`, `NavigationSuiteScaffold`, deep links wired in
15. `:core:work` — WorkManager, Hilt worker factory, `SyncWorker`, `UploadWorker`
16. `:core:testing` — fakes, test rules, shared test utilities
17. Unit and integration tests for all core modules
18. CI/CD pipelines — build, test, lint, and release workflows
19. `:baselineprofile` and `:benchmark` — performance measurement baseline established
20. Polish — accessibility audit, RTL verification, screenshot test baseline, Compose compiler metrics review

---

## Contributing

Please read `CONTRIBUTING.md` before opening a pull request. It covers branch naming conventions, the Conventional Commits format enforced by commitlint, the PR checklist, and the code review process.

When adding a new feature module, verify the following before marking the PR ready for review: no hardcoded strings exist in the Kotlin source, all interactive components have correct accessibility semantics, the feature nav graph is registered in `AppNavHost`, a LeakCanary verification has been done on a physical device, and any new public APIs in core modules have corresponding fakes added to `:core:testing`.

---

## License

```
Copyright 2024 Your Name

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
