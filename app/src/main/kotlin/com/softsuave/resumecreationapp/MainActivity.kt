package com.softsuave.resumecreationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import com.softsuave.resumecreationapp.core.ui.component.ThemeMode
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import com.softsuave.resumecreationapp.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single Activity architecture entry point.
 *
 * Reads the user's theme mode preference from [UserPreferencesRepository]:
 *   - "dark"   → always dark
 *   - "light"  → always light
 *   - "system" → follow the device system setting
 *
 * The preference is observed reactively, so any change made via the
 * Settings screen or the [ThemeSwitcherButton] takes effect immediately.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStorage: TokenStorage
    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe theme mode string reactively — "dark" | "light" | "system"
        var themeMode: String? by mutableStateOf(null)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferencesRepository.themeModeString.collect { mode ->
                    themeMode = mode
                }
            }
        }

        val startDestination: Any = if (tokenStorage.accessToken != null) {
            com.softsuave.resumecreationapp.feature.resume.navigation.ResumeGraph
        } else {
            com.softsuave.resumecreationapp.feature.auth.navigation.AuthGraphRoute
        }

        setContent {
            val systemDark = isSystemInDarkTheme()

            // Resolve effective dark mode from stored string preference
            val darkTheme = when (themeMode) {
                "dark"   -> true
                "light"  -> false
                "system" -> systemDark
                else     -> systemDark   // null (not yet loaded) → system default
            }

            // Map string preference → ThemeMode enum for the switcher button
            val currentThemeMode = when (themeMode) {
                "dark"  -> ThemeMode.Dark
                "light" -> ThemeMode.Light
                else    -> ThemeMode.System
            }

            AppTheme(darkTheme = darkTheme) {
                AppNavHost(
                    startDestination = startDestination,
                    currentThemeMode = currentThemeMode,
                    onThemeChanged = { newMode ->
                        // Persist the new choice — DataStore emits → recompose
                        lifecycleScope.launch {
                            preferencesRepository.setThemeMode(
                                when (newMode) {
                                    ThemeMode.Dark   -> "dark"
                                    ThemeMode.Light  -> "light"
                                    ThemeMode.System -> "system"
                                }
                            )
                        }
                    },
                )
            }
        }
    }
}
