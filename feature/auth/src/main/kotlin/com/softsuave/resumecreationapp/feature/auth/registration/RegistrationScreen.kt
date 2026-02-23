package com.softsuave.resumecreationapp.feature.auth.registration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.AppButton
import com.softsuave.resumecreationapp.core.ui.component.AppButtonVariant
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.feature.auth.R

@Composable
fun RegistrationRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RegistrationUiEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                is RegistrationUiEvent.NavigateToLogin -> onNavigateToLogin()
                is RegistrationUiEvent.ShowSnackbar -> { /* handled by parent */ }
            }
        }
    }

    RegistrationScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun RegistrationScreen(
    uiState: RegistrationUiState,
    onEvent: (RegistrationUserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.large, vertical = spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_register_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = stringResource(R.string.auth_register_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        AppTextField(
            value = uiState.displayName,
            onValueChange = { onEvent(RegistrationUserIntent.DisplayNameChanged(it)) },
            label = stringResource(R.string.auth_display_name_label),
            errorMessage = uiState.displayNameError,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        AppTextField(
            value = uiState.email,
            onValueChange = { onEvent(RegistrationUserIntent.EmailChanged(it)) },
            label = stringResource(R.string.auth_email_label),
            errorMessage = uiState.emailError,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        AppTextField(
            value = uiState.password,
            onValueChange = { onEvent(RegistrationUserIntent.PasswordChanged(it)) },
            label = stringResource(R.string.auth_password_label),
            errorMessage = uiState.passwordError,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        AppTextField(
            value = uiState.confirmPassword,
            onValueChange = { onEvent(RegistrationUserIntent.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.auth_confirm_password_label),
            errorMessage = uiState.confirmPasswordError,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.large))

        AppButton(
            text = stringResource(R.string.auth_register_button),
            onClick = { onEvent(RegistrationUserIntent.RegisterClicked) },
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        AppButton(
            text = stringResource(R.string.auth_login_prompt),
            onClick = { onEvent(RegistrationUserIntent.LoginClicked) },
            variant = AppButtonVariant.Text,
        )
    }
}
