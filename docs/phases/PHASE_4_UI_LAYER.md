# Phase 4 — UI Layer, Theme & Component Library

**Goal**: Build the complete Compose UI infrastructure — theme system, reusable component library, and the presentation layer patterns (ViewModel + UDF). After this phase, feature screens can be built rapidly using pre-made, accessible components.

**Implementation Order Steps**: 12 (partial)

---

## Table of Contents

- [Overview](#overview)
- [core:ui Module — Theme System](#coreui-module--theme-system)
- [core:ui Module — Component Library](#coreui-module--component-library)
- [Presentation Layer Patterns](#presentation-layer-patterns)
- [Compose Performance](#compose-performance)
- [Adaptive Layouts](#adaptive-layouts)
- [Accessibility](#accessibility)
- [Localization](#localization)
- [Deliverables Checklist](#deliverables-checklist)

---

## Overview

`:core:ui` is the shared Compose component library used by every feature module. It contains the theme, all reusable components, and enforces accessibility from the start. This phase also establishes the ViewModel + Unidirectional Data Flow patterns that every feature screen will follow.

---

## core:ui Module — Theme System

**Plugin**: `AndroidComposeConventionPlugin` + `AndroidLibraryConventionPlugin`

### Color System

| Variant | Behavior |
|---------|----------|
| **Light mode** | Custom `ColorScheme` definitions |
| **Dark mode** | Custom `ColorScheme` definitions |
| **Dynamic color** (Android 12+) | Supported with graceful fallback to custom colors on older API levels |

### Typography

- Defined type scale using Material3 typography
- Consistent across all screens

### Shape Tokens

- Centralized shape definitions (small, medium, large corners)

### Spacing via CompositionLocal

```kotlin
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

val LocalSpacing = compositionLocalOf { Spacing() }
```

Screens **never hardcode** numeric margin or padding values — they use `LocalSpacing.current`.

---

## core:ui Module — Component Library

Every component has accessibility built in from the start — content descriptions, semantic roles, and minimum **48dp touch targets** enforced at the base component level.

### Components

| Component | Variants / Features |
|-----------|-------------------|
| `AppButton` | Primary, secondary, text, loading state |
| `AppTextField` | Error state, label, icon slots |
| `AppTopBar` | Configurable back navigation, action support |
| `AppBottomBar` | Navigation item configuration |
| `NavigationSuiteScaffold` wrapper | Adaptive navigation (bottom bar / rail / drawer) |
| `AppScaffold` | Standard loading, error, empty state delegation |
| `LoadingIndicator` | Centered loading spinner |
| `EmptyState` | Configurable icon, title, message, action |
| `ErrorState` | Retry action, error message display |
| `AppDialog` | Standard dialog wrapper |
| `AppBottomSheet` | Standard bottom sheet wrapper |
| `NetworkImage` | Coil wrapper with loading placeholder and error placeholder |

### Component Testing

Every component in `:core:ui` has its own Compose test that verifies:
- Correct rendering in every variant
- Accessibility properties are correctly set
- Catches regressions before any feature uses the component

---

## Presentation Layer Patterns

Every feature screen follows these patterns. They are established here so feature modules (Phase 5) are consistent.

### ViewModel Pattern

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // UI state as StateFlow
    val uiState: StateFlow<ExampleUiState> = someUseCase(params)
        .map { result -> result.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000), // 5s = ANR timeout
            initialValue = ExampleUiState.Loading,
        )

    // One-time events via Channel
    private val _events = Channel<ExampleEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ExampleAction) {
        when (action) {
            is ExampleAction.ItemClicked -> { /* handle */ }
        }
    }
}
```

### UI State Pattern

```kotlin
sealed interface ExampleUiState {
    data object Loading : ExampleUiState
    data class Success(val items: List<Item>) : ExampleUiState
    data class Error(val message: String) : ExampleUiState
}
```

### Composable Pattern

```kotlin
@Composable
fun ExampleScreen(viewModel: ExampleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExampleEvent.NavigateTo -> { /* navigate */ }
                is ExampleEvent.ShowSnackbar -> { /* show snackbar */ }
            }
        }
    }

    ExampleContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
```

### Key Rules

| Rule | Reason |
|------|--------|
| Use `collectAsStateWithLifecycle` | Avoids background flow collection |
| `WhileSubscribed(5_000)` for `stateIn` | Config change doesn't restart flow; genuine navigation away stops it |
| One-time events via `Channel` | Never replayed to new collectors |
| Composables send intents, not direct calls | ViewModel decides what to do |

---

## Compose Performance

### Stability Annotations

| Annotation | When to Use |
|------------|------------|
| `@Immutable` | Data classes whose properties never change after construction |
| `@Stable` | Classes whose properties may change but notify Compose on change |

Domain and UI model classes used as Composable parameters **must** be annotated appropriately. Getting stability wrong silently degrades Compose performance.

### Compose Compiler Reports

The convention plugin includes a Gradle task to generate compiler reports showing:
- Which composables are skippable
- Which parameters are stable vs unstable
- **Must be run before release builds** — unstable classes cause unnecessary recompositions

### Shared Element Transitions

Reference implementation included for shared element transitions between list item ↔ detail screen using Compose 1.7+ stable APIs. This is significantly harder to retrofit later.

---

## Adaptive Layouts

### WindowSizeClass

Primary mechanism for adapting to different screen sizes:

| Window Size | Navigation Pattern |
|------------|-------------------|
| **Compact** (phone) | Bottom navigation bar |
| **Medium** (tablet portrait) | Navigation rail |
| **Expanded** (tablet landscape) | Navigation drawer |

Handled automatically by `NavigationSuiteScaffold` — no conditional logic needed in individual feature screens.

### Additional Adaptive Features

- List-detail pattern: two-pane on expanded, single-pane on compact
- Foldable posture handling via `WindowInfoTracker`
- Edge-to-edge display enabled by default
- `WindowInsets` applied correctly via `AppScaffold`

---

## Accessibility

| Enforcement | Level |
|-------------|-------|
| Content descriptions | Required on all image components (enforced at `NetworkImage` / icon button level) |
| Touch targets | Minimum 48dp × 48dp (enforced at `AppButton` level) |
| Semantic roles | Custom interactive components include semantics blocks |
| Testing | Compose UI tests verify accessibility properties for every component variant |

---

## Localization

| Rule | Details |
|------|---------|
| All strings in `strings.xml` | From the first commit — no string literals in Kotlin source |
| Plurals | Use `<plurals>` resource type |
| RTL support | Verified via layout direction overrides in Compose Previews |
| `setup.py` | Flags hardcoded strings during project renaming |

---

## Deliverables Checklist

- [ ] `:core:ui` theme — `ColorScheme` (light + dark), dynamic color support, typography, shapes, spacing via `CompositionLocal`
- [ ] `:core:ui` component library — all 12 components listed above with full variant support
- [ ] Accessibility enforcement at base component level (content descriptions, 48dp touch targets, semantic roles)
- [ ] Compose tests for every component variant in `:core:ui`
- [ ] ViewModel + UDF pattern reference implementation
- [ ] `@Immutable` / `@Stable` annotations on domain and UI model classes
- [ ] Shared element transition reference implementation
- [ ] `NavigationSuiteScaffold` wrapper for adaptive navigation
- [ ] `strings.xml` with all user-facing strings (no hardcoded strings in Kotlin)
- [ ] RTL layout verification setup
