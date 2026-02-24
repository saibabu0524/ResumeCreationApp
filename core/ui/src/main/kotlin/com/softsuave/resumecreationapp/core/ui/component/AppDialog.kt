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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.softsuave.resumecreationapp.core.ui.R

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Surface0  = Color(0xFF1A1814)
private val Surface1  = Color(0xFF242019)
private val Amber     = Color(0xFFD4A853)
private val AmberDim  = Color(0xFF8A6930)
private val TextPri   = Color(0xFFF0EAD6)
private val TextMuted = Color(0xFF9A8E78)
private val BorderMid = Color(0xFF4A4238)
private val BorderSub = Color(0xFF2E2A24)
private val ErrorRed  = Color(0xFFB04A3A)

/**
 * Dark editorial application dialog.
 *
 * Replaces Material3 AlertDialog with a fully custom layout to match the app's
 * dark canvas / amber accent aesthetic:
 * - Warm near-black container with hairline border
 * - Serif italic title
 * - Monospace body text
 * - Amber confirm button · Outlined/text cancel button
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
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .testTag("app_dialog")
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width  = 0.5.dp,
                    brush  = Brush.verticalGradient(listOf(BorderMid, BorderSub)),
                    shape  = RoundedCornerShape(4.dp),
                )
                .background(Surface0)
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
                    color         = Amber.copy(alpha = 0.5f),
                )

                Spacer(Modifier.height(6.dp))

                // Title — Serif italic
                Text(
                    text       = title,
                    fontFamily = FontFamily.Serif,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle  = FontStyle.Italic,
                    color      = TextPri,
                )

                Spacer(Modifier.height(12.dp))

                // Hairline divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(BorderSub)
                )

                Spacer(Modifier.height(12.dp))

                // Body — Monospace
                Text(
                    text          = text,
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 12.sp,
                    lineHeight    = 20.sp,
                    color         = TextMuted,
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
                            colors  = ButtonDefaults.textButtonColors(contentColor = TextMuted),
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
                            containerColor = Amber,
                            contentColor   = Canvas,
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
