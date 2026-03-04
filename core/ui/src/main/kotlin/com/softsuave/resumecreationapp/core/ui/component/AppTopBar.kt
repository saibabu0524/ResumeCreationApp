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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softsuave.resumecreationapp.core.ui.R

/**
 * Application top app bar — theme-aware editorial style.
 *
 * - Monospace title with amber/primary tint
 * - Hairline bottom border derived from [MaterialTheme.colorScheme.outline]
 * - Back arrow tinted to [onSurfaceVariant] — unobtrusive but clear
 * - Background follows the theme surface colour
 *
 * @param title         Primary title (Monospace caps).
 * @param subtitle      Optional secondary line.
 * @param onNavigateBack When non-null, a back arrow is shown.
 * @param actions       Trailing action icons / buttons slot.
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
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .drawBehind {
                // Hairline bottom border
                drawLine(
                    color       = outline.copy(alpha = 0.5f),
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
                        tint               = onSurfaceVariant,
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
                    color         = primary.copy(alpha = 0.8f),
                )
                if (subtitle != null) {
                    Text(
                        text          = subtitle,
                        fontFamily    = FontFamily.Monospace,
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp,
                        color         = onSurfaceVariant.copy(alpha = 0.6f),
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
