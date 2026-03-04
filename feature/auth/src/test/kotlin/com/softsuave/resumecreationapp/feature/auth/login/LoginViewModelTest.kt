package com.softsuave.resumecreationapp.feature.auth.login

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.common.util.ValidationUtil
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.usecase.auth.LoginUseCase
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
 * Unit tests for [LoginViewModel].
 *
 * Paths that call [android.util.Patterns] (via [ValidationUtil.isValidEmail]) are
 * covered by stubbing [ValidationUtil] with MockK's mockkObject so they run on the
 * JVM without Robolectric. Tests that only exercise blank-string fast-paths need
 * no stubbing because [ValidationUtil.isValidEmail] short-circuits before calling
 * [android.util.Patterns] when the input is blank.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class LoginViewModelTest {

    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        analyticsTracker = FakeAnalyticsTracker()
        loginUseCase = mockk(relaxed = true)
        viewModel = LoginViewModel(loginUseCase, analyticsTracker)
    }

    @AfterEach
    fun tearDown() {
        try { unmockkObject(ValidationUtil) } catch (_: Exception) { /* not mocked */ }
    }

    // ─── Initial state ────────────────────────────────────────────────────────

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

    // ─── Field updates ────────────────────────────────────────────────────────

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
        viewModel.onEvent(LoginUserIntent.EmailChanged("bad"))
        viewModel.onEvent(LoginUserIntent.EmailChanged("new@example.com"))
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `password change clears password error`() {
        viewModel.onEvent(LoginUserIntent.PasswordChanged(""))
        viewModel.onEvent(LoginUserIntent.PasswordChanged("newpass"))
        assertNull(viewModel.uiState.value.passwordError)
    }

    // ─── Navigation event ─────────────────────────────────────────────────────

    @Test
    fun `register clicked emits navigate to registration event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(LoginUserIntent.RegisterClicked)

            val event = awaitItem()
            assertTrue(event is LoginUiEvent.NavigateToRegistration)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    fun `blank email shows email error and does not start loading`() {
        // ValidationUtil.isValidEmail("") short-circuits before Patterns — no stub needed
        viewModel.onEvent(LoginUserIntent.PasswordChanged("Password1"))
        viewModel.onEvent(LoginUserIntent.LoginClicked)

        assertNotNull(viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `blank password shows password error and does not start loading`() {
        mockkObject(ValidationUtil)
        every { ValidationUtil.isValidEmail(any()) } returns true

        viewModel.onEvent(LoginUserIntent.EmailChanged("valid@example.com"))
        // password left blank
        viewModel.onEvent(LoginUserIntent.LoginClicked)

        assertNotNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Submit paths ─────────────────────────────────────────────────────────

    @Test
    fun `login success emits NavigateToHome and clears loading`() = runTest {
        mockkObject(ValidationUtil)
        every { ValidationUtil.isValidEmail(any()) } returns true
        coEvery { loginUseCase(any()) } returns Result.Success(Unit)

        viewModel.onEvent(LoginUserIntent.EmailChanged("valid@example.com"))
        viewModel.onEvent(LoginUserIntent.PasswordChanged("Password1"))

        viewModel.uiEvent.test {
            viewModel.onEvent(LoginUserIntent.LoginClicked)

            val event = awaitItem()
            assertTrue(event is LoginUiEvent.NavigateToHome)
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `login failure shows general error and clears loading`() = runTest {
        mockkObject(ValidationUtil)
        every { ValidationUtil.isValidEmail(any()) } returns true
        coEvery { loginUseCase(any()) } returns Result.Error(
            AppException.Unauthorized(message = "Bad credentials"),
        )

        viewModel.onEvent(LoginUserIntent.EmailChanged("valid@example.com"))
        viewModel.onEvent(LoginUserIntent.PasswordChanged("Password1"))
        viewModel.onEvent(LoginUserIntent.LoginClicked)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Bad credentials", viewModel.uiState.value.generalError)
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    @Test
    fun `analytics tracker starts empty`() {
        assertTrue(analyticsTracker.events.isEmpty())
    }
}
