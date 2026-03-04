package com.softsuave.resumecreationapp.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
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
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows settings correctly`() {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        themeMode = ThemeMode.Dark,
                        notificationsEnabled = true,
                        isLoading = false,
                    ),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme settings").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Push Notifications").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Out").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Account").assertIsDisplayed()
    }
}
