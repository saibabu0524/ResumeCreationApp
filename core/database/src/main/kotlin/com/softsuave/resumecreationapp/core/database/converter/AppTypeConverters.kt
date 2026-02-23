package com.softsuave.resumecreationapp.core.database.converter

import androidx.room.TypeConverter

/**
 * Room [TypeConverter]s for all types that Room cannot store natively.
 *
 * Room supports: `Int`, `Long`, `Double`, `Float`, `Boolean`, `String`, `ByteArray`.
 * Everything else must be converted to and from one of those primitives.
 *
 * Applied at the database level via `@TypeConverters(AppTypeConverters::class)`
 * on [com.softsuave.resumecreationapp.core.database.AppDatabase] — no per-entity annotation needed.
 */
class AppTypeConverters {

    // ─── List<String> ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.split(SEPARATOR)

    // ─── Map<String, String> ─────────────────────────────────────────────────

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? =
        value?.entries?.joinToString(MAP_SEPARATOR) { "${it.key}$ENTRY_SEPARATOR${it.value}" }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.split(MAP_SEPARATOR)
            ?.associate { entry ->
                val (k, v) = entry.split(ENTRY_SEPARATOR, limit = 2)
                k to v
            }

    companion object {
        private const val SEPARATOR = ","
        private const val MAP_SEPARATOR = "||"
        private const val ENTRY_SEPARATOR = "::"
    }
}
