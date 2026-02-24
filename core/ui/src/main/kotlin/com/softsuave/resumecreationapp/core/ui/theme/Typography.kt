package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  RESUME TAILOR — Typography Scale
//
//  Pairing philosophy:
//    Display / Headline  → FontFamily.Serif   (editorial, authoritative)
//    Body / Label / UI   → FontFamily.Monospace (structured, precise)
//
//  To swap in a custom font (e.g. Playfair Display + JetBrains Mono):
//    1. Place .ttf / .otf files in res/font/
//    2. Define FontFamily entries with Font() calls
//    3. Replace FontFamily.Serif / FontFamily.Monospace below
//
//  Example:
//    val PlayfairDisplay = FontFamily(Font(R.font.playfair_display_regular), ...)
//    val JetBrainsMono   = FontFamily(Font(R.font.jetbrains_mono_regular), ...)
// ─────────────────────────────────────────────────────────────────────────────

private val DisplayFont = FontFamily.Serif
private val UIFont      = FontFamily.Monospace

val AppTypography = Typography(

    // ─── Display — large serif headings ──────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.Light,
        fontSize      = 54.sp,
        lineHeight    = 62.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.Light,
        fontSize      = 42.sp,
        lineHeight    = 50.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 34.sp,
        lineHeight    = 42.sp,
        letterSpacing = (-0.25).sp,
    ),

    // ─── Headline — section / screen titles ───────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 30.sp,
        lineHeight    = 38.sp,
        letterSpacing = (-0.15).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 26.sp,
        lineHeight    = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 30.sp,
        letterSpacing = 0.sp,
    ),

    // ─── Title — card / dialog / sheet titles ─────────────────────────────────
    titleLarge = TextStyle(
        fontFamily    = DisplayFont,
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    titleSmall = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.5.sp,
    ),

    // ─── Body — reading text ──────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        lineHeight    = 17.sp,
        letterSpacing = 0.4.sp,
    ),

    // ─── Label — buttons, chips, eyebrow text ─────────────────────────────────
    labelLarge = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Bold,
        fontSize      = 13.sp,
        lineHeight    = 20.sp,
        letterSpacing = 2.sp,           // uppercase monospace labels
    ),
    labelMedium = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 1.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = UIFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 2.sp,
    ),
)
