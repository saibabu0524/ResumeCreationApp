package com.softsuave.resumecreationapp.core.testing.fake

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository

/**
 * Fake implementation of [AuthRepository] for testing.
 *
 * Philosophy: **Fakes over mocks** — tests configure behaviour through
 * simple setter functions rather than MockK `every {}` stubs.
 *
 * Configure behaviour via:
 *  - [setLoginResult] — override what [login] returns
 *  - [setRegisterResult] — override what [register] returns
 *  - [setShouldReturnError] — make ALL calls return an error
 *
 * Invocation counts are tracked so tests can assert call counts.
 */
class FakeAuthRepository : AuthRepository {

    private var loginResult: Result<Unit> = Result.Success(Unit)
    private var registerResult: Result<Unit> = Result.Success(Unit)
    private var refreshTokenResult: Result<Unit> = Result.Success(Unit)
    private var logoutResult: Result<Unit> = Result.Success(Unit)

    private var shouldReturnError = false
    private var errorToReturn: AppException = AppException.Unknown()

    var loginCallCount: Int = 0
        private set
    var registerCallCount: Int = 0
        private set
    var logoutCallCount: Int = 0
        private set

    // ─── Test Helpers ─────────────────────────────────────────────────────────

    fun setLoginResult(result: Result<Unit>) {
        loginResult = result
    }

    fun setRegisterResult(result: Result<Unit>) {
        registerResult = result
    }

    fun setShouldReturnError(
        shouldError: Boolean,
        error: AppException = AppException.Unknown(),
    ) {
        shouldReturnError = shouldError
        errorToReturn = error
    }

    fun reset() {
        loginResult = Result.Success(Unit)
        registerResult = Result.Success(Unit)
        refreshTokenResult = Result.Success(Unit)
        logoutResult = Result.Success(Unit)
        shouldReturnError = false
        errorToReturn = AppException.Unknown()
        loginCallCount = 0
        registerCallCount = 0
        logoutCallCount = 0
    }

    // ─── AuthRepository Implementation ───────────────────────────────────────

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCallCount++
        return if (shouldReturnError) Result.Error(errorToReturn) else loginResult
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        registerCallCount++
        return if (shouldReturnError) Result.Error(errorToReturn) else registerResult
    }

    override suspend fun refreshToken(): Result<Unit> =
        if (shouldReturnError) Result.Error(errorToReturn) else refreshTokenResult

    override suspend fun logout(): Result<Unit> {
        logoutCallCount++
        return if (shouldReturnError) Result.Error(errorToReturn) else logoutResult
    }
}
