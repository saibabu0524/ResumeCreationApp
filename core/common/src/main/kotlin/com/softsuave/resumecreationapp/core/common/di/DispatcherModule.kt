package com.softsuave.resumecreationapp.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides coroutine dispatchers bound to qualifier annotations.
 *
 * Always inject a dispatcher via qualifier rather than referencing
 * [Dispatchers.IO] / [Dispatchers.Main] / [Dispatchers.Default] directly.
 * This makes all classes that depend on dispatchers fully testable with
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher].
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    fun provideGenericDispatcher(
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineDispatcher = dispatcher
}
