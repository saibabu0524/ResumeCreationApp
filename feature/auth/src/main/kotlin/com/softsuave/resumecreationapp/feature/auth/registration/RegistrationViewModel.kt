package com.softsuave.resumecreationapp.feature.auth.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.common.util.ValidationUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<RegistrationUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: RegistrationUserIntent) {
        when (event) {
            is RegistrationUserIntent.DisplayNameChanged -> _uiState.update {
                it.copy(displayName = event.name, displayNameError = null)
            }
            is RegistrationUserIntent.EmailChanged -> _uiState.update {
                it.copy(email = event.email, emailError = null)
            }
            is RegistrationUserIntent.PasswordChanged -> _uiState.update {
                it.copy(password = event.password, passwordError = null)
            }
            is RegistrationUserIntent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.password, confirmPasswordError = null)
            }
            is RegistrationUserIntent.RegisterClicked -> onRegisterClicked()
            is RegistrationUserIntent.LoginClicked -> viewModelScope.launch {
                _uiEvent.send(RegistrationUiEvent.NavigateToLogin)
            }
        }
    }

    private fun onRegisterClicked() {
        val state = _uiState.value

        val displayNameError = if (state.displayName.isBlank()) "Display name is required" else null
        val emailError = if (!ValidationUtil.isValidEmail(state.email)) "Please enter a valid email" else null
        val passwordError = if (!ValidationUtil.isValidPassword(state.password)) {
            "Password must be 8+ characters with uppercase, lowercase, and digit"
        } else {
            null
        }
        val confirmPasswordError = if (!ValidationUtil.doStringsMatch(state.password, state.confirmPassword)) {
            "Passwords do not match"
        } else {
            null
        }

        if (listOfNotNull(displayNameError, emailError, passwordError, confirmPasswordError).isNotEmpty()) {
            _uiState.update {
                it.copy(
                    displayNameError = displayNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            // Note: Replace with actual RegisterUseCase call when backend integration is ready
            kotlinx.coroutines.delay(SIMULATED_NETWORK_DELAY_MS)

            analyticsTracker.track(AnalyticsEvent.SignUp(method = "email_password"))
            _uiState.update { it.copy(isLoading = false) }
            _uiEvent.send(RegistrationUiEvent.NavigateToOnboarding)
        }
    }

    companion object {
        private const val SIMULATED_NETWORK_DELAY_MS = 1_500L
    }
}
