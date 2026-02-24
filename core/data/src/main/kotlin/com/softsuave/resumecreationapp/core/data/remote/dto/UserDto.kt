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

    // ── Fields returned by the backend UserPublic schema ─────────────────────

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("is_superuser")
    val isSuperuser: Boolean = false,

    // ── App-enriched fields (not in the backend response; defaults applied) ──
    // These are populated from the local Room cache after the initial network fetch.

    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("is_email_verified")
    val isEmailVerified: Boolean = false,

    @SerialName("created_at")
    val createdAt: Long? = null,

    @SerialName("updated_at")
    val updatedAt: Long? = null,
)
