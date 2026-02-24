package com.softsuave.resumecreationapp.feature.resume.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softsuave.resumecreationapp.feature.resume.ResumeUiState
import com.softsuave.resumecreationapp.feature.resume.ResumeViewModel

// Elegant Theme Palette
private val LuxBg = Color(0xFFFAF9F6)
private val LuxTextPrimary = Color(0xFF111111)
private val LuxTextSecondary = Color(0xFF767676)
private val LuxAccent = Color(0xFFB59A70)
private val LuxBorder = Color(0xFFE0E0E0)
private val LuxSurface = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ResumeViewModel = hiltViewModel(),
    onNavigateToResult: (ByteArray) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedPdfUri by remember<MutableState<Uri?>> { mutableStateOf(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("gemini") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
        selectedPdfName = uri?.lastPathSegment?.substringAfterLast("/") ?: "DOCUMENT.PDF"
    }

    LaunchedEffect(uiState) {
        if (uiState is ResumeUiState.Success) {
            onNavigateToResult((uiState as ResumeUiState.Success).pdfBytes)
            viewModel.reset()
        }
    }

    val isLoading = uiState is ResumeUiState.Loading
    val isError = uiState is ResumeUiState.Error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Elegant Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
                Text(
                    text = "RESUME",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 4.sp,
                    color = LuxAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tailor.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Normal,
                    color = LuxTextPrimary,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = LuxTextPrimary, thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AI-powered curation for the modern professional. Upload your existing document, define the role, and let the algorithm sculpt perfection.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = LuxTextSecondary
                )
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                
                // STEP 1
                Column {
                    ElegantSectionHeader(number = "01", title = "The Document")
                    ElegantUploadCard(
                        hasFile = selectedPdfUri != null,
                        fileName = selectedPdfName.uppercase(),
                        onClick = { pdfPickerLauncher.launch("application/pdf") }
                    )
                }

                // STEP 2
                Column {
                    ElegantSectionHeader(number = "02", title = "The Role")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LuxSurface,
                        border = BorderStroke(0.5.dp, LuxBorder),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        BasicTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = LuxTextPrimary
                            ),
                            cursorBrush = SolidColor(LuxAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp)
                                .padding(20.dp),
                            decorationBox = { innerTextField ->
                                if (jobDescription.isEmpty()) {
                                    Text(
                                        text = "Paste the complete job description here...",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 15.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = LuxTextSecondary.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // STEP 3
                Column {
                    ElegantSectionHeader(number = "03", title = "The Engine")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ElegantProviderCard(
                            label = "GEMINI",
                            subtitle = "Cloud API",
                            selected = selectedProvider == "gemini",
                            onClick = { selectedProvider = "gemini" },
                            modifier = Modifier.weight(1f)
                        )
                        ElegantProviderCard(
                            label = "OLLAMA",
                            subtitle = "Local Neural",
                            selected = selectedProvider == "ollama",
                            onClick = { selectedProvider = "ollama" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Error State
                AnimatedVisibility(
                    visible = isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (isError) {
                        Text(
                            text = (uiState as ResumeUiState.Error).message.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                val canSubmit = selectedPdfUri != null && jobDescription.isNotBlank()
                Button(
                    onClick = {
                        selectedPdfUri?.let { uri ->
                            viewModel.tailorResume(uri, jobDescription, selectedProvider)
                        }
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxTextPrimary,
                        disabledContainerColor = LuxBorder,
                        contentColor = LuxSurface,
                        disabledContentColor = LuxTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text(
                        text = "COMMENCE TAILORING",
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(800)),
            exit = fadeOut(tween(800))
        ) {
            ElegantLoadingOverlay(
                message = if (isLoading) (uiState as ResumeUiState.Loading).stepMessage else ""
            )
        }
    }
}

@Composable
private fun ElegantSectionHeader(number: String, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$number.",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = LuxAccent,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp,
            color = LuxTextPrimary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        HorizontalDivider(color = LuxBorder, thickness = 0.5.dp)
    }
}

@Composable
private fun ElegantUploadCard(
    hasFile: Boolean,
    fileName: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(if (hasFile) LuxAccent.copy(alpha = 0.05f) else LuxSurface, tween(600))
    val borderColor by animateColorAsState(if (hasFile) LuxAccent else LuxBorder, tween(600))
    val iconColor by animateColorAsState(if (hasFile) LuxAccent else LuxTextSecondary.copy(alpha = 0.3f), tween(600))

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (hasFile) Icons.Default.CheckCircle else Icons.Default.Description,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (hasFile) fileName else "SELECT RESUME DOCUMENT",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (hasFile) LuxTextPrimary else LuxTextSecondary
            )
        }
    }
}

@Composable
private fun ElegantProviderCard(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(if (selected) LuxTextPrimary else LuxSurface, tween(400))
    val contentColor by animateColorAsState(if (selected) LuxSurface else LuxTextPrimary, tween(400))
    val subtitleColor by animateColorAsState(if (selected) LuxSurface.copy(alpha = 0.6f) else LuxTextSecondary, tween(400))

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(0.5.dp, if (selected) LuxTextPrimary else LuxBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun ElegantLoadingOverlay(message: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxBg.copy(alpha = 0.95f))
            .pointerInput(Unit) {}, // Consume clicks
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CRAFTING",
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                letterSpacing = 12.sp,
                color = LuxTextPrimary,
                modifier = Modifier.alpha(alpha)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(80.dp)
                    .background(LuxAccent.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = message.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = LuxTextSecondary
            )
        }
    }
}
