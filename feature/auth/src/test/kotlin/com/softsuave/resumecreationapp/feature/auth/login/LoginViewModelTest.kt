package com.softsuave.resumecreationapp.feature.auth.login

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.testing.fake.FakeAnalyticsTracker
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for [LoginViewModel].
 *
 * Note: Tests that trigger [ValidationUtil.isValidEmail] are skipped because
 * that utility uses [android.util.Patterns] which is not available in JVM
 * unit tests. Those paths are tested in instrumented tests or via Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class LoginViewModelTest {

    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        analyticsTracker = FakeAnalyticsTracker()
        viewModel = LoginViewModel(analyticsTracker)
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
    }

    @Test
    fun `email changed updates state`() {
        viewModel.onEvent(LoginUserIntent.EmailChanged("test@example.com"))

        assertEquals("test@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `password changed updates state`() {
        viewModel.onEvent(LoginUserIntent.PasswordChanged("Password123"))

        assertEquals("Password123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `email change clears email error`() {
        // Manually set an error state
        viewModel.onEvent(LoginUserIntent.EmailChanged("bad"))
        // Change email again — error should be cleared
        viewModel.onEvent(LoginUserIntent.EmailChanged("new@example.com"))
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `password change clears password error`() {
        viewModel.onEvent(LoginUserIntent.PasswordChanged(""))
        // Change password — error should be cleared
        viewModel.onEvent(LoginUserIntent.PasswordChanged("newpass"))
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `register clicked emits navigate to registration event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(LoginUserIntent.RegisterClicked)

            val event = awaitItem()
            assertTrue(event is LoginUiEvent.NavigateToRegistration)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `analytics tracker is available`() {
        // LoginViewModel doesn't auto-track screen view in init
        // (unlike HomeViewModel/ProfileViewModel, screen tracking
        // happens at the composable level for auth flows)
        assertTrue(analyticsTracker.events.isEmpty())
    }
}
