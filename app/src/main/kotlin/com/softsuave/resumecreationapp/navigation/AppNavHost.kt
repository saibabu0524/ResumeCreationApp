package com.softsuave.resumecreationapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.softsuave.resumecreationapp.core.ui.component.ThemeMode
import com.softsuave.resumecreationapp.feature.auth.navigation.AuthGraphRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.RegistrationScreenRoute
import com.softsuave.resumecreationapp.feature.auth.navigation.authNavGraph
import com.softsuave.resumecreationapp.feature.ats.navigation.AtsGraph
import com.softsuave.resumecreationapp.feature.ats.navigation.atsNavGraph
import com.softsuave.resumecreationapp.feature.history.navigation.HistoryScreenRoute
import com.softsuave.resumecreationapp.feature.history.navigation.historyNavGraph
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
 * Cross-feature navigation is resolved here through lambdas —
 * individual feature modules are decoupled from each other.
 *
 * @param currentThemeMode The active theme mode passed down to screens that show the switcher.
 * @param onThemeChanged   Callback to change the theme from within a screen.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = AuthGraphRoute,
    currentThemeMode: ThemeMode = ThemeMode.System,
    onThemeChanged: (ThemeMode) -> Unit = {},
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
            onNavigateToHistory = {
                navController.navigate(HistoryScreenRoute)
            },
            currentThemeMode = currentThemeMode,
            onThemeChanged = onThemeChanged,
        )

        // ─── ATS Scanner ─────────────────────────────────────────────
        atsNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // ─── Auth ────────────────────────────────────────────────────
        authNavGraph(
            onNavigateToHome = {
                navController.navigate(ResumeGraph) {
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
            onNavigateToLogin = {
                navController.navigate(AuthGraphRoute) {
                    popUpTo(0)
                }
            }
        )

        // ─── Profile ─────────────────────────────────────────────────
        profileNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // ─── History ─────────────────────────────────────────────────
        historyNavGraph(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}
