package com.softsuave.resumecreationapp.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject

/**
 * Secure storage for auth tokens backed by [EncryptedSharedPreferences].
 *
 * AES256-GCM encryption via the Android Keystore, transparent to callers.
 *
 * Access tokens and refresh tokens are NEVER stored in plain [SharedPreferences]
 * or [UserPreferencesRepository] — only here.
 *
 * Usage:
 * ```kotlin
 * tokenStorage.saveTokens(accessToken = "eyJ...", refreshToken = "rtk...")
 * val accessToken = tokenStorage.accessToken // used by AuthInterceptor
 * tokenStorage.clearTokens()                 // on sign-out
 * ```
 */
class TokenStorage @Inject constructor(
    private val prefs: SharedPreferences,
) {

    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    val refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val ENCRYPTED_PREFS_FILE = "secure_token_prefs"

        /**
         * Creates the [SharedPreferences] backed by [EncryptedSharedPreferences].
         * Called from the Hilt DI module — not called directly in app code.
         */
        fun create(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
