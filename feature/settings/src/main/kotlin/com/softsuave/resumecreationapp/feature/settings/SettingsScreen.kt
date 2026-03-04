package com.softsuave.resumecreationapp.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.ThemeMode
import com.softsuave.resumecreationapp.core.ui.component.ThemeSwitcherButton
import com.softsuave.resumecreationapp.feature.settings.R

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsUiEvent.NavigateBack -> onNavigateBack()
                is SettingsUiEvent.NavigateToLogin -> onNavigateToLogin()
                is SettingsUiEvent.ShowSnackbar -> { /* handled by scaffold */ }
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUserIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

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
                            "PREFERENCES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 3.sp,
                            color = primary.copy(alpha = 0.7f),
                        )
                        Text(
                            "Settings",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            color = onBg,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(surfaceVariant)
                        .border(0.5.dp, outline, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Settings, null, tint = primary, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = outline.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ── Scrollable Content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // ─── Theme Section ────────────────────────────────────────────
                SettingsSectionHeader(
                    label = "APPEARANCE",
                    text = stringResource(R.string.settings_theme_section),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaceVariant)
                        .border(0.5.dp, outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    val modes = listOf(
                        Triple(ThemeMode.Light, stringResource(R.string.settings_theme_light), Icons.Default.Brightness7),
                        Triple(ThemeMode.Dark, stringResource(R.string.settings_theme_dark), Icons.Default.Brightness4),
                        Triple(ThemeMode.System, stringResource(R.string.settings_theme_system), Icons.Default.PhoneAndroid),
                    )
                    modes.forEachIndexed { i, (mode, label, icon) ->
                        ThemeModeRow(
                            icon = icon,
                            label = label,
                            selected = uiState.themeMode == mode,
                            onClick = { onEvent(SettingsUserIntent.ThemeModeChanged(mode)) },
                        )
                        if (i < modes.lastIndex) {
                            HorizontalDivider(color = outline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }

                // ─── Notifications Section ────────────────────────────────────
                SettingsSectionHeader(
                    label = "COMMUNICATION",
                    text = stringResource(R.string.settings_notifications_section),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaceVariant)
                        .border(0.5.dp, outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        tint = primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_push_notifications),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.settings_push_notifications_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = onSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { onEvent(SettingsUserIntent.NotificationsToggled(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.3f),
                        ),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ─── Account / Logout Section ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { onEvent(SettingsUserIntent.LogoutClicked) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "SIGN OUT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(label: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ThemeModeRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            null,
            tint = if (selected) primary else onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (selected) primary else onSurface,
            modifier = Modifier.weight(1f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        if (selected) {
            Icon(Icons.Default.Check, "Selected", tint = primary, modifier = Modifier.size(16.dp))
        }
    }
}
