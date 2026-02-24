package com.softsuave.resumecreationapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape token definitions for Resume Tailor.
 *
 * Design direction: predominantly sharp/flat corners (0–2 dp) for the
 * editorial brutalist feel, with very slight rounding only at larger scales
 * (dialogs, bottom sheets) to avoid complete harshness on small screens.
 *
 * Mapping intent:
 *   extraSmall  → 0 dp  — text fields, buttons, inline chips
 *   small       → 2 dp  — cards, upload panels, provider chips
 *   medium      → 4 dp  — section containers, snack bars
 *   large       → 8 dp  — bottom sheets, modal surfaces
 *   extraLarge  → 12 dp — full-screen dialogs, onboarding panels
 *
 * Individual components never hardcode RoundedCornerShape values directly;
 * they consume MaterialTheme.shapes.* tokens instead.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(2.dp),
    medium     = RoundedCornerShape(4.dp),
    large      = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
