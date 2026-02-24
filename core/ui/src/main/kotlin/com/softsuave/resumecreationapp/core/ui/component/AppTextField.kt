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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Surface1  = Color(0xFF242019)
private val Amber     = Color(0xFFD4A853)
private val BorderMid = Color(0xFF4A4238)
private val TextPri   = Color(0xFFF0EAD6)
private val TextMuted = Color(0xFF9A8E78)
private val ErrorRed  = Color(0xFFB04A3A)

/**
 * Standard application text field with dark editorial styling.
 *
 * - Amber focus ring
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

    Column(modifier = modifier) {
        // Floating monospace label
        if (label != null) {
            Text(
                text  = label.uppercase(),
                fontFamily    = FontFamily.Monospace,
                fontSize      = 10.sp,
                letterSpacing = 2.sp,
                color = if (isError) ErrorRed else TextMuted,
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
                { Text(it, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextMuted.copy(alpha = 0.45f)) }
            },
            leadingIcon    = leadingIcon,
            trailingIcon   = trailingIcon,
            isError        = isError,
            enabled        = enabled,
            singleLine     = singleLine,
            maxLines       = maxLines,
            keyboardOptions  = keyboardOptions,
            keyboardActions  = keyboardActions,
            visualTransformation = visualTransformation,
            shape          = MaterialTheme.shapes.extraSmall,
            textStyle      = LocalTextStyle.current.copy(
                fontFamily    = FontFamily.Monospace,
                fontSize      = 14.sp,
                color         = TextPri,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = Surface1,
                unfocusedContainerColor = Surface1,
                disabledContainerColor  = Surface1.copy(alpha = 0.5f),
                focusedTextColor        = TextPri,
                unfocusedTextColor      = TextPri,
                disabledTextColor       = TextMuted,
                cursorColor             = Amber,
                focusedBorderColor      = Amber,
                unfocusedBorderColor    = if (value.isNotEmpty()) Amber.copy(alpha = 0.4f) else BorderMid,
                disabledBorderColor     = BorderMid.copy(alpha = 0.4f),
                errorBorderColor        = ErrorRed,
                errorCursorColor        = ErrorRed,
                focusedLeadingIconColor  = Amber,
                unfocusedLeadingIconColor = TextMuted,
                disabledLeadingIconColor  = TextMuted.copy(alpha = 0.4f),
                errorLeadingIconColor    = ErrorRed,
                focusedTrailingIconColor  = TextMuted,
                unfocusedTrailingIconColor = TextMuted,
                errorTrailingIconColor   = ErrorRed,
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
                    color         = ErrorRed,
                    modifier      = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }
    }
}
