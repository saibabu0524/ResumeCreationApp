package com.softsuave.resumecreationapp.core.network.di

import com.softsuave.resumecreationapp.core.network.api.ResumeApi
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

import com.softsuave.resumecreationapp.core.network.di.NAMED_BASE_URL

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ResumeClient

@Module
@InstallIn(SingletonComponent::class)
object ResumeNetworkModule {

    @Provides
    @Singleton
    @ResumeClient
    fun provideResumeOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: com.softsuave.resumecreationapp.core.network.interceptor.AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // 5 min for PDF download
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideResumeApi(
        @ResumeClient okHttpClient: OkHttpClient,
        json: Json,
        @Named(NAMED_BASE_URL) baseUrl: String,
    ): ResumeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        // JSON converter needed for 202 job-submission response and polling responses.
        // ResponseBody return types (PDF download) bypass conversion automatically.
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ResumeApi::class.java)
}

