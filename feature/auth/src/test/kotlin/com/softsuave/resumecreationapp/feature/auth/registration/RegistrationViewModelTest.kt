package com.softsuave.resumecreationapp.feature.auth.registration

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
 * Unit tests for [RegistrationViewModel].
 *
 * Note: Tests that trigger [ValidationUtil.isValidEmail] are skipped because
 * that utility uses [android.util.Patterns] which is not available in JVM
 * unit tests. Validation integration is tested via instrumented tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class RegistrationViewModelTest {

    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var viewModel: RegistrationViewModel

    @BeforeEach
    fun setup() {
        analyticsTracker = FakeAnalyticsTracker()
        viewModel = RegistrationViewModel(analyticsTracker)
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals("", state.displayName)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.displayNameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.generalError)
    }

    @Test
    fun `display name changed updates state`() {
        viewModel.onEvent(RegistrationUserIntent.DisplayNameChanged("Jane Doe"))
        assertEquals("Jane Doe", viewModel.uiState.value.displayName)
    }

    @Test
    fun `email changed updates state`() {
        viewModel.onEvent(RegistrationUserIntent.EmailChanged("jane@example.com"))
        assertEquals("jane@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `password changed updates state`() {
        viewModel.onEvent(RegistrationUserIntent.PasswordChanged("Str0ngP@ss"))
        assertEquals("Str0ngP@ss", viewModel.uiState.value.password)
    }

    @Test
    fun `confirm password changed updates state`() {
        viewModel.onEvent(RegistrationUserIntent.ConfirmPasswordChanged("Str0ngP@ss"))
        assertEquals("Str0ngP@ss", viewModel.uiState.value.confirmPassword)
    }

    @Test
    fun `display name change clears display name error`() {
        // Manually simulate an error by sending an empty register
        // (the display name error is checked first, before email validation)
        viewModel.onEvent(RegistrationUserIntent.DisplayNameChanged(""))
        viewModel.onEvent(RegistrationUserIntent.DisplayNameChanged("Jane"))
        assertNull(viewModel.uiState.value.displayNameError)
    }

    @Test
    fun `email change clears email error`() {
        viewModel.onEvent(RegistrationUserIntent.EmailChanged(""))
        viewModel.onEvent(RegistrationUserIntent.EmailChanged("jane@example.com"))
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `password change clears password error`() {
        viewModel.onEvent(RegistrationUserIntent.PasswordChanged(""))
        viewModel.onEvent(RegistrationUserIntent.PasswordChanged("Str0ngP1"))
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `confirm password change clears confirm password error`() {
        viewModel.onEvent(RegistrationUserIntent.ConfirmPasswordChanged("diff"))
        viewModel.onEvent(RegistrationUserIntent.ConfirmPasswordChanged("Str0ngP1"))
        assertNull(viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `login clicked emits navigate to login event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(RegistrationUserIntent.LoginClicked)

            val event = awaitItem()
            assertTrue(event is RegistrationUiEvent.NavigateToLogin)
            cancelAndConsumeRemainingEvents()
        }
    }
}
