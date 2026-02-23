package com.softsuave.resumecreationapp.core.featureflags.impl

import com.softsuave.resumecreationapp.core.featureflags.Feature
import com.softsuave.resumecreationapp.core.featureflags.FeatureFlags
import javax.inject.Inject

/**
 * Default implementation of [FeatureFlags] backed by a hard-coded map.
 *
 * The map starts with each flag's [Feature.defaultValue]. Individual entries
 * can be overridden for local testing without touching the remote config:
 *
 * ```kotlin
 * LocalFeatureFlags(overrides = mapOf(Feature.NEW_HOME_LAYOUT to true))
 * ```
 *
 * This implementation is bound in the `dev` product-flavor Hilt module so that
 * local builds never need a Firebase project or network connection to run.
 */
class LocalFeatureFlags @Inject constructor(
    private val overrides: Map<Feature, Boolean> = emptyMap(),
) : FeatureFlags {

    override fun isEnabled(feature: Feature): Boolean =
        overrides[feature] ?: feature.defaultValue
}
