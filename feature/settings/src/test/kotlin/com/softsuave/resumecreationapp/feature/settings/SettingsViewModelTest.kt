package com.softsuave.resumecreationapp.feature.settings

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.testing.fake.FakeAnalyticsTracker
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for [SettingsViewModel].
 *
 * Uses MockK for [UserPreferencesRepository] since it's a concrete class
 * (not an interface) and uses [FakeAnalyticsTracker] for analytics verification.
 *
 * Note: MockK is acceptable here per ADR-005 — mocks are used for leaf
 * dependencies like DataStore, only fakes for repository interfaces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class SettingsViewModelTest {

    private val isDarkModeFlow = MutableStateFlow(false)
    private val isNotificationsEnabledFlow = MutableStateFlow(true)
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        preferencesRepository = mockk(relaxed = true) {
            every { isDarkMode } returns isDarkModeFlow
            every { isPushNotificationsEnabled } returns isNotificationsEnabledFlow
        }
        analyticsTracker = FakeAnalyticsTracker()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(preferencesRepository, analyticsTracker)
    }

    @Test
    fun `loads preferences on init`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ThemeMode.Light, state.themeMode)
            assertTrue(state.notificationsEnabled)
            assertFalse(state.isLoading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `dark mode preference maps to Dark theme`() = runTest {
        isDarkModeFlow.value = true

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ThemeMode.Dark, state.themeMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `theme mode change updates state and persists`() = runTest {
        viewModel = createViewModel()

        viewModel.onEvent(SettingsUserIntent.ThemeModeChanged(ThemeMode.Dark))

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
        coVerify { preferencesRepository.setDarkMode(true) }
    }

    @Test
    fun `notifications toggle updates state and persists`() = runTest {
        viewModel = createViewModel()

        viewModel.onEvent(SettingsUserIntent.NotificationsToggled(false))

        assertFalse(viewModel.uiState.value.notificationsEnabled)
        coVerify { preferencesRepository.setPushNotificationsEnabled(false) }
    }

    @Test
    fun `back clicked emits navigate back event`() = runTest {
        viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.onEvent(SettingsUserIntent.BackClicked)

            val event = awaitItem()
            assertTrue(event is SettingsUiEvent.NavigateBack)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tracks screen view analytics on init`() {
        viewModel = createViewModel()

        val screenViews = analyticsTracker.eventsOfType<AnalyticsEvent.ScreenView>()
        assertTrue(screenViews.any { it.screenName == "settings" })
    }
}
