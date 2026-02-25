package com.softsuave.resumecreationapp.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.domain.model.ResumeHistoryItem
import com.softsuave.resumecreationapp.core.ui.theme.Amber80
import com.softsuave.resumecreationapp.core.ui.theme.Canvas

@Composable
fun HistoryRoute(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HistoryUiEvent.NavigateBack -> onNavigateBack()
                is HistoryUiEvent.ShowSnackbar -> { /* handled via snackbar */ }
            }
        }
    }

    HistoryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onEvent: (HistoryUserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background == Canvas

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onBg = MaterialTheme.colorScheme.onBackground
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // Ambient glow (dark theme only)
        if (isDark) {
            val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(8000), RepeatMode.Reverse),
                label = "ga",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(300.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Amber80.copy(alpha = 0.03f + glowAnim * 0.02f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top Bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEvent(HistoryUserIntent.BackClicked) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "RESUME HISTORY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 3.sp,
                            color = primary.copy(alpha = 0.7f),
                        )
                        Text(
                            "Your tailored documents",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            color = onBg,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(surfaceVariant)
                        .border(0.5.dp, outline, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = outline.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ── Content ──────────────────────────────────────────────────
            when {
                uiState.isLoading -> HistoryLoadingState()
                uiState.errorMessage != null -> HistoryErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onEvent(HistoryUserIntent.Retry) },
                )
                uiState.items.isEmpty() -> HistoryEmptyState()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        item {
                            // Stats header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                StatChip(
                                    label = "TOTAL",
                                    value = "${uiState.items.size}",
                                    modifier = Modifier.weight(1f),
                                )
                                StatChip(
                                    label = "GEMINI",
                                    value = "${uiState.items.count { it.provider == "gemini" }}",
                                    modifier = Modifier.weight(1f),
                                )
                                StatChip(
                                    label = "OLLAMA",
                                    value = "${uiState.items.count { it.provider == "ollama" }}",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }

                        itemsIndexed(
                            items = uiState.items,
                            key = { _, item -> item.id },
                        ) { index, item ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300, delayMillis = index * 50)) +
                                        expandVertically(tween(300, delayMillis = index * 50)),
                            ) {
                                HistoryItemCard(
                                    item = item,
                                    index = index,
                                    onClick = { onEvent(HistoryUserIntent.ItemClicked(item)) },
                                )
                            }
                            if (index < uiState.items.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        }

                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  History Item Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryItemCard(
    item: ResumeHistoryItem,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Index badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(surfaceVariant)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d", index + 1),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Filename
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = item.originalFilename,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Job description preview
            Text(
                text = item.jobDescription,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 16.sp,
                color = onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            // Meta row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Provider badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(primary.copy(alpha = 0.1f))
                        .border(0.5.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (item.provider == "gemini") Icons.Default.Analytics else Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = item.provider.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        color = primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Date
                Text(
                    text = formatDate(item.createdAt),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stat Chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty / Loading / Error states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryEmptyState() {
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(48.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = primary, modifier = Modifier.size(36.dp))
            }
            Text(
                "NO HISTORY YET",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = primary.copy(alpha = 0.7f),
            )
            Text(
                "Tailor your first resume to see the history of your AI-powered documents here.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                color = onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HistoryLoadingState() {
    val primary = MaterialTheme.colorScheme.primary
    val rot by rememberInfiniteTransition(label = "rot").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r",
    )
    val alpha by rememberInfiniteTransition(label = "alpha").animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "a",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .drawBehind {
                        rotate(rot) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, primary.copy(0.4f), primary)
                                ),
                                startAngle = 0f, sweepAngle = 260f, useCenter = false,
                                style = Stroke(4f, cap = StrokeCap.Round),
                            )
                        }
                        drawCircle(
                            color = primary.copy(0.05f),
                            radius = size.minDimension * 0.32f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = primary.copy(alpha),
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                "LOADING HISTORY",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 4.sp,
                color = primary.copy(alpha),
            )
        }
    }
}

@Composable
private fun HistoryErrorState(message: String, onRetry: () -> Unit) {
    val error = MaterialTheme.colorScheme.error

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(0.5.dp, error.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .padding(16.dp),
            ) {
                Text(
                    "⚠  $message",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    lineHeight = 18.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(0.5f), RoundedCornerShape(2.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    "RETRY →",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Formats an ISO-8601 datetime string like "2026-02-25T09:00:00" into "25 Feb 2026".
 * Falls back to the raw string on any parse error.
 */
private fun formatDate(isoDate: String): String {
    return try {
        val parts = isoDate.substringBefore("T").split("-")
        val year = parts[0]
        val month = when (parts[1]) {
            "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
            "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
            "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; else -> "Dec"
        }
        val day = parts[2]
        "$day $month $year"
    } catch (e: Exception) {
        isoDate
    }
}
