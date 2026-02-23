# Implementation Phases

This project is divided into **8 phases**, ordered so that each phase builds on the previous one. Follow them in sequence — skipping steps results in refactoring work when the first feature is built.

---

## Phase Overview

| Phase | Name | Modules / Scope | README |
|-------|------|-----------------|--------|
| **1** | Project Foundation & Build System | `build-logic/`, `libs.versions.toml`, Gradle config | [PHASE_1_PROJECT_FOUNDATION.md](PHASE_1_PROJECT_FOUNDATION.md) |
| **2** | Core Domain & Common Infrastructure | `:core:common`, `:core:domain`, `:core:analytics`, `:core:feature-flags` | [PHASE_2_CORE_DOMAIN.md](PHASE_2_CORE_DOMAIN.md) |
| **3** | Networking, Storage & Data Layer | `:core:network`, `:core:database`, `:core:datastore`, `:core:data` | [PHASE_3_NETWORKING_AND_STORAGE.md](PHASE_3_NETWORKING_AND_STORAGE.md) |
| **4** | UI Layer, Theme & Component Library | `:core:ui`, presentation patterns, adaptive layouts | [PHASE_4_UI_LAYER.md](PHASE_4_UI_LAYER.md) |
| **5** | Feature Modules & Navigation | `:feature:auth`, `:feature:home`, `:feature:settings`, `:feature:profile`, `AppNavHost` | [PHASE_5_FEATURES_AND_NAVIGATION.md](PHASE_5_FEATURES_AND_NAVIGATION.md) |
| **6** | Background Work, Notifications & Security | `:core:work`, `:core:notifications`, security hardening, splash, in-app updates | [PHASE_6_WORK_NOTIFICATIONS_SECURITY.md](PHASE_6_WORK_NOTIFICATIONS_SECURITY.md) |
| **7** | Testing Strategy & Developer Tooling | `:core:testing`, unit/integration/UI tests, Ktlint, Detekt, Dependency Guard | [PHASE_7_TESTING_AND_TOOLING.md](PHASE_7_TESTING_AND_TOOLING.md) |
| **8** | CI/CD, Performance & Polish | GitHub Actions, Baseline Profiles, benchmarks, accessibility audit, final polish | [PHASE_8_CICD_PERFORMANCE_POLISH.md](PHASE_8_CICD_PERFORMANCE_POLISH.md) |

---

## Dependency Flow

```
Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 4 ──► Phase 5
                                                    │
                                               Phase 6
                                                    │
                                               Phase 7
                                                    │
                                               Phase 8
```

- **Phases 1–5** are strictly sequential — each depends on the previous
- **Phase 6** can be started after Phase 5 is stable
- **Phase 7** should be done alongside or immediately after Phase 6
- **Phase 8** is the final phase and requires all others to be complete

---

## Quick Start

1. Read through each phase README in order
2. Complete all items in the **Deliverables Checklist** at the bottom of each phase
3. Verify the project compiles after each phase before moving to the next
4. Refer to the main [README.md](../../README.md) for the complete technical specification
