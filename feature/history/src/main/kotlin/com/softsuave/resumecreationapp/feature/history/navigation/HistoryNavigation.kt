package com.softsuave.resumecreationapp.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.softsuave.resumecreationapp.feature.history.HistoryRoute
import kotlinx.serialization.Serializable

@Serializable
data object HistoryScreenRoute

fun NavController.navigateToHistory(navOptions: NavOptions? = null) {
    navigate(HistoryScreenRoute, navOptions)
}

fun NavGraphBuilder.historyNavGraph(
    onNavigateBack: () -> Unit,
) {
    composable<HistoryScreenRoute> {
        HistoryRoute(onNavigateBack = onNavigateBack)
    }
}
