package com.softsuave.resumecreationapp.feature.auth.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows idle state correctly`() {
        composeTestRule.setContent {
            AppTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onLoginClicked = {},
                    onRegisterClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sign into\nYour Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMAIL ADDRESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("PASSWORD").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIGN IN").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIGN IN").assertIsEnabled()
    }

    @Test
    fun `shows loading state`() {
        composeTestRule.setContent {
            AppTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoading = true),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onLoginClicked = {},
                    onRegisterClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("SIGNING IN...").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIGNING IN...").assertIsNotEnabled()
    }

    @Test
    fun `shows validation errors`() {
        composeTestRule.setContent {
            AppTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        emailError = "Invalid email",
                        passwordError = "Password too short",
                        generalError = "Server down"
                    ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onLoginClicked = {},
                    onRegisterClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("⚠ Invalid email").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠ Password too short").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server down").assertIsDisplayed()
    }
}
