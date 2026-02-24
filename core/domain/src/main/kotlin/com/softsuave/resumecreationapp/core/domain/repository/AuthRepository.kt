package com.softsuave.resumecreationapp.core.domain.repository

import com.softsuave.resumecreationapp.core.domain.model.Result

interface AuthRepository {
    /**
     * Authenticates a user and stores the auth token locally.
     */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Registers a new user and automatically logs them in upon success.
     */
    suspend fun register(email: String, password: String): Result<Unit>
}
