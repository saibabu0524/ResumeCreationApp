package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Represents one of three possible theme modes.
 */
enum class ThemeMode {
    Dark,
    Light,
    System,
}

/**
 * A compact floating theme-switcher FAB that cycles through  Dark → Light → System → Dark.
 *
 * Place this as an overlay anywhere in your screen hierarchy. Typically positioned
 * as a floating button in the top-right or bottom-right corner.
 *
 * @param currentMode The currently active [ThemeMode].
 * @param onModeChanged Called when the user taps to cycle to the next mode.
 * @param modifier Optional layout modifier.
 * @param showLabel Whether to show a text label alongside the icon.
 */
@Composable
fun ThemeSwitcherButton(
    currentMode: ThemeMode,
    onModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val nextMode = when (currentMode) {
        ThemeMode.Dark   -> ThemeMode.Light
        ThemeMode.Light  -> ThemeMode.System
        ThemeMode.System -> ThemeMode.Dark
    }

    val icon: ImageVector = when (currentMode) {
        ThemeMode.Dark   -> Icons.Default.Brightness4
        ThemeMode.Light  -> Icons.Default.Brightness7
        ThemeMode.System -> Icons.Default.PhoneAndroid
    }

    val label = when (currentMode) {
        ThemeMode.Dark   -> "DARK"
        ThemeMode.Light  -> "LIGHT"
        ThemeMode.System -> "SYSTEM"
    }

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface

    val iconBgColor by animateColorAsState(
        targetValue = when (currentMode) {
            ThemeMode.Dark   -> primary.copy(alpha = 0.15f)
            ThemeMode.Light  -> primary.copy(alpha = 0.10f)
            ThemeMode.System -> primary.copy(alpha = 0.08f)
        },
        label = "iconBg",
    )

    val rotationDeg by animateFloatAsState(
        targetValue = when (currentMode) {
            ThemeMode.Dark   -> 0f
            ThemeMode.Light  -> 180f
            ThemeMode.System -> 90f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconRotation",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(surface)
            .border(0.5.dp, outline.copy(alpha = 0.6f), RoundedCornerShape(50))
            .clickable { onModeChanged(nextMode) }
            .padding(horizontal = if (showLabel) 12.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Theme: $label — tap to switch",
                    tint = primary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotationDeg),
                )
            }
            if (showLabel) {
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = primary,
                    )
                    Text(
                        text = "tap to change",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = onSurface.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

/**
 * A compact icon-only version of [ThemeSwitcherButton] — good for tight spaces.
 */
@Composable
fun ThemeSwitcherIcon(
    currentMode: ThemeMode,
    onModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemeSwitcherButton(
        currentMode = currentMode,
        onModeChanged = onModeChanged,
        modifier = modifier,
        showLabel = false,
    )
}
