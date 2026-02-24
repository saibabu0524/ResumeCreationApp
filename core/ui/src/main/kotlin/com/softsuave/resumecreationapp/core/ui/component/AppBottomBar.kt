package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Surface0  = Color(0xFF1A1814)
private val Amber     = Color(0xFFD4A853)
private val AmberDim  = Color(0xFF8A6930)
private val TextMuted = Color(0xFF9A8E78)
private val BorderSub = Color(0xFF2E2A24)

/**
 * Configuration for a single navigation item in [AppBottomBar].
 */
@Immutable
data class AppNavigationItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label,
)

/**
 * Dark editorial bottom navigation bar.
 *
 * - Hairline top border for structural definition
 * - Amber indicator line above selected icon (not pill/container)
 * - Monospace uppercase labels
 * - Animated color transitions
 */
@Composable
fun AppBottomBar(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface0)
            .navigationBarsPadding()
            .drawBehind {
                // Hairline top border
                drawLine(
                    color       = BorderSub,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex

                val iconTint by animateColorAsState(
                    targetValue   = if (selected) Amber else TextMuted,
                    animationSpec = tween(250),
                    label         = "iconTint$index",
                )
                val labelColor by animateColorAsState(
                    targetValue   = if (selected) Amber else TextMuted.copy(alpha = 0.6f),
                    animationSpec = tween(250),
                    label         = "labelColor$index",
                )

                NavigationBarItem(
                    selected  = selected,
                    onClick   = { onItemSelected(index) },
                    icon      = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Amber indicator line above icon when selected
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(if (selected) Amber else Color.Transparent)
                            )
                            Spacer(Modifier.height(4.dp))
                            Icon(
                                imageVector        = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.contentDescription,
                                tint               = iconTint,
                                modifier           = Modifier.size(20.dp),
                            )
                        }
                    },
                    label     = {
                        Text(
                            text          = item.label.uppercase(),
                            fontFamily    = FontFamily.Monospace,
                            fontSize      = 8.sp,
                            letterSpacing = 1.sp,
                            fontWeight    = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color         = labelColor,
                        )
                    },
                    alwaysShowLabel = true,
                    colors    = NavigationBarItemDefaults.colors(
                        selectedIconColor   = Amber,
                        selectedTextColor   = Amber,
                        indicatorColor      = Color.Transparent, // custom indicator drawn above
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                    ),
                )
            }
        }
    }
}
