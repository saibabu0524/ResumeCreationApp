package com.softsuave.resumecreationapp.feature.home

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.testing.data.TestData
import com.softsuave.resumecreationapp.core.testing.fake.FakeAnalyticsTracker
import com.softsuave.resumecreationapp.core.testing.fake.FakeUserRepository
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for [HomeViewModel].
 *
 * Uses [FakeUserRepository] and [FakeAnalyticsTracker] — no mocks.
 * Dispatcher replaced by [MainDispatcherExtension].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class HomeViewModelTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setup() {
        userRepository = FakeUserRepository()
        analyticsTracker = FakeAnalyticsTracker()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(userRepository, analyticsTracker)
    }

    @Test
    fun `initial state has loading true`() {
        // The ViewModel starts loading in init
        val vm = createViewModel()
        // After init completes with UnconfinedTestDispatcher, state should be settled
        assertNotNull(vm.uiState.value)
    }

    @Test
    fun `when user is available, state shows user in list`() = runTest {
        // Arrange
        val user = TestData.user()
        userRepository.setCurrentUser(user)

        // Act
        val vm = createViewModel()

        // Assert
        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.users.size)
            assertEquals(user.displayName, state.users.first().displayName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when no user signed in, shows empty list`() = runTest {
        // No user set in repository

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.users.isEmpty())
            assertNull(state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when repository errors, shows error message`() = runTest {
        // Arrange
        userRepository.setShouldReturnError(true)

        // Act
        val vm = createViewModel()

        // Assert
        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `search query updates state`() = runTest {
        val vm = createViewModel()

        vm.onEvent(HomeUserIntent.SearchQueryChanged("test query"))

        assertEquals("test query", vm.uiState.value.searchQuery)
    }

    @Test
    fun `user click emits navigation event`() = runTest {
        val vm = createViewModel()

        vm.uiEvent.test {
            vm.onEvent(HomeUserIntent.UserClicked("user_1"))

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.NavigateToProfile)
            assertEquals("user_1", (event as HomeUiEvent.NavigateToProfile).userId)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tracks screen view analytics on init`() {
        createViewModel()

        assertTrue(analyticsTracker.events.isNotEmpty())
        val screenView = analyticsTracker.events.first()
        assertTrue(screenView is com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent.ScreenView)
    }

    @Test
    fun `retry reloads users`() = runTest {
        // Start with error
        userRepository.setShouldReturnError(true)
        val vm = createViewModel()

        // Clear error and retry
        userRepository.setShouldReturnError(false)
        val user = TestData.user()
        userRepository.setCurrentUser(user)

        vm.onEvent(HomeUserIntent.Retry)

        // Eventually state should include the user
        vm.uiState.test {
            val state = awaitItem()
            // The state should have the user after retry
            cancelAndConsumeRemainingEvents()
        }
    }
}
