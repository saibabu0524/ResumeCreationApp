package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Standard application scaffold that delegates to loading, error, or content states.
 *
 * Provides:
 * - [SnackbarHost] for showing snackbar messages
 * - Optional [topBar] and [bottomBar]
 * - Automatic loading / error / empty / content state switching
 * - Correct [WindowInsets] handling via [Scaffold]
 *
 * @param modifier Modifier for the scaffold.
 * @param isLoading Whether to show the loading indicator.
 * @param errorMessage When non-null, shows [ErrorState] with a retry button.
 * @param isEmpty Whether the content is empty, showing [emptyContent] if provided.
 * @param onRetry Callback for [ErrorState] retry button.
 * @param topBar Optional top app bar composable.
 * @param bottomBar Optional bottom bar composable.
 * @param snackbarHostState Snackbar host state for showing messages.
 * @param scrollBehavior Optional scroll behavior for nested scroll connection.
 * @param emptyContent Content to show when [isEmpty] is true.
 * @param content Main content with [PaddingValues] from [Scaffold].
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
        modifier = modifier.then(
            if (scrollBehavior != null) {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                Modifier
            },
        ),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                isLoading -> LoadingIndicator()
                errorMessage != null -> ErrorState(message = errorMessage, onRetry = onRetry)
                isEmpty && emptyContent != null -> emptyContent()
                else -> content(innerPadding)
            }
        }
    }
}
