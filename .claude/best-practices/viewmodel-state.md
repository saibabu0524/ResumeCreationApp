# viewmodel-state.md — UDF Pattern, StateFlow, AI Pitfalls

> **Full templates (ViewModel, UiState, UserIntent, UiEvent):** `docs/ai/PROJECT_MEMORY.md` §Patterns
> **Flow/StateFlow standards:** `docs/ai/CODING_STANDARDS.md` §4 Coroutines & Flow Standards
> **ADR reasoning (ADR-001 to ADR-004):** `docs/ai/ARCHITECTURE_DECISIONS.md`

---

## UDF Contract (quick visual)

```
UI  ──onEvent(UserIntent)──▶  ViewModel  ──StateFlow──▶  UI recomposes
                                 │
                                 └──Channel<UiEvent>──▶  LaunchedEffect handles once
```

Single entry point: `fun onEvent(intent: XxxUserIntent)` — no other public methods
that mutate state.

---

## Critical ViewModel Structure Rules

```kotlin
// These three lines are the required pattern — never deviate
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()   // never expose Mutable

private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED)   // not SharedFlow
val uiEvent = _uiEvent.receiveAsFlow()
```

**Why `Channel` not `SharedFlow` for events?** `SharedFlow` drops events when there's no
collector (e.g. screen is in the back stack). `Channel.BUFFERED` queues them safely.

---

## Atomic State Update — One Rule

```kotlin
// ✅ Always — atomic, thread-safe
_uiState.update { it.copy(isLoading = true, error = null) }

// ❌ Never — race condition when multiple coroutines update simultaneously
_uiState.value = _uiState.value.copy(isLoading = true)
```

---

## UiState — Flat Data Class (ADR-003)

AI often generates sealed `Loading/Success/Error`. Always reject this. UiState is **always** a
flat `data class` so cached data can be shown while refreshing:

```kotlin
// ✅ flat — can show users list while isRefreshing = true
data class HomeUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)

// ❌ sealed — hides data when loading, impossible to show stale + spinner
sealed class HomeUiState { object Loading; data class Success(...); data class Error(...) }
```

---

## Common AI Mistakes

- **Exposing `MutableStateFlow` publicly** — must always be `private val _uiState`
  with `val uiState = _uiState.asStateFlow()`
- **Using `sealed class UiState`** — always flat `data class` (ADR-003)
- **Using `SharedFlow` for one-shot events** — use `Channel<UiEvent>(Channel.BUFFERED)`
- **Hardcoding `Dispatchers.IO`** — inject `@IoDispatcher CoroutineDispatcher`
  (defined in `core:common/di/CoroutineDispatchers.kt`)
- **Context in ViewModel** — never pass `Activity` or `LifecycleOwner`; use
  `@ApplicationContext` only if truly needed
- **Collecting flow inside `init {}` without `launchIn`** — always
  `flow.onEach { }.launchIn(viewModelScope)` so it cancels with the ViewModel
