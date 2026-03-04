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

/**
 * Editorial network image with animated loading ring and error fallback.
 *
 * Loading state: canvas-drawn primary arc spinner.
 * Error state: primary-tinted broken-image icon on surface background.
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
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface, shape)
            .drawBehind {
                rotate(rotation) {
                    drawArc(
                        brush      = Brush.sweepGradient(listOf(Color.Transparent, primary.copy(0.5f), primary)),
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
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Default.BrokenImage,
            contentDescription = null,
            tint               = primary.copy(alpha = 0.25f),
            modifier           = Modifier.fillMaxSize(0.35f),
        )
    }
}
