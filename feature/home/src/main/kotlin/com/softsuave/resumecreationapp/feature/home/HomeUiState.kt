package com.softsuave.resumecreationapp.feature.home

import com.softsuave.resumecreationapp.core.domain.model.User
import androidx.compose.runtime.Immutable

/**
 * UI state for the Home screen.
 */
@Immutable
data class HomeUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
)

/**
 * User intents from the Home screen.
 */
sealed interface HomeUserIntent {
    data object LoadUsers : HomeUserIntent
    data object Retry : HomeUserIntent
    data class SearchQueryChanged(val query: String) : HomeUserIntent
    data class UserClicked(val userId: String) : HomeUserIntent
}

/**
 * One-time events from the Home ViewModel.
 */
sealed interface HomeUiEvent {
    data class NavigateToProfile(val userId: String) : HomeUiEvent
    data class ShowSnackbar(val message: String) : HomeUiEvent
}
