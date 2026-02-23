package com.softsuave.resumecreationapp.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.ui.component.AppScaffold
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
import com.softsuave.resumecreationapp.core.ui.component.AppTopBar
import com.softsuave.resumecreationapp.core.ui.component.EmptyState
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.feature.home.R

@Composable
fun HomeRoute(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.NavigateToProfile -> onNavigateToProfile(event.userId)
                is HomeUiEvent.ShowSnackbar -> { /* handled by scaffold */ }
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUserIntent) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    AppScaffold(
        modifier = modifier,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        isEmpty = uiState.users.isEmpty() && !uiState.isLoading && uiState.errorMessage == null,
        onRetry = { onEvent(HomeUserIntent.Retry) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_title),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                        )
                    }
                },
            )
        },
        emptyContent = {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                icon = Icons.Default.Person,
                actionText = stringResource(R.string.home_empty_action),
                onAction = { onEvent(HomeUserIntent.Retry) },
            )
        },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            // ─── Search Bar ──────────────────────────────────────────
            item {
                AppTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onEvent(HomeUserIntent.SearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.home_search_placeholder),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                )
            }

            // ─── User List ───────────────────────────────────────────
            items(
                items = uiState.users,
                key = { it.id },
            ) { user ->
                UserCard(
                    user = user,
                    onClick = { onEvent(HomeUserIntent.UserClicked(user.id)) },
                    modifier = Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.extraSmall,
                    ),
                )
            }
        }
    }
}

/**
 * User card composable with proper semantic role and touch target.
 */
@Composable
private fun UserCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "RTL Mode")
@Composable
private fun HomeScreenRtlPreview() {
    val fakeState = HomeUiState(
        users = listOf(
            User("1", "ali@example.com", "Ali", null, true, 0L, 0L),
            User("2", "omar@example.com", "Omar", null, true, 0L, 0L)
        )
    )
    com.softsuave.resumecreationapp.core.ui.theme.AppTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            HomeScreen(
                uiState = fakeState,
                onEvent = {},
                onNavigateToSettings = {}
            )
        }
    }
}
