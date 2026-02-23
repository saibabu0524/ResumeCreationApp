package com.softsuave.resumecreationapp.core.network.di

import com.softsuave.resumecreationapp.core.network.api.ResumeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideResumeApi(
        @ResumeClient okHttpClient: OkHttpClient,
        @Named(NAMED_BASE_URL) baseUrl: String
    ): ResumeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(ResumeApi::class.java)
}
