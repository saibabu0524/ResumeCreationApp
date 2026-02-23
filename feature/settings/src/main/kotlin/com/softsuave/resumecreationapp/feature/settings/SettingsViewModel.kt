package com.softsuave.resumecreationapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        analyticsTracker.track(AnalyticsEvent.ScreenView(screenName = "settings"))
        observePreferences()
    }

    fun onEvent(event: SettingsUserIntent) {
        when (event) {
            is SettingsUserIntent.ThemeModeChanged -> onThemeModeChanged(event.mode)
            is SettingsUserIntent.NotificationsToggled -> onNotificationsToggled(event.enabled)
            is SettingsUserIntent.BackClicked -> viewModelScope.launch {
                _uiEvent.send(SettingsUiEvent.NavigateBack)
            }
        }
    }

    /**
     * Reference implementation for [combine] operator —
     * merges multiple preference flows into a single state.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferencesRepository.isDarkMode,
                preferencesRepository.isPushNotificationsEnabled,
            ) { isDarkMode, isNotificationsEnabled ->
                SettingsUiState(
                    themeMode = if (isDarkMode) ThemeMode.Dark else ThemeMode.Light,
                    notificationsEnabled = isNotificationsEnabled,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun onThemeModeChanged(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            preferencesRepository.setDarkMode(mode == ThemeMode.Dark)
        }
    }

    private fun onNotificationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setPushNotificationsEnabled(enabled)
        }
    }
}
