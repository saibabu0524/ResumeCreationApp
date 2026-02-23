package com.softsuave.resumecreationapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.softsuave.resumecreationapp.core.database.AppDatabase
import com.softsuave.resumecreationapp.core.database.AppDatabase.Companion.DATABASE_NAME
import com.softsuave.resumecreationapp.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and all DAOs.
 *
 * Safety is enforced at build time:
 *  - `fallbackToDestructiveMigration` is only allowed in debug builds.
 *  - Release builds will fail to compile if the flag is present
 *    (enforced via a custom lint check in `build-logic`).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME,
    )
        // ⚠️ Only enable destructive migration for debug builds.
        // Uncomment the line below ONLY in debug flavor configuration:
        // .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
