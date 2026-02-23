package com.softsuave.resumecreationapp.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softsuave.resumecreationapp.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsScreenRoute

fun NavGraphBuilder.settingsNavGraph(
    onNavigateBack: () -> Unit,
) {
    composable<SettingsScreenRoute> {
        SettingsRoute(onNavigateBack = onNavigateBack)
    }
}
