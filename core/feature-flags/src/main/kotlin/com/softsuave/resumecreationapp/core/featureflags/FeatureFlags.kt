package com.softsuave.resumecreationapp.core.featureflags

/**
 * Interface for querying feature flag state.
 *
 * Feature modules depend ONLY on this interface — never on Firebase Remote Config
 * or any other config service directly.
 *
 * Implementations:
 *  - [com.softsuave.resumecreationapp.core.featureflags.impl.LocalFeatureFlags] — hard-coded defaults; works offline
 *  - [com.softsuave.resumecreationapp.core.featureflags.impl.RemoteConfigFeatureFlags] — backed by Firebase Remote Config
 */
interface FeatureFlags {

    /**
     * Returns `true` if [feature] is currently enabled.
     *
     * Implementations must never throw; if the remote config is unavailable,
     * fall back to [Feature.defaultValue].
     */
    fun isEnabled(feature: Feature): Boolean
}
