package com.softsuave.resumecreationapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root application theme for Resume Tailor.
 *
 * Design direction: Dark Editorial — near-black warm canvas with amber gold
 * accents and a monospace/serif typographic pairing.
 *
 * Dynamic color is intentionally disabled to preserve the curated palette.
 * The app always renders in dark mode regardless of system preference.
 *
 * @param content The composable content to render inside the theme.
 */
@Composable
fun AppTheme(
    // Kept for signature compatibility. Ignored — app is always dark.
    darkTheme: Boolean = true,
    // Dynamic color is disabled to preserve the curated amber palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge: let Compose draw behind bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Status bar icons — always light (white) on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }

            // Transparent system bars so Compose content shows through
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
