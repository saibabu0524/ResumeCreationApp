package com.softsuave.resumecreationapp.core.domain.model

/**
 * Domain model representing a user of the application.
 *
 * Pure Kotlin — zero Android imports.
 * This is the canonical user shape for the entire domain layer.
 * DTOs (network) and Entities (database) are always mapped TO this type.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val isEmailVerified: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
