package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

/**
 * Theme-aware editorial loading indicator.
 *
 * Multi-ring spinner using primary colour gradient arcs and orbiting dots.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    loadingDescription: String = "Loading",
) {
    val primary    = MaterialTheme.colorScheme.primary
    val primaryDim = MaterialTheme.colorScheme.primaryContainer
    val bg         = MaterialTheme.colorScheme.background

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
                                brush       = Brush.sweepGradient(listOf(primary.copy(0f), primary.copy(0.4f), primary)),
                                startAngle  = 0f,
                                sweepAngle  = 260f,
                                useCenter   = false,
                                style       = Stroke(width = 5f, cap = StrokeCap.Round),
                            )
                        }
                        // Inner counter-arc
                        rotate(rot2) {
                            drawArc(
                                brush       = Brush.sweepGradient(listOf(primaryDim.copy(0f), primaryDim.copy(0.6f), primaryDim)),
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
                                color  = primary.copy(alpha = 1f - i * 0.28f),
                                radius = (4f - i * 1f),
                                center = Offset(
                                    size.width / 2 + orbitR * cos(angle).toFloat(),
                                    size.height / 2 + orbitR * sin(angle).toFloat(),
                                ),
                            )
                        }
                        // Centre fill (blends with background)
                        drawCircle(
                            color  = bg,
                            radius = size.minDimension * 0.28f,
                        )
                    },
            )

            Text(
                "LOADING",
                fontFamily    = FontFamily.Monospace,
                fontSize      = 10.sp,
                letterSpacing = 4.sp,
                color         = primary.copy(textAlpha),
                modifier      = Modifier.alpha(textAlpha),
            )
        }
    }
}
