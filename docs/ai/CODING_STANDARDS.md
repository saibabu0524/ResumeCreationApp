# 📏 CODING STANDARDS & QUALITY GUIDE
## Android Jetpack Compose Template

> This document defines the exact coding standards for this project. Every AI session and every developer must follow these rules. Code reviews enforce all of these without exception.

---

## 1. Kotlin Code Style

### General Rules
- **Max line length:** 120 characters
- **Indentation:** 4 spaces (no tabs)
- **Trailing commas:** Always use trailing commas in multi-line function calls, lists, when expressions
- **Explicit return types:** Always on public and internal functions; optional on private
- **`val` over `var`:** Default to `val`. Use `var` only with explicit justification in comment
- **`data class` over plain class:** For all models (domain, DTO, entity, state)
- **`object` for singletons and utility classes** with no state
- **`sealed interface` over `sealed class`:** For UiEvent, UserIntent (lighter, composable)

### Kotlin Idioms to Always Use
```kotlin
// ✅ Elvis operator for null fallback
val name = user?.name ?: "Unknown"

// ✅ Scope functions — apply the right one
// let: nullable check, transforming non-null value
user?.let { sendWelcome(it) }
// apply: object configuration
val intent = Intent().apply { action = "..." }
// run: object config + returns result
val result = client.run { fetch(endpoint) }
// with: calling multiple methods on non-null object
with(viewBinding) { titleText.text = "..."; submitButton.isEnabled = true }
// also: side effects without altering the object
list.also { Timber.d("List size: ${it.size}") }

// ✅ Destructuring
val (name, age) = user

// ✅ Spread operator for varargs
val combined = listOf(*first.toTypedArray(), *second.toTypedArray())

// ✅ Inline functions for performance with lambdas
inline fun <T> measureTime(block: () -> T): T { ... }

// ✅ Extension functions for clean APIs
fun String.isValidEmail(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()
fun Context.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
```

### Kotlin Antipatterns to Avoid
```kotlin
// ❌ Unnecessary nullability
fun getUser(): User? // If this NEVER returns null, remove the ?

// ❌ Force unwrap (!!) — NEVER use in production code
val name = user!!.name // FORBIDDEN

// ❌ Mutable public state
class ViewModel {
    var uiState = HomeUiState() // FORBIDDEN — use StateFlow
}

// ❌ Magic numbers
if (retryCount > 3) // FORBIDDEN — use named constant: MAX_RETRY_COUNT = 3

// ❌ Commented-out code in commits
// val oldCode = doSomething() // FORBIDDEN — delete it or keep it

// ❌ Deep nesting (max 3 levels)
fun process() {
    if (a) {
        if (b) {
            if (c) { // FORBIDDEN — extract function at this point
            }
        }
    }
}
```

---

## 2. Clean Architecture Code Quality

### UseCase Rules
```kotlin
// ✅ CORRECT: One responsibility, one public function
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : UseCase<GetUserProfileUseCase.Params, UserProfile>() {

    override suspend fun invoke(params: Params): Result<UserProfile> =
        withContext(dispatcher) { userRepository.getUserProfile(params.userId) }

    data class Params(val userId: String)
}

// ❌ WRONG: Multiple responsibilities
class UserUseCase {
    suspend fun getProfile(id: String) { ... }
    suspend fun updateProfile(user: User) { ... } // WRONG — split into separate UseCases
    suspend fun deleteAccount(id: String) { ... } // WRONG — split into separate UseCases
}
```

### Mapper Rules
```kotlin
// ✅ Always write explicit bidirectional mappers
class UserMapper @Inject constructor() {
    fun toDomain(dto: UserDto): User = User(
        id = dto.id,
        name = dto.fullName ?: dto.username, // handle API inconsistencies here
        email = dto.email
    )

    fun toDomain(entity: UserEntity): User = User(
        id = entity.id,
        name = entity.name,
        email = entity.email
    )

    fun toEntity(domain: User): UserEntity = UserEntity(
        id = domain.id,
        name = domain.name,
        email = domain.email,
        cachedAt = System.currentTimeMillis()
    )
}

// ❌ WRONG: Mapping inside the Repository
class UserRepositoryImpl {
    override suspend fun getUser(id: String): Result<User> {
        val dto = remote.getUser(id)
        return Result.Success(User(id = dto.id, name = dto.fullName)) // FORBIDDEN — use mapper
    }
}
```

