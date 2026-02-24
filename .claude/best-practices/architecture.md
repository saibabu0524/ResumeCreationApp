# architecture.md — Module Boundaries, Checklists, AI Pitfalls

> **Full layer/module details:** `docs/ai/PROJECT_MEMORY.md` (Module Map + Layer Responsibilities)
> **ADR rationale:** `docs/ai/ARCHITECTURE_DECISIONS.md` (ADR-001 through ADR-010)

---

## Dependency Direction — Banned Combinations

These must never exist. If AI generates them, reject immediately:

| Banned | Why |
|---|---|
| Feature → Feature | Circular dependency, breaks independent builds |
| `core:domain` → any Android import | Pure Kotlin boundary |
| `core:data` → any feature module | Unidirectional data flow |
| Any module → `:app` | Only `:app` is the aggregator |
| `core:testing` as `implementation`/`api` | testImplementation only, everywhere |

---

## Convention Plugin Map (quick ref)

| Module type | Apply this plugin |
|---|---|
| `:app` | `AndroidApplicationPlugin` |
| Android library | `AndroidLibraryPlugin` |
| Feature module | `AndroidFeaturePlugin` (Compose + Hilt included) |
| Compose-only lib | `AndroidComposePlugin` |
| Hilt-only lib | `AndroidHiltPlugin` |
| Room lib | `AndroidRoomPlugin` |
| Test module | `AndroidTestingPlugin` |

Never add Compose or Hilt config manually to a module — always via the plugin.

---

## Adding a New Feature Module — Checklist

1. `feature/<name>/build.gradle.kts` → apply `AndroidFeaturePlugin`
2. `settings.gradle.kts` → `include(":feature:<name>")`
3. `:app/build.gradle.kts` → add `implementation(project(":feature:<name>"))`
4. `navigation/<Name>Navigation.kt` → `@Serializable` route object/data class
5. `:app/navigation/AppNavHost.kt` → register `composable<Route> { ... }`
6. Create: `<Name>Screen.kt`, `<Name>Route.kt`, `<Name>ViewModel.kt`,
   `<Name>UiState.kt`, `<Name>UserIntent.kt`, `<Name>UiEvent.kt`
7. `src/test/` → add `<Name>ViewModelTest.kt` with `MainDispatcherRule`

---

## Adding a New Core Module — Checklist

1. `core/<name>/build.gradle.kts` → apply `AndroidLibraryPlugin` (+ others as needed)
2. `settings.gradle.kts` → `include(":core:<name>")`
3. `di/<Name>Module.kt` → `@Module @InstallIn(SingletonComponent::class)`
4. Consuming modules → add `implementation(project(":core:<name>"))` or `api(...)`

---

## AI-Specific Pitfalls (the most common mistakes)

**Pitfall 1 — Repository implementation placed in domain**
AI often writes `UserRepositoryImpl` inside the `core:domain` package. Always move it to `core:data`.
Domain contains only the interface; data contains the implementation.

**Pitfall 2 — Cross-feature navigation via direct import**
AI writes `import feature.profile.navigateToProfile`. This is banned.
Feature A exposes `onNavigateToB: () -> Unit` as a lambda param. `:app` NavHost wires it.

**Pitfall 3 — Version hardcoded in build.gradle.kts**
AI writes `implementation("com.squareup.retrofit2:retrofit:2.11.0")`.
All versions belong in `gradle/libs.versions.toml`. The build file uses `libs.retrofit`.

**Pitfall 4 — `buildSrc/` suggested instead of `build-logic/`**
AI frequently suggests `buildSrc/` for shared build logic. This project uses `build-logic/convention/`.
`buildSrc/` invalidates the full Gradle cache on every change — never use it.
