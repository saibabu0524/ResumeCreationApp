package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing

/**
 * Configurable empty state display.
 *
 * Shows an optional icon, title, subtitle message, and optional action button.
 * Used when a screen has no data to display.
 *
 * @param title Primary heading for the empty state.
 * @param modifier Modifier for the component.
 * @param icon Optional icon displayed above the title.
 * @param message Optional subtitle message below the title.
 * @param actionText Optional text for the action button.
 * @param onAction Optional callback when the action button is tapped.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Decorative — title conveys meaning
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (message != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(spacing.large))
            AppButton(
                text = actionText,
                onClick = onAction,
                variant = AppButtonVariant.Primary,
            )
        }
    }
}
