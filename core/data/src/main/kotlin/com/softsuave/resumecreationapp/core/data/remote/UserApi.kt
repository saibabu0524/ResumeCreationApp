package com.softsuave.resumecreationapp.core.data.remote

import com.softsuave.resumecreationapp.core.data.remote.dto.UserDto
import com.softsuave.resumecreationapp.core.data.remote.dto.UserUpdateRequestDto
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.dto.MessageResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * Retrofit API service for user endpoints.
 *
 * All paths are relative to the base URL which must end with `/api/v1/`.
 * Uses the authenticated [OkHttpClient] so every request carries a Bearer token.
 *
 * All functions return [Result] via [com.softsuave.resumecreationapp.core.network.adapter.ApiResultCallAdapterFactory].
 * No try-catch is needed at the call site.
 */
interface UserApi {

    /**
     * GET /api/v1/users/me — returns the currently authenticated user's profile.
     * The user identity is derived from the Bearer token; no path parameter is needed.
     */
    @GET("users/me")
    suspend fun getMe(): Result<ApiResponseDto<UserDto>>

    /**
     * PATCH /api/v1/users/me — partial update of email and/or password.
     * Changing the password triggers server-side revocation of all refresh tokens.
     */
    @PATCH("users/me")
    suspend fun updateMe(
        @Body dto: UserUpdateRequestDto,
    ): Result<ApiResponseDto<UserDto>>

    /**
     * DELETE /api/v1/users/me — deactivates (soft-deletes) the current user account.
     * The caller must clear local tokens and navigate to the auth screen after this call.
     */
    @DELETE("users/me")
    suspend fun deleteAccount(): Result<MessageResponseDto>
}
