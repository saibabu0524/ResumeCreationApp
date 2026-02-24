# testing.md — Fakes, Dispatchers, ViewModel Tests, Screenshot Tests

> **Full ViewModel test example:** `docs/ai/CODING_STANDARDS.md` §6 Testing Standards
> **ADR-005 (Fakes over Mocks):** `docs/ai/ARCHITECTURE_DECISIONS.md`
> **PROJECT_MEMORY testing rules:** `docs/ai/PROJECT_MEMORY.md` §Testing Rules

---

## Testing Layer Quick Reference

| Layer | Test type | Key rule |
|---|---|---|
| ViewModel | Unit — `runTest` | `MainDispatcherRule` + `FakeXxx` (no mocks for repos) |
| UseCase | Unit — `runTest` | `FakeXxx` repos + `UnconfinedTestDispatcher()` |
| Repository | Unit — `runTest` | MockK for DAO/API service only |
| Composable | Screenshot | Paparazzi — `core:ui/src/test/` |

---

## MockK — Allowed vs Forbidden

```kotlin
// ✅ ALLOWED — DAO/API service have no domain interface
val mockUserDao = mockk<UserDao>()
coEvery { mockUserDao.getUser("123") } returns UserEntity(id = "123", name = "Alice")

// ❌ FORBIDDEN — use FakeUserRepository instead
val mockRepository = mockk<UserRepository>()
coEvery { mockRepository.getUser("123") } returns Result.Success(TestData.alice)
```

---

## UseCase Test Template

> Not covered in `docs/ai/CODING_STANDARDS.md` — unique to this file.

```kotlin
class GetUsersUseCaseTest {
    private val fakeRepository = FakeUserRepository()
    private val useCase = GetUsersUseCase(
        userRepository = fakeRepository,
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `invoke — returns users from repository`() = runTest {
        fakeRepository.users = TestData.users
        val result = useCase(Unit)
        assertThat((result as Result.Success).data).isEqualTo(TestData.users)
    }

    @Test
    fun `invoke — returns error when repository fails`() = runTest {
        fakeRepository.shouldReturnError = true
        assertThat(useCase(Unit)).isInstanceOf(Result.Error::class.java)
    }
}
```

---

## TestData Object (core:testing)

> Convention: always expand this file when a new domain model is added. Never define
> ad-hoc objects inside individual test files.

```kotlin
object TestData {
    val alice = User(id = "u1", name = "Alice", email = "alice@example.com")
    val bob   = User(id = "u2", name = "Bob",   email = "bob@example.com")
    val users = listOf(alice, bob)
}
```

---

## ResultAssertions (core:testing)

```kotlin
fun <T> Result<T>.assertSuccess(): T {
    assertThat(this).isInstanceOf(Result.Success::class.java)
    return (this as Result.Success).data
}
fun <T> Result<T>.assertError(): AppException {
    assertThat(this).isInstanceOf(Result.Error::class.java)
    return (this as Result.Error).exception
}
```

---

## Screenshot Tests (Paparazzi)

> Not covered in `docs/ai/` — unique to this file.

Screenshot tests live in `core:ui/src/test/`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class AppButtonScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `AppButton — enabled state`() {
        paparazzi.snapshot { AppTheme { AppButton(text = "Continue", onClick = {}) } }
    }
}
```

- Record golden images: `./gradlew :core:ui:recordPaparazziDebug`
- Verify in CI: `./gradlew :core:ui:verifyPaparazziDebug`

---

## ViewModel Testing Checklist

- [ ] `@get:Rule val mainDispatcherRule = MainDispatcherRule()` present
- [ ] Uses `FakeXxx` from `core:testing` — zero mocks for repositories
- [ ] Happy path covered
- [ ] Error path covered
- [ ] One-shot `UiEvent` tested via `uiEvent.first()`
- [ ] `runTest {}` wraps all coroutine assertions
