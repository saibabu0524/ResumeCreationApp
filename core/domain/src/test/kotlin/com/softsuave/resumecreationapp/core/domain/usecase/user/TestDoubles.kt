package com.softsuave.resumecreationapp.core.domain.usecase.user

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-module fake for [UserRepository] used by domain layer tests.
 *
 * This is a lightweight copy of the fake in `core:testing` — necessary
 * because `core:domain` is a pure JVM module and cannot depend on
 * `core:testing` (which is an Android library module).
 */
internal class FakeUserRepository : UserRepository {

    private val users = mutableListOf<User>()
    private val currentUser = MutableStateFlow<User?>(null)
    private var shouldReturnError = false
    private var errorToReturn: AppException = AppException.Unknown()

    fun addUser(user: User) {
        users.add(user)
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

    override fun observeCurrentUser(): Flow<Result<User?>> =
        currentUser.map { user ->
            if (shouldReturnError) Result.Error(errorToReturn)
            else Result.Success(user)
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
        if (index == -1) return Result.Error(AppException.NotFound())
        users[index] = user
        if (currentUser.value?.id == user.id) currentUser.value = user
        return Result.Success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        if (shouldReturnError) return Result.Error(errorToReturn)
        currentUser.value = null
        users.clear()
        return Result.Success(Unit)
    }
}

/**
 * Factory for creating test [User] instances in domain tests.
 */
internal object TestUsers {
    const val DEFAULT_USER_ID = "test_user_123"

    fun user(
        id: String = DEFAULT_USER_ID,
        email: String = "jane.doe@example.com",
        displayName: String = "Jane Doe",
        avatarUrl: String? = "https://example.com/avatar.jpg",
        isEmailVerified: Boolean = true,
        createdAt: Long = 1700000000000L,
        updatedAt: Long = 1700100000000L,
    ): User = User(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun users(count: Int = 3): List<User> =
        (1..count).map { i -> user(id = "user_$i", displayName = "User $i", email = "user$i@example.com") }
}
