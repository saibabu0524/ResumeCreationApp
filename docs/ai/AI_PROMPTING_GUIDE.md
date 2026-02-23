# 🤖 AI PROMPTING GUIDE
## How to Work with AI on This Android Template Project

> This guide maximizes the quality and correctness of AI-generated code for this project. Read it before every AI session. The better your prompts, the less time you spend fixing AI mistakes.

---

## 🔑 The Golden Rule

**Always give the AI context before asking for code.** AI has no memory between sessions. Without context, it will write generic Android code that violates this project's architecture. With context, it writes code that fits perfectly.

---

## 📋 Session Starter Template

Copy-paste this at the START of every AI session before asking anything:

```
I'm working on an Android Jetpack Compose multi-module template project.

Key rules for all code you write:
- Language: Kotlin only (no Java)
- UI: Jetpack Compose only (no XML, no Fragments)
- Architecture: MVVM + Clean Architecture with strict layer separation
- DI: Hilt with constructor injection preferred
- Async: Coroutines + Flow only (no LiveData, no RxJava)
- State: StateFlow for UI state, Channel for one-time events
- No force unwraps (!!), no hardcoded dispatchers, no business logic in ViewModel
- Dispatchers injected via @IoDispatcher, @MainDispatcher, @DefaultDispatcher qualifiers
- Result<T> sealed class for all errors (never throw to UI)
- collectAsStateWithLifecycle() in all Composables (not collectAsState())
- Every UseCase has one responsibility and one invoke() function
- Composables are stateless — all state in ViewModel

Module structure:
:app → :feature:* → :core:domain ← :core:data → :core:network, :core:database, :core:datastore
:core:ui (shared components), :core:common (utils), :core:work (WorkManager), :core:testing (fakes)

Now I need help with: [YOUR TASK]
```

---

## 🎯 Task-Specific Prompt Templates

### Creating a New Feature Module

```
Create the full feature module for [FeatureName] following the project's MVVM + Clean Architecture.

The feature needs to:
- [describe what this feature does]
- [list the screens involved]
- [describe data it needs]

Create in this order:
1. Domain layer: [X]UseCase, [X]Repository interface, [X] domain model
2. Data layer: [X]RepositoryImpl, [X]RemoteDataSource, [X]LocalDataSource, [X]Mapper
3. Presentation layer: [X]ViewModel, [X]UiState (data class), [X]UserIntent (sealed interface), [X]UiEvent (sealed interface)
4. UI layer: [X]Screen (stateless), [X]Route (connects to ViewModel), component composables
5. DI: [X]Module with @Binds for repository

Rules:
- UiState is a data class with sensible defaults
- Events via Channel<UiEvent>.receiveAsFlow()
- State updates via _uiState.update { }
- ViewModel takes @IoDispatcher injected dispatcher
- All errors mapped through ExceptionMapper to AppException
- Include KDoc on all public functions
- Include Previews for all composables (light + dark)
```

### Adding a New API Endpoint

```
Add a new API endpoint for [endpoint description].

API details:
- Method: [GET/POST/PUT/DELETE]
- Endpoint: [/api/v1/path]
- Request body: [describe or provide JSON]
- Response: [describe or provide JSON]

Create in this order:
1. UserDto data class (annotated for Gson/Moshi serialization)
2. API service interface function (suspend fun)
3. UserRemoteDataSource with Result<T> wrapping
4. UserMapper (DTO → Domain)
5. Update UserRepository interface with new function
6. Update UserRepositoryImpl
7. Create new UseCase for this operation

Rules:
- All HTTP errors wrapped by ExceptionMapper
- DTO classes in core:network, domain models in core:domain
- Mapper in core:data
- No business logic in data source or repository — only in UseCase
```

### Writing Unit Tests

```
Write comprehensive unit tests for [ClassName].

Context:
- Class type: [UseCase / ViewModel / Repository]
- Class code: [paste the class here]
- Available fakes: FakeUserRepository (add/setError methods), FakeNetworkDataSource

Test cases to cover:
- Happy path (success scenario)
- Error scenarios: network failure, not found, unauthorized
- Edge cases: empty list, null fields, concurrent requests
- Loading state transitions (for ViewModel)

Rules:
- Use runTest {} from kotlinx.coroutines.test
- Use Turbine for Flow assertions
- Use FakeX classes (not mocks) for repositories and data sources
- Use MockK (@mockk) only for leaf dependencies (API service, DAO)
- MainDispatcherRule from core:testing for ViewModel tests
- UnconfinedTestDispatcher for UseCase tests
- AAA pattern: Arrange, Act, Assert with comments
- Descriptive test names: "when X, then Y"
- @BeforeEach for setup
```

### Building a Compose Screen

