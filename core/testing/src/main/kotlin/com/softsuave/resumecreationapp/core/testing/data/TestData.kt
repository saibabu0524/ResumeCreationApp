package com.softsuave.resumecreationapp.core.testing.data

import com.softsuave.resumecreationapp.core.domain.model.User

/**
 * Pre-built test data objects (shared fixtures).
 *
 * Every test that needs a [User] should use these factories instead of
 * creating ad-hoc instances. This ensures consistency and reduces
 * boilerplate across the test suite.
 *
 * Usage:
 * ```kotlin
 * val user = TestData.user()
 * val customUser = TestData.user(displayName = "Custom Name")
 * ```
 */
object TestData {

    // ─── Default values ──────────────────────────────────────────────────────

    const val DEFAULT_USER_ID = "test_user_123"
    const val DEFAULT_EMAIL = "jane.doe@example.com"
    const val DEFAULT_DISPLAY_NAME = "Jane Doe"
    const val DEFAULT_AVATAR_URL = "https://example.com/avatar.jpg"
    const val DEFAULT_CREATED_AT = 1700000000000L
    const val DEFAULT_UPDATED_AT = 1700100000000L

    // ─── User Factory ────────────────────────────────────────────────────────

    /**
     * Creates a [User] with sensible defaults. Override individual fields as needed.
     */
    fun user(
        id: String = DEFAULT_USER_ID,
        email: String = DEFAULT_EMAIL,
        displayName: String = DEFAULT_DISPLAY_NAME,
        avatarUrl: String? = DEFAULT_AVATAR_URL,
        isEmailVerified: Boolean = true,
        createdAt: Long = DEFAULT_CREATED_AT,
        updatedAt: Long = DEFAULT_UPDATED_AT,
    ): User = User(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /**
     * Creates a list of [count] distinct users.
     */
    fun users(count: Int = 3): List<User> =
        (1..count).map { index ->
            user(
                id = "user_$index",
                email = "user$index@example.com",
                displayName = "User $index",
            )
        }
}
