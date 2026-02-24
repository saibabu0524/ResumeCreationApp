package com.softsuave.resumecreationapp.feature.auth.registration

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.common.util.ValidationUtil
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.usecase.auth.RegisterUseCase
import com.softsuave.resumecreationapp.core.testing.fake.FakeAnalyticsTracker
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for [RegistrationViewModel].
 *
 * Paths that call [android.util.Patterns] (via [ValidationUtil.isValidEmail]) are
 * covered by stubbing [ValidationUtil] with MockK's mockkObject so they run on the
 * JVM without Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class RegistrationViewModelTest {

    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: RegistrationViewModel

    @BeforeEach
    fun setup() {
        analyticsTracker = FakeAnalyticsTracker()
        registerUseCase = mockk(relaxed = true)
        viewModel = RegistrationViewModel(registerUseCase, analyticsTracker)
    }

    @AfterEach
    fun tearDown() {
        try { unmockkObject(ValidationUtil) } catch (_: Exception) { /* not mocked */ }
    }

    // ─── Initial state ────────────────────────────────────────────────────────

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

    // ─── Field updates ────────────────────────────────────────────────────────

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

    // ─── Navigation event ─────────────────────────────────────────────────────

    @Test
    fun `login clicked emits navigate to login event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(RegistrationUserIntent.LoginClicked)

            val event = awaitItem()
            assertTrue(event is RegistrationUiEvent.NavigateToLogin)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    fun `blank display name shows display name error without calling use case`() {
        // No email validation is triggered when display name is blank (fails first)
        viewModel.onEvent(RegistrationUserIntent.RegisterClicked)

        assertNotNull(viewModel.uiState.value.displayNameError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Submit paths ─────────────────────────────────────────────────────────

    @Test
    fun `register success emits NavigateToOnboarding and clears loading`() = runTest {
        mockkObject(ValidationUtil)
        every { ValidationUtil.isValidEmail(any()) } returns true
        coEvery { registerUseCase(any()) } returns Result.Success(Unit)

        viewModel.onEvent(RegistrationUserIntent.DisplayNameChanged("Jane Doe"))
        viewModel.onEvent(RegistrationUserIntent.EmailChanged("jane@example.com"))
        viewModel.onEvent(RegistrationUserIntent.PasswordChanged("Str0ngP1"))
        viewModel.onEvent(RegistrationUserIntent.ConfirmPasswordChanged("Str0ngP1"))

        viewModel.uiEvent.test {
            viewModel.onEvent(RegistrationUserIntent.RegisterClicked)

            val event = awaitItem()
            assertTrue(event is RegistrationUiEvent.NavigateToOnboarding)
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `register failure shows general error and clears loading`() = runTest {
        mockkObject(ValidationUtil)
        every { ValidationUtil.isValidEmail(any()) } returns true
        coEvery { registerUseCase(any()) } returns Result.Error(
            AppException.Conflict(message = "Email already registered"),
        )

        viewModel.onEvent(RegistrationUserIntent.DisplayNameChanged("Jane Doe"))
        viewModel.onEvent(RegistrationUserIntent.EmailChanged("jane@example.com"))
        viewModel.onEvent(RegistrationUserIntent.PasswordChanged("Str0ngP1"))
        viewModel.onEvent(RegistrationUserIntent.ConfirmPasswordChanged("Str0ngP1"))
        viewModel.onEvent(RegistrationUserIntent.RegisterClicked)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Email already registered", viewModel.uiState.value.generalError)
    }
}