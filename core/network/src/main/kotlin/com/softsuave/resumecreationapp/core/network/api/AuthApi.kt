package com.softsuave.resumecreationapp.core.network.api

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.dto.MessageResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// ─── Request DTOs ─────────────────────────────────────────────────────────────

/** Body for `POST /api/v1/auth/register`. Password min 8, max 128 chars. */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
)

/** Body for `POST /api/v1/auth/login`. */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/** Body for `POST /api/v1/auth/refresh`. */
@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

/** Body for `POST /api/v1/auth/logout`. */
@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

// ─── Response DTOs ────────────────────────────────────────────────────────────

/**
 * User representation returned by `POST /api/v1/auth/register`.
 * Matches backend `UserPublic` schema.
 */
@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_superuser") val isSuperuser: Boolean = false,
)

/**
 * Token pair returned by `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh`.
 */
@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
)

// ─── API Interface ────────────────────────────────────────────────────────────

/**
 * Retrofit API service for authentication endpoints.
 *
 * All paths are relative to the base URL which must end with `/api/v1/`.
 * Uses the unauthenticated `OkHttpClient` — no `Authorization` header is attached.
 *
 * All functions return [Result] via [com.softsuave.resumecreationapp.core.network.adapter.ApiResultCallAdapterFactory].
 */
interface AuthApi {

    /** POST /api/v1/auth/register — creates a new user account. Returns 201 on success. */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto,
    ): Result<ApiResponseDto<UserResponseDto>>

    /** POST /api/v1/auth/login — authenticates and returns an access + refresh token pair. */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): Result<ApiResponseDto<TokenResponseDto>>

    /** POST /api/v1/auth/refresh — rotates the refresh token. Returns a new token pair. */
    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequestDto,
    ): Result<ApiResponseDto<TokenResponseDto>>

    /**
     * POST /api/v1/auth/logout — revokes the refresh token.
     * Always returns 200 regardless of token validity (server-side idempotent).
     */
    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequestDto,
    ): Result<MessageResponseDto>
}
