package com.softsuave.resumecreationapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.softsuave.resumecreationapp.feature.auth.navigation.AuthGraphRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.RegistrationScreenRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.authNavGraph
import com.softsuave.resumecreationapp.feature.home.navigation.HomeScreenRoute
import com.softsuave.resumecreationapp.feature.home.navigation.homeNavGraph
import com.softsuave.resumecreationapp.feature.profile.navigation.ProfileScreenRoute
import com.softsuave.resumecreationapp.feature.profile.navigation.profileNavGraph
import com.softsuave.resumecreationapp.feature.settings.navigation.SettingsScreenRoute
import com.softsuave.resumecreationapp.feature.settings.navigation.settingsNavGraph

import com.softsuave.resumecreationapp.feature.resume.navigation.ResumeGraph
import com.softsuave.resumecreationapp.feature.resume.navigation.resumeGraph

/**
 * Root navigation host assembling all feature navigation graphs.
 *
 * This is the ONLY place where feature modules' navigation graphs are wired
 * together. Individual features never know about each other's routes or
 * navigation graphs — all cross-feature navigation goes through lambdas
 * resolved here.
 *
 * @param modifier Modifier applied to the [NavHost].
 * @param navController The [NavHostController] managing navigation state.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = AuthGraphRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ─── Resume ──────────────────────────────────────────────────
        resumeGraph(navController = navController)

        // ─── Auth ────────────────────────────────────────────────────
        authNavGraph(
            onNavigateToHome = {
                navController.navigate(ResumeGraph) {
                    // Clear auth back stack — user shouldn't go back to login
                    popUpTo(AuthGraphRoute) { inclusive = true }
                }
            },
            onNavigateToRegistration = {
                navController.navigate(RegistrationScreenRoute)
            },
            onNavigateToLogin = {
                navController.popBackStack()
            },
        )

        // ─── Home ────────────────────────────────────────────────────
        homeNavGraph(
            onNavigateToProfile = { userId ->
                navController.navigate(ProfileScreenRoute(userId = userId))
            },
            onNavigateToSettings = {
                navController.navigate(SettingsScreenRoute)
            },
        )

        // ─── Settings ────────────────────────────────────────────────
        settingsNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // ─── Profile ─────────────────────────────────────────────────
        profileNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}
