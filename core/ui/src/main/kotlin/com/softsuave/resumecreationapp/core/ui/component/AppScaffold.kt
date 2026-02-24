package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

// ── Local tokens ─────────────────────────────────────────────────────────────
private val Canvas   = Color(0xFF0E0D0B)
private val Surface0 = Color(0xFF1A1814)
private val Amber    = Color(0xFFD4A853)

/**
 * Standard application scaffold — dark editorial.
 *
 * Delegates to [LoadingIndicator], [ErrorState], or [content] based on state.
 * The Scaffold background is always [Canvas] to maintain the dark foundation.
 *
 * The snackbar is styled with [Surface0] container and [Amber] action text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isEmpty: Boolean = false,
    onRetry: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scrollBehavior: TopAppBarScrollBehavior? = null,
    emptyContent: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier
            .background(Canvas)
            .then(
                if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                else Modifier,
            ),
        topBar    = topBar,
        bottomBar = bottomBar,
        containerColor = Canvas,
        contentColor   = Color(0xFFF0EAD6),
        snackbarHost  = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape           = MaterialTheme.shapes.small,
                    containerColor  = Surface0,
                    contentColor    = Color(0xFFF0EAD6),
                    actionColor     = Amber,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Canvas),
        ) {
            when {
                isLoading                           -> LoadingIndicator()
                errorMessage != null                -> ErrorState(message = errorMessage, onRetry = onRetry)
                isEmpty && emptyContent != null     -> emptyContent()
                else                                -> content(innerPadding)
            }
        }
    }
}
