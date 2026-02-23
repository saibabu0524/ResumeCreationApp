package com.softsuave.resumecreationapp.feature.settings

/**
 * Theme mode selection.
 */
enum class ThemeMode {
    Light,
    Dark,
    System,
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

sealed interface SettingsUserIntent {
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsUserIntent
    data class NotificationsToggled(val enabled: Boolean) : SettingsUserIntent
    data object BackClicked : SettingsUserIntent
}

sealed interface SettingsUiEvent {
    data object NavigateBack : SettingsUiEvent
    data class ShowSnackbar(val message: String) : SettingsUiEvent
}
