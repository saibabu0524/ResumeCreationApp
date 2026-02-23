package com.softsuave.resumecreationapp.core.featureflags

/**
 * Enumeration of all feature flags in the application.
 *
 * Each flag has a [defaultValue] that is used when no remote configuration
 * is available, ensuring the app works correctly in all environments without
 * requiring a network call.
 *
 * Naming convention: SCREAMING_SNAKE_CASE — each name should be self-describing.
 *
 * To add a new flag:
 *  1. Add an entry here with a safe default.
 *  2. Map it in [com.softsuave.resumecreationapp.core.featureflags.impl.RemoteConfigFeatureFlags]
 *     if remote override support is needed.
 *  3. [com.softsuave.resumecreationapp.core.featureflags.impl.LocalFeatureFlags] will pick it up automatically.
 */
enum class Feature(val defaultValue: Boolean) {

    /** Allows users to opt-in to the system dark theme. */
    DARK_MODE(defaultValue = true),

    /** Enables biometric authentication on the login and profile screens. */
    BIOMETRIC_LOGIN(defaultValue = false),

    /** Enables the redesigned home screen layout. */
    NEW_HOME_LAYOUT(defaultValue = false),

    /** Enables in-app notifications panel. */
    IN_APP_NOTIFICATIONS(defaultValue = false),

    /** Shows the onboarding flow for first-time users. */
    ONBOARDING_FLOW(defaultValue = true),
}
