package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
 * Adaptive navigation scaffold — theme-aware.
 *
 * Automatically uses bottom bar / rail / drawer based on window size class.
 * Icon tints and labels animate between primary (selected) and onSurfaceVariant.
 */
@Composable
fun AppNavigationSuiteScaffold(
    destinations: List<AppNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bg           = MaterialTheme.colorScheme.background
    val onBg         = MaterialTheme.colorScheme.onBackground
    val surface      = MaterialTheme.colorScheme.surface
    val primary      = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Pre-compute colors per destination in composable scope
    val itemColorsList = destinations.map { destination ->
        val selected = currentRoute == destination.route

        val iconTint by animateColorAsState(
            targetValue   = if (selected) primary else onSurfaceVariant,
            animationSpec = tween(250),
            label         = "navIcon${destination.route}",
        )
        val labelColor by animateColorAsState(
            targetValue   = if (selected) primary else onSurfaceVariant.copy(alpha = 0.6f),
            animationSpec = tween(250),
            label         = "navLabel${destination.route}",
        )

        Triple(selected, iconTint, labelColor)
    }

    // Build item colors once in composable scope — these functions are @Composable
    val navBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor   = primary,
        selectedTextColor   = primary,
        indicatorColor      = Color.Transparent,
        unselectedIconColor = onSurfaceVariant,
        unselectedTextColor = onSurfaceVariant,
    )
    val navRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor   = primary,
        selectedTextColor   = primary,
        indicatorColor      = surface,
        unselectedIconColor = onSurfaceVariant,
        unselectedTextColor = onSurfaceVariant,
    )
    val suiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors  = navBarItemColors,
        navigationRailItemColors = navRailItemColors,
    )

    NavigationSuiteScaffold(
        modifier = modifier.background(bg),
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
            navigationBarContainerColor    = surface,
            navigationBarContentColor      = onSurfaceVariant,
            navigationRailContainerColor   = surface,
            navigationRailContentColor     = onSurfaceVariant,
            navigationDrawerContainerColor = surface,
            navigationDrawerContentColor   = onSurfaceVariant,
        ),
        containerColor = bg,
        contentColor   = onBg,
    ) {
        content()
    }
}
