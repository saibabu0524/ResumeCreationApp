package com.softsuave.resumecreationapp.feature.ats.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.SectionScores
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import com.softsuave.resumecreationapp.feature.ats.AtsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AtsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows idle state correctly`() {
        composeTestRule.setContent {
            AppTheme {
                AtsScreen(
                    uiState = AtsUiState.Idle,
                    onAnalyse = { _, _, _ -> },
                    onReset = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("ATS SCANNER").assertIsDisplayed()
        // AtsSectionHeader renders title.uppercase(), so check the uppercased form
        composeTestRule.onNodeWithText("UPLOAD RESUME").assertIsDisplayed()
        composeTestRule.onNodeWithText("JOB DESCRIPTION").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI PROVIDER").assertIsDisplayed()

        // Scan button should be disabled initially
        composeTestRule.onNodeWithText("SCAN MY RESUME →").assertIsNotEnabled()
    }

    @Test
    fun `shows loading overlay`() {
        composeTestRule.setContent {
            AppTheme {
                AtsScreen(
                    uiState = AtsUiState.Loading("Analysing with magic..."),
                    onAnalyse = { _, _, _ -> },
                    onReset = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Analysing with magic...").assertIsDisplayed()
    }

    @Test
    fun `shows result screen on success`() {
        val mockResult = AtsResult(
            overallScore = 95,
            scoreLabel = "Excellent",
            keywordsPresent = listOf("Kotlin", "Android"),
            keywordsMissing = listOf("Java"),
            sectionScores = SectionScores(
                skillsMatch = 100,
                experienceRelevance = 90,
                educationMatch = 100,
                formatting = 90
            ),
            suggestions = listOf("Add more metrics"),
            strengths = listOf("Good keywords"),
            summary = "Great resume"
        )

        composeTestRule.setContent {
            AppTheme {
                AtsScreen(
                    uiState = AtsUiState.Success(mockResult),
                    onAnalyse = { _, _, _ -> },
                    onReset = {},
                    onNavigateBack = {}
                )
            }
        }

        // Actual strings in UI
        composeTestRule.onNodeWithText("ATS ANALYSIS REPORT").assertIsDisplayed()
        composeTestRule.onNodeWithText("95").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXCELLENT").assertIsDisplayed()
        // Text is rendered as part of other UI compose blocks so it matches the data directly
        composeTestRule.onNodeWithText("Great resume").assertIsDisplayed()
    }

    @Test
    fun `shows error state`() {
        composeTestRule.setContent {
            AppTheme {
                AtsScreen(
                    uiState = AtsUiState.Error("Something went wrong"),
                    onAnalyse = { _, _, _ -> },
                    onReset = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("⚠  Something went wrong").assertIsDisplayed()
    }
}
