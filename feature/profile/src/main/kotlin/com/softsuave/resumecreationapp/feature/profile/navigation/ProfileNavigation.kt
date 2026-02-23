package com.softsuave.resumecreationapp.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softsuave.resumecreationapp.feature.profile.ProfileRoute
import kotlinx.serialization.Serializable

@Serializable
data class ProfileScreenRoute(val userId: String)

fun NavGraphBuilder.profileNavGraph(
    onNavigateBack: () -> Unit,
) {
    composable<ProfileScreenRoute> {
        ProfileRoute(onNavigateBack = onNavigateBack)
    }
}
