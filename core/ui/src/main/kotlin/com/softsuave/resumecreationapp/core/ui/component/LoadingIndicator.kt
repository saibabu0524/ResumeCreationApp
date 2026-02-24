package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas   = Color(0xFF0E0D0B)
private val Amber    = Color(0xFFD4A853)
private val AmberDim = Color(0xFF8A6930)
private val TextMuted = Color(0xFF9A8E78)

/**
 * Dark editorial loading indicator.
 *
 * Replaces the plain CircularProgressIndicator with a canvas-drawn
 * multi-ring spinner using amber gradient arcs and orbiting dots —
 * matching the overlay used in HomeScreen's loading state.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    loadingDescription: String = "Loading",
) {
    val rot1 by rememberInfiniteTransition(label = "r1").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "rot1",
    )
    val rot2 by rememberInfiniteTransition(label = "r2").animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "rot2",
    )
    val orbitAngle by rememberInfiniteTransition(label = "orb").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "orb",
    )
    val textAlpha by rememberInfiniteTransition(label = "ta").animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "ta",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("loading_indicator")
            .semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Spinner
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .drawBehind {
                        // Outer sweep arc
                        rotate(rot1) {
                            drawArc(
                                brush       = Brush.sweepGradient(listOf(Color.Transparent, Amber.copy(0.4f), Amber)),
                                startAngle  = 0f,
                                sweepAngle  = 260f,
                                useCenter   = false,
                                style       = Stroke(width = 5f, cap = StrokeCap.Round),
                            )
                        }
                        // Inner counter-arc
                        rotate(rot2) {
                            drawArc(
                                brush       = Brush.sweepGradient(listOf(Color.Transparent, AmberDim.copy(0.6f), AmberDim)),
                                startAngle  = 0f,
                                sweepAngle  = 180f,
                                useCenter   = false,
                                topLeft     = Offset(14f, 14f),
                                size        = Size(size.width - 28f, size.height - 28f),
                                style       = Stroke(width = 3f, cap = StrokeCap.Round),
                            )
                        }
                        // 3 orbiting dots
                        val orbitR = size.width / 2 - 6f
                        for (i in 0..2) {
                            val angle = Math.toRadians((orbitAngle + i * 120.0))
                            drawCircle(
                                color  = Amber.copy(alpha = 1f - i * 0.28f),
                                radius = (4f - i * 1f),
                                center = Offset(
                                    size.width / 2 + orbitR * cos(angle).toFloat(),
                                    size.height / 2 + orbitR * sin(angle).toFloat(),
                                ),
                            )
                        }
                        // Centre fill
                        drawCircle(
                            brush  = Brush.radialGradient(listOf(Color(0xFF2A2218), Canvas)),
                            radius = size.minDimension * 0.28f,
                        )
                    },
            )

            Text(
                "LOADING",
                fontFamily    = FontFamily.Monospace,
                fontSize      = 10.sp,
                letterSpacing = 4.sp,
                color         = Amber.copy(textAlpha),
                modifier      = Modifier.alpha(textAlpha),
            )
        }
    }
}