### Repository Rules
```kotlin
// ✅ Correct offline-first repository pattern
class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val mapper: UserMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : UserRepository {

    // Emit local first, then refresh from remote
    override fun getUsers(): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        // Emit cached data immediately
        val cached = localDataSource.getUsers().first()
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map(mapper::toDomain)))
        }
        // Fetch fresh data
        remoteDataSource.getUsers()
            .onSuccess { dtos ->
                val entities = dtos.map(mapper::toEntity)
                localDataSource.saveUsers(entities)
            }
            .onError { emit(Result.Error(it)) }
    }.flowOn(dispatcher)
}
```

---

## 3. Compose UI Standards

### Composable Function Rules
```kotlin
// ✅ CORRECT: Stateless composable, all state hoisted
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onUserClicked: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier  // Modifier ALWAYS last with default
) {
    // No ViewModel reference here — only state + lambdas
}

// ✅ CORRECT: Screen-level composable that connects ViewModel
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.NavigateToDetail -> onNavigateToDetail(event.id)
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onUserClicked = { viewModel.onEvent(HomeUserIntent.OnUserClicked(it)) },
        onRetry = { viewModel.onEvent(HomeUserIntent.Retry) }
    )
}

// ❌ WRONG: ViewModel inside non-route composable
@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = hiltViewModel() // FORBIDDEN in non-route composables
}
```

### Preview Rules
```kotlin
// ✅ Every composable needs BOTH previews
@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(
            uiState = HomeUiState(
                users = listOf(User.preview(), User.preview()),
                isLoading = false
            ),
            onUserClicked = {},
            onRetry = {}
        )
    }
}

// ✅ Add preview factory for domain models
// In core:testing or companion object:
fun User.Companion.preview() = User(id = "1", name = "Jane Doe", email = "jane@example.com")
```

### Reusable Component Rules
```kotlin
// ✅ All reusable components in core:ui, prefixed with App
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    // Implementation with loading state built in
}

// ✅ Use LocalContentColor, LocalTextStyle — never hardcode colors
Text(
    text = "Label",
    color = MaterialTheme.colorScheme.onSurface // ✅
)
Text(
    text = "Label",
    color = Color(0xFF000000) // ❌ FORBIDDEN — breaks dark mode
)
```

---

## 4. Coroutines & Flow Standards

### Flow Rules
```kotlin
// ✅ Always use flowOn to shift context, not withContext wrapping the flow builder
fun getData(): Flow<Data> = flow {
    emit(fetchFromDatabase()) // runs on IO (set below)
}.flowOn(Dispatchers.IO) // ✅ correct

// ❌ WRONG:
fun getData(): Flow<Data> = withContext(Dispatchers.IO) { // WRONG — withContext doesn't work cleanly with flows
    flow { emit(fetchFromDatabase()) }
}

// ✅ Always catch exceptions in flows
flow {
    emit(repository.getData())
}.catch { throwable ->
    emit(Result.Error(throwable.toAppException())) // ✅
}

// ✅ Use distinctUntilChanged() to avoid redundant emissions
repository.getUserStream()
    .distinctUntilChanged()
    .collect { user -> /* ... */ }

// ✅ Use debounce for search inputs
searchQueryFlow
    .debounce(300)
    .distinctUntilChanged()
    .flatMapLatest { query -> repository.search(query) }
    .collect { /* ... */ }
```

### StateFlow / SharedFlow Rules
```kotlin
// ✅ UiState — always StateFlow
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// ✅ One-time events — always Channel → Flow
private val _events = Channel<HomeUiEvent>(Channel.BUFFERED)
val events: Flow<HomeUiEvent> = _events.receiveAsFlow()

// Emit event
viewModelScope.launch { _events.send(HomeUiEvent.ShowSnackbar("Done")) }

// ✅ Update state atomically
_uiState.update { currentState ->
    currentState.copy(isLoading = false, users = newUsers)
}

// ❌ WRONG: Non-atomic state update
_uiState.value = _uiState.value.copy(isLoading = false) // Race condition risk
```

