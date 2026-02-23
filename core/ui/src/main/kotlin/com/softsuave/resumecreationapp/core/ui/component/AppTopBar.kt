package com.softsuave.resumecreationapp.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.softsuave.resumecreationapp.core.ui.R

/**
 * Standard application top app bar with configurable back navigation and actions.
 *
 * - Uses [CenterAlignedTopAppBar] for a modern centered-title look.
 * - Back/up navigation button shown when [onNavigateBack] is provided.
 * - Supports optional [actions] slot for toolbar icons / menus.
 * - Integrates with [TopAppBarScrollBehavior] for collapsing toolbar effects.
 *
 * @param title The title displayed in the app bar.
 * @param modifier Modifier for the app bar.
 * @param onNavigateBack Optional callback; when non-null, a back arrow is shown.
 * @param actions Optional actions displayed on the trailing side.
 * @param scrollBehavior Optional scroll behavior for collapsing toolbar effects.
 * @param colors Custom colors — defaults to transparent surface style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    ),
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.core_ui_navigate_back),
                    )
                }
            }
        },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        colors = colors,
    )
}
