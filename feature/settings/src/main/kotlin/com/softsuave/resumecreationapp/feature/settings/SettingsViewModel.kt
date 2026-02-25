package com.softsuave.resumecreationapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import com.softsuave.resumecreationapp.core.ui.component.ThemeMode
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
     * Merges theme-mode string and notifications preference into a single [SettingsUiState].
     */
    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferencesRepository.themeModeString,
                preferencesRepository.isPushNotificationsEnabled,
            ) { themeModeStr, isNotificationsEnabled ->
                val themeMode = when (themeModeStr) {
                    "dark"   -> ThemeMode.Dark
                    "light"  -> ThemeMode.Light
                    else     -> ThemeMode.System
                }
                SettingsUiState(
                    themeMode = themeMode,
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
            val modeStr = when (mode) {
                ThemeMode.Dark   -> "dark"
                ThemeMode.Light  -> "light"
                ThemeMode.System -> "system"
            }
            preferencesRepository.setThemeMode(modeStr)
        }
    }

    private fun onNotificationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setPushNotificationsEnabled(enabled)
        }
    }
}
