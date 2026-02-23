package com.softsuave.resumecreationapp.feature.auth.login

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

/**
 * ViewModel for the Login screen.
 *
 * Follows UDF — exposing [uiState] as a read-only [StateFlow] and
 * processing user actions through [onEvent]. One-time events are
 * delivered via [uiEvent] using a [Channel].
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: LoginUserIntent) {
        when (event) {
            is LoginUserIntent.EmailChanged -> onEmailChanged(event.email)
            is LoginUserIntent.PasswordChanged -> onPasswordChanged(event.password)
            is LoginUserIntent.LoginClicked -> onLoginClicked()
            is LoginUserIntent.RegisterClicked -> onRegisterClicked()
        }
    }

    private fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(email = email, emailError = null, generalError = null)
        }
    }

    private fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, generalError = null)
        }
    }

    private fun onLoginClicked() {
        val state = _uiState.value

        // ─── Validate ────────────────────────────────────────────────
        val emailError = if (!ValidationUtil.isValidEmail(state.email)) {
            "Please enter a valid email address"
        } else {
            null
        }

        val passwordError = if (state.password.isBlank()) {
            "Password is required"
        } else {
            null
        }

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }

        // ─── Perform login ───────────────────────────────────────────
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            // Note: Replace with actual LoginUseCase call when backend integration is ready
            // For now, simulate successful login for template demonstration
            kotlinx.coroutines.delay(SIMULATED_NETWORK_DELAY_MS)

            analyticsTracker.track(AnalyticsEvent.Login(method = "email_password"))

            _uiState.update { it.copy(isLoading = false) }
            _uiEvent.send(LoginUiEvent.NavigateToHome)
        }
    }

    private fun onRegisterClicked() {
        viewModelScope.launch {
            _uiEvent.send(LoginUiEvent.NavigateToRegistration)
        }
    }

    companion object {
        private const val SIMULATED_NETWORK_DELAY_MS = 1_500L
    }
}
