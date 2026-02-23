package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Button variant enumeration for [AppButton].
 */
enum class AppButtonVariant {
    Primary,
    Secondary,
    Text,
}

/**
 * Standard application button with built-in loading state and accessibility.
 *
 * Enforces:
 * - Minimum 48dp touch target height (WCAG compliance)
 * - Proper semantic role for screen readers
 * - Loading spinner replaces text when [isLoading] is true
 *
 * @param text Button label text.
 * @param onClick Callback when the button is clicked.
 * @param modifier Modifier for the button.
 * @param variant Visual variant — [AppButtonVariant.Primary], [AppButtonVariant.Secondary], or [AppButtonVariant.Text].
 * @param enabled Whether the button is enabled. Disabled when [isLoading] is true.
 * @param isLoading Whether to show a loading indicator.
 * @param leadingIcon Optional composable drawn before the text.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val effectiveEnabled = enabled && !isLoading

    val buttonContent: @Composable () -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = when (variant) {
                    AppButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
                    AppButtonVariant.Secondary -> MaterialTheme.colorScheme.primary
                    AppButtonVariant.Text -> MaterialTheme.colorScheme.primary
                },
            )
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text)
        }
    }

    val semanticsModifier = modifier
        .heightIn(min = 48.dp)
        .semantics { role = Role.Button }

    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = semanticsModifier,
                enabled = effectiveEnabled,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                content = { buttonContent() },
            )
        }

        AppButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = semanticsModifier,
                enabled = effectiveEnabled,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                content = { buttonContent() },
            )
        }

        AppButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = semanticsModifier,
                enabled = effectiveEnabled,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                content = { buttonContent() },
            )
        }
    }
}
