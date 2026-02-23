package com.softsuave.resumecreationapp.feature.profile

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.testing.data.TestData
import com.softsuave.resumecreationapp.core.testing.fake.FakeAnalyticsTracker
import com.softsuave.resumecreationapp.core.testing.fake.FakeUserRepository
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import androidx.lifecycle.SavedStateHandle
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
 * Unit tests for [ProfileViewModel].
 *
 * Tests use [FakeUserRepository] and [SavedStateHandle] to simulate navigation
 * arguments and verify profile load, edit, and save flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class ProfileViewModelTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var analyticsTracker: FakeAnalyticsTracker
    private lateinit var viewModel: ProfileViewModel

    private val testUser = TestData.user()
    private val savedStateHandle = SavedStateHandle(mapOf("userId" to TestData.DEFAULT_USER_ID))

    @BeforeEach
    fun setup() {
        userRepository = FakeUserRepository()
        analyticsTracker = FakeAnalyticsTracker()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(savedStateHandle, userRepository, analyticsTracker)
    }

    @Test
    fun `loads profile on init when user exists`() = runTest {
        // Arrange
        userRepository.addUser(testUser)

        // Act
        viewModel = createViewModel()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.user)
            assertEquals(testUser.displayName, state.user?.displayName)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `shows error when user not found`() = runTest {
        // Arrange — no user added

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.user)
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `shows error when repository fails`() = runTest {
        userRepository.setShouldReturnError(true, AppException.ServerError())

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `edit clicked enters edit mode`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.onEvent(ProfileUserIntent.EditClicked)

        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals(testUser.displayName, viewModel.uiState.value.editDisplayName)
    }

    @Test
    fun `cancel edit exits edit mode`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.onEvent(ProfileUserIntent.EditClicked)
        assertTrue(viewModel.uiState.value.isEditing)

        viewModel.onEvent(ProfileUserIntent.CancelEdit)
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `display name changed updates edit field`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.onEvent(ProfileUserIntent.DisplayNameChanged("New Name"))

        assertEquals("New Name", viewModel.uiState.value.editDisplayName)
    }

    @Test
    fun `save profile updates user and exits edit mode`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.onEvent(ProfileUserIntent.EditClicked)
        viewModel.onEvent(ProfileUserIntent.DisplayNameChanged("Updated Name"))

        viewModel.uiEvent.test {
            viewModel.onEvent(ProfileUserIntent.SaveClicked)

            val event = awaitItem()
            assertTrue(event is ProfileUiEvent.ShowSnackbar)
            assertEquals("Profile updated", (event as ProfileUiEvent.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("Updated Name", viewModel.uiState.value.user?.displayName)
    }

    @Test
    fun `back clicked emits navigate back event`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.onEvent(ProfileUserIntent.BackClicked)

            val event = awaitItem()
            assertTrue(event is ProfileUiEvent.NavigateBack)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tracks screen view on init`() {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        val screenViews = analyticsTracker.eventsOfType<AnalyticsEvent.ScreenView>()
        assertTrue(screenViews.any { it.screenName == "profile" })
    }

    @Test
    fun `retry reloads profile`() = runTest {
        userRepository.addUser(testUser)
        viewModel = createViewModel()

        viewModel.onEvent(ProfileUserIntent.Retry)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.user)
            cancelAndConsumeRemainingEvents()
        }
    }
}
