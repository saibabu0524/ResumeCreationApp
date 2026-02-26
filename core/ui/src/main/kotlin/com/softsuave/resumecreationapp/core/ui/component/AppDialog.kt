package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.softsuave.resumecreationapp.core.ui.R

/**
 * Editorial application dialog, theme aware.
 *
 * Replaces Material3 AlertDialog with a fully custom layout:
 * - Container with hairline border
 * - Serif italic title
 * - Monospace body text
 * - Confirm button · Outlined/text cancel button
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
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .testTag("app_dialog")
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width  = 0.5.dp,
                    brush  = Brush.verticalGradient(listOf(outline, outlineVariant)),
                    shape  = RoundedCornerShape(4.dp),
                )
                .background(surface)
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Icon (optional)
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.CenterHorizontally),
                    ) { icon() }
                }

                // Eyebrow line
                Text(
                    "NOTICE",
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 9.sp,
                    letterSpacing = 3.sp,
                    color         = primary.copy(alpha = 0.5f),
                )

                Spacer(Modifier.height(6.dp))

                // Title — Serif italic
                Text(
                    text       = title,
                    fontFamily = FontFamily.Serif,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle  = FontStyle.Italic,
                    color      = onSurface,
                )

                Spacer(Modifier.height(12.dp))

                // Hairline divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(outlineVariant)
                )

                Spacer(Modifier.height(12.dp))

                // Body — Monospace
                Text(
                    text          = text,
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 12.sp,
                    lineHeight    = 20.sp,
                    color         = onSurfaceVariant,
                    letterSpacing = 0.3.sp,
                )

                Spacer(Modifier.height(24.dp))

                // Action row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (dismissText != null) {
                        TextButton(
                            onClick = onDismiss,
                            colors  = ButtonDefaults.textButtonColors(contentColor = onSurfaceVariant),
                        ) {
                            Text(
                                dismissText.uppercase(),
                                fontFamily    = FontFamily.Monospace,
                                fontSize      = 11.sp,
                                letterSpacing = 1.5.sp,
                            )
                        }
                    }

                    Button(
                        onClick    = { onConfirm(); onDismiss() },
                        shape      = RoundedCornerShape(2.dp),
                        elevation  = ButtonDefaults.buttonElevation(0.dp),
                        colors     = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor   = onPrimary,
                        ),
                    ) {
                        Text(
                            confirmText.uppercase(),
                            fontFamily    = FontFamily.Monospace,
                            fontSize      = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight    = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
