package com.softsuave.resumecreationapp.core.domain.repository

import com.softsuave.resumecreationapp.core.domain.model.Result

interface AuthRepository {
    /**
     * Authenticates a user and stores the access + refresh token pair locally.
     */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Registers a new user and automatically logs them in upon success.
     */
    suspend fun register(email: String, password: String): Result<Unit>

    /**
     * Rotates the stored refresh token and saves the new token pair.
     * Returns [com.softsuave.resumecreationapp.core.domain.model.AppException.NotAuthenticated]
     * if no refresh token is stored locally.
     */
    suspend fun refreshToken(): Result<Unit>

    /**
     * Revokes the refresh token on the server and clears all locally stored tokens.
     * Always succeeds locally — the server call is best-effort.
     */
    suspend fun logout(): Result<Unit>
}
