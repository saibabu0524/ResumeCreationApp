package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appButtonScreenshot() {
        composeTestRule.setContent {
            com.softsuave.resumecreationapp.core.ui.theme.AppTheme {
                AppButton(
                    text = "Test Button",
                    onClick = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Test Button"))
            .captureRoboImage("build/outputs/roborazzi/app-button.png")
    }

    @Test
    fun emptyStateScreenshot() {
        composeTestRule.setContent {
            com.softsuave.resumecreationapp.core.ui.theme.AppTheme {
                EmptyState(
                    title = "No Items",
                    message = "You don't have any items yet.",
                    icon = androidx.compose.material.icons.Icons.Default.Person,
                    actionText = "Retry",
                    onAction = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        composeTestRule.onNode(androidx.compose.ui.test.hasText("No Items"))
            .captureRoboImage("build/outputs/roborazzi/empty-state.png")
    }
}
