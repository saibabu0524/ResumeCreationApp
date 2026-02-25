package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

/**
 * Standard application scaffold — theme-aware.
 *
 * Delegates to [LoadingIndicator], [ErrorState], or [content] based on state.
 * Background and snackbar colors follow [MaterialTheme.colorScheme].
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
    val bg      = MaterialTheme.colorScheme.background
    val onBg    = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier
            .background(bg)
            .then(
                if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                else Modifier,
            ),
        topBar         = topBar,
        bottomBar      = bottomBar,
        containerColor = bg,
        contentColor   = onBg,
        snackbarHost   = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape           = MaterialTheme.shapes.small,
                    containerColor  = surface,
                    contentColor    = onBg,
                    actionColor     = primary,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bg),
        ) {
            when {
                isLoading                       -> LoadingIndicator()
                errorMessage != null            -> ErrorState(message = errorMessage, onRetry = onRetry)
                isEmpty && emptyContent != null -> emptyContent()
                else                            -> content(innerPadding)
            }
        }
    }
}
