package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Surface0  = Color(0xFF1A1814)
private val Amber     = Color(0xFFD4A853)
private val TextMuted = Color(0xFF9A8E78)

/**
 * Configuration for a single navigation destination in [AppNavigationSuiteScaffold].
 */
@Immutable
data class AppNavDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label,
    val route: String,
)

/**
 * Adaptive navigation scaffold — dark editorial.
 *
 * Automatically uses bottom bar / rail / drawer based on window size class.
 * Icon tints and labels animate between [Amber] (selected) and [TextMuted].
 */
@Composable
fun AppNavigationSuiteScaffold(
    destinations: List<AppNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Pre-compute colors per destination in composable scope
    val itemColorsList = destinations.map { destination ->
        val selected = currentRoute == destination.route

        val iconTint by animateColorAsState(
            targetValue   = if (selected) Amber else TextMuted,
            animationSpec = tween(250),
            label         = "navIcon${destination.route}",
        )
        val labelColor by animateColorAsState(
            targetValue   = if (selected) Amber else TextMuted.copy(alpha = 0.6f),
            animationSpec = tween(250),
            label         = "navLabel${destination.route}",
        )

        Triple(selected, iconTint, labelColor)
    }

    // Build item colors once in composable scope — these functions are @Composable
    val navBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor   = Amber,
        selectedTextColor   = Amber,
        indicatorColor      = Color.Transparent,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted,
    )
    val navRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor   = Amber,
        selectedTextColor   = Amber,
        indicatorColor      = Surface0,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted,
    )
    val suiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors  = navBarItemColors,
        navigationRailItemColors = navRailItemColors,
    )

    NavigationSuiteScaffold(
        modifier = modifier.background(Canvas),
        navigationSuiteItems = {
            destinations.forEachIndexed { index, destination ->
                val (selected, iconTint, labelColor) = itemColorsList[index]

                item(
                    selected = selected,
                    onClick  = { onNavigate(destination.route) },
                    icon     = {
                        Icon(
                            imageVector        = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.contentDescription,
                            tint               = iconTint,
                        )
                    },
                    label = {
                        Text(
                            text          = destination.label.uppercase(),
                            fontFamily    = FontFamily.Monospace,
                            fontSize      = 8.sp,
                            letterSpacing = 1.sp,
                            fontWeight    = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color         = labelColor,
                        )
                    },
                    colors = suiteItemColors,
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor    = Surface0,
            navigationBarContentColor      = TextMuted,
            navigationRailContainerColor   = Surface0,
            navigationRailContentColor     = TextMuted,
            navigationDrawerContainerColor = Surface0,
            navigationDrawerContentColor   = TextMuted,
        ),
        containerColor = Canvas,
        contentColor   = Color(0xFFF0EAD6),
    ) {
        content()
    }
}
