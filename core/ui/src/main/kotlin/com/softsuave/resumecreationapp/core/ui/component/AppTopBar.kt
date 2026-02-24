package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softsuave.resumecreationapp.core.ui.R

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas    = Color(0xFF0E0D0B)
private val Amber     = Color(0xFFD4A853)
private val TextPri   = Color(0xFFF0EAD6)
private val TextMuted = Color(0xFF9A8E78)
private val Border    = Color(0xFF2E2A24)

/**
 * Application top app bar — dark editorial style.
 *
 * - Monospace eyebrow + Serif title pairing (pass via [title] and [subtitle])
 * - Hairline bottom border for structural definition
 * - Back arrow tinted to [TextMuted] — unobtrusive but clear
 * - Transparent background so screen canvas shows through edge-to-edge
 *
 * @param title         Primary title (rendered in Monospace uppercase for short labels,
 *                      Serif for longer screen titles — caller decides which to pass).
 * @param modifier      Modifier for the bar.
 * @param subtitle      Optional secondary line rendered in Monospace at smaller size.
 * @param onNavigateBack When non-null, a back arrow is shown.
 * @param actions       Trailing action icons / buttons slot.
 * @param scrollBehavior Collapsing toolbar scroll behaviour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Canvas)
            .statusBarsPadding()
            .drawBehind {
                // Hairline bottom border
                drawLine(
                    color       = Border,
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Back navigation
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.core_ui_navigate_back),
                        tint               = TextMuted,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }

            // Title block — centred
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text          = title,
                    fontFamily    = FontFamily.Monospace,
                    fontSize      = 11.sp,
                    letterSpacing = 2.5.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = Amber.copy(alpha = 0.8f),
                )
                if (subtitle != null) {
                    Text(
                        text          = subtitle,
                        fontFamily    = FontFamily.Monospace,
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp,
                        color         = TextMuted.copy(alpha = 0.6f),
                    )
                }
            }

            // Actions slot (keeps width symmetric with back arrow box)
            Box(
                modifier        = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                actions()
            }
        }
    }
}
