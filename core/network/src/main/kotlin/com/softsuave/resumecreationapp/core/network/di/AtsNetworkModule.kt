package com.softsuave.resumecreationapp.core.network.di

import com.softsuave.resumecreationapp.core.network.api.AtsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
annotation class AtsClient

@Module
@InstallIn(SingletonComponent::class)
object AtsNetworkModule {

    @Provides
    @Singleton
    @AtsClient
    fun provideAtsOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: com.softsuave.resumecreationapp.core.network.interceptor.AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideAtsApi(
        @AtsClient okHttpClient: OkHttpClient,
        json: Json,
        @Named(NAMED_BASE_URL) baseUrl: String,
    ): AtsApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AtsApi::class.java)
}
