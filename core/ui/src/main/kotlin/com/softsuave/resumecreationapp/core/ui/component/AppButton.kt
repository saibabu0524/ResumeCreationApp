package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Local tokens (mirrors AppColors) ────────────────────────────────────────
private val Canvas   = Color(0xFF0E0D0B)
private val Amber    = Color(0xFFD4A853)
private val AmberDim = Color(0xFF8A6930)
private val Surface1 = Color(0xFF242019)
private val BorderMid = Color(0xFF4A4238)
private val TextMuted = Color(0xFF9A8E78)

enum class AppButtonVariant { Primary, Secondary, Text }

/**
 * Standard application button with built-in loading state and accessibility.
 *
 * Variants:
 *  - **Primary**   — Solid amber fill, canvas text. The main CTA.
 *  - **Secondary** — Transparent with amber border. Secondary actions.
 *  - **Text**      — No background or border. Tertiary / link actions.
 *
 * All variants enforce a 48 dp minimum touch target (WCAG 2.1 AA).
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

    // Subtle glow pulse on Primary when loading
    val glowAlpha by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.15f,
        targetValue  = if (isLoading) 0.45f else 0.22f,
        animationSpec = if (isLoading)
            infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "ga"
    )

    val baseModifier = modifier
        .heightIn(min = 48.dp)
        .semantics { role = Role.Button }

    val labelContent: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = when (variant) {
                    AppButtonVariant.Primary   -> Canvas
                    AppButtonVariant.Secondary -> Amber
                    AppButtonVariant.Text      -> Amber
                },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "PLEASE WAIT",
                fontFamily    = FontFamily.Monospace,
                fontSize      = 11.sp,
                letterSpacing = 2.sp,
                fontWeight    = FontWeight.Medium,
            )
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text.uppercase(),
                fontFamily    = FontFamily.Monospace,
                fontSize      = 12.sp,
                letterSpacing = 2.5.sp,
                fontWeight    = FontWeight.Bold,
            )
        }
    }

    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick  = onClick,
                modifier = baseModifier.drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Amber.copy(alpha = glowAlpha * 0.6f), Color.Transparent)
                        )
                    )
                },
                enabled          = effectiveEnabled,
                shape            = MaterialTheme.shapes.extraSmall,
                contentPadding   = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                elevation        = ButtonDefaults.buttonElevation(0.dp),
                colors           = ButtonDefaults.buttonColors(
                    containerColor         = Amber,
                    disabledContainerColor = AmberDim.copy(alpha = 0.5f),
                    contentColor           = Canvas,
                    disabledContentColor   = Canvas.copy(alpha = 0.5f),
                ),
                content = labelContent,
            )
        }

        AppButtonVariant.Secondary -> {
            OutlinedButton(
                onClick        = onClick,
                modifier       = baseModifier.border(
                    width  = if (effectiveEnabled) 1.dp else 0.5.dp,
                    color  = if (effectiveEnabled) Amber.copy(alpha = 0.6f) else BorderMid,
                    shape  = MaterialTheme.shapes.extraSmall,
                ),
                enabled        = effectiveEnabled,
                shape          = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                border         = null,          // border drawn manually above
                colors         = ButtonDefaults.outlinedButtonColors(
                    containerColor         = Color.Transparent,
                    contentColor           = if (effectiveEnabled) Amber else TextMuted,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor   = TextMuted.copy(alpha = 0.5f),
                ),
                content = labelContent,
            )
        }

        AppButtonVariant.Text -> {
            TextButton(
                onClick        = onClick,
                modifier       = baseModifier,
                enabled        = effectiveEnabled,
                shape          = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                colors         = TextButtonDefaults.textButtonColors(
                    contentColor         = Amber,
                    disabledContentColor = TextMuted.copy(alpha = 0.5f),
                ),
                content = labelContent,
            )
        }
    }
}

// Alias so existing call-sites that import TextButtonDefaults still compile
private val TextButtonDefaults get() = ButtonDefaults
