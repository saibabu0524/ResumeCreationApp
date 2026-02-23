package com.softsuave.resumecreationapp.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root application theme wrapping Material3 with dynamic color support.
 *
 * - **Dynamic Color** (Android 12+): Uses system wallpaper-based color scheme
 *   with graceful fallback to custom [LightColorScheme] / [DarkColorScheme]
 *   on older API levels.
 * - Provides [LocalSpacing] via [CompositionLocalProvider].
 * - Sets the status bar appearance to match the current theme.
 *
 * Every screen in the app must be wrapped in [AppTheme].
 *
 * @param darkTheme Whether to use dark mode. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic color on Android 12+. Defaults to `true`.
 * @param content The composable content to render inside the theme.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar icon style to match the theme (edge-to-edge handles color)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
