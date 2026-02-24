package com.softsuave.resumecreationapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.softsuave.resumecreationapp.feature.auth.navigation.AuthGraphRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.OnboardingScreenRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.RegistrationScreenRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.authNavGraph
import com.softsuave.resumecreationapp.feature.ats.navigation.AtsGraph
import com.softsuave.resumecreationapp.feature.ats.navigation.atsNavGraph
import com.softsuave.resumecreationapp.feature.home.navigation.homeNavGraph
import com.softsuave.resumecreationapp.feature.profile.navigation.ProfileScreenRoute
import com.softsuave.resumecreationapp.feature.profile.navigation.profileNavGraph
import com.softsuave.resumecreationapp.feature.resume.navigation.ResumeGraph
import com.softsuave.resumecreationapp.feature.resume.navigation.resumeGraph
import com.softsuave.resumecreationapp.feature.settings.navigation.SettingsScreenRoute
import com.softsuave.resumecreationapp.feature.settings.navigation.settingsNavGraph

/**
 * Root navigation host assembling all feature navigation graphs.
 *
 * This is the ONLY place where feature modules' navigation graphs are wired
 * together. Individual features never know about each other's routes or
 * navigation graphs — all cross-feature navigation goes through lambdas
 * resolved here.
 *
 * Auth flow:  Login → (Registration →) Onboarding → ResumeGraph
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
        resumeGraph(
            navController = navController,
            onNavigateToAts = {
                navController.navigate(AtsGraph)
            },
        )

        // ─── ATS Scanner ─────────────────────────────────────────────
        atsNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // ─── Auth ────────────────────────────────────────────────────
        authNavGraph(
            // Login success → go directly to ResumeGraph (skip onboarding for returning users)
            onNavigateToHome = {
                navController.navigate(ResumeGraph) {
                    popUpTo(AuthGraphRoute) { inclusive = true }
                }
            },
            // Registration → show onboarding first
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
