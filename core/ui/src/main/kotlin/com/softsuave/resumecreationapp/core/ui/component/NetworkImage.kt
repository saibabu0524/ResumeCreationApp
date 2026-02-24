package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Surface0  = Color(0xFF1A1814)
private val Surface1  = Color(0xFF242019)
private val Amber     = Color(0xFFD4A853)
private val TextMuted = Color(0xFF9A8E78)

/**
 * Dark editorial network image with animated loading ring and error fallback.
 *
 * Loading state: canvas-drawn amber arc spinner (no plain CircularProgressIndicator).
 * Error state: amber-tinted broken-image icon on [Surface0] background.
 */
@Composable
fun NetworkImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = MaterialTheme.shapes.small,
) {
    SubcomposeAsyncImage(
        model              = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier           = modifier.clip(shape),
        contentScale       = contentScale,
        loading            = { EditorialImageLoader(shape) },
        error              = { EditorialImageError(shape) },
    )
}

@Composable
private fun EditorialImageLoader(shape: Shape) {
    val rotation by rememberInfiniteTransition(label = "imgRot").animateFloat(
        initialValue    = 0f,
        targetValue     = 360f,
        animationSpec   = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label           = "ir",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface1, shape)
            .drawBehind {
                rotate(rotation) {
                    drawArc(
                        brush      = Brush.sweepGradient(listOf(Color.Transparent, Amber.copy(0.5f), Amber)),
                        startAngle = 0f,
                        sweepAngle = 220f,
                        useCenter  = false,
                        topLeft    = Offset(size.width * 0.3f, size.height * 0.3f),
                        size       = Size(size.width * 0.4f, size.height * 0.4f),
                        style      = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) { /* spinner drawn on canvas */ }
}

@Composable
private fun EditorialImageError(shape: Shape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface0, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Default.BrokenImage,
            contentDescription = null,
            tint               = Amber.copy(alpha = 0.25f),
            modifier           = Modifier.fillMaxSize(0.35f),
        )
    }
}
