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

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Surface0  = Color(0xFF1A1814)
private val Surface1  = Color(0xFF242019)
private val Amber     = Color(0xFFD4A853)
private val BorderSub = Color(0xFF2E2A24)

/**
 * Dark editorial modal bottom sheet.
 *
 * - Warm near-black [Surface0] container
 * - Amber drag handle for brand consistency
 * - Hairline top border for structural definition
 * - Square top corners (2 dp) matching the app's sharp aesthetic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        modifier          = modifier.testTag("app_bottom_sheet"),
        sheetState        = sheetState,
        shape             = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        containerColor    = Surface0,
        contentColor      = Color(0xFFF0EAD6),
        tonalElevation    = 0.dp,
        scrimColor        = Color(0xCC0E0D0B),
        dragHandle        = {
            // Custom amber drag handle
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
                            color = Amber.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50),
                        )
                )
            }
        },
        content           = content,
    )
}
