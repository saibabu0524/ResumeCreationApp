package com.softsuave.resumecreationapp.feature.resume.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import com.softsuave.resumecreationapp.feature.resume.ResumeUiState
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
    fun `shows input form on idle state`() {
        composeTestRule.setContent {
            AppTheme {
                // Use the stateless HomeScreenContent overload so Hilt is not required
                HomeScreenContent(uiState = ResumeUiState.Idle)
            }
        }

        // Top bar label
        composeTestRule.onNodeWithText("RESUME TAILOR").assertIsDisplayed()
        // SectionHeader renders title.uppercase(), so assert uppercased values
        composeTestRule.onNodeWithText("UPLOAD RESUME").assertIsDisplayed()
        composeTestRule.onNodeWithText("JOB DESCRIPTION").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI PROVIDER").assertIsDisplayed()
        // Submit button
        composeTestRule.onNodeWithText("TAILOR MY RESUME →").assertIsDisplayed()
    }
}
