package com.softsuave.resumecreationapp.core.featureflags.impl

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.softsuave.resumecreationapp.core.featureflags.Feature
import com.softsuave.resumecreationapp.core.featureflags.FeatureFlags
import javax.inject.Inject

/**
 * [FeatureFlags] implementation backed by Firebase Remote Config.
 *
 * The Remote Config key for each flag is the lowercase name of the [Feature] enum
 * constant (e.g., `Feature.DARK_MODE` → key `"dark_mode"`).
 *
 * Falls back to [Feature.defaultValue] if:
 *  - The key is not present in Remote Config.
 *  - The Remote Config fetch has not completed yet.
 *  - Any exception occurs during the lookup.
 *
 * Bind this implementation in the `prod`/`staging` Hilt modules:
 * ```kotlin
 * @Binds
 * abstract fun bindFeatureFlags(impl: RemoteConfigFeatureFlags): FeatureFlags
 * ```
 */
class RemoteConfigFeatureFlags @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : FeatureFlags {

    override fun isEnabled(feature: Feature): Boolean =
        runCatching {
            val key = feature.name.lowercase()
            // getBoolean returns the default (false) when the key is absent.
            // We explicitly fall back to Feature.defaultValue to honour project defaults.
            if (remoteConfig.getKeysByPrefix(key).contains(key)) {
                remoteConfig.getBoolean(key)
            } else {
                feature.defaultValue
            }
        }.getOrDefault(feature.defaultValue)
}
