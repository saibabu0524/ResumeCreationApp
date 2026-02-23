package com.softsuave.resumecreationapp.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_preferences")

/**
 * Hilt module providing DataStore and encrypted token storage.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userPreferencesDataStore

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>,
    ): UserPreferencesRepository = UserPreferencesRepository(dataStore)

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context,
    ): TokenStorage = TokenStorage(TokenStorage.create(context))
}
