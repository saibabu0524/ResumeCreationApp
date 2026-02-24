package com.softsuave.resumecreationapp.feature.resume.ui

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
class ResultScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows result correctly`() {
        // A minimal test rendering the result screen using empty bytes
        composeTestRule.setContent {
            AppTheme {
                ResultScreen(
                    pdfBytes = byteArrayOf(), // empty for test context
                    onStartOver = {}
                )
            }
        }

        // Top bar label is always shown
        composeTestRule.onNodeWithText("THE DOCUMENT").assertIsDisplayed()
        // Bottom bar always has action buttons and start-over
        composeTestRule.onNodeWithText("SAVE PDF").assertIsDisplayed()
        composeTestRule.onNodeWithText("SHARE").assertIsDisplayed()
        composeTestRule.onNodeWithText("← START OVER").assertIsDisplayed()
    }
}
