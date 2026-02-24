package com.softsuave.resumecreationapp.core.data.mapper

import com.softsuave.resumecreationapp.core.data.remote.dto.UserDto
import com.softsuave.resumecreationapp.core.database.entity.UserEntity
import com.softsuave.resumecreationapp.core.domain.model.User

/**
 * Mapper functions for converting between the three [User] model types:
 *
 *  - [UserDto]    — network response shape (lives in `:core:data`)
 *  - [UserEntity] — Room database row (lives in `:core:database`)
 *  - [User]       — domain model (lives in `:core:domain`)
 *
 * These three types are NEVER mixed. Conversion always runs through these functions.
 *
 * Data flow:
 * ```
 * UserDto  ──toEntity()──► UserEntity  ──toDomain()──► User
 * UserDto  ──toDomain()──────────────────────────────► User
 * User     ──toEntity()──► UserEntity
 * ```
 */
object UserMapper {

    // ─── DTO → Domain ─────────────────────────────────────────────────────────

    fun UserDto.toDomain(): User = User(
        id = id,
        email = email,
        displayName = displayName ?: "",
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt ?: 0L,
        updatedAt = updatedAt ?: 0L,
    )

    // ─── DTO → Entity ─────────────────────────────────────────────────────────

    fun UserDto.toEntity(): UserEntity = UserEntity(
        id = id,
        email = email,
        displayName = displayName ?: "",
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt ?: 0L,
        updatedAt = updatedAt ?: 0L,
    )

    // ─── Entity → Domain ──────────────────────────────────────────────────────

    fun UserEntity.toDomain(): User = User(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ─── Domain → Entity ──────────────────────────────────────────────────────

    fun User.toEntity(): UserEntity = UserEntity(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
