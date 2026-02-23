package com.softsuave.resumecreationapp.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for the user resource returned by the API.
 *
 * This is NOT the domain [com.softsuave.resumecreationapp.core.domain.model.User].
 * Never pass this class to the domain or presentation layer — use [UserMapper] to convert.
 *
 * Field names match the server JSON contract. Use `@SerialName` when the server
 * uses a different naming convention (e.g., snake_case vs camelCase).
 */
@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("is_email_verified")
    val isEmailVerified: Boolean = false,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,
)
