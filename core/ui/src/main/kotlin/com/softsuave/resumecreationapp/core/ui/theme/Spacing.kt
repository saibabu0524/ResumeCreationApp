package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale used throughout the application.
 *
 * Screens never hardcode numeric margin or padding values.
 * Use [LocalSpacing] to access the current spacing values:
 *
 * ```kotlin
 * val spacing = LocalSpacing.current
 * Modifier.padding(spacing.medium)
 * ```
 */
@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val xxLarge: Dp = 48.dp,
)

/**
 * CompositionLocal providing the current [Spacing] values.
 *
 * Override at the theme level if a project needs different spacing.
 */
val LocalSpacing = compositionLocalOf { Spacing() }