---

## 5. Hilt DI Standards

### Module Organization
```kotlin
// ✅ Each module has a single clear responsibility
@Module
@InstallIn(SingletonComponent::class) // App-scoped singletons
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient = ...

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit = ...
}

// ✅ Use @Binds for interface → implementation binding (preferred over @Provides)
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// ✅ ViewModelScoped dependencies use ViewModelComponent
@Module
@InstallIn(ViewModelComponent::class)
abstract class UserViewModelModule {
    @Binds @ViewModelScoped
    abstract fun bindSomeViewModelScopedDep(impl: ConcreteImpl): AbstractDep
}
```

### Injection Rules
```kotlin
// ✅ Always prefer constructor injection
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource
) : UserRepository

// ✅ Field injection ONLY for Android framework classes that Hilt creates
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var analytics: AnalyticsTracker // Only acceptable here
}

// ❌ WRONG: Field injection in ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor( // ✅ Constructor injection
    // ...
) : ViewModel()
```

---

## 6. Testing Standards

### Unit Test Structure
```kotlin
// ✅ Use the AAA pattern — Arrange, Act, Assert
class GetUserProfileUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // Custom rule from core:testing

    private val userRepository: UserRepository = FakeUserRepository() // Fake, not mock
    private lateinit var useCase: GetUserProfileUseCase

    @BeforeEach
    fun setup() {
        useCase = GetUserProfileUseCase(userRepository, UnconfinedTestDispatcher())
    }

    @Test
    fun `when user exists, returns success with user profile`() = runTest {
        // Arrange
        val userId = "user_123"
        userRepository.addUser(User(id = userId, name = "Jane"))

        // Act
        val result = useCase(GetUserProfileUseCase.Params(userId))

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.name).isEqualTo("Jane")
    }

    @Test
    fun `when user not found, returns error`() = runTest {
        val result = useCase(GetUserProfileUseCase.Params("nonexistent"))
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }
}
```

### ViewModel Test Structure
```kotlin
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getUsersUseCase: GetUsersUseCase = mockk()
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setup() {
        viewModel = HomeViewModel(getUsersUseCase, SavedStateHandle())
    }

    @Test
    fun `loading state is true when fetching users`() = runTest {
        // Arrange
        coEvery { getUsersUseCase(any()) } coAnswers { delay(1000); Result.Success(emptyList()) }

        // Act
        viewModel.onEvent(HomeUserIntent.LoadUsers)

        // Assert loading
        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `users are populated on success`() = runTest {
        // Arrange
        val users = listOf(User.preview())
        coEvery { getUsersUseCase(any()) } returns Result.Success(users)

        // Act & Assert with Turbine
        viewModel.uiState.test {
            viewModel.onEvent(HomeUserIntent.LoadUsers)
            assertThat(awaitItem().isLoading).isTrue()
            assertThat(awaitItem().users).isEqualTo(users)
        }
    }
}
```

### Fake Pattern (preferred over mocks for repositories)
```kotlin
// In core:testing
class FakeUserRepository : UserRepository {
    private val users = mutableListOf<User>()
    private var shouldReturnError = false

    fun addUser(user: User) { users.add(user) }
    fun setError(error: Boolean) { shouldReturnError = error }

    override fun getUsers(): Flow<Result<List<User>>> = flow {
        if (shouldReturnError) emit(Result.Error(AppException.Unknown()))
        else emit(Result.Success(users.toList()))
    }

    override suspend fun getUserById(id: String): Result<User> {
        if (shouldReturnError) return Result.Error(AppException.Unknown())
        return users.find { it.id == id }
            ?.let { Result.Success(it) }
            ?: Result.Error(AppException.NotFound())
    }
}
```

---

## 7. Documentation Standards