```
Build the Compose UI for [ScreenName].

Screen purpose: [describe what this screen does]
UiState: [paste the UiState data class]
UserIntents: [paste the intent sealed interface]

Create:
1. [Screen]Screen composable — fully stateless, takes UiState + lambda parameters
2. [Screen]Route composable — connects ViewModel, collects state, handles UiEvents
3. Individual component composables in components/ subfolder
4. Preview functions for Screen (light + dark, different states: loading, error, empty, populated)

Rules:
- Modifier always last parameter with default Modifier
- collectAsStateWithLifecycle() in Route, not Screen
- LaunchedEffect for one-time UiEvent collection in Route
- LazyColumn with key = { item.id } for lists
- Use AppTheme wrapper in previews
- contentDescription on all images (null for decorative)
- Min 48.dp touch targets on interactive elements
- Use theme colors (MaterialTheme.colorScheme.*), never hardcoded Color()
- No hardcoded strings — use stringResource()
- Accessibility: meaningful semantics blocks on custom interactive elements
```

### Setting Up WorkManager Worker

```
Create a WorkManager worker for [WorkerName].

Worker purpose: [describe what this worker does]
Triggers: [periodic/one-time, constraints: network required?, battery?]
Dependencies needed: [list injected dependencies]

Create:
1. [X]Worker extending CoroutineWorker with @HiltWorker and @AssistedInject
2. Constraints setup (network, battery, storage)
3. WorkRequest builder utility function
4. WorkManager schedule/cancel functions in a [X]WorkScheduler class
5. WorkInfo observation as Flow<WorkState> in the scheduler
6. Update WorkerFactory in the app module if needed

Rules:
- @HiltWorker + @AssistedInject constructor
- setProgress() for observable long-running work
- Exponential backoff via BackoffPolicy.EXPONENTIAL
- Return Result.retry() on transient errors, Result.failure() on permanent
- Tag all work for easy cancellation: "tag_sync_user"
- Map WorkInfo.state to your own WorkState sealed class
```

---

## 🧠 Memory Anchors — Paste Into AI When It Forgets

### When AI writes LiveData instead of Flow:
```
CORRECTION: This project uses StateFlow and Flow exclusively. No LiveData.
Replace:
- MutableLiveData<X> → MutableStateFlow<X>
- LiveData<X> → StateFlow<X>
- observe() → collectAsStateWithLifecycle()
- postValue() → _uiState.update { it.copy(...) }
```

### When AI writes Dispatchers.IO directly:
```
CORRECTION: Dispatchers must be injected, never hardcoded.
Add @IoDispatcher CoroutineDispatcher to constructor.
Add qualifier: @Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
Provide in DispatcherModule with @Provides @IoDispatcher fun provideIoDispatcher() = Dispatchers.IO
```

### When AI puts business logic in ViewModel:
```
CORRECTION: Business logic belongs in UseCase classes, not ViewModels.
ViewModel responsibilities: hold state, receive user intents, call UseCases, update state.
UseCase responsibilities: validate input, orchestrate repository calls, apply business rules.
Extract the logic to [X]UseCase with single invoke() function.
```

### When AI uses collectAsState() in Composable:
```
CORRECTION: Use collectAsStateWithLifecycle() instead of collectAsState().
collectAsStateWithLifecycle() respects lifecycle and stops collection when app is backgrounded.
Import: androidx.lifecycle:lifecycle-runtime-compose
```

### When AI puts Android imports in domain layer:
```
CORRECTION: core:domain is pure Kotlin — zero Android framework imports.
No Context, no Uri, no Parcelable in domain models.
If you need Android-specific types, create a separate UI model in the feature module
and map from domain model in the Composable or ViewModel.
```

### When AI creates a Fragment:
```
CORRECTION: This project uses Jetpack Compose exclusively. No Fragments.
Replace Fragment with a Composable function.
Replace FragmentManager/transactions with NavController.navigate().
Use @AndroidEntryPoint only on Activity, not Fragment.
```

### When AI throws exceptions instead of using Result<T>:
```
CORRECTION: Never throw exceptions to the UI layer. Wrap in Result<T>.
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
Map all exceptions via ExceptionMapper before returning from data sources.
```

---

## ⚡ High-Quality Prompt Patterns

### Pattern 1: Show existing code, ask to follow the pattern
```
Here is our existing [SomeClass] implementation:
[paste code]

Now create [NewClass] following the exact same pattern for [different domain concept].
```

### Pattern 2: Ask AI to validate before writing
```
Before writing code, confirm your understanding:
1. Which module will this code live in?
2. What are the dependencies (injected via constructor)?
3. What Result/Flow types will be returned?
4. What's the test strategy?

Then write the code.
```

