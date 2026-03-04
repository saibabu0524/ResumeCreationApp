package com.softsuave.resumecreationapp.feature.auth.registration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
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
class RegistrationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows idle state correctly`() {
        composeTestRule.setContent {
            AppTheme {
                RegistrationScreen(
                    uiState = RegistrationUiState(),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("CREATE\nYour Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("DISPLAY NAME").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMAIL ADDRESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("PASSWORD").assertIsDisplayed()
        composeTestRule.onNodeWithText("CONFIRM PASSWORD").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATE ACCOUNT →").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATE ACCOUNT →").assertIsEnabled()
    }

    @Test
    fun `shows loading state`() {
        composeTestRule.setContent {
            AppTheme {
                RegistrationScreen(
                    uiState = RegistrationUiState(isLoading = true),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("CREATING ACCOUNT...").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATING ACCOUNT...").assertIsNotEnabled()
    }

    @Test
    fun `shows validation errors`() {
        composeTestRule.setContent {
            AppTheme {
                RegistrationScreen(
                    uiState = RegistrationUiState(
                        displayNameError = "Required",
                        emailError = "Invalid email",
                        passwordError = "Too short",
                        confirmPasswordError = "Mismatch"
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("⚠ Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠ Invalid email").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠ Too short").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠ Mismatch").assertIsDisplayed()
    }
}
