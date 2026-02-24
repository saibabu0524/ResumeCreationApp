package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  RESUME TAILOR — Dark Editorial Color Palette
//  Aesthetic: Near-black warm canvas · Amber gold accent · Warm off-white text
// ─────────────────────────────────────────────────────────────────────────────

// ─── Canvas / Background ─────────────────────────────────────────────────────
val Canvas        = Color(0xFF0E0D0B)   // deepest background
val CanvasWarm    = Color(0xFF131109)   // slightly warm variant

// ─── Surfaces ────────────────────────────────────────────────────────────────
val Surface0      = Color(0xFF1A1814)   // base card surface
val Surface1      = Color(0xFF242019)   // elevated surface
val Surface2      = Color(0xFF2E2A21)   // highest elevation

// ─── Borders ─────────────────────────────────────────────────────────────────
val BorderSubtle  = Color(0xFF2E2A24)
val BorderMid     = Color(0xFF4A4238)
val BorderStrong  = Color(0xFF7A6E5E)

// ─── Amber — Primary Accent ──────────────────────────────────────────────────
val Amber10       = Color(0xFF2A1E08)
val Amber20       = Color(0xFF4A3412)
val Amber30       = Color(0xFF6A4D1C)
val Amber40       = Color(0xFF8A6930)   // dim / disabled
val Amber70       = Color(0xFFCFA050)
val Amber80       = Color(0xFFD4A853)   // primary accent
val Amber90       = Color(0xFFDFBB75)
val Amber95       = Color(0xFFEFC97A)   // bright / hover glow
val Amber99       = Color(0xFFF7E5C0)   // near-white tint

// ─── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF0EAD6)   // warm off-white
val TextSecondary = Color(0xFFBFB49A)
val TextMuted     = Color(0xFF9A8E78)
val TextFaint     = Color(0xFF5A5040)

// ─── Semantic ─────────────────────────────────────────────────────────────────
val SemanticError       = Color(0xFFB04A3A)
val SemanticErrorDim    = Color(0xFF2D1410)
val SemanticErrorBright = Color(0xFFE06050)
val SemanticSuccess     = Color(0xFF4A7C59)
val SemanticSuccessDim  = Color(0xFF0F1F14)
val SemanticWarning     = Color(0xFFC08030)

// ─────────────────────────────────────────────────────────────────────────────
//  Color Schemes
//  The app uses DarkColorScheme exclusively (enforced in AppTheme).
//  LightColorScheme is kept for tooling / preview compatibility.
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

// Light scheme retained for IDE previews — not used at runtime.
val LightColorScheme = lightColorScheme(
    primary                = Amber80,
    onPrimary              = Canvas,
    primaryContainer       = Amber99,
    onPrimaryContainer     = Amber20,
    secondary              = Amber70,
    onSecondary            = Canvas,
    secondaryContainer     = Amber99,
    onSecondaryContainer   = Amber30,
    tertiary               = TextMuted,
    onTertiary             = Color.White,
    tertiaryContainer      = Color(0xFFF5F0E8),
    onTertiaryContainer    = Amber30,
    error                  = SemanticError,
    onError                = Color.White,
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = SemanticError,
    background             = Color(0xFFFAF8F4),
    onBackground           = Color(0xFF1A1814),
    surface                = Color(0xFFFFFBF5),
    onSurface              = Color(0xFF1A1814),
    surfaceVariant         = Color(0xFFF0EAD6),
    onSurfaceVariant       = Color(0xFF5A5040),
    outline                = Color(0xFFBFB49A),
    outlineVariant         = Color(0xFFE0D8C8),
)
