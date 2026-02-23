package com.softsuave.resumecreationapp.core.featureflags.di

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.softsuave.resumecreationapp.core.featureflags.FeatureFlags
import com.softsuave.resumecreationapp.core.featureflags.impl.RemoteConfigFeatureFlags
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires [FeatureFlags] to [RemoteConfigFeatureFlags].
 *
 * For the `dev` product flavor, override [FeatureFlags] with
 * [com.softsuave.resumecreationapp.core.featureflags.impl.LocalFeatureFlags] instead.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagsModule {

    @Binds
    @Singleton
    abstract fun bindFeatureFlags(impl: RemoteConfigFeatureFlags): FeatureFlags

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig =
            Firebase.remoteConfig.also { config ->
                val settings = remoteConfigSettings {
                    // In debug builds, use a short fetch interval; in release it defaults to 12h
                    minimumFetchIntervalInSeconds = 3_600L
                }
                config.setConfigSettingsAsync(settings)
            }
    }
}
