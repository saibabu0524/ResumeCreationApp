package com.softsuave.resumecreationapp.feature.resume.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
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

fun NavGraphBuilder.resumeGraph(navController: NavController) {
    navigation<ResumeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToResult = { bytes ->
                    // Since passing large byte arrays in Navigation arguments might exceed the Intent size limit,
                    // we could save it temporarily and pass a URI, or Use a shared ViewModel.
                    // For simplicity, we assume ByteArray is manageable if it's a small PDF, but it's risky (TransactionTooLargeException).
                    // Navigation Compose route args are bundled. If it's too large, or if the type isn't natively supported, it crashes.
                    // We created a hack for now to store it in a companion object Holder.
                    PdfHolder.bytes = bytes
                    navController.navigate(ResultRoute)
                }
            )
        }
        
        composable<ResultRoute> {
            // Use the PDF bytes from the holder instead of the bundle argument to avoid crashes
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

// Simple in-memory holder for the PDF bytes strictly for navigation purposes
object PdfHolder {
    var bytes: ByteArray? = null
}
