package com.softsuave.resumecreationapp.core.network.di

import android.content.Context
import android.net.ConnectivityManager
import com.softsuave.resumecreationapp.core.network.NetworkMonitor
import com.softsuave.resumecreationapp.core.network.adapter.ApiResultCallAdapterFactory
import com.softsuave.resumecreationapp.core.network.interceptor.AuthInterceptor
import com.softsuave.resumecreationapp.core.network.interceptor.ConnectivityInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** Qualifier for the app's API base URL string. Provided by the app-level Hilt module. */
const val NAMED_BASE_URL = "base_url"

/**
 * Hilt module that provides all networking infrastructure.
 *
 * Consumers never import OkHttp or Retrofit directly — they depend only on
 * the API service interfaces and [NetworkMonitor].
 *
 * The [AuthInterceptor] requires a [tokenProvider] lambda. In the `app` module,
 * bind this to whatever source holds the access token (e.g., EncryptedSharedPreferences
 * via the DataStore token repository).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true       // Forward-compatible with new server fields
        isLenient = true               // Tolerates minor server JSON quirks
        encodeDefaults = true          // Serializes default values
        coerceInputValues = true       // Coerces null into default for non-null fields
    }

    @Provides
    @Singleton
    fun provideConnectivityInterceptor(
        @ApplicationContext context: Context,
    ): ConnectivityInterceptor {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return ConnectivityInterceptor(cm)
    }

    /**
     * The unauthenticated OkHttp client used for login / token-refresh calls.
     * Does not attach an Authorization header.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        connectivityInterceptor: ConnectivityInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(connectivityInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Authenticated OkHttp client — attaches the Bearer token on every request.
     * Provide the [tokenProvider] by binding it in your app-level Hilt module.
     */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        connectivityInterceptor: ConnectivityInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(connectivityInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideRetrofit(
        @AuthenticatedClient okHttpClient: OkHttpClient,
        json: Json,
        @Named(NAMED_BASE_URL) baseUrl: String,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ApiResultCallAdapterFactory())
        .build()

    @Provides
    @Singleton
    @Named("UnauthenticatedRetrofit")
    fun provideUnauthenticatedRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @Named(NAMED_BASE_URL) baseUrl: String,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ApiResultCallAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("UnauthenticatedRetrofit") retrofit: Retrofit
    ): com.softsuave.resumecreationapp.core.network.api.AuthApi = retrofit.create(com.softsuave.resumecreationapp.core.network.api.AuthApi::class.java)

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
    ): NetworkMonitor {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return NetworkMonitor(cm)
    }
}
