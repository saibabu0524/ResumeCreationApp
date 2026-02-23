package com.softsuave.resumecreationapp.feature.profile

import com.softsuave.resumecreationapp.core.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editDisplayName: String = "",
)

sealed interface ProfileUserIntent {
    data object Retry : ProfileUserIntent
    data object EditClicked : ProfileUserIntent
    data object SaveClicked : ProfileUserIntent
    data object CancelEdit : ProfileUserIntent
    data class DisplayNameChanged(val name: String) : ProfileUserIntent
    data object BackClicked : ProfileUserIntent
}

sealed interface ProfileUiEvent {
    data object NavigateBack : ProfileUiEvent
    data class ShowSnackbar(val message: String) : ProfileUiEvent
}
