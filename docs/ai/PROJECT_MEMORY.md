# 🧠 PROJECT MEMORY — Android Jetpack Compose Template

> **IMPORTANT FOR AI:** Read this entire file before writing any code. This is the single source of truth for this project. Every decision here was intentional. Do not deviate from the patterns described without explicit instruction.

---

## 📌 Project Identity

| Property | Value |
|---|---|
| **Project Type** | Android Multi-Module Template Repository |
| **Language** | Kotlin (100%) — No Java files ever |
| **UI Framework** | Jetpack Compose (No XML layouts, ever) |
| **Architecture** | MVVM + Clean Architecture (strict layer separation) |
| **DI Framework** | Hilt (Dagger-Hilt) |
| **Async** | Coroutines + Flow (No RxJava, no LiveData) |
| **Build System** | Gradle with Version Catalog + Convention Plugins |
| **Min SDK** | 26 |
| **Target SDK** | 35 |
| **Compile SDK** | 35 |
| **Package Name (Template)** | `com.softsuave.resumecreationapp` |
| **GitHub Template** | Yes — enable "Template Repository" in settings |

---

## 🗂️ Module Map (Source of Truth)

```
root/
├── app/                        → Entry point, DI wiring, NavHost, Application class
├── build-logic/                → Convention plugins (Gradle)
│   └── convention/
│       ├── AndroidApplicationPlugin
│       ├── AndroidLibraryPlugin
│       ├── AndroidFeaturePlugin
│       ├── AndroidComposePlugin
│       ├── AndroidHiltPlugin
│       ├── AndroidRoomPlugin
│       └── AndroidTestingPlugin
├── core/
│   ├── common/                 → Extensions, utils, base classes, Result<T>
│   ├── ui/                     → Compose theme, components, design system
│   ├── domain/                 → UseCases, Repository interfaces, domain models
│   ├── data/                   → Repository implementations, mappers, data sources
│   ├── network/                → Retrofit, OkHttp, interceptors, NetworkMonitor
│   ├── database/               → Room DB, DAOs, Entities, TypeConverters
│   ├── datastore/              → Proto DataStore, Preferences DataStore
│   ├── work/                   → WorkManager workers, WorkerFactory
│   └── testing/                → Fakes, test rules, test dispatchers
└── feature/
    ├── auth/                   → Login, Register, Onboarding screens
    ├── home/                   → Home screen
    ├── settings/               → Settings screen
    └── profile/                → User profile screen
```

### Module Dependency Rules (NEVER break these)
- Feature modules NEVER depend on other feature modules
- Domain layer has ZERO Android dependencies (pure Kotlin)
- Data layer knows about domain, never about presentation
- App module is the ONLY module that knows about all feature modules
- core:testing is ONLY a testImplementation dependency, never api/implementation

---

## 🏛️ Architecture Rules (Strict — Do Not Break)

### Layer Responsibilities

**Domain Layer** (`core:domain`)
- Pure Kotlin — zero Android imports
- Defines Repository interfaces (not implementations)
- Contains UseCase classes — one public operator function per class
- Defines domain models — immutable data classes
- Contains `Result<T>` sealed class
- NEVER knows about Room, Retrofit, DataStore, or any framework

**Data Layer** (`core:data`)
- Implements domain Repository interfaces
- Contains mappers: DTO→Domain, Entity→Domain
- Orchestrates between RemoteDataSource and LocalDataSource
- Handles caching decisions (network-first vs cache-first)
- NEVER contains business logic — only data orchestration

**Presentation Layer** (each `feature:*`)
- ViewModel per screen with `@HiltViewModel`
- One UiState data class per screen (sealed or data class)
- Events via `Channel<UiEvent>` exposed as `Flow<UiEvent>`
- Composables observe state via `collectAsStateWithLifecycle()`
- Composables are STATELESS — all state lives in ViewModel

