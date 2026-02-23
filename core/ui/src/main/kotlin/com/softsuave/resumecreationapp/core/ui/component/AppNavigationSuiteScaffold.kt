package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Configuration for a single navigation destination in [AppNavigationSuiteScaffold].
 *
 * @param label Display label.
 * @param icon Default icon.
 * @param selectedIcon Icon shown when selected.
 * @param contentDescription Accessibility content description.
 * @param route Route key for navigation.
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
 * Adaptive navigation scaffold that automatically switches between:
 * - **Bottom navigation bar** on compact (phone) screens
 * - **Navigation rail** on medium (tablet portrait) screens
 * - **Navigation drawer** on expanded (tablet landscape) screens
 *
 * Feature screens never contain conditional navigation logic —
 * this component handles it automatically via [NavigationSuiteScaffold].
 *
 * @param destinations List of navigation destinations.
 * @param currentRoute Currently active route.
 * @param onNavigate Callback when a destination is selected.
 * @param modifier Modifier for the scaffold.
 * @param content Screen content for the selected destination.
 */
@Composable
fun AppNavigationSuiteScaffold(
    destinations: List<AppNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                item(
                    selected = selected,
                    onClick = { onNavigate(destination.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.contentDescription,
                        )
                    },
                    label = { Text(text = destination.label) },
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(),
    ) {
        content()
    }
}
