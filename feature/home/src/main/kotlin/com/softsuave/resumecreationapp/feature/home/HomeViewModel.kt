package com.softsuave.resumecreationapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home screen ViewModel — reference implementation for:
 * - Flow operators (debounce, distinctUntilChanged for search)
 * - UDF with [HomeUiState], [HomeUserIntent], [HomeUiEvent]
 * - Proper error handling via [Result] wrapper
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    // Search debounce — observe query changes with 300ms debounce
    private val searchQuery = MutableStateFlow("")

    init {
        analyticsTracker.track(AnalyticsEvent.ScreenView(screenName = "home"))
        loadUsers()
        observeSearchQuery()
    }

    fun onEvent(event: HomeUserIntent) {
        when (event) {
            is HomeUserIntent.LoadUsers -> loadUsers()
            is HomeUserIntent.Retry -> loadUsers()
            is HomeUserIntent.SearchQueryChanged -> onSearchChanged(event.query)
            is HomeUserIntent.UserClicked -> onUserClicked(event.userId)
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .collect { result ->
                    when (result) {
                        is Result.Loading -> _uiState.update {
                            it.copy(isLoading = true, errorMessage = null)
                        }
                        is Result.Success -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                users = listOfNotNull(result.data),
                                errorMessage = null,
                            )
                        }
                        is Result.Error -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
        }
    }

    private fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    /**
     * Debounced search observation.
     * Reference implementation for Flow operators: debounce + distinctUntilChanged.
     */
    @Suppress("MagicNumber")
    private fun observeSearchQuery() {
        searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .map { query ->
                analyticsTracker.track(
                    AnalyticsEvent.Search(query = query, screenName = "home"),
                )
            }
            .onEach { /* Apply filter logic when real data is available */ }
            .launchIn(viewModelScope)
    }

    private fun onUserClicked(userId: String) {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent.ButtonClick(buttonId = "user_card", screenName = "home"),
            )
            _uiEvent.send(HomeUiEvent.NavigateToProfile(userId))
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
