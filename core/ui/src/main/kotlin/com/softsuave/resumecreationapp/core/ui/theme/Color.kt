package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Primary ─────────────────────────────────────────────────────────────────
val Purple10 = Color(0xFF1D0040)
val Purple20 = Color(0xFF330066)
val Purple30 = Color(0xFF4A0099)
val Purple40 = Color(0xFF6200EE)
val Purple80 = Color(0xFFCFBCFF)
val Purple90 = Color(0xFFE9DDFF)

// ─── Secondary ───────────────────────────────────────────────────────────────
val Teal10 = Color(0xFF003B3F)
val Teal20 = Color(0xFF005457)
val Teal30 = Color(0xFF006D72)
val Teal40 = Color(0xFF03DAC6)
val Teal80 = Color(0xFF80F0E5)
val Teal90 = Color(0xFFB3F5EF)

// ─── Tertiary ────────────────────────────────────────────────────────────────
val Rose10 = Color(0xFF3E0021)
val Rose20 = Color(0xFF5D0035)
val Rose30 = Color(0xFF7D004C)
val Rose40 = Color(0xFFBB0066)
val Rose80 = Color(0xFFFFB1C8)
val Rose90 = Color(0xFFFFD9E2)

// ─── Error ───────────────────────────────────────────────────────────────────
val Red10 = Color(0xFF410002)
val Red20 = Color(0xFF690005)
val Red30 = Color(0xFF93000A)
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

// ─── Neutral ─────────────────────────────────────────────────────────────────
val Neutral10 = Color(0xFF1C1B1F)
val Neutral20 = Color(0xFF313033)
val Neutral90 = Color(0xFFE6E1E5)
val Neutral95 = Color(0xFFF4EFF4)
val Neutral99 = Color(0xFFFFFBFE)

// ─── Neutral Variant ─────────────────────────────────────────────────────────
val NeutralVariant30 = Color(0xFF49454F)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant60 = Color(0xFF938F99)
val NeutralVariant80 = Color(0xFFCAC4D0)
val NeutralVariant90 = Color(0xFFE7E0EC)

// ─── Color Schemes ───────────────────────────────────────────────────────────

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Rose40,
    onTertiary = Color.White,
    tertiaryContainer = Rose90,
    onTertiaryContainer = Rose10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Purple80,
)

val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple20,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Rose80,
    onTertiary = Rose20,
    tertiaryContainer = Rose30,
    onTertiaryContainer = Rose90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Purple40,
)
