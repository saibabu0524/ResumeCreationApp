package com.softsuave.resumecreationapp.feature.auth.login

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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.softsuave.resumecreationapp.core.ui.component.AppButton
import com.softsuave.resumecreationapp.core.ui.component.AppButtonVariant
import com.softsuave.resumecreationapp.core.ui.component.AppTextField
import com.softsuave.resumecreationapp.core.ui.theme.Amber80
import com.softsuave.resumecreationapp.feature.auth.R

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
                is LoginUiEvent.ShowSnackbar -> {}
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

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    // Theme tokens
    val bg         = MaterialTheme.colorScheme.background
    val onBg       = MaterialTheme.colorScheme.onBackground
    val primary    = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface    = MaterialTheme.colorScheme.surfaceVariant
    val outline    = MaterialTheme.colorScheme.outline
    val error      = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    // Animated ambient glow
    val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        // Ambient top glow (subtle primary colour)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.06f + glowAnim * 0.04f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
        )

        // Decorative corner lines
        DecorativeCornerLines(accentColor = primary)

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700, easing = FastOutSlowInEasing)) { 80 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(64.dp))

                // ── Logo Mark ────────────────────────────────────────────────
                LogoMark(bg = bg, primary = primary)

                Spacer(Modifier.height(40.dp))

                // ── Heading ──────────────────────────────────────────────────
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = onBg, fontWeight = FontWeight.Light)) {
                            append("Sign into\n")
                        }
                        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append("Your Account")
                        }
                    },
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Continue tailoring your career story",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = onSurfaceVariant,
                )

                Spacer(Modifier.height(40.dp))

                // ── Form Card ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, outline, RoundedCornerShape(4.dp))
                        .background(surface)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email field
                    AppTextField(
                        value = uiState.email,
                        onValueChange = onEmailChanged,
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

                    // Password field
                    AppTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChanged,
                        label = "PASSWORD",
                        placeholder = "••••••••",
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    // General error
                    AnimatedVisibility(
                        visible = uiState.generalError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        uiState.generalError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(errorContainer)
                                    .border(0.5.dp, error.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    err,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = onErrorContainer,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Login Button ─────────────────────────────────────────────
                AppButton(
                    text = if (uiState.isLoading) "SIGNING IN..." else "SIGN IN",
                    onClick = onLoginClicked,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ── Register Link ─────────────────────────────────────────────
                TextButton(onClick = onRegisterClicked) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = onSurfaceVariant, fontSize = 12.sp)) {
                                append("New here? ")
                            }
                            withStyle(SpanStyle(color = primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                                append("Create an account →")
                            }
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

// ─────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────

@Composable
private fun LogoMark(bg: Color, primary: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .drawBehind {
                // Outer primary ring
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 1f)
                )
                // Inner fill (blends with bg)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(0.08f), bg)
                    ),
                    radius = size.minDimension / 2 - 4
                )
                // Accent arc
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "RT",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = primary,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun DecorativeCornerLines(accentColor: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner bracket
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .drawBehind {
                    val c = accentColor.copy(alpha = 0.3f)
                    drawLine(c, Offset(0f, 0f), Offset(30f, 0f), strokeWidth = 1f)
                    drawLine(c, Offset(0f, 0f), Offset(0f, 30f), strokeWidth = 1f)
                }
        )
        // Bottom-right corner bracket
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .drawBehind {
                    val c = accentColor.copy(alpha = 0.3f)
                    drawLine(c, Offset(size.width, size.height), Offset(size.width - 30f, size.height), strokeWidth = 1f)
                    drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - 30f), strokeWidth = 1f)
                }
        )
    }
}
