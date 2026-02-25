package com.softsuave.resumecreationapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root application theme for Resume Tailor.
 *
 * Design direction: Dual editorial modes —
 *  - Dark: Near-black warm canvas with amber gold accents.
 *  - Light: Warm parchment with rich amber and deep brown text.
 *
 * Dynamic color is intentionally disabled to preserve the curated palette.
 *
 * @param darkTheme Whether to force dark mode.
 *                  Defaults to the system dark-mode preference.
 * @param dynamicColor Disabled — always uses the curated amber palette.
 * @param content The composable content to render inside the theme.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to preserve the curated amber palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge: let Compose draw behind bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Status bar icons — light icons on dark bg, dark icons on light bg
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
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
