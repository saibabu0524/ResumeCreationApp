package com.softsuave.resumecreationapp.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.AppButton
import com.softsuave.resumecreationapp.core.ui.component.AppButtonVariant
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.feature.auth.R

/**
 * Route-level composable that connects [LoginViewModel] to [LoginScreen].
 *
 * - Collects [LoginUiState] via [collectAsStateWithLifecycle]
 * - Processes one-time events from [LoginViewModel.uiEvent]
 * - Delegates UI to stateless [LoginScreen]
 */
@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LoginUiEvent.NavigateToHome -> onNavigateToHome()
                is LoginUiEvent.NavigateToRegistration -> onNavigateToRegistration()
                is LoginUiEvent.ShowSnackbar -> { /* handled by parent scaffold */ }
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onEmailChanged = { viewModel.onEvent(LoginUserIntent.EmailChanged(it)) },
        onPasswordChanged = { viewModel.onEvent(LoginUserIntent.PasswordChanged(it)) },
        onLoginClicked = { viewModel.onEvent(LoginUserIntent.LoginClicked) },
        onRegisterClicked = { viewModel.onEvent(LoginUserIntent.RegisterClicked) },
    )
}

/**
 * Stateless Login screen composable.
 *
 * All state is hoisted via parameters. Preview-friendly.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.large, vertical = spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ─── Title ───────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.auth_login_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = stringResource(R.string.auth_login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // ─── Email ───────────────────────────────────────────────────
        AppTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = stringResource(R.string.auth_email_label),
            placeholder = stringResource(R.string.auth_email_placeholder),
            errorMessage = uiState.emailError,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // ─── Password ────────────────────────────────────────────────
        AppTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.auth_password_label),
            placeholder = stringResource(R.string.auth_password_placeholder),
            errorMessage = uiState.passwordError,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.auth_hide_password
                            else R.string.auth_show_password,
                        ),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // ─── General Error ───────────────────────────────────────────
        if (uiState.generalError != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = uiState.generalError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        // ─── Login Button ────────────────────────────────────────────
        AppButton(
            text = stringResource(R.string.auth_login_button),
            onClick = onLoginClicked,
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // ─── Register Button ─────────────────────────────────────────
        AppButton(
            text = stringResource(R.string.auth_register_prompt),
            onClick = onRegisterClicked,
            variant = AppButtonVariant.Text,
        )
    }
}
