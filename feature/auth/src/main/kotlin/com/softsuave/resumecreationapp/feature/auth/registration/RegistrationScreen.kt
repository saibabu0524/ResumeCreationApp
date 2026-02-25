package com.softsuave.resumecreationapp.feature.auth.registration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
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
                is RegistrationUiEvent.ShowSnackbar -> {}
            }
        }
    }
    RegistrationScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun RegistrationScreen(
    uiState: RegistrationUiState,
    onEvent: (RegistrationUserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    // Theme tokens
    val bg           = MaterialTheme.colorScheme.background
    val onBg         = MaterialTheme.colorScheme.onBackground
    val primary      = MaterialTheme.colorScheme.primary
    val onPrimary    = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface      = MaterialTheme.colorScheme.surfaceVariant
    val outline      = MaterialTheme.colorScheme.outline
    val error        = MaterialTheme.colorScheme.error

    // Password strength calculation
    val strength = remember(uiState.password) {
        when {
            uiState.password.length >= 12 &&
                uiState.password.any { it.isUpperCase() } &&
                uiState.password.any { it.isDigit() } &&
                uiState.password.any { !it.isLetterOrDigit() } -> 4
            uiState.password.length >= 8 &&
                uiState.password.any { it.isUpperCase() } &&
                uiState.password.any { it.isDigit() } -> 3
            uiState.password.length >= 6 -> 2
            uiState.password.isNotEmpty() -> 1
            else -> 0
        }
    }

    val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "g"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.TopCenter
    ) {
        // Ambient bottom glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.05f + glowAnim * 0.03f), Color.Transparent),
                        radius = 500f
                    )
                )
        )

        // Decorative vertical line
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(1.dp)
                .padding(start = 16.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, primary.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 60 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                // ── Header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vertical accent bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(72.dp)
                            .background(
                                Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.4f)))
                            )
                    )
                    Column {
                        Text(
                            "CREATE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 4.sp,
                            color = primary.copy(alpha = 0.7f)
                        )
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = onBg, fontWeight = FontWeight.Light)) {
                                    append("Your ")
                                }
                                withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                                    append("Account")
                                }
                            },
                            fontFamily = FontFamily.Serif,
                            fontSize = 30.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Join thousands of professionals landing their dream roles",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )

                Spacer(Modifier.height(32.dp))

                // ── Form ─────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, outline, RoundedCornerShape(4.dp))
                        .background(surface)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Display Name
                    AppTextField(
                        value = uiState.displayName,
                        onValueChange = { onEvent(RegistrationUserIntent.DisplayNameChanged(it)) },
                        label = "DISPLAY NAME",
                        placeholder = "John Doe",
                        errorMessage = uiState.displayNameError,
                        leadingIcon = {
                            Icon(Icons.Default.Person, null,
                                tint = if (uiState.displayNameError != null) error else onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    HorizontalDivider(color = outline, thickness = 0.5.dp)

                    // Email
                    AppTextField(
                        value = uiState.email,
                        onValueChange = { onEvent(RegistrationUserIntent.EmailChanged(it)) },
                        label = "EMAIL ADDRESS",
                        placeholder = "you@example.com",
                        errorMessage = uiState.emailError,
                        leadingIcon = {
                            Icon(Icons.Default.Email, null,
                                tint = if (uiState.emailError != null) error else onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    HorizontalDivider(color = outline, thickness = 0.5.dp)

                    // Password
                    AppTextField(
                        value = uiState.password,
                        onValueChange = { onEvent(RegistrationUserIntent.PasswordChanged(it)) },
                        label = "PASSWORD",
                        placeholder = "Min. 8 characters",
                        errorMessage = uiState.passwordError,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null,
                                tint = if (uiState.passwordError != null) error else onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = onSurfaceVariant, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                    )

                    // Password strength bar
                    AnimatedVisibility(visible = uiState.password.isNotEmpty()) {
                        PasswordStrengthBar(strength = strength, outline = outline)
                    }

                    HorizontalDivider(color = outline, thickness = 0.5.dp)

                    // Confirm Password
                    AppTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { onEvent(RegistrationUserIntent.ConfirmPasswordChanged(it)) },
                        label = "CONFIRM PASSWORD",
                        placeholder = "Repeat password",
                        errorMessage = uiState.confirmPasswordError,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null,
                                tint = if (uiState.confirmPasswordError != null) error else onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = onSurfaceVariant, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Register Button ──────────────────────────────────────────
                val btnGlow by rememberInfiniteTransition(label = "btnGlow").animateFloat(
                    initialValue = 0.3f, targetValue = 0.8f,
                    animationSpec = if (uiState.isLoading)
                        infiniteRepeatable(tween(700), RepeatMode.Reverse)
                    else
                        infiniteRepeatable(tween(3000), RepeatMode.Reverse),
                    label = "bg"
                )
                Button(
                    onClick = { onEvent(RegistrationUserIntent.RegisterClicked) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .drawBehind {
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(primary.copy(alpha = btnGlow * 0.12f), Color.Transparent)
                                )
                            )
                        },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        disabledContainerColor = primary.copy(alpha = 0.4f),
                        contentColor = onPrimary,
                        disabledContentColor = onPrimary.copy(alpha = 0.6f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (uiState.isLoading) "CREATING ACCOUNT..." else "CREATE ACCOUNT →",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Login Link ───────────────────────────────────────────────
                TextButton(onClick = { onEvent(RegistrationUserIntent.LoginClicked) }) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = onSurfaceVariant, fontSize = 12.sp)) { append("Already have an account? ") }
                            withStyle(SpanStyle(color = primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) { append("Sign in →") }
                        },
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(strength: Int, outline: Color) {
    val labels = listOf("Weak", "Fair", "Good", "Strong")
    val colors = listOf(
        Color(0xFFB04A3A),
        Color(0xFFC08030),
        Color(0xFF7A9A50),
        Color(0xFF4A9A6A)
    )
    val idx = (strength - 1).coerceIn(0, 3)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { i ->
                val filled = i < strength
                val color by animateColorAsState(
                    targetValue = if (filled) colors[idx] else outline,
                    animationSpec = tween(300),
                    label = "barC$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
        AnimatedVisibility(visible = strength > 0) {
            Text(
                if (strength > 0) labels[idx] else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors[idx],
                letterSpacing = 1.sp
            )
        }
    }
}
