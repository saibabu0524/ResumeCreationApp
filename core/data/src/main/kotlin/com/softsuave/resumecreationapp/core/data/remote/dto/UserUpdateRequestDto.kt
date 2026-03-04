package com.softsuave.resumecreationapp.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `PATCH /api/v1/users/me`.
 *
 * Both fields are optional — only non-null fields are sent.
 * Matches the backend `UserUpdateRequest` schema:
 *   - `email`    — must be a valid email; 409 if already taken.
 *   - `password` — min 8, max 128 characters; triggers refresh-token revocation.
 */
@Serializable
data class UserUpdateRequestDto(
    @SerialName("email")
    val email: String? = null,

    @SerialName("password")
    val password: String? = null,
)
