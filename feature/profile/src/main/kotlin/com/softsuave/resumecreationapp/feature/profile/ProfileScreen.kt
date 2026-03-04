package com.softsuave.resumecreationapp.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
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

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileUserIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onBg = MaterialTheme.colorScheme.onBackground
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "PROFILE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 3.sp,
                            color = primary.copy(alpha = 0.7f),
                        )
                        Text(
                            "Your account",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            color = onBg,
                        )
                    }
                }
                // Edit action top right
                if (!uiState.isEditing && uiState.user != null && !uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(surfaceVariant)
                            .border(0.5.dp, outline, RoundedCornerShape(2.dp))
                            .clickable { onEvent(ProfileUserIntent.EditClicked) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Edit, "Edit", tint = primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = outline.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ── Loading / Error / Content ─────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "LOADING PROFILE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 4.sp,
                            color = primary.copy(alpha = 0.5f),
                        )
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .border(0.5.dp, error.copy(0.4f), RoundedCornerShape(2.dp))
                                    .padding(16.dp),
                            ) {
                                Text(
                                    "⚠  ${uiState.errorMessage}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(0.5.dp, primary.copy(0.5f), RoundedCornerShape(2.dp))
                                    .clickable { onEvent(ProfileUserIntent.Retry) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "RETRY →",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    letterSpacing = 2.sp,
                                    color = primary,
                                )
                            }
                        }
                    }
                }
                uiState.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // ── Avatar Header Section ─────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Avatar circle
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(surfaceVariant)
                                    .border(1.dp, outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = uiState.user.displayName
                                        .take(2)
                                        .uppercase(),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primary,
                                )
                            }

                            // Display name
                            Text(
                                text = uiState.user.displayName,
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Light,
                                color = onBg,
                            )

                            // Verified badge
                            if (uiState.user.isEmailVerified) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(primary.copy(alpha = 0.1f))
                                        .border(0.5.dp, primary.copy(0.3f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.VerifiedUser, null, tint = primary, modifier = Modifier.size(10.dp))
                                    Text(
                                        "VERIFIED",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        letterSpacing = 2.sp,
                                        color = primary,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = outline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))

                        Spacer(Modifier.height(24.dp))

                        // ── Edit Mode ──────────────────────────────────────────
                        AnimatedVisibility(
                            visible = uiState.isEditing,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    "EDIT PROFILE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 3.sp,
                                    color = primary.copy(alpha = 0.6f),
                                )
                                AppTextField(
                                    value = uiState.editDisplayName,
                                    onValueChange = { onEvent(ProfileUserIntent.DisplayNameChanged(it)) },
                                    label = stringResource(R.string.profile_display_name),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Save
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(primary)
                                            .clickable { onEvent(ProfileUserIntent.SaveClicked) }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                            Text(
                                                "SAVE",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                letterSpacing = 2.sp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    // Cancel
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .border(0.5.dp, outline, RoundedCornerShape(2.dp))
                                            .clickable { onEvent(ProfileUserIntent.CancelEdit) }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            Text(
                                                "CANCEL",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                letterSpacing = 2.sp,
                                                color = onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = outline.copy(0.3f), thickness = 0.5.dp)
                            }
                        }

                        // ── Info Cards ────────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            ProfileInfoRow(
                                icon = { Icon(Icons.Default.Person, null, tint = primary, modifier = Modifier.size(14.dp)) },
                                label = "DISPLAY NAME",
                                value = uiState.user.displayName,
                            )
                            HorizontalDivider(color = outline.copy(0.2f), thickness = 0.5.dp)
                            ProfileInfoRow(
                                icon = { Icon(Icons.Default.Email, null, tint = primary, modifier = Modifier.size(14.dp)) },
                                label = "EMAIL",
                                value = uiState.user.email,
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Column {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                letterSpacing = 2.sp,
                color = onSurfaceVariant,
            )
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
