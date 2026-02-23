package com.softsuave.resumecreationapp.core.data.remote

import com.softsuave.resumecreationapp.core.data.remote.dto.UserDto
import com.softsuave.resumecreationapp.core.domain.model.Result
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API service for user endpoints.
 *
 * All functions return [Result] via the [com.softsuave.resumecreationapp.core.network.adapter.ApiResultCallAdapterFactory].
 * No try-catch is needed at the call site.
 */
interface UserApi {

    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Result<UserDto>

    @PUT("users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body dto: UserDto,
    ): Result<UserDto>

    @DELETE("users/me")
    suspend fun deleteAccount(): Result<Unit>
}
