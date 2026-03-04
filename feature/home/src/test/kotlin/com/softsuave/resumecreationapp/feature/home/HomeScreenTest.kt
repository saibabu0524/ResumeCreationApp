package com.softsuave.resumecreationapp.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows loading initially`() {
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    uiState = HomeUiState(isLoading = true),
                    onEvent = {},
                    onNavigateToSettings = {}
                )
            }
        }
    }

    @Test
    fun `shows users list`() {
        val users = listOf(
            User(
                id = "1",
                displayName = "Alice Smith",
                email = "alice@example.com",
                avatarUrl = null,
                isEmailVerified = true,
                createdAt = 0L,
                updatedAt = 0L
            )
        )

        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    uiState = HomeUiState(users = users),
                    onEvent = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Alice Smith").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice@example.com").assertIsDisplayed()
    }

    @Test
    fun `shows empty state when no users`() {
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    uiState = HomeUiState(users = emptyList(), isLoading = false),
                    onEvent = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No Users Found").assertIsDisplayed()
    }
}
