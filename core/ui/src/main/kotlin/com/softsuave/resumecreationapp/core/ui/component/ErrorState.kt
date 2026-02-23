package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.softsuave.resumecreationapp.core.ui.R
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing

/**
 * Error state display with retry action.
 *
 * Shows an error icon, error message, and a retry button.
 *
 * @param message Error message to display.
 * @param onRetry Callback when the retry button is tapped.
 * @param modifier Modifier for the component.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge)
            .testTag("error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null, // Decorative — message conveys the error
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.large))

        AppButton(
            text = stringResource(R.string.core_ui_retry),
            onClick = onRetry,
            variant = AppButtonVariant.Primary,
        )
    }
}
