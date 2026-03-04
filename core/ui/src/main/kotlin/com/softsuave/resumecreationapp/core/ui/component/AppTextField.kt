package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard application text field — theme-aware (light + dark).
 *
 * - Primary colour focus ring
 * - Monospace label (all-caps) floated above the field
 * - Error message with ⚠ prefix, animated in/out
 * - Proper semantic error state for accessibility
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val primary  = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline  = MaterialTheme.colorScheme.outline
    val error    = MaterialTheme.colorScheme.error

    Column(modifier = modifier) {
        // Floating monospace label
        if (label != null) {
            Text(
                text  = label.uppercase(),
                fontFamily    = FontFamily.Monospace,
                fontSize      = 10.sp,
                letterSpacing = 2.sp,
                color = if (isError) error else onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        OutlinedTextField(
            value          = value,
            onValueChange  = onValueChange,
            modifier       = Modifier
                .fillMaxWidth()
                .then(
                    if (isError) Modifier.semantics { error(errorMessage!!) }
                    else Modifier,
                ),
            placeholder    = placeholder?.let {
                { Text(it, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = onSurfaceVariant.copy(alpha = 0.45f)) }
            },
            leadingIcon    = leadingIcon,
            trailingIcon   = trailingIcon,
            isError        = isError,
            enabled        = enabled,
            singleLine     = singleLine,
            maxLines       = maxLines,
            keyboardOptions   = keyboardOptions,
            keyboardActions   = keyboardActions,
            visualTransformation = visualTransformation,
            shape          = MaterialTheme.shapes.extraSmall,
            textStyle      = LocalTextStyle.current.copy(
                fontFamily    = FontFamily.Monospace,
                fontSize      = 14.sp,
                color         = onSurface,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                // Container
                focusedContainerColor   = surfaceVariant,
                unfocusedContainerColor = surfaceVariant,
                disabledContainerColor  = surfaceVariant.copy(alpha = 0.5f),
                // Text
                focusedTextColor        = onSurface,
                unfocusedTextColor      = onSurface,
                disabledTextColor       = onSurfaceVariant,
                // Cursor
                cursorColor             = primary,
                // Border
                focusedBorderColor      = primary,
                unfocusedBorderColor    = if (value.isNotEmpty()) primary.copy(alpha = 0.4f) else outline,
                disabledBorderColor     = outline.copy(alpha = 0.4f),
                errorBorderColor        = error,
                errorCursorColor        = error,
                // Leading icon
                focusedLeadingIconColor  = primary,
                unfocusedLeadingIconColor = onSurfaceVariant,
                disabledLeadingIconColor  = onSurfaceVariant.copy(alpha = 0.4f),
                errorLeadingIconColor    = error,
                // Trailing icon
                focusedTrailingIconColor  = onSurfaceVariant,
                unfocusedTrailingIconColor = onSurfaceVariant,
                errorTrailingIconColor   = error,
                // Label inside the field (when no external label)
                focusedLabelColor        = primary,
                unfocusedLabelColor      = onSurfaceVariant,
                errorLabelColor          = error,
            ),
        )

        // Animated error message
        AnimatedVisibility(
            visible = isError,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            if (errorMessage != null) {
                Text(
                    text          = "⚠  $errorMessage",
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 10.sp,
                    letterSpacing = 0.5.sp,
                    color         = error,
                    modifier      = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }
    }
}
