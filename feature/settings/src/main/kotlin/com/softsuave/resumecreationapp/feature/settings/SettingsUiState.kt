package com.softsuave.resumecreationapp.feature.settings

import com.softsuave.resumecreationapp.core.ui.component.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

sealed interface SettingsUserIntent {
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsUserIntent
    data class NotificationsToggled(val enabled: Boolean) : SettingsUserIntent
    data object LogoutClicked : SettingsUserIntent
    data object BackClicked : SettingsUserIntent
}

sealed interface SettingsUiEvent {
    data object NavigateBack : SettingsUiEvent
    data object NavigateToLogin : SettingsUiEvent
    data class ShowSnackbar(val message: String) : SettingsUiEvent
}
