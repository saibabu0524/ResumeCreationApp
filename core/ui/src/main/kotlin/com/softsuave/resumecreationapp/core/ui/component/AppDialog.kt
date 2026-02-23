package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.softsuave.resumecreationapp.core.ui.R

/**
 * Standard application dialog wrapper.
 *
 * Provides a consistent dialog appearance across the app with
 * configurable title, text, confirm and dismiss actions.
 *
 * @param title Dialog title text.
 * @param text Dialog body text.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param modifier Modifier for the dialog.
 * @param confirmText Text for the confirm button.
 * @param dismissText Text for the dismiss button. When null, no dismiss button is shown.
 * @param onConfirm Callback when the confirm button is tapped.
 * @param icon Optional icon composable displayed above the title.
 */
@Composable
fun AppDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.core_ui_ok),
    dismissText: String? = stringResource(R.string.core_ui_cancel),
    onConfirm: () -> Unit = onDismiss,
    icon: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(text = confirmText)
            }
        },
        modifier = modifier.testTag("app_dialog"),
        dismissButton = if (dismissText != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissText)
                }
            }
        } else {
            null
        },
        icon = icon,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        shape = MaterialTheme.shapes.extraLarge,
    )
}
