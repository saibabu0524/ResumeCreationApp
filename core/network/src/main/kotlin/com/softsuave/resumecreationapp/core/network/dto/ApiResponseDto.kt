package com.softsuave.resumecreationapp.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Standard response envelope used by all backend endpoints.
 *
 * Every route returns `{ "data": {...}, "message": "...", "success": true }`.
 * Use this as the outermost type in any Retrofit service that calls the API.
 *
 * ```kotlin
 * interface UserApi {
 *     @GET("users/me")
 *     suspend fun getMe(): Result<ApiResponseDto<UserDto>>
 * }
 * // Unwrap: result.data?.data   ← inner payload
 * ```
 */
@Serializable
data class ApiResponseDto<T>(
    val data: T? = null,
    val message: String = "",
    val success: Boolean = true,
)

/**
 * Simple acknowledgement response returned by endpoints that carry no data payload
 * (logout, delete). Matches backend `MessageResponse`.
 */
@Serializable
data class MessageResponseDto(
    val message: String,
    val success: Boolean = true,
)