### Unidirectional Data Flow
```
User Action
    ↓
ViewModel.onEvent(UserIntent)
    ↓
UseCase.invoke(params)
    ↓
Repository.getData()
    ↓
RemoteDataSource / LocalDataSource
    ↓ (Result<T> flows back up)
ViewModel updates _uiState (StateFlow)
    ↓
Composable recomposes
```

---

## 📦 Dependency Catalog (libs.versions.toml)

### Current Versions (Update only after explicit approval)

```toml
[versions]
# Core
kotlin = "2.0.21"
ksp = "2.0.21-1.0.25"
agp = "8.7.3"

# Compose
compose-bom = "2024.12.01"
activity-compose = "1.9.3"

# Lifecycle
lifecycle = "2.8.7"

# Hilt
hilt = "2.53.1"
hilt-navigation-compose = "1.2.0"

# Navigation
navigation-compose = "2.8.5"

# Network
retrofit = "2.11.0"
okhttp = "4.12.0"

# Room
room = "2.6.1"

# DataStore
datastore = "1.1.1"

# WorkManager
work = "2.10.0"

# Coroutines
coroutines = "1.9.0"

# Serialization
kotlin-serialization = "1.7.3"

# Image Loading
coil = "2.7.0"

# Security
security-crypto = "1.1.0-alpha06"

# Testing
junit = "4.13.2"
junit5 = "5.11.3"
mockk = "1.13.13"
turbine = "1.2.0"
coroutines-test = "1.9.0"

# Debug
leakcanary = "2.14"
chucker = "4.0.0"
timber = "5.0.1"
```

---

## 🧩 Patterns to Always Use

### Result Wrapper (always use this, never throw raw exceptions to UI)
```kotlin
// In core:common
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (AppException) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
```

### UseCase Pattern (one use case = one file = one responsibility)
```kotlin
// Base classes in core:domain
abstract class UseCase<in P, out R> {
    abstract suspend operator fun invoke(params: P): Result<R>
}

abstract class FlowUseCase<in P, out R> {
    abstract operator fun invoke(params: P): Flow<Result<R>>
}

// Concrete use case — one file per use case
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<GetUserProfileUseCase.Params, UserProfile>() {
    override suspend fun invoke(params: Params): Result<UserProfile> =
        userRepository.getUserProfile(params.userId)

    data class Params(val userId: String)
}
```

### ViewModel Pattern
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<HomeUiEvent> = _uiEvent.receiveAsFlow()

    fun onEvent(event: HomeUserIntent) {
        when (event) {
            is HomeUserIntent.LoadUsers -> loadUsers()
            is HomeUserIntent.OnUserClicked -> navigateToUser(event.userId)
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUsersUseCase(Unit)
                .onSuccess { users -> _uiState.update { it.copy(users = users, isLoading = false) } }
                .onError { error -> _uiState.update { it.copy(error = error.message, isLoading = false) } }
        }
    }
}

// UiState — always a data class, never a sealed class
data class HomeUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// User intents
sealed interface HomeUserIntent {
    data object LoadUsers : HomeUserIntent
    data class OnUserClicked(val userId: String) : HomeUserIntent
}

// One-time events
sealed interface HomeUiEvent {
    data class NavigateToUserDetail(val userId: String) : HomeUiEvent
    data class ShowSnackbar(val message: String) : HomeUiEvent
}
```

### Repository Pattern
```kotlin
// Interface in core:domain
interface UserRepository {
    fun getUsers(): Flow<Result<List<User>>>
    suspend fun getUserById(id: String): Result<User>
    suspend fun syncUsers(): Result<Unit>
}

// Implementation in core:data
class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val userMapper: UserMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : UserRepository {
    override fun getUsers(): Flow<Result<List<User>>> =
        localDataSource.getUsers()
            .map { entities -> Result.Success(entities.map(userMapper::toDomain)) }
            .flowOn(dispatcher)
            .catch { emit(Result.Error(it.toAppException())) }
}
```

### Coroutine Dispatcher Injection (never hardcode Dispatchers.IO)
```kotlin
// Qualifiers in core:common
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

