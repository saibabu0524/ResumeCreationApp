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
import androidx.compose.ui.draw.alpha
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
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.core.ui.theme.*
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

    // Animated ambient glow
    val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas),
        contentAlignment = Alignment.Center
    ) {
        // Ambient top glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Amber.copy(alpha = 0.06f + glowAnim * 0.04f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
        )

        // Decorative corner lines
        DecorativeCornerLines()

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
                LogoMark()

                Spacer(Modifier.height(40.dp))

                // ── Heading ──────────────────────────────────────────────────
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Light)) {
                            append("Sign into\n")
                        }
                        withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
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
                    color = TextMuted,
                )

                Spacer(Modifier.height(40.dp))

                // ── Form Card ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 0.5.dp,
                            brush = Brush.verticalGradient(listOf(BorderMid, Border)),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(Surface)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email field
                    EditorialTextField(
                        value = uiState.email,
                        onValueChange = onEmailChanged,
                        label = "EMAIL ADDRESS",
                        placeholder = "you@example.com",
                        error = uiState.emailError,
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = if (uiState.emailError != null) ErrorRed else TextMuted, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    // Password field
                    EditorialTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChanged,
                        label = "PASSWORD",
                        placeholder = "••••••••",
                        error = uiState.passwordError,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = if (uiState.passwordError != null) ErrorRed else TextMuted, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextMuted, modifier = Modifier.size(18.dp)
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
                                    .background(ErrorDim)
                                    .border(0.5.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    err,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ErrorRed,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Login Button ─────────────────────────────────────────────
                EditorialButton(
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
                            withStyle(SpanStyle(color = TextMuted, fontSize = 12.sp)) {
                                append("New here? ")
                            }
                            withStyle(SpanStyle(color = AmberGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
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
private fun LogoMark() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .drawBehind {
                // Outer amber ring
                drawCircle(
                    color = Amber.copy(alpha = 0.25f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 1f)
                )
                // Inner fill
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2A2218), Color(0xFF1A1410))
                    ),
                    radius = size.minDimension / 2 - 4
                )
                // Accent arc
                drawArc(
                    color = Amber,
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
            color = Amber,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun DecorativeCornerLines() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner bracket
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .drawBehind {
                    val c = Amber.copy(alpha = 0.3f)
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
                    val c = Amber.copy(alpha = 0.3f)
                    drawLine(c, Offset(size.width, size.height), Offset(size.width - 30f, size.height), strokeWidth = 1f)
                    drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - 30f), strokeWidth = 1f)
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val hasError = error != null
    val borderColor = when {
        hasError -> ErrorRed
        value.isNotEmpty() -> Amber.copy(alpha = 0.6f)
        else -> BorderMid
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = if (hasError) ErrorRed else TextMuted
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextMuted.copy(alpha = 0.5f))
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            isError = hasError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(2.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Amber,
                focusedBorderColor = Amber,
                unfocusedBorderColor = borderColor,
                errorBorderColor = ErrorRed,
                errorCursorColor = ErrorRed,
                focusedLeadingIconColor = Amber,
                unfocusedLeadingIconColor = TextMuted,
                focusedTrailingIconColor = TextMuted,
                unfocusedTrailingIconColor = TextMuted,
            ),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TextPrimary
            )
        )
        AnimatedVisibility(visible = hasError) {
            error?.let {
                Text(
                    "⚠ $it",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = ErrorRed,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun EditorialButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val glowAlpha by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = if (isLoading) infiniteRepeatable(tween(800), RepeatMode.Reverse)
        else infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "g"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(56.dp)
            .drawBehind {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Amber.copy(alpha = glowAlpha * 0.15f), Color.Transparent)
                    )
                )
            },
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            disabledContainerColor = AmberDim,
            contentColor = Canvas,
            disabledContentColor = Canvas.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Canvas,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
