package com.softsuave.resumecreationapp.core.datastore

/**
 * Keys for Preferences DataStore entries.
 *
 * All keys are centralised here — never referenced by string literal.
 * This makes refactoring safe and prevents key collisions.
 */
object PreferencesKeys {
    // Theme mode: "light" | "dark" | "system"
    const val KEY_THEME_MODE = "theme_mode"

    // Legacy dark-mode boolean — kept for backward compat, new code uses KEY_THEME_MODE
    const val KEY_IS_DARK_MODE = "is_dark_mode"

    // Onboarding
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    // Notifications
    const val KEY_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"

    // Currently signed-in user id (non-sensitive — auth tokens live in EncryptedSharedPreferences)
    const val KEY_CURRENT_USER_ID = "current_user_id"
}

