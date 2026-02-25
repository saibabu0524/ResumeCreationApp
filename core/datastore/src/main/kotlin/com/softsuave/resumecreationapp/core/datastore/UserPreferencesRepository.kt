package com.softsuave.resumecreationapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository for non-sensitive user preferences backed by [DataStore<Preferences>].
 *
 * This class is the ONLY public API for preferences — raw [Preferences] keys are never
 * exposed outside this module. All access goes through typed functions and [Flow]s.
 *
 * Auth tokens (access token, refresh token) are stored separately in
 * [TokenStorage] which uses `EncryptedSharedPreferences`.
 */
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    // ─── Keys (private — encapsulated here) ──────────────────────────────────

    private val themeModeKey = stringPreferencesKey(PreferencesKeys.KEY_THEME_MODE)
    private val isDarkModeKey = booleanPreferencesKey(PreferencesKeys.KEY_IS_DARK_MODE)
    private val onboardingCompletedKey = booleanPreferencesKey(PreferencesKeys.KEY_ONBOARDING_COMPLETED)
    private val pushNotificationsKey = booleanPreferencesKey(PreferencesKeys.KEY_PUSH_NOTIFICATIONS_ENABLED)
    private val currentUserIdKey = stringPreferencesKey(PreferencesKeys.KEY_CURRENT_USER_ID)

    // ─── Theme Mode ──────────────────────────────────────────────────────────
    // Stored as a plain string: "light" | "dark" | "system"

    /** Emits one of "light", "dark", or "system". Defaults to "system" if unset. */
    val themeModeString: Flow<String> = dataStore.data.map {
        it[themeModeKey] ?: "system"
    }

    /**
     * Derived boolean convenience flow used by MainActivity.
     * - "dark"   → true
     * - "light"  → false
     * - "system" → false (MainActivity uses isSystemInDarkTheme() as the fallback)
     */
    val isDarkMode: Flow<Boolean> = dataStore.data.map {
        when (it[themeModeKey]) {
            "dark"  -> true
            "light" -> false
            else    -> it[isDarkModeKey] ?: false   // legacy fallback
        }
    }

    /**
     * Whether the theme is set to follow the system.
     * MainActivity uses this to decide whether to call isSystemInDarkTheme().
     */
    val isSystemTheme: Flow<Boolean> = dataStore.data.map {
        it[themeModeKey] == "system" || it[themeModeKey] == null
    }

    /**
     * Saves the theme mode preference.
     * @param mode one of "light", "dark", or "system"
     */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode
            // Keep legacy boolean in sync
            prefs[isDarkModeKey] = (mode == "dark")
        }
    }

    /** Legacy convenience wrapper — prefer [setThemeMode]. */
    suspend fun setDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) "dark" else "light")
    }

    // ─── Onboarding ───────────────────────────────────────────────────────────

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { it[onboardingCompletedKey] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[onboardingCompletedKey] = completed }
    }

    // ─── Push Notifications ───────────────────────────────────────────────────

    val isPushNotificationsEnabled: Flow<Boolean> = dataStore.data.map { it[pushNotificationsKey] ?: true }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[pushNotificationsKey] = enabled }
    }

    // ─── Current User ─────────────────────────────────────────────────────────

    val currentUserId: Flow<String?> = dataStore.data.map { it[currentUserIdKey] }

    suspend fun setCurrentUserId(userId: String?) {
        dataStore.edit { prefs ->
            if (userId != null) {
                prefs[currentUserIdKey] = userId
            } else {
                prefs.remove(currentUserIdKey)
            }
        }
    }

    // ─── Clear all ────────────────────────────────────────────────────────────

    /** Clears all stored preferences. Call on sign-out to wipe local state. */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
