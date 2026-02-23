package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Centered loading spinner.
 *
 * Fills the available space and centers a [CircularProgressIndicator].
 * Includes a test tag and semantic content description for testing
 * and accessibility.
 *
 * @param modifier Modifier applied to the outermost container.
 * @param loadingDescription Accessibility description for the progress indicator.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    loadingDescription: String = "Loading",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("loading_indicator"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = loadingDescription },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
        )
    }
}
