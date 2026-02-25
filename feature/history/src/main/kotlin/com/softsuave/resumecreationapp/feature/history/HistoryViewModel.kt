package com.softsuave.resumecreationapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.domain.model.ResumeHistoryItem
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.ResumeHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 *
 * Loads the user's tailored resume history from [ResumeHistoryRepository] and
 * exposes it as [HistoryUiState]. Follows the same UDF pattern used in other
 * feature ViewModels (StateFlow + Channel for one-shot events).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: ResumeHistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HistoryUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        analyticsTracker.track(AnalyticsEvent.ScreenView(screenName = "history"))
        loadHistory()
    }

    fun onEvent(intent: HistoryUserIntent) {
        when (intent) {
            is HistoryUserIntent.Retry -> loadHistory()
            is HistoryUserIntent.BackClicked -> viewModelScope.launch {
                _uiEvent.send(HistoryUiEvent.NavigateBack)
            }
            is HistoryUserIntent.ItemClicked -> { /* future: open detail/download */ }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = historyRepository.getHistory()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
                is Result.Loading -> { /* handled by isLoading flag above */ }
            }
        }
    }
}
