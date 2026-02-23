# Phase 5 — Feature Modules & Navigation

**Goal**: Build the first feature modules end-to-end and wire up the full navigation system. The auth feature validates that all core modules work together. After this phase, the app has real screens, real navigation, and a functional end-to-end flow.

**Implementation Order Steps**: 13–14

---

## Table of Contents

- [Overview](#overview)
- [feature:auth Module](#featureauth-module)
- [feature:home Module](#featurehome-module)
- [feature:settings Module](#featuresettings-module)
- [feature:profile Module](#featureprofile-module)
- [App Navigation](#app-navigation)
- [Navigation Architecture Rules](#navigation-architecture-rules)
- [Deep Links](#deep-links)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

Feature modules depend on `:core:ui`, `:core:domain`, `:core:analytics`, `:core:feature-flags`, and `:core:common` — never on other feature modules. Each uses the `AndroidFeatureConventionPlugin` from Phase 1. `:feature:auth` is the first feature built end-to-end to validate all core modules work together.

---

## feature:auth Module

**Plugin**: `AndroidFeatureConventionPlugin`

The **first feature built end-to-end** — validates all core modules integrate correctly.

### Screens
- Login screen
- Registration screen
- Onboarding flow

### Responsibilities
- Token management (store via `:core:datastore` encrypted preferences)
- Auth state observation (logged in / logged out)
- Input validation (email, password strength)
- Error state handling (invalid credentials, network errors)

---

## feature:home Module

**Plugin**: `AndroidFeatureConventionPlugin`

Primary post-login screen and **reference implementation** for several patterns.

### Reference Patterns

| Pattern | Implementation |
|---------|---------------|
| **Paging 3** | Complete implementation with `RemoteMediator` for network + database pagination |
| **`collectAsLazyPagingItems`** | Correct Compose integration for paginated lists |
| **Flow operators** | `combine`, `flatMapLatest` (search/filter), `debounce` (search input) |
| **Shared mutable state** | `Mutex` usage for concurrent cache access |

---

## feature:settings Module

**Plugin**: `AndroidFeatureConventionPlugin`

### Screens
- Theme selection (light / dark / system)
- Notification preferences
- App settings

---

## feature:profile Module

**Plugin**: `AndroidFeatureConventionPlugin`

### Screens
- User profile display / edit

### Reference Implementation
- **Biometric authentication** using `BiometricPromptManager` from `:core:common`

---

## App Navigation

### Type-Safe Routes

Routes are defined using `@Serializable` Kotlin data classes for compile-time safety:

```kotlin
@Serializable
data class ProfileRoute(val userId: String)

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute
```

### Feature Navigation Graphs

Each feature module exposes its navigation graph as an extension function:

```kotlin
fun NavGraphBuilder.authNavGraph(
    onNavigateToHome: () -> Unit,
) {
    navigation<AuthRoute>(startDestination = LoginRoute) {
        composable<LoginRoute> { /* ... */ }
        composable<RegistrationRoute> { /* ... */ }
    }
}
```

### Root AppNavHost

`:app` assembles all feature graphs in one place:

```kotlin
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AuthRoute) {
        authNavGraph(onNavigateToHome = { /* ... */ })
        homeNavGraph(/* ... */)
        settingsNavGraph(/* ... */)
        profileNavGraph(/* ... */)
    }
}
```

---

## Navigation Architecture Rules

| Rule | Reason |
|------|--------|
| Features **never navigate directly** to other features | No cross-feature dependencies |
| Navigation events → ViewModel as intents → events channel → `AppNavHost` handles | Keeps features decoupled |
| Back stack state saved/restored correctly | `rememberSaveable` for UI state, `SavedStateHandle` for process death |

---

## Deep Links

- Every screen reachable from a notification or external link has its deep link defined alongside its route
- Configured from the beginning — retrofitting is painful
- Uses the same `@Serializable` route definitions as in-app navigation

---

## Deliverables Checklist

- [ ] `:feature:auth` — login, registration, onboarding screens (end-to-end validation of all core modules)
- [ ] `:feature:home` — primary screen with Paging 3, flow operators reference
- [ ] `:feature:settings` — theme selection, notification preferences
- [ ] `:feature:profile` — user profile with biometric authentication reference
- [ ] `AppNavHost` in `:app` — assembles all feature navigation graphs
- [ ] `NavigationSuiteScaffold` — adaptive navigation (bottom bar / rail / drawer)
- [ ] Type-safe `@Serializable` routes for all screens
- [ ] Deep links configured for all externally reachable screens
- [ ] Shared element transitions between list and detail screens
- [ ] Verify: no feature depends on another feature
