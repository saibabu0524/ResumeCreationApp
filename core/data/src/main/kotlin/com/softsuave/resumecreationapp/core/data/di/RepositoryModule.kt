package com.softsuave.resumecreationapp.core.data.di

import com.softsuave.resumecreationapp.core.data.remote.UserApi
import com.softsuave.resumecreationapp.core.data.repository.AtsRepositoryImpl
import com.softsuave.resumecreationapp.core.data.repository.ResumeHistoryRepositoryImpl
import com.softsuave.resumecreationapp.core.data.repository.UserRepositoryImpl
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository
import com.softsuave.resumecreationapp.core.domain.repository.ResumeHistoryRepository
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.network.api.ResumeApi
import com.softsuave.resumecreationapp.core.network.di.AuthenticatedClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier to disambiguate the history-capable [ResumeApi] (uses authenticated Retrofit
 * with full JSON converter) from the raw [ResumeApi] in [ResumeNetworkModule] (which uses
 * a plain Retrofit client optimised for PDF streaming).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HistoryResumeApi

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
    abstract fun bindAuthRepository(impl: com.softsuave.resumecreationapp.core.data.repository.AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAtsRepository(impl: AtsRepositoryImpl): AtsRepository

    @Binds
    @Singleton
    abstract fun bindResumeHistoryRepository(impl: ResumeHistoryRepositoryImpl): ResumeHistoryRepository

    companion object {

        @Provides
        @Singleton
        fun provideUserApi(@AuthenticatedClient retrofit: Retrofit): UserApi =
            retrofit.create(UserApi::class.java)

        /**
         * Provides a [ResumeApi] instance built on top of the authenticated Retrofit client
         * (which includes the JSON converter and auth interceptor). This is used exclusively
         * by [ResumeHistoryRepositoryImpl] for the `GET /resume/history` endpoint.
         *
         * The [ResumeNetworkModule] provides a different [ResumeApi] binding used for
         * multipart PDF streaming (via its own OkHttp client with longer timeouts).
         * The [@HistoryResumeApi] qualifier prevents the Hilt binding conflict.
         */
        @Provides
        @Singleton
        @HistoryResumeApi
        fun provideHistoryResumeApi(@AuthenticatedClient retrofit: Retrofit): ResumeApi =
            retrofit.create(ResumeApi::class.java)
    }
}
