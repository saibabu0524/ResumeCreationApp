package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * Editorial empty state, theme aware.
 *
 * - Icon inside a subtle primary ring
 * - Serif italic "nothing here" heading
 * - Monospace subtitle
 * - Optional primary CTA button
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
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline

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
                            color  = primary.copy(alpha = 0.15f),
                            style  = Stroke(width = 0.5.dp.toPx()),
                        )
                        drawCircle(
                            brush  = Brush.radialGradient(listOf(outlineVariant, bg)),
                            radius = size.minDimension * 0.35f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    modifier           = Modifier.size(36.dp),
                    tint               = primary.copy(alpha = 0.5f),
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
            color         = primary.copy(0.4f),
        )

        Spacer(Modifier.height(8.dp))

        // Serif italic title
        Text(
            text       = title,
            fontFamily = FontFamily.Serif,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontStyle  = FontStyle.Italic,
            color      = onSurface,
            textAlign  = TextAlign.Center,
        )

        if (message != null) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(surface)
                    .border(0.5.dp, outlineVariant, RoundedCornerShape(2.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text          = message,
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 11.sp,
                    lineHeight    = 18.sp,
                    color         = onSurfaceVariant,
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
