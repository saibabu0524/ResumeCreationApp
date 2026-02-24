package com.softsuave.resumecreationapp.core.data.di

import com.softsuave.resumecreationapp.core.data.remote.UserApi
import com.softsuave.resumecreationapp.core.data.repository.AtsRepositoryImpl
import com.softsuave.resumecreationapp.core.data.repository.UserRepositoryImpl
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.network.di.AuthenticatedClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt module that:
 *  1. Provides Retrofit API service instances.
 *  2. Binds repository implementations to domain interfaces.
 *
 * This is the ONLY place where repository impls are referenced directly.
 * All feature modules depend only on domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: com.softsuave.resumecreationapp.core.data.repository.AuthRepositoryImpl): com.softsuave.resumecreationapp.core.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindAtsRepository(impl: AtsRepositoryImpl): AtsRepository

    companion object {

        @Provides
        @Singleton
        fun provideUserApi(@AuthenticatedClient retrofit: Retrofit): UserApi =
            retrofit.create(UserApi::class.java)
    }
}

