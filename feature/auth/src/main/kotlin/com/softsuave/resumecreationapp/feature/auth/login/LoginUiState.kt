package com.softsuave.resumecreationapp.feature.auth.login

/**
 * UI state for the Login screen.
 *
 * @param email Current email text input.
 * @param password Current password text input.
 * @param isLoading Whether an auth request is in-flight.
 * @param emailError Validation error for the email field.
 * @param passwordError Validation error for the password field.
 * @param generalError Non-field-specific error message (network, server, etc.).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
)

/**
 * User intents originating from the Login screen.
 */
sealed interface LoginUserIntent {
    data class EmailChanged(val email: String) : LoginUserIntent
    data class PasswordChanged(val password: String) : LoginUserIntent
    data object LoginClicked : LoginUserIntent
    data object RegisterClicked : LoginUserIntent
}

/**
 * One-time events emitted by the Login ViewModel.
 */
sealed interface LoginUiEvent {
    data object NavigateToHome : LoginUiEvent
    data object NavigateToRegistration : LoginUiEvent
    data class ShowSnackbar(val message: String) : LoginUiEvent
}
