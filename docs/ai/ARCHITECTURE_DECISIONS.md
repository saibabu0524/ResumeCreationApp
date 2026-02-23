# 🏗️ ARCHITECTURE DECISIONS & COMMON PITFALLS
## Android Jetpack Compose Template

> This document records WHY decisions were made and lists every pitfall encountered. Read this before debugging strange issues or questioning a design choice.

---

## Architecture Decision Records (ADRs)

### ADR-001: No LiveData, Flow Only
**Decision:** Kotlin Flow + StateFlow for all reactive data. LiveData is banned.
**Why:** Flow is lifecycle-unaware by design — lifecycle handling is explicit via `collectAsStateWithLifecycle()`. Flow works in pure Kotlin (domain layer), LiveData doesn't. Flow has better operators (`combine`, `flatMapLatest`, `debounce`). LiveData is Android-only and can't live in domain.
**Consequence:** All ViewModels use `StateFlow`. Events use `Channel.receiveAsFlow()`. Composables use `collectAsStateWithLifecycle()`.

### ADR-002: Sealed Interface over Sealed Class for Events and Intents
**Decision:** Use `sealed interface` for `UiEvent`, `UserIntent`, `AppException`.
**Why:** Sealed interfaces are composable (a class can implement multiple sealed interfaces). They don't force a constructor, reducing boilerplate. Kotlin 1.5+ makes them as powerful as sealed classes.
**Consequence:** All new sealed hierarchies use `sealed interface`. Existing `sealed class` ones are not forcibly migrated unless touched.

### ADR-003: UiState is Always a Data Class
**Decision:** Screen UiState is always a single `data class` with all fields, not a sealed class.
**Why:** Sealed UiState (Loading/Success/Error) forces UI to switch on state, making it hard to show partial data during refresh. A flat data class with `isLoading: Boolean`, `error: String?`, and data fields lets the UI show cached data while refreshing. This is the recommended Google approach post-2022.
**Consequence:** Every screen has one flat UiState. `isLoading` + `error` coexist with data fields. UI decides what to show based on combination.

### ADR-004: One UseCase Per File, One Public Function
**Decision:** Each UseCase has exactly one `operator fun invoke()`. No UseCase has multiple business operations.
**Why:** Single Responsibility Principle. Easy to test in isolation. Easy to name. Easy to find. Easy to mock/fake.
**Consequence:** You will have many UseCase files. This is intentional and correct. Don't combine them to "reduce files."

### ADR-005: Fakes Over Mocks in Tests
**Decision:** Repository and DataSource tests use hand-written `FakeX` classes from `core:testing`. Mocks (MockK) are only for leaf dependencies like Retrofit API service or Room DAO.
**Why:** Fakes are refactor-safe (compile error when interface changes). Fakes are readable test fixtures. Mocks with `every { }` blocks break silently when implementation changes and create brittle tests that test nothing real.
**Consequence:** `core:testing` contains `FakeUserRepository`, `FakeNetworkDataSource`, etc. Add to these rather than creating mocks.

### ADR-006: Dispatcher Injection via Qualifiers
**Decision:** `CoroutineDispatcher` is always injected via `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher` qualifiers. `Dispatchers.IO` is NEVER written directly in production code.
**Why:** Without this, unit tests run on real threads and are slow/flaky. With injected dispatchers, tests replace with `UnconfinedTestDispatcher` for deterministic behavior.
**Consequence:** All classes that do async work take a dispatcher constructor parameter. Tests pass `UnconfinedTestDispatcher()`.

### ADR-007: Compose Navigation with Kotlin Serialization Routes
**Decision:** Navigation routes use `@Serializable` data classes/objects (type-safe routes) introduced in Navigation 2.8+. No string-based routes.
**Why:** Compile-time safety. Arguments are type-checked. No manual string formatting or parsing.
**Consequence:** All route classes are in `core:common` or per-feature `navigation/` package. `@Serializable` annotation required.

### ADR-008: Feature Modules Never Know About Each Other
**Decision:** Feature modules cannot import each other. Cross-feature navigation is done through `:app` NavHost.
**Why:** Prevents circular dependencies. Keeps features independently buildable. Enables future module replacement.
**Consequence:** Navigation callbacks bubble up from feature to `:app`'s NavHost. Feature modules expose navigation callbacks as lambda parameters.

