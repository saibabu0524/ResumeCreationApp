package com.softsuave.resumecreationapp.di

import com.softsuave.resumecreationapp.BuildConfig
import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.network.di.NAMED_BASE_URL
import com.softsuave.resumecreationapp.core.network.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

/**
 * App-level Hilt module that wires dependencies that require knowledge of
 * both `core:network` and `core:datastore` — two modules that must not know
 * about each other.
 *
 * The [AuthInterceptor] is provided here because it depends on [TokenStorage]
 * from `core:datastore`, and `core:network` must not import `core:datastore`.
 *
 * The API base URL is provided here from [BuildConfig.BASE_URL] which is set
 * per product flavor by the convention plugin.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStorage: TokenStorage): AuthInterceptor =
        AuthInterceptor(tokenProvider = { tokenStorage.accessToken })

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenStorage: TokenStorage,
        authApiProvider: javax.inject.Provider<com.softsuave.resumecreationapp.core.network.api.AuthApi>
    ): com.softsuave.resumecreationapp.core.network.interceptor.TokenAuthenticator =
        com.softsuave.resumecreationapp.core.network.interceptor.TokenAuthenticator(
            authApiProvider = authApiProvider,
            tokenProvider = { tokenStorage.accessToken },
            refreshTokenProvider = { tokenStorage.refreshToken },
            onTokenRefreshed = { newAccess, newRefresh ->
                tokenStorage.saveTokens(newAccess, newRefresh)
            },
            onLogout = {
                tokenStorage.clearTokens()
            }
        )

    @Provides
    @Singleton
    @Named(NAMED_BASE_URL)
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /**
     * Override the logging interceptor level from the app module so it can
     * read [BuildConfig.DEBUG] which is set per build variant.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
}
