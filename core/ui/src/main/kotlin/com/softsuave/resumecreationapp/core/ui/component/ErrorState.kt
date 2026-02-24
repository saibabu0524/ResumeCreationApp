package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softsuave.resumecreationapp.core.ui.R
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Surface0  = Color(0xFF1A1814)
private val Amber     = Color(0xFFD4A853)
private val AmberDim  = Color(0xFF8A6930)
private val TextPri   = Color(0xFFF0EAD6)
private val TextMuted = Color(0xFF9A8E78)
private val ErrorRed  = Color(0xFFB04A3A)
private val ErrorDim  = Color(0xFF2D1410)
private val ErrorBright = Color(0xFFE06050)

/**
 * Dark editorial error state with animated icon ring and retry button.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    val rotation by rememberInfiniteTransition(label = "errRot").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "er",
    )
    val pulse by rememberInfiniteTransition(label = "errPulse").animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ep",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge)
            .testTag("error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated error icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .drawBehind {
                    rotate(rotation) {
                        drawArc(
                            color      = ErrorRed.copy(alpha = 0.35f),
                            startAngle = 0f, sweepAngle = 200f,
                            useCenter  = false,
                            style      = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    drawCircle(
                        color  = ErrorDim,
                        radius = size.minDimension * 0.35f * pulse,
                    )
                    drawCircle(
                        color  = ErrorRed.copy(alpha = 0.2f),
                        radius = size.minDimension * 0.35f * pulse,
                        style  = Stroke(0.5.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier           = Modifier.size(36.dp),
                tint               = ErrorRed.copy(alpha = 0.8f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Eyebrow
        Text(
            "ERROR",
            fontFamily    = FontFamily.Monospace,
            fontSize      = 9.sp,
            letterSpacing = 3.sp,
            color         = ErrorRed.copy(0.5f),
        )

        Spacer(Modifier.height(8.dp))

        // Serif italic error label
        Text(
            "Something went wrong",
            fontFamily = FontFamily.Serif,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontStyle  = FontStyle.Italic,
            color      = TextPri,
        )

        Spacer(Modifier.height(12.dp))

        // Message in bordered monospace box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(2.dp))
                .background(ErrorDim)
                .border(0.5.dp, ErrorRed.copy(0.3f), RoundedCornerShape(2.dp))
                .padding(14.dp),
        ) {
            Text(
                text          = message,
                fontFamily    = FontFamily.Monospace,
                fontSize      = 11.sp,
                lineHeight    = 18.sp,
                color         = ErrorBright.copy(0.8f),
                textAlign     = TextAlign.Center,
                letterSpacing = 0.3.sp,
            )
        }

        Spacer(Modifier.height(spacing.large))

        AppButton(
            text    = stringResource(R.string.core_ui_retry),
            onClick = onRetry,
            variant = AppButtonVariant.Primary,
        )
    }
}
