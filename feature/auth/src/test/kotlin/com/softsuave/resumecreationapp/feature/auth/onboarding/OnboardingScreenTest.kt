package com.softsuave.resumecreationapp.feature.auth.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows first page correctly`() {
        composeTestRule.setContent {
            AppTheme {
                OnboardingScreen(onOnboardingComplete = {})
            }
        }

        composeTestRule.onNodeWithText("STEP 01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upload &\nAnalyse\nYour Resume").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT →").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }

    @Test
    fun `skip button triggers onOnboardingComplete callback`() {
        var completeCalled = false
        composeTestRule.setContent {
            AppTheme {
                OnboardingScreen(onOnboardingComplete = { completeCalled = true })
            }
        }

        composeTestRule.onNodeWithText("Skip for now").performClick()
        composeTestRule.waitForIdle()

        assert(completeCalled) { "onOnboardingComplete was not called after Skip" }
    }

    @Test
    fun `clicking NEXT advances to page 2`() {
        composeTestRule.setContent {
            AppTheme {
                OnboardingScreen(onOnboardingComplete = {})
            }
        }

        composeTestRule.onNodeWithText("NEXT →").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600L) // let pager animation settle

        composeTestRule.onNodeWithText("STEP 02").assertIsDisplayed()
    }

    @Test
    fun `clicking NEXT twice reaches last page with GET STARTED button`() {
        composeTestRule.setContent {
            AppTheme {
                OnboardingScreen(onOnboardingComplete = {})
            }
        }

        // Advance to page 2
        composeTestRule.onNodeWithText("NEXT →").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600L)

        // Advance to page 3 (last)
        composeTestRule.onNodeWithText("NEXT →").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600L)

        composeTestRule.onNodeWithText("GET STARTED →").assertIsDisplayed()
        // Skip link should be hidden on last page
        composeTestRule.onNodeWithText("Skip for now").assertIsNotDisplayed()
    }

    @Test
    fun `GET STARTED triggers onOnboardingComplete callback`() {
        var completeCalled = false
        composeTestRule.setContent {
            AppTheme {
                OnboardingScreen(onOnboardingComplete = { completeCalled = true })
            }
        }

        // Navigate to last page
        repeat(2) {
            composeTestRule.onNodeWithText("NEXT →").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(600L)
        }

        composeTestRule.onNodeWithText("GET STARTED →").performClick()
        composeTestRule.waitForIdle()

        assert(completeCalled) { "onOnboardingComplete was not called after GET STARTED" }
    }
}
