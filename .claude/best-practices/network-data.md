# network-data.md — Retrofit, ApiResultCallAdapterFactory, DataStore

> **Result<T> definition + error mapping table:** `docs/ai/PROJECT_MEMORY.md` §Patterns + §Network Error Mapping
> **Mapper and Repository code examples:** `docs/ai/CODING_STANDARDS.md` §2 Clean Architecture
> **Offline-first flow pattern:** `docs/ai/CODING_STANDARDS.md` §2 Repository Rules

---

## ExceptionMapper — Full HTTP Code Mapping

All HTTP/network exceptions flow through `ExceptionMapper.kt` in `core:network`:

```kotlin
fun Throwable.toAppException(): AppException = when (this) {
    is HttpException -> when (code()) {
        401       -> AppException.AuthError(message())          // Unauthorized
        403       -> AppException.AuthError(message())          // Forbidden
        404       -> AppException.NotFoundError(message())      // Not Found
        in 500..599 -> AppException.ServerError(code(), message())
        else      -> AppException.UnknownError(message())
    }
    is SocketTimeoutException -> AppException.Timeout(message)
    is UnknownHostException   -> AppException.NoInternet(message)
    is IOException            -> AppException.NetworkError(message)
    else                      -> AppException.UnknownError(message)
}
```

---

## Retrofit Service Rules (quick bullets)

- Return type is the **DTO directly** — never `Response<T>`
- The `ApiResultCallAdapterFactory` wraps the result in `Result<T>` and catches all exceptions
- DTOs are `@Serializable data class` suffixed with `Dto` — live in `core:network`
- Never return domain models from an API service
- No try/catch in RepositoryImpl — the factory handles it

---

## ApiResultCallAdapterFactory — How It Works

> Not covered in `docs/ai/` — unique to this file.

This Retrofit `CallAdapter.Factory` intercepts every call, catches `HttpException` and `IOException`,
and returns `Result<T>` so RepositoryImpl never needs try/catch:

```kotlin
// In NetworkModule.kt — must be last in the adapter chain
Retrofit.Builder()
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .addCallAdapterFactory(ApiResultCallAdapterFactory())    // ← catches all HTTP errors
    .build()

// RepositoryImpl becomes clean — no try/catch
override suspend fun getUsers(): Result<List<User>> =
    when (val result = apiService.getUsers()) {
        is Result.Success -> Result.Success(result.data.map(mapper::toDomain))
        is Result.Error   -> result
    }
```

---

## OkHttp + Retrofit Setup (NetworkModule)

> Not covered in `docs/ai/` — unique to this file.

```kotlin
@Module @InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        connectivityInterceptor: ConnectivityInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(connectivityInterceptor)   // rejects offline requests early
        .addInterceptor(authInterceptor)            // adds Bearer token header
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG)
                addInterceptor(HttpLoggingInterceptor().apply { level = BODY })
        }
        .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)          // from build flavor, never hardcoded
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResultCallAdapterFactory())
            .build()
}
```

---

## DataStore — Token Storage Pattern

> Not covered in `docs/ai/` — unique to this file.

```kotlin
// core:datastore/TokenStorage.kt
class TokenStorage @Inject constructor(@ApplicationContext private val context: Context) {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("auth_prefs") }
    )

    val accessToken: Flow<String?> = dataStore.data.map { it[PreferencesKeys.ACCESS_TOKEN] }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit {
            it[PreferencesKeys.ACCESS_TOKEN]  = accessToken
            it[PreferencesKeys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearTokens() = dataStore.edit {
        it.remove(PreferencesKeys.ACCESS_TOKEN)
        it.remove(PreferencesKeys.REFRESH_TOKEN)
    }
}
```

**Critical:** `DataStore` must be a `@Singleton`. Never create it inside a function or property
delegate in multi-module projects — causes `IllegalStateException: multiple DataStores active`.
(See `ARCHITECTURE_DECISIONS.md` Pitfall 8.)

---

## Mapper Rules (quick bullets)

Full code example in `docs/ai/CODING_STANDARDS.md` §2 Mapper Rules.

- One `XxxMapper` class per domain entity — never inline inside Repository
- All three directions in one class: DTO→Domain, Entity→Domain, Domain→Entity
- Handle API inconsistencies (`dto.fullName ?: dto.username`) in the mapper — not in ViewModel
- Domain model is source of truth — never pass DTOs or entities to ViewModel or UseCase

---

## Common AI Mistakes

- **`try/catch` in RepositoryImpl alongside `ApiResultCallAdapterFactory`** — the factory
  already catches everything; double-wrapping creates confusing nested `Result<Result<T>>`
- **`Response<T>` return type on Retrofit interface** — return `T` directly
- **Inline mapping in Repository** — extract to `XxxMapper`
- **Same class used for DTO and Entity** — always separate; they have different fields
  (e.g. `cachedAt` on entity, no `avatarUrl_raw` on domain)
- **Hardcoding `BASE_URL`** — must come from `BuildConfig.BASE_URL` (set per flavor)
