package com.softsuave.resumecreationapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user row in the local database.
 *
 * This is NOT the domain [com.softsuave.resumecreationapp.core.domain.model.User] model.
 * Conversion between entity and domain model is done by a mapper in `:core:data`.
 *
 * Table name: `users`
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "is_email_verified")
    val isEmailVerified: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
