package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  RESUME TAILOR — Dual-Mode Color Palette
//  Dark:  Near-black warm canvas · Amber gold accent · Warm off-white text
//  Light: Warm parchment canvas · Rich amber · Deep warm-brown text
// ─────────────────────────────────────────────────────────────────────────────

// ─── Dark Background / Canvas ─────────────────────────────────────────────────
val Canvas        = Color(0xFF0E0D0B)   // dark deepest background
val CanvasWarm    = Color(0xFF131109)   // dark slightly warm variant

// ─── Dark Surfaces ────────────────────────────────────────────────────────────
val Surface0      = Color(0xFF1A1814)   // dark base card surface
val Surface1      = Color(0xFF242019)   // dark elevated surface
val Surface2      = Color(0xFF2E2A21)   // dark highest elevation

// ─── Dark Borders ─────────────────────────────────────────────────────────────
val BorderSubtle  = Color(0xFF2E2A24)
val BorderMid     = Color(0xFF4A4238)
val BorderStrong  = Color(0xFF7A6E5E)

// ─── Amber — Primary Accent (shared between themes) ──────────────────────────
val Amber10       = Color(0xFF2A1E08)
val Amber20       = Color(0xFF4A3412)
val Amber30       = Color(0xFF6A4D1C)
val Amber40       = Color(0xFF8A6930)   // dim / disabled
val Amber70       = Color(0xFFCFA050)
val Amber80       = Color(0xFFD4A853)   // primary accent on dark
val Amber90       = Color(0xFFDFBB75)
val Amber95       = Color(0xFFEFC97A)   // bright / hover glow
val Amber99       = Color(0xFFF7E5C0)   // near-white tint

// ─── Dark Text ─────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF0EAD6)   // warm off-white (dark mode)
val TextSecondary = Color(0xFFBFB49A)
val TextMuted     = Color(0xFF9A8E78)
val TextFaint     = Color(0xFF5A5040)

// ─── Light Theme Raw Values ───────────────────────────────────────────────────
// Warm parchment/ivory palette — editorial feel on light backgrounds
val LightPaper        = Color(0xFFFAF7F0)   // warmest background (parchment)
val LightPaperWarm    = Color(0xFFF5F0E8)   // slightly warmer paper
val LightSurface0     = Color(0xFFEFE9DC)   // card surface
val LightSurface1     = Color(0xFFE8E0D0)   // elevated card
val LightSurface2     = Color(0xFFDFD5C2)   // highest elevation
val LightBorderSubtle = Color(0xFFD8CDB8)
val LightBorderMid    = Color(0xFFC0B49A)
val LightBorderStrong = Color(0xFF9A8E78)
val LightTextPrimary  = Color(0xFF1F1A12)   // deep warm brown
val LightTextSecondary = Color(0xFF4A3D28)
val LightTextMuted    = Color(0xFF7A6E58)
val LightAmberDark    = Color(0xFF9B6E18)   // darker amber for light bg contrast

// ─── Semantic ─────────────────────────────────────────────────────────────────
val SemanticError       = Color(0xFFB04A3A)
val SemanticErrorDim    = Color(0xFF2D1410)
val SemanticErrorBright = Color(0xFFE06050)
val SemanticSuccess     = Color(0xFF4A7C59)
val SemanticSuccessDim  = Color(0xFF0F1F14)
val SemanticWarning     = Color(0xFFC08030)
val SemanticMiss        = Color(0xFF9A3A2A)

// ─── Convenience aliases (backward compat for feature screens that use raw tokens) ─
val Amber       = Amber80
val AmberGlow   = Amber95
val AmberDim    = Amber40
val Surface     = Surface0
val SurfaceHigh = Surface1
val Border      = BorderSubtle
val ErrorRed    = SemanticError
val ErrorDim    = SemanticErrorDim
val SuccessGreen = SemanticSuccess
val RedMiss     = SemanticMiss
val GreenOk     = SemanticSuccess

// ─────────────────────────────────────────────────────────────────────────────
//  Dark Color Scheme
// ─────────────────────────────────────────────────────────────────────────────
val DarkColorScheme = darkColorScheme(
    // Primary — Amber
    primary                = Amber80,
    onPrimary              = Canvas,
    primaryContainer       = Amber20,
    onPrimaryContainer     = Amber95,

    // Secondary — muted amber-brown
    secondary              = Amber70,
    onSecondary            = Canvas,
    secondaryContainer     = Amber10,
    onSecondaryContainer   = Amber90,

    // Tertiary — warm neutral
    tertiary               = TextSecondary,
    onTertiary             = Canvas,
    tertiaryContainer      = Surface2,
    onTertiaryContainer    = TextPrimary,

    // Error
    error                  = SemanticError,
    onError                = TextPrimary,
    errorContainer         = SemanticErrorDim,
    onErrorContainer       = SemanticErrorBright,

    // Background / Canvas
    background             = Canvas,
    onBackground           = TextPrimary,

    // Surface hierarchy
    surface                = Surface0,
    onSurface              = TextPrimary,
    surfaceVariant         = Surface1,
    onSurfaceVariant       = TextMuted,

    // Borders / outlines
    outline                = BorderMid,
    outlineVariant         = BorderSubtle,

    // Inverse (used by SnackBar, tooltips, etc.)
    inverseSurface         = TextPrimary,
    inverseOnSurface       = Canvas,
    inversePrimary         = Amber40,

    // Scrim
    scrim                  = Color(0xCC0E0D0B),
)

// ─────────────────────────────────────────────────────────────────────────────
//  Light Color Scheme — warm parchment editorial feel
// ─────────────────────────────────────────────────────────────────────────────
val LightColorScheme = lightColorScheme(
    // Primary — Rich amber (darker for contrast on light bg)
    primary                = LightAmberDark,
    onPrimary              = Color.White,
    primaryContainer       = Amber99,
    onPrimaryContainer     = Amber20,

    // Secondary — muted amber
    secondary              = Amber70,
    onSecondary            = Color.White,
    secondaryContainer     = Color(0xFFF0E4C8),
    onSecondaryContainer   = Amber30,

    // Tertiary — warm brown neutral
    tertiary               = LightTextMuted,
    onTertiary             = Color.White,
    tertiaryContainer      = LightSurface1,
    onTertiaryContainer    = LightTextPrimary,

    // Error
    error                  = SemanticError,
    onError                = Color.White,
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),

    // Background — parchment paper
    background             = LightPaper,
    onBackground           = LightTextPrimary,

    // Surface hierarchy
    surface                = LightPaperWarm,
    onSurface              = LightTextPrimary,
    surfaceVariant         = LightSurface0,
    onSurfaceVariant       = LightTextMuted,

    // Borders / outlines
    outline                = LightBorderMid,
    outlineVariant         = LightBorderSubtle,

    // Inverse
    inverseSurface         = LightTextPrimary,
    inverseOnSurface       = LightPaper,
    inversePrimary         = Amber80,

    // Scrim
    scrim                  = Color(0x661F1A12),
)
