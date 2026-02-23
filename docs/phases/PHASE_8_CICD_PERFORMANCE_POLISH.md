# Phase 8 — CI/CD, Performance & Polish

**Goal**: Set up the full CI/CD pipeline, establish performance baselines, and complete the final polish pass. After this phase, the template is production-ready — automated quality gates, optimized performance, and verified accessibility.

**Implementation Order Steps**: 18–20

---

## Table of Contents

- [Overview](#overview)
- [CI/CD Pipelines](#cicd-pipelines)
- [Performance — Baseline Profiles & Benchmarks](#performance--baseline-profiles--benchmarks)
- [Polish Pass](#polish-pass)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

This is the final phase. CI/CD ensures every PR is automatically validated. Baseline Profiles and Macrobenchmarks establish performance baselines. The polish pass covers accessibility auditing, RTL verification, screenshot test baselines, and Compose compiler metrics review.

---

## CI/CD Pipelines

Four GitHub Actions workflows in `.github/workflows/`:

| Workflow | Trigger | What It Does |
|----------|---------|-------------|
| `build.yml` | Every PR | Builds debug APK — verifies compilation |
| `test.yml` | Every PR | Runs all unit tests across all modules |
| `lint.yml` | Every PR | Runs Ktlint, Detekt, Dependency Guard — fails on any violation |
| `release.yml` | Version tag push (`v*.*.*`) | Builds signed release AAB, uploads as workflow artifact |

### Release Signing

| Environment | How Secrets Are Provided |
|-------------|-------------------------|
| **Local** | `local.properties` — keystore path, password, alias |
| **CI** | Encrypted GitHub repository secrets — same Gradle property names |

The keystore is stored as an encrypted GitHub repository secret. The signing config reads from `local.properties` locally and from environment secrets in CI using identical property names.

---

## Performance — Baseline Profiles & Benchmarks

### :baselineprofile Module

Macrobenchmark-based Baseline Profile generator:

1. Runs on a connected device
2. Records critical user journeys:
   - Cold app startup
   - Home screen initial load
   - Common scroll interactions
3. Generated profile → `app/src/main/baseline-prof.txt`
4. Committed to version control
5. Play Store uses it for ahead-of-time compilation at install time

> **Why this matters**: Without a Baseline Profile, Compose apps have a noticeably slower cold start on first launch because the JIT compiler hasn't optimized hot code paths yet.

### :benchmark Module

Macrobenchmark tests for runtime performance:

| Metric | What's Measured |
|--------|----------------|
| Startup time | Cold, warm, and hot start durations |
| Scroll performance | Frame timing during list scrolling |
| Other key journeys | Any performance-critical user flows |

---

## Polish Pass

### Accessibility Audit

- [ ] All image components have content descriptions
- [ ] All interactive elements have 48dp × 48dp minimum touch targets
- [ ] Custom interactive components have correct semantic roles
- [ ] Screen reader navigation order is logical
- [ ] Color contrast meets WCAG AA standards

### RTL Verification

- [ ] Layout direction overrides tested in Compose Previews
- [ ] No layout bugs visible in RTL mode
- [ ] Documented manual verification step included

### Screenshot Test Baseline

- [ ] Roborazzi or Paparazzi baseline images generated for all components
- [ ] Baseline committed to version control
- [ ] CI fails on any visual difference

### Compose Compiler Metrics Review

- [ ] Run Compose compiler reports Gradle task
- [ ] Review all composables for skippability
- [ ] Verify parameters are stable (not unstable)
- [ ] Fix any unnecessary recompositions in list-heavy screens

### Memory Leak Verification

- [ ] LeakCanary checked manually on physical device for every screen
- [ ] Step added to `CONTRIBUTING.md` for new screens
- [ ] No active leaks present

### Additional Polish

- [ ] `CHANGELOG.md` — initial release notes
- [ ] All TODO placeholders resolved or documented
- [ ] Final dependency versions verified and locked
- [ ] Gradle remote build cache URL placeholder documented

---

## Deliverables Checklist

- [ ] `.github/workflows/build.yml` — debug APK build on every PR
- [ ] `.github/workflows/test.yml` — all unit tests on every PR
- [ ] `.github/workflows/lint.yml` — Ktlint, Detekt, Dependency Guard on every PR
- [ ] `.github/workflows/release.yml` — signed AAB on version tag push
- [ ] Release signing configuration (local + CI)
- [ ] `:baselineprofile` — Baseline Profile generator with critical user journeys
- [ ] `:benchmark` — Macrobenchmark tests for startup and scroll performance
- [ ] `baseline-prof.txt` committed to `app/src/main/`
- [ ] Accessibility audit complete
- [ ] RTL verification complete
- [ ] Screenshot test baseline established
- [ ] Compose compiler metrics reviewed — no unnecessary recompositions
- [ ] Memory leak verification on physical device
- [ ] `CHANGELOG.md` written
- [ ] Template is production-ready
