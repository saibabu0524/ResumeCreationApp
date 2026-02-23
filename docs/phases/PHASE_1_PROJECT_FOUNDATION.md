# Phase 1 — Project Foundation & Build System

**Goal**: Set up the multi-module project skeleton, convention plugins, dependency management, and build variants. Nothing compiles as a feature yet — this phase creates the infrastructure every other module will rely on.

**Implementation Order Steps**: 1–2

---

## Table of Contents

- [Overview](#overview)
- [Convention Plugins](#convention-plugins)
- [Dependency Management](#dependency-management)
- [Build Variants and Flavors](#build-variants-and-flavors)
- [Gradle Build Performance](#gradle-build-performance)
- [Project Structure After Phase 1](#project-structure-after-phase-1)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

This phase builds the entire `build-logic/` infrastructure and configures the root project. Every module added in later phases will apply exactly one or two convention plugins and declare only its own specific dependencies. No version strings, plugin IDs, or repeated configuration blocks should ever appear in module-level build files.

---

## Convention Plugins

Convention plugins live in `build-logic/convention/`. They eliminate copy-pasted Gradle configuration across modules.

### Plugins to Create

| Plugin | Applies To | Responsibility |
|--------|-----------|----------------|
| `AndroidApplicationConventionPlugin` | `:app` only | Application plugin, signing config, product flavors, build types |
| `AndroidLibraryConventionPlugin` | All Android library modules | Common Android library configuration |
| `JvmLibraryConventionPlugin` | `:core:domain` and pure Kotlin modules | Java Library + Kotlin JVM plugins only (no Android Gradle Plugin) |
| `AndroidFeatureConventionPlugin` | All feature modules | Combines library plugin with Hilt, Compose, ViewModel, and testing deps |
| `AndroidComposeConventionPlugin` | Compose-enabled modules | Compose compiler options, compiler reports/metrics Gradle task |
| `AndroidHiltConventionPlugin` | Hilt-enabled modules | Hilt + KSP setup |
| `AndroidRoomConventionPlugin` | `:core:database` | Room + KSP setup, schema export config |
| `AndroidWorkConventionPlugin` | `:core:work` | WorkManager dependency setup |
| `AndroidTestingConventionPlugin` | `:core:testing` | Full test dependency stack |

### Key Details

- **JUnit 5 support** via the `android-junit5` Gradle plugin must be declared as a dependency of the `build-logic` module itself, not in the modules that use it. This is a common point of confusion.
- The **Compose plugin** should include a Gradle task to generate Compose compiler reports and metrics for performance analysis.
- Each plugin provides the standard dependencies and configuration so module-level build files remain minimal.

---

## Dependency Management

### Version Catalog — `gradle/libs.versions.toml`

This file is the **single source of truth** for every library version, alias, and bundle across the entire project.

**Rules**:
- No module ever declares a version string directly in its build file
- Libraries commonly used together are grouped into **bundles** (e.g., a `compose` bundle instead of five individual deps)
- Compose libraries are imported via the **BOM** for version consistency
- All versions, libraries, bundles, and plugins are declared here

### Libraries to Include

| Category | Libraries |
|----------|-----------|
| **Kotlin & Coroutines** | Kotlin 2.0, Coroutines (core, android), Kotlin Serialization |
| **Compose** | Compose BOM, UI, Material3, Foundation, Runtime, Tooling, Navigation |
| **Adaptive** | material3-adaptive-navigation-suite |
| **Architecture** | ViewModel, SavedStateHandle, Lifecycle, collectAsStateWithLifecycle |
| **DI** | Hilt (android, compiler, navigation-compose) |
| **Networking** | Retrofit, OkHttp (core, logging), Kotlin Serialization converter |
| **Storage** | Room (runtime, compiler, KTX), DataStore (preferences, proto), Security Crypto |
| **Background** | WorkManager (runtime, KTX, testing) |
| **Paging** | Paging 3 (runtime, compose) |
| **Image** | Coil (compose) |
| **Firebase** | Crashlytics, Analytics, Messaging, Remote Config |
| **Testing** | JUnit 4, JUnit 5, MockK, Turbine, kotlinx-coroutines-test, Compose UI testing, Roborazzi/Paparazzi |
| **Debug** | LeakCanary, Chucker, Timber, Flipper |
| **Code Quality** | Ktlint, Detekt, Spotless, Dependency Guard |
| **Play** | In-App Updates, In-App Review |
| **Biometric** | AndroidX Biometric |

---

## Build Variants and Flavors

### Three Product Flavors

| Flavor | Package Suffix | Purpose |
|--------|---------------|---------|
| `dev` | `.dev` | Local development — tinted launcher icon |
| `staging` | `.staging` | Pre-release testing — tinted launcher icon |
| `prod` | *(none)* | Production release |

### Configuration

- Each flavor has its own `BASE_URL` sourced from `local.properties`
- Each flavor gets its own `google-services.json` under `app/src/<flavor>/`
- All three flavors can be installed **side by side** on the same device
- Dev and staging use a **tinted launcher icon** to be visually distinct from production

### Secrets Management

- API keys and secrets live in `local.properties` and are injected as `BuildConfig` fields at build time
- **Never** hardcode secrets in source files
- In CI, secrets are provided as encrypted environment secrets using the same Gradle property names
- `.env.example` is provided as a template; `local.properties` is in `.gitignore`

### StrictMode

- Enabled in the `Application` class for **debug builds only**
- Thread policy: catches disk and network reads on the main thread
- VM policy: catches leaked activities, closeable objects, and cursors

---

## Gradle Build Performance

Pre-configure `gradle.properties` with:

| Setting | Purpose |
|---------|---------|
| `org.gradle.parallel=true` | Parallel module execution |
| `org.gradle.caching=true` | Build caching |
| `org.gradle.configuration-cache=true` | Configuration caching |
| `org.gradle.vfs.watch=true` | File system watching |
| `org.gradle.jvmargs=-Xmx4g` | Increased JVM heap |
| `android.nonTransitiveRClass=true` | Prevents modules accessing transitive resources; reduces incremental build times |
| `kotlin.incremental=true` | Kotlin incremental compilation |

Remote build cache is configured as a commented-out placeholder in `settings.gradle.kts`.

---

## Project Structure After Phase 1

```
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
├── app/
│   └── src/
│       ├── main/
│       ├── dev/
│       ├── staging/
│       └── prod/
├── gradle/
│   └── libs.versions.toml
├── gradle.properties
├── settings.gradle.kts
├── local.properties          (gitignored)
├── .env.example
└── setup.py
```

---

## Deliverables Checklist

- [ ] `build-logic/convention/` with all 9 convention plugins
- [ ] `gradle/libs.versions.toml` with all library versions, aliases, and bundles
- [ ] `gradle.properties` with performance flags configured
- [ ] `settings.gradle.kts` with module includes and remote cache placeholder
- [ ] Product flavor configuration (dev / staging / prod) in the application plugin
- [ ] `.env.example` template file
- [ ] `setup.py` script for project renaming
- [ ] Empty module directories created for all core and feature modules
- [ ] Project compiles successfully with no source code yet
