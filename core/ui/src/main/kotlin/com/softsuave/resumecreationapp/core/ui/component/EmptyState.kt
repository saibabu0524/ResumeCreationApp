package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Surface0  = Color(0xFF1A1814)
private val Amber     = Color(0xFFD4A853)
private val AmberDim  = Color(0xFF2A1E08)
private val TextPri   = Color(0xFFF0EAD6)
private val TextMuted = Color(0xFF9A8E78)
private val BorderSub = Color(0xFF2E2A24)
private val BorderMid = Color(0xFF4A4238)

/**
 * Dark editorial empty state.
 *
 * - Icon inside a subtle amber ring
 * - Serif italic "nothing here" heading
 * - Monospace subtitle
 * - Optional amber CTA button
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            // Icon enclosed in a canvas-drawn ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawBehind {
                        drawCircle(
                            color  = Amber.copy(alpha = 0.15f),
                            style  = Stroke(width = 0.5.dp.toPx()),
                        )
                        drawCircle(
                            brush  = Brush.radialGradient(listOf(AmberDim, Canvas)),
                            radius = size.minDimension * 0.35f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    modifier           = Modifier.size(36.dp),
                    tint               = Amber.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Eyebrow label
        Text(
            "NOTHING HERE",
            fontFamily    = FontFamily.Monospace,
            fontSize      = 9.sp,
            letterSpacing = 3.sp,
            color         = Amber.copy(0.4f),
        )

        Spacer(Modifier.height(8.dp))

        // Serif italic title
        Text(
            text       = title,
            fontFamily = FontFamily.Serif,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontStyle  = FontStyle.Italic,
            color      = TextPri,
            textAlign  = TextAlign.Center,
        )

        if (message != null) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Surface0)
                    .border(0.5.dp, BorderSub, RoundedCornerShape(2.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text          = message,
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 11.sp,
                    lineHeight    = 18.sp,
                    color         = TextMuted,
                    textAlign     = TextAlign.Center,
                    letterSpacing = 0.3.sp,
                )
            }
        }

        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(spacing.large))
            AppButton(
                text    = actionText,
                onClick = onAction,
                variant = AppButtonVariant.Primary,
            )
        }
    }
}
