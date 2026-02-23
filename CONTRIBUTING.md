# Contributing to Android Compose Template

Thank you for contributing! This document outlines the conventions and workflows for this project.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Branch Naming](#branch-naming)
- [Commit Messages](#commit-messages)
- [Code Style](#code-style)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Code Review Guidelines](#code-review-guidelines)
- [Architecture Rules](#architecture-rules)

---

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/Resumecreationapp.git
   cd Resumecreationapp
   ```

2. **Open in Android Studio** (Hedgehog or later recommended).

3. **Sync Gradle** and let the project index.

4. **Run the app** to verify the setup:
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run tests** before making changes:
   ```bash
   ./gradlew test
   ```

---

## Branch Naming

Use the following prefixes:

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/short-description` | `feature/biometric-login` |
| Bug fix | `fix/issue-description` | `fix/login-crash-on-empty-email` |
| Refactor | `refactor/module-or-area` | `refactor/user-repository` |
| Chore | `chore/task-description` | `chore/update-compose-bom` |
| Docs | `docs/description` | `docs/add-contributing-guide` |
| Test | `test/description` | `test/add-home-vm-tests` |

**Rules:**
- Always branch from `main`
- Use lowercase with hyphens, no underscores
- Keep descriptions concise (2-4 words)

---

## Commit Messages

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): short description (max 72 chars)

[optional body explaining the "why"]

[optional footer, e.g. Closes #123]
```

### Allowed Types

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `build` | Build system or dependency changes |
| `ci` | CI/CD configuration |
| `chore` | Housekeeping (non-code) |
| `perf` | Performance improvement |

### Scope Examples

Scope should match the affected module:
- `feat(auth): add biometric login support`
- `fix(network): handle 503 service unavailable`
- `refactor(home): extract UserCard to core:ui`
- `test(profile): add unit tests for UpdateProfileUseCase`
- `build(deps): update compose-bom to 2024.12.01`

### Breaking Changes

Add `!` after the type/scope and explain in the body:
```
feat(auth)!: replace token storage with EncryptedDataStore

BREAKING CHANGE: TokenStorage API has changed. All callers must migrate.
```

---

## Code Style

Code formatting is enforced automatically:

| Tool | What it does | How to run |
|------|-------------|------------|
| **Ktlint** | Code formatting | `./gradlew ktlintFormat` |
| **Detekt** | Architecture & quality rules | `./gradlew detekt` |
| **Spotless** | Auto-format (wraps Ktlint) | `./gradlew spotlessApply` |

### Before Committing

```bash
# Auto-format all code
./gradlew ktlintFormat

# Check for issues
./gradlew detekt

# Run all tests
./gradlew test
```

### Key Rules

- **Max line length:** 120 characters
- **Trailing commas:** Always use in multi-line constructs
- **Explicit return types:** Required for `public` and `internal` functions
- **No `!!` force unwraps** in production code
- **No `Dispatchers.IO`** — use `@IoDispatcher` qualifier
- **No `LiveData`** — use `StateFlow` / `Flow`
- **No hardcoded strings** — use `stringResource()` from `strings.xml`
- **No hardcoded colors** — use `MaterialTheme.colorScheme`

See [`docs/ai/CODING_STANDARDS.md`](docs/ai/CODING_STANDARDS.md) for the complete list.

---

## Testing Requirements

Every PR **must** include tests for the changed code:

### Unit Tests
- Every new `UseCase` → corresponding test class
- Every new `ViewModel` → corresponding test class
- Every new `Repository` → test class using fakes from `core:testing`

### Test Conventions
- Use **fakes over mocks** (fakes live in `core:testing`)
- Use **Turbine** for Flow testing — assert exact values in exact order
- Use **`UnconfinedTestDispatcher`** from `core:testing`
- Follow the **AAA pattern**: Arrange, Act, Assert
- Use backtick test names: `` `when condition, does expected thing` ``

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :feature:home:test

# Run tests with coverage
./gradlew testDebugUnitTest
```

---

## Pull Request Process

### Before Opening a PR

1. ✅ Ensure all tests pass: `./gradlew test`
2. ✅ Format code: `./gradlew ktlintFormat`
3. ✅ Run quality checks: `./gradlew detekt`
4. ✅ Verify clean build: `./gradlew assembleDebug`
5. ✅ Rebase on latest `main` — resolve conflicts before opening PR

### PR Rules

- **< 400 lines** changed per PR — split large changes
- **One feature/fix per PR** — no bundling unrelated changes
- Include **screenshots** for UI changes (light + dark mode)
- Fill out the **PR template** completely

### PR Title Format

Follow Conventional Commits format:
```
feat(home): add user search functionality
fix(auth): prevent crash on empty email validation
```

### Pre-release Checklist

Before cutting a new release or merging significant feature work, complete these manual verifications and updates:
1. **Baseline Profile Regeneration**: Run the `:baselineprofile` generator on a physical device. Commit the updated `baseline-prof.txt` file. This must be regenerated whenever significant new screens or user flows are added to keep cold startup optimized.
2. **Screenshot Test Baselines**: When you make deliberate UI changes, run Roborazzi/Paparazzi locally to regenerate the baseline images for `:core:ui`. Commit these new baselines so CI doesn't fail on visual diffs.
3. **Memory Leak Verification**: With LeakCanary active in a dev build, manually navigate through every screen: launch, interact with lists, navigate forward and back, and rotate the device. Clear out any reported leaks, paying special attention to screens using `DisposableEffect` or holding Context references.

---

## Code Review Guidelines

### As an Author

- Self-review your code before requesting review
- Respond to all comments, even if just acknowledging
- Don't force-push during review — makes tracking changes difficult

### As a Reviewer

Check for:

**Architecture:**
- [ ] Module boundaries respected (no cross-layer violations)
- [ ] Business logic in UseCases, not ViewModel or Repository
- [ ] Domain models free of Android imports

**Coroutines:**
- [ ] Dispatchers injected (not `Dispatchers.IO` hardcoded)
- [ ] Exceptions caught and mapped to `AppException`
- [ ] `collectAsStateWithLifecycle()` used in Composables

**Compose:**
- [ ] Composables are stateless (state hoisted)
- [ ] Preview functions for light + dark mode
- [ ] Touch targets ≥ 48.dp
- [ ] Images have proper `contentDescription`

**Testing:**
- [ ] New code has corresponding unit tests
- [ ] Flows tested with Turbine
- [ ] Fakes used over mocks for repositories

---

## Architecture Rules

These are **non-negotiable** and enforced in code review:

1. **Feature modules NEVER depend on other features** — only via `:app` NavHost
2. **`:core:domain` has ZERO Android dependencies** — pure Kotlin
3. **Data layer knows about domain, never presentation**
4. **App module is the ONLY module that knows all features**
5. **`core:testing` is ONLY a testImplementation dependency**

See [`docs/ai/ARCHITECTURE_DECISIONS.md`](docs/ai/ARCHITECTURE_DECISIONS.md) for detailed ADRs.

---

## Need Help?

- Check the [Phase documentation](docs/phases/) for implementation details
- Read the [Project Memory](docs/ai/PROJECT_MEMORY.md) for design decisions
- Review the [Architecture Decisions](docs/ai/ARCHITECTURE_DECISIONS.md) for why things are structured the way they are
