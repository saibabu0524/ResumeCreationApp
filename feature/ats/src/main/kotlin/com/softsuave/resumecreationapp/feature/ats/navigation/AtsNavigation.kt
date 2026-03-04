package com.softsuave.resumecreationapp.feature.ats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.softsuave.resumecreationapp.feature.ats.ui.AtsRoute
import kotlinx.serialization.Serializable

// ── Route Definitions ─────────────────────────────────────────────────────────

@Serializable
object AtsGraph

@Serializable
object AtsScreenRoute

// ── Navigation helpers ────────────────────────────────────────────────────────

fun NavController.navigateToAts(navOptions: NavOptions? = null) {
    navigate(AtsGraph, navOptions)
}

fun NavGraphBuilder.atsNavGraph(
    onNavigateBack: () -> Unit,
) {
    navigation<AtsGraph>(startDestination = AtsScreenRoute) {
        composable<AtsScreenRoute> {
            AtsRoute(onNavigateBack = onNavigateBack)
        }
    }
}