// Module in core:common or app
@Module @InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

---

## 🚫 What is FORBIDDEN in This Project

| Forbidden | Use Instead |
|---|---|
| `LiveData` | `StateFlow` / `Flow` |
| `Fragment` | Composable functions |
| XML layouts (`layout/*.xml`) | Jetpack Compose |
| `findNavController()` (Fragment) | Navigation Compose |
| `Dispatchers.IO` hardcoded | `@IoDispatcher` injected |
| Raw exceptions thrown to UI | `Result<T>` sealed class |
| `SharedPreferences` directly | `DataStore` or `EncryptedSharedPreferences` |
| Business logic in ViewModel | UseCase classes |
| Business logic in Repository | Repository only orchestrates data |
| Android imports in `core:domain` | Pure Kotlin only |
| Feature module importing another feature | Only via `:app` NavGraph |
| `runBlocking` in production code | `viewModelScope.launch`, `lifecycleScope.launch` |
| `GlobalScope` | Scoped coroutines only |
| Hardcoded strings in Kotlin/Compose | `stringResource()` from `strings.xml` |
| `collectAsState()` in Composable | `collectAsStateWithLifecycle()` |
| `Log.d/e/w` | `Timber.d/e/w` (debug only via DebugTree) |

---

## 🔧 Naming Conventions

| Type | Convention | Example |
|---|---|---|
| ViewModel | `[Screen]ViewModel` | `HomeViewModel` |
| UiState | `[Screen]UiState` | `HomeUiState` |
| User Intent | `[Screen]UserIntent` | `HomeUserIntent` |
| Ui Event | `[Screen]UiEvent` | `HomeUiEvent` |
| UseCase | `[Verb][Noun]UseCase` | `GetUserProfileUseCase` |
| Repository Interface | `[Domain]Repository` | `UserRepository` |
| Repository Impl | `[Domain]RepositoryImpl` | `UserRepositoryImpl` |
| Remote Data Source | `[Domain]RemoteDataSource` | `UserRemoteDataSource` |
| Local Data Source | `[Domain]LocalDataSource` | `UserLocalDataSource` |
| Room Entity | `[Domain]Entity` | `UserEntity` |
| Room DAO | `[Domain]Dao` | `UserDao` |
| DTO (API response) | `[Domain]Dto` | `UserDto` |
| Mapper | `[Domain]Mapper` | `UserMapper` |
| DI Module | `[Domain]Module` | `NetworkModule` |
| Composable Screen | `[Screen]Screen` | `HomeScreen` |
| Composable (reusable) | `App[Component]` | `AppButton`, `AppTextField` |
| Hilt Worker | `[Action]Worker` | `SyncWorker`, `UploadWorker` |

---

## 📁 File Organization Within Modules

```
feature/home/
├── di/
│   └── HomeModule.kt
├── data/              (if feature-specific data — prefer core:data)
├── domain/            (if feature-specific domain — prefer core:domain)
└── presentation/
    ├── HomeScreen.kt
    ├── HomeViewModel.kt
    ├── HomeUiState.kt
    ├── HomeUserIntent.kt
    ├── HomeUiEvent.kt
    └── components/
        ├── UserCard.kt
        └── HomeTopBar.kt
```

---

## ⚙️ Gradle Convention Plugin Rules

- Every module applies exactly ONE of: `android-application`, `android-library`, `android-feature`
- Compose must ONLY be enabled via `android-compose` convention plugin (never manually)
- Hilt must ONLY be enabled via `android-hilt` convention plugin (never manually)
- All version numbers live in `gradle/libs.versions.toml` — NOWHERE else
- Never use `implementation` for domain-layer dependencies in feature modules — use the feature convention plugin bundles

---

## 🧪 Testing Rules

