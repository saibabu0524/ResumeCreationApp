package com.softsuave.resumecreationapp.feature.history

import com.softsuave.resumecreationapp.core.domain.model.ResumeHistoryItem

data class HistoryUiState(
    val items: List<ResumeHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface HistoryUserIntent {
    data object Retry : HistoryUserIntent
    data object BackClicked : HistoryUserIntent
    data class ItemClicked(val item: ResumeHistoryItem) : HistoryUserIntent
}

sealed interface HistoryUiEvent {
    data object NavigateBack : HistoryUiEvent
    data class ShowSnackbar(val message: String) : HistoryUiEvent
}