### ADR-009: Build Logic in Convention Plugins, Not buildSrc Scripts
**Decision:** All build logic lives in `build-logic/convention/` as Gradle convention plugins. Not in `buildSrc/`. Not duplicated per module.
**Why:** Convention plugins are independently cacheable by Gradle. They're composable. Changing one doesn't invalidate all module builds (unlike `buildSrc`).
**Consequence:** Adding a new shared Gradle configuration = add a convention plugin. Applying it = one line in the module's `build.gradle.kts`.

### ADR-010: App Startup for Library Initialization
**Decision:** Library initialization (WorkManager, Coil, Timber, etc.) uses `Jetpack App Startup` initializers, not `Application.onCreate()`.
**Why:** Startup order is controlled. Lazy initialization possible. Initialization is testable. `Application.onCreate()` becomes clean.
**Consequence:** New libraries that need initialization get an `Initializer<T>` class in `:app/startup/`.

---

## 🐛 Common Pitfalls & Their Fixes

### Pitfall 1: Recomposition Storm from Unstable Lambdas
**Symptom:** UI is slow, Compose Layout Inspector shows frequent full recompositions.
**Cause:** Lambdas created inside `items {}` block create new references each recomposition.
```kotlin
// ❌ BAD — new lambda every recomposition
items(users) { user ->
    UserCard(onClick = { viewModel.onEvent(HomeUserIntent.OnUserClicked(user.id)) })
}
```
**Fix:**
```kotlin
// ✅ GOOD — stable reference
items(users, key = { it.id }) { user ->
    UserCard(onClick = onUserClicked) // passed as stable param
}
```

### Pitfall 2: Flow Collected on Wrong Dispatcher
**Symptom:** Database operations crashing with `Cannot access database on the main thread`.
**Cause:** `flowOn()` not applied or applied incorrectly.
```kotlin
// ❌ BAD — missing flowOn
fun getUsers(): Flow<List<UserEntity>> = dao.getAll() // Runs on whoever collects it
```
**Fix:**
```kotlin
// ✅ GOOD
fun getUsers(): Flow<Result<List<User>>> = dao.getAll()
    .map { entities -> Result.Success(entities.map(mapper::toDomain)) }
    .flowOn(ioDispatcher) // Always specify upstream context
    .catch { emit(Result.Error(it.toAppException())) }
```

### Pitfall 3: WorkManager Worker Not Injected Properly
**Symptom:** Hilt injection fails in Worker. `@Inject` fields are null.
**Cause:** Worker not annotated with `@HiltWorker`, or `HiltWorkerFactory` not set on WorkManager.
**Fix:**
```kotlin
// Worker must have @HiltWorker + @AssistedInject
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: UserRepository // ✅ injected
) : CoroutineWorker(context, params) { ... }

// In Application class:
@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}

// In AndroidManifest.xml — disable auto-init:
<provider
    android:name="androidx.startup.InitializationProvider"
    tools:node="remove" />
```

### Pitfall 4: SavedStateHandle Not Used — State Lost on Process Death
**Symptom:** User fills a form, switches apps, returns — form is empty.
**Cause:** Form input stored only in ViewModel `StateFlow`, not persisted.
**Fix:**
```kotlin
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Use savedStateHandle for user-entered values
    var nameInput by savedStateHandle.saveable { mutableStateOf("") }
    var emailInput by savedStateHandle.saveable { mutableStateOf("") }
}
```

### Pitfall 5: Room Migration Crash on Schema Change
**Symptom:** App crashes with `IllegalStateException: Room cannot verify the data integrity`.
**Cause:** Added/changed a Room column without adding a Migration.
**Fix:**
```kotlin
// Option A (dev only): destructive migration
Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .fallbackToDestructiveMigration() // ONLY during development

// Option B (production): write a migration
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE users ADD COLUMN bio TEXT")
    }
}

Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

### Pitfall 6: Hilt ViewModel Not Found in Compose
**Symptom:** `hiltViewModel()` crashes or ViewModel is re-created unexpectedly.
**Cause:** Composable is inside a non-`@AndroidEntryPoint` Activity, or Navigation back stack entry is not used correctly.
**Fix:**
```kotlin
// In MainActivity — must be @AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppNavHost() }
    }
}

// For nav-scoped ViewModels in Composables:
@Composable
fun SomeRoute(navController: NavController) {
    val backStackEntry = remember(navController) {
        navController.getBackStackEntry("route")
    }
    val viewModel: SomeViewModel = hiltViewModel(backStackEntry)
}
```

### Pitfall 7: Flows Not Cancelled — Memory Leaks
**Symptom:** LeakCanary reports leaks from Flow collectors. Background work continues after navigating away.
**Cause:** Flow collected outside of `viewModelScope` or without lifecycle awareness.
**Fix:**
```kotlin
// In Composable — use collectAsStateWithLifecycle (auto-cancels)
val state by viewModel.uiState.collectAsStateWithLifecycle()