### KDoc Rules
```kotlin
// ✅ Public API functions always have KDoc
/**
 * Retrieves the user profile for the given [userId].
 *
 * Checks local cache first. Fetches from remote if cache is stale or empty.
 *
 * @param userId The unique identifier of the user.
 * @return [Result.Success] with [UserProfile] on success,
 *         [Result.Error] with [AppException.NotFound] if user doesn't exist,
 *         [Result.Error] with [AppException.NetworkError] on connectivity issues.
 */
suspend fun getUserProfile(userId: String): Result<UserProfile>

// ✅ Complex logic gets inline comments explaining WHY, not WHAT
val adjustedTimeout = baseTimeout * retryCount.coerceAtLeast(1)
// Exponential backoff: multiply timeout by attempt number.
// coerceAtLeast(1) prevents zero timeout on first attempt.
```

### File Header (optional but consistent)
```kotlin
/*
 * Copyright (c) 2024 [Project Name]
 * Module: core:network
 * Description: Retrofit API service for user-related endpoints
 */
```

---

## 8. Git & PR Standards

### Commit Message Format (Conventional Commits)
```
type(scope): short description (max 72 chars)

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`, `perf`

Examples:
```
feat(auth): add biometric login support
fix(network): handle 503 service unavailable gracefully
refactor(home): extract UserCard to core:ui
test(profile): add unit tests for UpdateProfileUseCase
build(deps): update compose-bom to 2024.12.01
```

### Branch Naming
```
feature/short-description
fix/issue-description
refactor/module-name-change
chore/dependency-update
```

### PR Size Rules
- Aim for PRs under 400 lines changed
- One feature/fix per PR — no bundling unrelated changes
- Always include screenshots for UI changes (light + dark + different screen sizes)

---

## 9. Performance Rules

### Compose Performance
```kotlin
// ✅ Use keys in LazyColumn to help Compose track items
LazyColumn {
    items(users, key = { it.id }) { user ->
        UserCard(user = user)
    }
}

// ✅ Use derivedStateOf for expensive computed state
val isScrolled by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}

// ✅ Use stable types for composable parameters
@Stable
data class UserUiModel(val id: String, val name: String) // marked @Stable

// ❌ Avoid lambda captures that cause recomposition
LazyColumn {
    items(users) { user ->
        UserCard(
            user = user,
            onClick = { viewModel.onEvent(HomeUserIntent.OnUserClicked(user.id)) } // Creates new lambda each recomposition
        )
    }
}

// ✅ Better: hoist the click handler
LazyColumn {
    items(users, key = { it.id }) { user ->
        UserCard(
            user = user,
            onClick = onUserClicked // Stable reference
        )
    }
}
```

### Network & Database Performance
- Always paginate list endpoints — use Paging 3
- Room queries must be benchmarked with `@RoomDatabase.Builder.setQueryCoroutinesContext`
- Never run Room queries on Main dispatcher
- Cache responses aggressively — define TTL per data type in a `CachePolicy` class
- Use `@Transaction` annotation on Room queries that touch multiple tables

---

## 10. Code Review Checklist for Reviewers

**Architecture:**
- [ ] Does the code respect module boundaries?
- [ ] Is business logic in UseCases (not ViewModel or Repository)?
- [ ] Are domain models free of Android imports?

**Coroutines:**
- [ ] Are dispatchers injected (not hardcoded)?
- [ ] Are exceptions caught and mapped to `AppException`?
- [ ] Is `collectAsStateWithLifecycle()` used in all Composables?

**Compose:**
- [ ] Are Composables stateless (state hoisted to ViewModel)?
- [ ] Do all new Composables have light + dark Previews?
- [ ] Are touch targets ≥ 48.dp?
- [ ] Are images given proper `contentDescription`?

**Testing:**
- [ ] Are new UseCases unit-tested?
- [ ] Are new ViewModels unit-tested?
- [ ] Are Flows tested with Turbine?

**Security:**
- [ ] No new `!!` force unwraps?
- [ ] No hardcoded strings, colors, or numbers?
- [ ] No new secrets in code?
