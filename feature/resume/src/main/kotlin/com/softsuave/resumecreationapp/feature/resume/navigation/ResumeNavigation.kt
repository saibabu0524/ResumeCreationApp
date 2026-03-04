package com.softsuave.resumecreationapp.feature.resume.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.softsuave.resumecreationapp.core.ui.component.ThemeMode
import com.softsuave.resumecreationapp.feature.resume.ui.HomeScreen
import com.softsuave.resumecreationapp.feature.resume.ui.ResultScreen
import kotlinx.serialization.Serializable

@Serializable
object ResumeGraph

@Serializable
object HomeRoute

@Serializable
object ResultRoute

fun NavController.navigateToResumeGraph(navOptions: NavOptions? = null) {
    navigate(ResumeGraph, navOptions)
}

fun NavGraphBuilder.resumeGraph(
    navController: NavController,
    onNavigateToAts: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    currentThemeMode: ThemeMode = ThemeMode.System,
    onThemeChanged: (ThemeMode) -> Unit = {},
) {
    navigation<ResumeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToResult = { bytes ->
                    // In-memory holder avoids TransactionTooLargeException when passing
                    // large byte arrays through the navigation back-stack bundle.
                    PdfHolder.bytes = bytes
                    navController.navigate(ResultRoute)
                },
                onNavigateToAts = onNavigateToAts,
                onNavigateToHistory = onNavigateToHistory,
                currentThemeMode = currentThemeMode,
                onThemeChanged = onThemeChanged,
            )
        }

        composable<ResultRoute> {
            val bytes = PdfHolder.bytes ?: ByteArray(0)
            ResultScreen(
                pdfBytes = bytes,
                onStartOver = {
                    PdfHolder.bytes = null
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }
    }
}

/** Simple in-memory holder for the PDF bytes — avoids bundle size limits in navigation. */
object PdfHolder {
    var bytes: ByteArray? = null
}
