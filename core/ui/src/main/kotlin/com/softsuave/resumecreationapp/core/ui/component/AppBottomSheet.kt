package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Editorial modal bottom sheet.
 *
 * - Theme-aware container
 * - Primary drag handle for brand consistency
 * - Square top corners (4 dp) matching the app's aesthetic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val scrim = MaterialTheme.colorScheme.scrim

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        modifier          = modifier.testTag("app_bottom_sheet"),
        sheetState        = sheetState,
        shape             = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        containerColor    = surface,
        contentColor      = onSurface,
        tonalElevation    = 0.dp,
        scrimColor        = scrim,
        dragHandle        = {
            // Custom primary drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(
                            color = primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50),
                        )
                )
            }
        },
        content           = content,
    )
}
