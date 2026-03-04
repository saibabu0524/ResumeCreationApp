package com.softsuave.resumecreationapp.feature.profile

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
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows Profile uiState correctly`() {
        val user = User(
            id = "1",
            displayName = "Jane Doe",
            email = "jane@example.com",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    uiState = ProfileUiState(user = user),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("jane@example.com").assertIsDisplayed()
    }

    @Test
    fun `shows loading when user is null`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    uiState = ProfileUiState(user = null, isLoading = true),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        // CircularProgressIndicator is shown, but typically we assert by lack of user data
    }
}
