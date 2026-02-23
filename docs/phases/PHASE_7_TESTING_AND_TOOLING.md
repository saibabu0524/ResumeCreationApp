# Phase 7 — Testing Strategy & Developer Tooling

**Goal**: Build the shared testing infrastructure, write unit and integration tests for all core modules, and set up code quality tooling. After this phase, every module is tested and code quality is enforced automatically.

**Implementation Order Steps**: 16–17

---

## Table of Contents

- [Overview](#overview)
- [core:testing Module](#coretesting-module)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [UI Testing](#ui-testing)
- [Developer Tooling & Code Quality](#developer-tooling--code-quality)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

Testing philosophy: **fakes over mocks** at repository and data source boundaries. Fakes live in `:core:testing` alongside the interfaces they implement. MockK is reserved for verifying specific interactions (e.g., analytics event sent with correct parameters).

---

## core:testing Module

**Plugin**: `AndroidTestingConventionPlugin`

Shared test infrastructure used by all modules' test source sets.

### Contents

| Component | Purpose |
|-----------|---------|
| **Fake implementations** | Fakes for all repository interfaces — tests are resilient to refactoring |
| **`TestDispatcherRule`** | JUnit rule for coroutine test scope setup — used in every test class |
| **Test utilities** | Shared assertion helpers, test data builders |
| **Shared fixtures** | Common test data objects |

### Why Fakes Over Mocks

Changing internal repository implementation without changing its interface **does not break any test**. Mocks are tightly coupled to implementation details.

---

## Unit Testing

### ViewModel Tests

Every ViewModel is testable in isolation:
- Dispatchers are injected → replaced with `UnconfinedTestDispatcher` in tests
- Repository dependencies are injected interfaces → implemented by fakes

### Use Case Tests

Every use case is testable in isolation:
- Repository dependencies are injected interfaces → implemented by fakes

### Repository Tests

Every repository is testable:
- Tested with fake remote and local data sources

### Flow Testing with Turbine

```kotlin
@Test
fun `emits loading then success`() = runTest {
    viewModel.uiState.test {
        assertEquals(UiState.Loading, awaitItem())
        assertEquals(UiState.Success(data), awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}
```

Turbine tests assert on **exact values in exact order**.

### JUnit 5 Support

- Uses the `android-junit5` Gradle plugin
- Plugin must be declared as a **dependency of `build-logic`** itself (common confusion point)

---

## Integration Testing

| Target | Approach |
|--------|----------|
| **Room** | In-memory database — isolated and fast |
| **DataStore** | In-memory implementation |
| **WorkManager** | `TestDriver` — immediate constraint satisfaction, no real scheduling wait |

---

## UI Testing

### Compose UI Tests

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun `button shows loading state`() {
    composeTestRule.setContent {
        AppButton(text = "Submit", isLoading = true, onClick = {})
    }
    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
}
```

Use semantic matchers for accessibility-aware testing.

### Screenshot Regression Tests

| Tool | Purpose |
|------|---------|
| **Roborazzi** or **Paparazzi** | Run in CI, fail build on any visual difference |

Acts as a safety net for accidental UI changes.

---

## Developer Tooling & Code Quality

### Linting & Formatting

| Tool | Purpose | Enforcement |
|------|---------|-------------|
| **Ktlint** | Code formatting | Pre-commit Git hook — unformatted code cannot be committed |
| **Detekt** | Clean Architecture rules | Domain classes cannot import Android SDK; features cannot import each other |
| **Spotless** | Wraps Ktlint + Detekt | Single Gradle task for auto-formatting |

All three run in CI on every pull request.

### Dependency Guard

- Locks full dependency tree to a baseline file in the repo
- CI build fails on any unexpected dependency change (including transitive version bumps)
- Prevents silent dependency upgrades

### Git Conventions

| Convention | Enforcement |
|-----------|-------------|
| **Conventional Commits** | `commitlint` |
| **Branch naming** | Documented in `CONTRIBUTING.md` |
| **PR template** | `.github/pull_request_template.md` with pre-flight checklist |

---

## Deliverables Checklist

- [ ] `:core:testing` module — fakes for all repository interfaces, `TestDispatcherRule`, test utilities, shared fixtures
- [ ] Unit tests — all ViewModels, use cases, repositories testable in isolation
- [ ] Flow tests — Turbine tests for all `Flow` emissions
- [ ] Integration tests — Room (in-memory), DataStore (in-memory), WorkManager (`TestDriver`)
- [ ] UI tests — Compose tests for all `:core:ui` components
- [ ] Screenshot regression tests — Roborazzi/Paparazzi baseline
- [ ] Ktlint — configured with pre-commit Git hook
- [ ] Detekt — configured with Clean Architecture rule set
- [ ] Spotless — wraps Ktlint and Detekt
- [ ] Dependency Guard — baseline file committed
- [ ] `commitlint` — Conventional Commits enforced
- [ ] `CONTRIBUTING.md` — branch naming, PR checklist, code review process
- [ ] `.github/pull_request_template.md` — pre-flight checklist
