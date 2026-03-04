package com.softsuave.resumecreationapp.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.softsuave.resumecreationapp.feature.auth.login.LoginRoute
import com.softsuave.resumecreationapp.feature.auth.onboarding.OnboardingRoute
import com.softsuave.resumecreationapp.feature.auth.registration.RegistrationRoute
import kotlinx.serialization.Serializable

// ─── Route Definitions ───────────────────────────────────────────────────────

/** Root route for the auth navigation graph. */
@Serializable
data object AuthGraphRoute

@Serializable
data object LoginScreenRoute

@Serializable
data object RegistrationScreenRoute

@Serializable
data object OnboardingScreenRoute

// ─── Navigation Graph ────────────────────────────────────────────────────────

/**
 * Auth feature navigation graph.
 *
 * Called from [AppNavHost] in `:app`. All exit-navigation happens
 * via lambda callbacks — this feature module never knows about
 * other features or their routes.
 *
 * @param onNavigateToHome         Navigates to the home screen after onboarding completes.
 * @param onNavigateToRegistration Navigates to the registration screen.
 * @param onNavigateToLogin        Navigates back to login from registration.
 */
fun NavGraphBuilder.authNavGraph(
    onNavigateToHome: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    navigation<AuthGraphRoute>(startDestination = LoginScreenRoute) {
        composable<LoginScreenRoute> {
            LoginRoute(
                onNavigateToHome = onNavigateToHome,
                onNavigateToRegistration = onNavigateToRegistration,
            )
        }

        composable<RegistrationScreenRoute> {
            RegistrationRoute(
                // After registration success → go to Onboarding (not directly to Home)
                onNavigateToOnboarding = onNavigateToHome, // resolved in AppNavHost as OnboardingScreenRoute
                onNavigateToLogin = onNavigateToLogin,
            )
        }

        composable<OnboardingScreenRoute> {
            OnboardingRoute(
                onOnboardingComplete = onNavigateToHome,
            )
        }
    }
}
