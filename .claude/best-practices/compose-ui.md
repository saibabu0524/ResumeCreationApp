# compose-ui.md — Compose Patterns, Recomposition, AI Pitfalls

> **Full Compose standards:** `docs/ai/CODING_STANDARDS.md` §3 (Compose UI Standards)
> **Preview pattern:** `docs/ai/CODING_STANDARDS.md` §3 Preview Rules
> **Pitfall detail (10 items):** `docs/ai/ARCHITECTURE_DECISIONS.md` Common Pitfalls

---

## Non-Negotiable Rules (AI violations to watch for)

- Never pass ViewModel directly into a non-Route composable — pass `uiState` + lambda callbacks
- Always `collectAsStateWithLifecycle()` — never `collectAsState()` (breaks lifecycle awareness)
- `Modifier` is always the **last** parameter with default `= Modifier`
- Never use `Color(0xFFxxxxxx)` — always `MaterialTheme.colorScheme.*`
- Never hardcode `16.dp` — always `MaterialTheme.spacing.medium` (see spacing table below)
- `key = { it.id }` on every `LazyColumn items {}` — no exceptions
- No business logic inside any `@Composable` function

---

## Route vs Screen Split (AI forgets this constantly)

```kotlin
// Route — connects ViewModel, NOT previewed
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel(), onNavigateToProfile: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.NavigateToProfile -> onNavigateToProfile(event.userId)
            }
        }
    }
    HomeScreen(uiState = uiState, onUserClicked = { viewModel.onEvent(HomeUserIntent.UserClicked(it)) })
}

// Screen — stateless, easy to preview
@Composable
fun HomeScreen(uiState: HomeUiState, onUserClicked: (String) -> Unit, modifier: Modifier = Modifier) { ... }
```

---

## Recomposition Pitfalls (3 root causes)

**1 — Lambda inside `items {}`** creates a new object every recompose → hoist it:
```kotlin
// ❌  items(users) { user -> UserCard(onClick = { viewModel.onEvent(UserClicked(user.id)) }) }
// ✅  items(users, key = { it.id }) { user -> UserCard(onClick = onUserClicked) }
```

**2 — `List<T>` is unstable** → annotate UiState with `@Immutable` or use `ImmutableList<T>`:
```kotlin
@Immutable data class HomeUiState(val users: List<User> = emptyList(), ...)
```

**3 — Derived state not remembered** → use `derivedStateOf`:
```kotlin
// ❌  val isEmpty = uiState.users.isEmpty() && !uiState.isLoading
// ✅  val isEmpty by remember(uiState.users, uiState.isLoading) { derivedStateOf { ... } }
```

---

## Spacing Token Quick Reference

Defined in `core:ui/theme/Spacing.kt` — never use raw dp values:

| Token | Value |
|---|---|
| `MaterialTheme.spacing.extraSmall` | 4.dp |
| `MaterialTheme.spacing.small` | 8.dp |
| `MaterialTheme.spacing.medium` | 16.dp |
| `MaterialTheme.spacing.large` | 24.dp |
| `MaterialTheme.spacing.extraLarge` | 32.dp |

---

## Side Effects — Which One to Use

> Not in `docs/ai/` — unique to this file.

| Effect | Use case |
|---|---|
| `LaunchedEffect(key)` | One-shot event collection, async work keyed to a value |
| `DisposableEffect(key)` | Register/unregister listeners with cleanup |
| `SideEffect` | Push Compose state to non-Compose code on every composition |
| `rememberCoroutineScope()` | Launch coroutines from click handlers (NOT for init work) |
| `produceState` | Convert callbacks/Flows into `State<T>` inside a composable |

**Never** `LaunchedEffect(Unit)` for initial data load — ViewModel `init {}` handles that.
Use `LaunchedEffect(Unit)` only for collecting `uiEvent` one-shot events.

---

## Common AI Mistakes

- Putting `hiltViewModel()` inside `HomeScreen` instead of `HomeRoute`
- Collecting `uiEvent` with `collectAsStateWithLifecycle()` — events use `LaunchedEffect { flow.collect {} }`
- Using `remember { mutableStateOf(...) }` for data that must survive config change (→ ViewModel)
- Forgetting both light + dark `@Preview` functions
- Touch targets below 48.dp on interactive elements
