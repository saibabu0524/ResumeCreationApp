package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Standard application text field with built-in error state display.
 *
 * Features:
 * - Outlined Material3 style
 * - Optional label, placeholder, leading/trailing icons
 * - Error message displayed below the field when [errorMessage] is non-null
 * - Proper semantic error state for accessibility
 *
 * @param value Current text value.
 * @param onValueChange Callback when the text changes.
 * @param modifier Modifier for the text field.
 * @param label Optional label displayed above & inside when empty.
 * @param placeholder Optional placeholder text shown when the field is empty.
 * @param errorMessage Error message; when non-null, the field enters error state.
 * @param leadingIcon Optional icon composable on the leading side.
 * @param trailingIcon Optional icon composable on the trailing side.
 * @param enabled Whether the text field is editable.
 * @param singleLine Whether to constrain input to a single line.
 * @param maxLines Maximum number of lines.
 * @param keyboardOptions Software keyboard configuration.
 * @param keyboardActions IME action callbacks.
 * @param visualTransformation Visual transformation (e.g., password masking).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val isError = errorMessage != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isError) {
                        Modifier.semantics { error(errorMessage!!) }
                    } else {
                        Modifier
                    },
                ),
            label = label?.let { { Text(text = it) } },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = MaterialTheme.shapes.small,
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
