package com.softsuave.resumecreationapp.core.network.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import com.softsuave.resumecreationapp.core.domain.model.Result

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

interface AuthApi {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Result<UserResponse>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Result<TokenResponse>
}