- Every UseCase has a corresponding unit test
- Every ViewModel has a corresponding unit test with `MainDispatcherRule`
- Every Repository has a unit test using fake data sources (from `core:testing`)
- Flow testing ALWAYS uses `Turbine` library
- Coroutine testing uses `UnconfinedTestDispatcher`
- UI testing uses `createComposeRule()` — no Espresso
- Fakes (not mocks) are preferred for Repository and DataSource in tests
- Never use `Thread.sleep()` in tests — use `advanceTimeBy()` or `runTest`

---

## 🔐 Security Checklist (Always Verify)

- [ ] No API keys or secrets in code or `strings.xml`
- [ ] API keys loaded from `local.properties` via `BuildConfig`
- [ ] `local.properties` is in `.gitignore`
- [ ] Auth tokens stored in `EncryptedSharedPreferences` only
- [ ] Network Security Config restricts cleartext traffic
- [ ] ProGuard rules protect all serializable models
- [ ] `Timber` DebugTree only installed in debug builds
- [ ] Chucker only included in debug builds
- [ ] LeakCanary only included in debug builds

---

## 🌐 Network Error Mapping

All HTTP/network errors MUST be mapped through `ExceptionMapper` in `core:network`:

| Exception | Mapped To |
|---|---|
| `HttpException 401` | `AppException.Unauthorized` |
| `HttpException 403` | `AppException.Forbidden` |
| `HttpException 404` | `AppException.NotFound` |
| `HttpException 5xx` | `AppException.ServerError` |
| `IOException` | `AppException.NetworkError` |
| `SocketTimeoutException` | `AppException.Timeout` |
| `UnknownHostException` | `AppException.NoInternet` |
| Everything else | `AppException.Unknown` |

---

## 📐 Compose UI Rules

- Every screen Composable takes a single `uiState` param + event lambda params — no direct ViewModel reference in composables
- Preview functions for EVERY composable (at minimum light + dark theme)
- `Modifier` is always the last parameter in composable function signatures
- Modifier default value is always `Modifier` (empty)
- Use `remember` and `rememberSaveable` appropriately — UI-only transient state in Composable, everything else in ViewModel
- Never put business logic in Composables
- Use semantic `contentDescription` on all Image composables
- Edge-to-edge enabled, use `WindowInsets` APIs — never hardcode padding for status/nav bar

---

## 🔄 WorkManager Rules

- All workers extend `CoroutineWorker` — never `Worker`
- Workers are injected via `HiltWorkerFactory` — never instantiated manually
- All long-running workers must implement proper retry logic with exponential backoff
- Workers must report progress via `setProgress()` for observable operations
- Periodic work tagged for easy cancellation and observation
- WorkManager initialized via `Jetpack App Startup` — not in `Application.onCreate()`

---

## ♿ Accessibility Non-Negotiables

- All `Image` composables have a non-empty `contentDescription` or `contentDescription = null` explicitly set for decorative images
- Minimum touch target: 48.dp × 48.dp for all interactive elements
- All custom components pass `semantics {}` blocks for screen readers
- Color is never the ONLY way information is conveyed (add icon/text alongside)
- Text never scaled below `TextStyle` system font size minimums

---

## 📋 PR Checklist (Add to `.github/pull_request_template.md`)

- [ ] Architecture layers respected (no cross-layer violations)
- [ ] New feature has corresponding unit tests
- [ ] New composables have Preview functions
- [ ] No hardcoded strings — all in `strings.xml`
- [ ] No hardcoded colors or dimensions — all in theme
- [ ] No `LiveData` introduced
- [ ] No new `Dispatchers.IO` hardcoded — injected via qualifier
- [ ] ProGuard rules updated if new serializable classes added
- [ ] Accessibility: contentDescription on images, touch targets ≥ 48dp
- [ ] Dark mode tested
- [ ] `collectAsStateWithLifecycle()` used (not `collectAsState()`)
