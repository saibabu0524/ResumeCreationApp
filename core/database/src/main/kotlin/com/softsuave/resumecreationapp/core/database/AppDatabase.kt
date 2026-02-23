package com.softsuave.resumecreationapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.softsuave.resumecreationapp.core.database.converter.AppTypeConverters
import com.softsuave.resumecreationapp.core.database.dao.UserDao
import com.softsuave.resumecreationapp.core.database.entity.UserEntity

/**
 * The application's Room database.
 *
 * Rules enforced here:
 *  - Schema is exported to `schemas/` (set in `build.gradle.kts` via the Room convention plugin).
 *  - `fallbackToDestructiveMigration()` is ONLY enabled in debug builds (enforced in [DatabaseModule]).
 *  - Every multi-table write operation uses `withTransaction {}` at the call site.
 *
 * To add a new entity:
 *  1. Create a new `*Entity` in `entity/`.
 *  2. Create a corresponding `*Dao` in `dao/`.
 *  3. Add the entity class to `entities` below.
 *  4. Add an abstract accessor function for the DAO.
 *  5. Increment [DATABASE_VERSION] and add a migration (or `@AutoMigration` if applicable).
 */
@Database(
    entities = [
        UserEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [],
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "template_db"
    }
}

/** Increment this when the schema changes. Always add a migration or @AutoMigration. */
const val DATABASE_VERSION = 1
