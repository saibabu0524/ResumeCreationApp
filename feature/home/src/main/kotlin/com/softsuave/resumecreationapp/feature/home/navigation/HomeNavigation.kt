package com.softsuave.resumecreationapp.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softsuave.resumecreationapp.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenRoute

/**
 * Home feature navigation graph.
 *
 * @param onNavigateToProfile Navigates to the profile screen.
 * @param onNavigateToSettings Navigates to the settings screen.
 */
fun NavGraphBuilder.homeNavGraph(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    composable<HomeScreenRoute> {
        HomeRoute(
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