// In non-Compose Android code — use repeatOnLifecycle
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> updateUi(state) }
    }
}

// Never do this:
lifecycleScope.launch { // WRONG — collects even in background
    viewModel.uiState.collect { state -> updateUi(state) }
}
```

### Pitfall 8: DataStore Accessed From Multiple Instances
**Symptom:** `java.lang.IllegalStateException: There are multiple DataStores active for the same file`.
**Cause:** DataStore instance created per call instead of as a singleton.
**Fix:**
```kotlin
// ✅ Single instance via Hilt Singleton
@Module @InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_prefs") }
        )
}
// Never create DataStore with the property delegate (Context.dataStore) in a multi-module project
```

### Pitfall 9: Paging 3 Not Refreshing After Remote Data Change
**Symptom:** `LazyPagingItems` doesn't update after background sync.
**Cause:** `PagingSource` invalidation not triggered after database write.
**Fix:**
```kotlin
// Room's Flow-based PagingSource auto-invalidates on DB change
// Ensure you're using @Query that returns PagingSource from Room (not custom PagingSource)
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getUsersPaged(): PagingSource<Int, UserEntity> // ✅ Room handles invalidation
}
```

### Pitfall 10: Compose Preview Crashes Due to Hilt
**Symptom:** `@Preview` functions crash in Android Studio with Hilt-related errors.
**Cause:** Previews can't use `hiltViewModel()`.
**Fix:**
```kotlin
// ✅ Make screen Composables accept state directly — no ViewModel inside
@Composable
fun UserScreen(
    uiState: UserUiState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

// ✅ Preview uses the stateless composable with fake data
@Preview
@Composable
private fun UserScreenPreview() {
    AppTheme {
        UserScreen(
            uiState = UserUiState(users = listOf(User.preview())),
            onAction = {}
        )
    }
}

// ✅ Route composable connects ViewModel — not previewed
@Composable
fun UserRoute(viewModel: UserViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserScreen(uiState = state, onAction = viewModel::onEvent)
}
```

---

## 📈 Performance Benchmarks to Track

Set these up in the `:benchmark` module with `Macrobenchmark`:

| Metric | Target | Baseline (v1.0.0) | Measured With |
|---|---|---|---|
| App cold startup | < 500ms | 380ms | `StartupBenchmark` |
| App warm startup | < 300ms | 210ms | `StartupBenchmark` |
| App hot startup | < 150ms | 90ms | `StartupBenchmark` |
| Frame rate (home screen scroll) | 60fps / 90fps | P99 < 16ms | `ScrollBenchmark` |
| Room query (list 100 items) | < 16ms | 8ms | Custom benchmark |
| Network request (P95) | < 2000ms | 850ms | OkHttp EventListener |

*(Note: Run benchmarks on a physical test device to update these baseline numbers.)*

---

## 🏷️ Dependency Graph Visualization

Run this to visualize module dependencies (add to `build-logic`):
```bash
./gradlew generateModuleGraph
```

Expected output shows no circular dependencies and no feature-to-feature arrows.

---

## 📦 ProGuard Rules Reference

Add these to `proguard-rules.pro` when adding corresponding libraries:

```proguard
# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson / Moshi — keep all DTO/Entity classes
-keep class com.softsuave.resumecreationapp.core.network.dto.** { *; }
-keep class com.softsuave.resumecreationapp.core.database.entity.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt
-keepclasseswithmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }
```

---

## 🔗 Useful References

| Topic | Link |
|---|---|
| Compose Architecture | https://developer.android.com/jetpack/compose/architecture |
| Now in Android (reference project) | https://github.com/android/nowinandroid |
| Baseline Profiles | https://developer.android.com/topic/performance/baselineprofiles |
| Hilt with WorkManager | https://developer.android.com/training/dependency-injection/hilt-extension |
| Type-safe Navigation | https://developer.android.com/guide/navigation/design/type-safety |
| Convention Plugins Guide | https://developer.android.com/build/publish-library/prep-lib-release |
| Turbine (Flow testing) | https://github.com/cashapp/turbine |
| Compose Performance | https://developer.android.com/jetpack/compose/performance |
