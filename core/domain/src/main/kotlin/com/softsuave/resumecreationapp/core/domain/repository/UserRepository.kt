package com.softsuave.resumecreationapp.core.domain.repository

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-related data operations.
 *
 * Declared in `:core:domain` — the implementation lives in `:core:data`.
 * This interface depends only on domain types; it must never import
 * Room, Retrofit, DataStore, or any other framework.
 */
interface UserRepository {

    /**
     * Observes the currently authenticated user as a stream.
     * Emits [Result.Loading] initially, then [Result.Success] or [Result.Error].
     * Emits `null` inside [Result.Success] when no user is signed in.
     */
    fun observeCurrentUser(): Flow<Result<User?>>

    /**
     * Returns the user with the given [userId].
     * Returns [Result.Error] with [com.softsuave.resumecreationapp.core.domain.model.AppException.NotFound]
     * if no user matches the id.
     */
    suspend fun getUserById(userId: String): Result<User>

    /**
     * Updates the mutable fields of [user].
     * Returns [Result.Success] with [Unit] on success.
     */
    suspend fun updateUser(user: User): Result<Unit>

    /**
     * Deletes the account of the currently authenticated user.
     * Callers must sign the user out and clear local state after a successful response.
     */
    suspend fun deleteAccount(): Result<Unit>
}
