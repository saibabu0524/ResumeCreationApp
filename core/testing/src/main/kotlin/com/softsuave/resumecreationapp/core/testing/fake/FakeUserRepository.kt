package com.softsuave.resumecreationapp.core.testing.fake

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [UserRepository] for testing.
 *
 * Philosophy: **Fakes over mocks** — changing the internal repository
 * implementation without changing its interface does NOT break any test.
 *
 * Configure behavior via:
 *  - [addUser] / [setUsers] — populate test data
 *  - [setShouldReturnError] — make all calls return an error
 *  - [setCurrentUser] — set the "logged in" user observed by [observeCurrentUser]
 */
class FakeUserRepository : UserRepository {

    private val users = mutableListOf<User>()
    private val currentUser = MutableStateFlow<User?>(null)
    private var shouldReturnError = false
    private var errorToReturn: AppException = AppException.Unknown()

    // ─── Test Helpers ────────────────────────────────────────────────────────

    fun addUser(user: User) {
        users.add(user)
    }

    fun setUsers(userList: List<User>) {
        users.clear()
        users.addAll(userList)
    }

    fun setCurrentUser(user: User?) {
        currentUser.value = user
        user?.let { addUser(it) }
    }

    fun setShouldReturnError(
        shouldError: Boolean,
        error: AppException = AppException.Unknown(),
    ) {
        shouldReturnError = shouldError
        errorToReturn = error
    }

    fun clearAll() {
        users.clear()
        currentUser.value = null
        shouldReturnError = false
        errorToReturn = AppException.Unknown()
    }

    // ─── UserRepository Implementation ───────────────────────────────────────

    override fun observeCurrentUser(): Flow<Result<User?>> =
        currentUser.map { user ->
            if (shouldReturnError) {
                Result.Error(errorToReturn)
            } else {
                Result.Success(user)
            }
        }

    override suspend fun getUserById(userId: String): Result<User> {
        if (shouldReturnError) return Result.Error(errorToReturn)

        return users.find { it.id == userId }
            ?.let { Result.Success(it) }
            ?: Result.Error(AppException.NotFound("User with id $userId not found."))
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        if (shouldReturnError) return Result.Error(errorToReturn)

        val index = users.indexOfFirst { it.id == user.id }
        if (index == -1) {
            return Result.Error(AppException.NotFound("User with id ${user.id} not found."))
        }
        users[index] = user
        if (currentUser.value?.id == user.id) {
            currentUser.value = user
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        if (shouldReturnError) return Result.Error(errorToReturn)

        currentUser.value = null
        users.clear()
        return Result.Success(Unit)
    }
}
