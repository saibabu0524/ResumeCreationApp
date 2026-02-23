package com.softsuave.resumecreationapp.feature.auth.registration

/**
 * UI state for the Registration screen.
 */
data class RegistrationUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
)

/**
 * User intents originating from the Registration screen.
 */
sealed interface RegistrationUserIntent {
    data class DisplayNameChanged(val name: String) : RegistrationUserIntent
    data class EmailChanged(val email: String) : RegistrationUserIntent
    data class PasswordChanged(val password: String) : RegistrationUserIntent
    data class ConfirmPasswordChanged(val password: String) : RegistrationUserIntent
    data object RegisterClicked : RegistrationUserIntent
    data object LoginClicked : RegistrationUserIntent
}

/**
 * One-time events emitted by the Registration ViewModel.
 */
sealed interface RegistrationUiEvent {
    data object NavigateToOnboarding : RegistrationUiEvent
    data object NavigateToLogin : RegistrationUiEvent
    data class ShowSnackbar(val message: String) : RegistrationUiEvent
}
