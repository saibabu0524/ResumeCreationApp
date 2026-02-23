package com.softsuave.resumecreationapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.softsuave.resumecreationapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserEntity].
 *
 * All queries return [Flow] where the result is reactive (room auto-emits on changes),
 * or `suspend` for one-shot write operations.
 */
@Dao
interface UserDao {

    /** Observes a single user by [id]. Emits `null` when the user doesn't exist. */
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    /** Returns a user by [id] in a one-shot suspend call. Returns `null` if not found. */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    /** Observes all stored users ordered by display name. */
    @Query("SELECT * FROM users ORDER BY display_name ASC")
    fun observeAll(): Flow<List<UserEntity>>

    /**
     * Inserts or replaces a user.
     * Using [Upsert] instead of [Insert] + [Update] avoids a separate query
     * to check existence and prevents race conditions.
     */
    @Upsert
    suspend fun upsert(user: UserEntity)

    /** Upserts a list of users in a single transaction. */
    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    /** Deletes a user by [id]. Returns the number of rows deleted. */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /** Deletes all users. Used on sign-out to clear local state. */
    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
