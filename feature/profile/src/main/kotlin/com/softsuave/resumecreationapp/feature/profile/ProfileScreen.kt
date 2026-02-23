package com.softsuave.resumecreationapp.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.AppButton
import com.softsuave.resumecreationapp.core.ui.component.AppButtonVariant
import com.softsuave.resumecreationapp.core.ui.component.AppScaffold
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
import com.softsuave.resumecreationapp.core.ui.component.AppTopBar
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.feature.profile.R

@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateBack -> onNavigateBack()
                is ProfileUiEvent.ShowSnackbar -> { /* handled by scaffold */ }
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileUserIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    AppScaffold(
        modifier = modifier,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onRetry = { onEvent(ProfileUserIntent.Retry) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    if (!uiState.isEditing && uiState.user != null) {
                        IconButton(onClick = { onEvent(ProfileUserIntent.EditClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.profile_edit),
                            )
                        }
                    }
                },
            )
        },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ─── Avatar ──────────────────────────────────────────────
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(spacing.large))

            if (uiState.user != null) {
                if (uiState.isEditing) {
                    // ─── Edit Mode ───────────────────────────────────
                    AppTextField(
                        value = uiState.editDisplayName,
                        onValueChange = { onEvent(ProfileUserIntent.DisplayNameChanged(it)) },
                        label = stringResource(R.string.profile_display_name),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    AppButton(
                        text = stringResource(R.string.profile_save),
                        onClick = { onEvent(ProfileUserIntent.SaveClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(spacing.small))

                    AppButton(
                        text = stringResource(R.string.profile_cancel),
                        onClick = { onEvent(ProfileUserIntent.CancelEdit) },
                        variant = AppButtonVariant.Text,
                    )
                } else {
                    // ─── Display Mode ────────────────────────────────
                    Text(
                        text = uiState.user.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(spacing.small))

                    Text(
                        text = uiState.user.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (uiState.user.isEmailVerified) {
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Text(
                            text = stringResource(R.string.profile_email_verified),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}