### Pattern 3: Incremental building (best for complex features)
```
Let's build the [Feature] step by step. Don't write all the code at once.

Step 1 only: Write the domain models and repository interface.
Wait for my confirmation before proceeding to Step 2.
```

### Pattern 4: Ask for the plan first
```
Plan the implementation of [feature/component] without writing any code yet.
List:
1. Files to create and which module they belong to
2. Dependencies required
3. Any architectural concerns or trade-offs
4. Potential edge cases to handle

I'll approve the plan before you write any code.
```

### Pattern 5: Error fixing with context
```
This code has an error: [describe error or paste error message]

Code: [paste code]

Context: 
- Module: [which module]
- Project uses: Kotlin, Compose, Hilt, Coroutines+Flow, MVVM+Clean Architecture
- Dispatchers are injected via @IoDispatcher qualifier
- Result<T> sealed class for errors

Fix only the specific issue. Don't refactor unrelated code.
```

---

## 🚫 Prompts That Lead to Bad Code (Avoid These)

| Bad Prompt | Problem | Better Alternative |
|---|---|---|
| "Write me a user profile screen" | Too vague — AI will make up architecture | "Write the HomeScreen composable. Here is the UiState: [paste]. Here is the intent: [paste]..." |
| "Make it work" | No constraints — AI will take shortcuts | "Fix the issue while maintaining MVVM layers and using Result<T> for errors" |
| "Add a database" | AI might add Room without following existing patterns | "Add a Room entity and DAO for [X] following the same pattern as [existing entity]" |
| "Refactor everything" | Too broad — AI will break working code | "Refactor only [specific function] to use Flow instead of suspend" |
| "Add tests" | Vague — AI writes low-quality tests | "Write unit tests for [class] covering: happy path, network error, not found error. Use Turbine for flows, FakeX for dependencies" |

---

## 📊 AI Confidence Signals

Use these questions to verify AI-generated code before accepting it:

**Ask the AI:**
```
Before I accept this code, answer these:
1. Does any domain model have Android imports? (should be NO)
2. Is Dispatchers.IO hardcoded anywhere? (should be NO)
3. Are there any !! force unwraps? (should be NO)
4. Is LiveData used anywhere? (should be NO)
5. Is business logic in the ViewModel? (should be NO)
6. Are there unit tests covering error scenarios? (should be YES)
7. Do all composables have Preview functions? (should be YES)
```

---

## 🗂️ Context Files to Attach Per Task

| Task | Attach These Files |
|---|---|
| New feature | `PROJECT_MEMORY.md` + existing similar feature module |
| Network/API | `PROJECT_MEMORY.md` + `core/network/` structure |
| Database | `PROJECT_MEMORY.md` + existing Entity + DAO example |
| UI / Compose | `PROJECT_MEMORY.md` + `core/ui/` theme + existing screen |
| WorkManager | `PROJECT_MEMORY.md` + `core/work/` existing worker |
| Testing | `PROJECT_MEMORY.md` + `core/testing/` fakes + existing test |
| Build / Gradle | `PROJECT_MEMORY.md` + `build-logic/` convention plugins + `libs.versions.toml` |

---

## 🔁 Session Workflow Checklist

Before starting:
- [ ] Paste Session Starter Template
- [ ] Attach relevant context files
- [ ] Describe the task clearly with examples

During the session:
- [ ] Use Pattern 4 (plan first) for complex tasks
- [ ] Use Pattern 3 (incremental) for features with many files
- [ ] Correct the AI immediately when it deviates from architecture

After code is generated:
- [ ] Ask AI to self-review against the 7 confidence signal questions
- [ ] Check module imports — no wrong cross-module dependencies
- [ ] Verify no `!!`, no `LiveData`, no `Dispatchers.IO` hardcoded
- [ ] Verify test coverage for error and edge cases
- [ ] Run Ktlint + Detekt before committing

---

## 💡 Tips Learned from Building This Project

1. **Ask for one file at a time** for complex modules — AI loses track of the architecture when generating 10 files at once

2. **Always paste the existing interface when asking for an implementation** — prevents the AI from inventing its own interface signatures

3. **For Compose screens, always paste the UiState first** — the screen design follows from the state shape

4. **Tell AI which module the file goes in** — it helps prevent wrong imports leaking across layers

5. **When AI adds a dependency**, ask it to also show the `libs.versions.toml` entry and the `build.gradle.kts` change — AI often forgets one of the three

6. **For WorkManager**, always remind AI that workers use `@HiltWorker` + `@AssistedInject` — it frequently defaults to the non-Hilt pattern

7. **Request the ProGuard rule** any time AI adds a new serializable class — it will forget this 90% of the time without explicit asking
