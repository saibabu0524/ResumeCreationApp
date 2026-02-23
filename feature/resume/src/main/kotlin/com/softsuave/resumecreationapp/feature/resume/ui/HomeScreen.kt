package com.softsuave.resumecreationapp.feature.resume.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softsuave.resumecreationapp.feature.resume.ResumeUiState
import com.softsuave.resumecreationapp.feature.resume.ResumeViewModel
import kotlin.math.cos
import kotlin.math.sin

// ──────────────────────────────────────────────
// Animated loading overlay composable
// ──────────────────────────────────────────────
@Composable
private fun LoadingOverlay(message: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Outer ring rotation
    val outerRotation by rememberInfiniteTransition(label = "outer").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "outerRot"
    )
    // Inner ring counter-rotation
    val innerRotation by rememberInfiniteTransition(label = "inner").animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "innerRot"
    )
    // Pulsing center dot
    val centerScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "centerScale"
    )
    // Orbiting dots
    val orbitAngle by rememberInfiniteTransition(label = "orbit").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "orbit"
    )
    // Text alpha pulse
    val textAlpha by rememberInfiniteTransition(label = "textPulse").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Custom spinner
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)

                    // Outer arc ring
                    rotate(outerRotation, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0f),
                                    primaryColor.copy(alpha = 0.5f),
                                    primaryColor
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Inner arc ring
                    rotate(innerRotation, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = 0f),
                                    secondaryColor.copy(alpha = 0.5f),
                                    secondaryColor
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 200f,
                            useCenter = false,
                            topLeft = Offset(20.dp.toPx(), 20.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - 40.dp.toPx(),
                                size.height - 40.dp.toPx()
                            ),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 3 orbiting dots
                    val orbitRadius = size.width / 2 - 10.dp.toPx()
                    for (i in 0..2) {
                        val angle = Math.toRadians((orbitAngle + i * 120.0))
                        val dotX = center.x + orbitRadius * cos(angle).toFloat()
                        val dotY = center.y + orbitRadius * sin(angle).toFloat()
                        drawCircle(
                            color = tertiaryColor.copy(alpha = 0.8f - i * 0.2f),
                            radius = (5 - i).dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Center pulsing icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(centerScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Crafting Your Resume",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .alpha(textAlpha)
                )
            }

            // Step indicators
            StepIndicators()
        }
    }
}

@Composable
private fun StepIndicators() {
    val steps = listOf("Parsing PDF", "Analyzing JD", "Tailoring", "Generating")
    var currentStep by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            currentStep = (currentStep + 1) % steps.size
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentStep
            val isPast = index < currentStep

            val bgColor by animateColorAsState(
                targetValue = when {
                    isPast -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "stepBg"
            )
            val textColor by animateColorAsState(
                targetValue = when {
                    isPast -> MaterialTheme.colorScheme.onPrimary
                    isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "stepText"
            )

            Surface(
                color = bgColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Provider card
// ──────────────────────────────────────────────
@Composable
private fun ProviderCard(
    label: String,
    subtitle: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        label = "bg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Main HomeScreen
// ──────────────────────────────────────────────
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
        selectedPdfName = uri?.lastPathSegment?.substringAfterLast("/") ?: "resume.pdf"
    }

    LaunchedEffect(uiState) {
        if (uiState is ResumeUiState.Success) {
            onNavigateToResult((uiState as ResumeUiState.Success).pdfBytes)
            viewModel.reset()
        }
    }

    val isLoading = uiState is ResumeUiState.Loading
    val isError = uiState is ResumeUiState.Error

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Resume Tailor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "AI-powered resume customization",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Step 1: Upload PDF ──
                SectionLabel(number = "01", title = "Upload Resume")

                AnimatedUploadCard(
                    hasFile = selectedPdfUri != null,
                    fileName = selectedPdfName,
                    onClick = { pdfPickerLauncher.launch("application/pdf") }
                )

                // ── Step 2: Job Description ──
                SectionLabel(number = "02", title = "Job Description")

                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    placeholder = {
                        Text(
                            "Paste the job description here…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 20
                )

                // ── Step 3: AI Provider ──
                SectionLabel(number = "03", title = "AI Provider")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProviderCard(
                        label = "Gemini",
                        subtitle = "Cloud · Fast",
                        value = "gemini",
                        selected = selectedProvider == "gemini",
                        onClick = { selectedProvider = "gemini" }
                    )
                    ProviderCard(
                        label = "Ollama",
                        subtitle = "Local · Private",
                        value = "ollama",
                        selected = selectedProvider == "ollama",
                        onClick = { selectedProvider = "ollama" }
                    )
                }

                // ── Error ──
                AnimatedVisibility(
                    visible = isError,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    if (isError) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                (uiState as ResumeUiState.Error).message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Submit ──
                val canSubmit = selectedPdfUri != null && jobDescription.isNotBlank()
                val buttonScale by animateFloatAsState(
                    targetValue = if (canSubmit) 1f else 0.97f,
                    label = "btnScale"
                )

                Button(
                    onClick = {
                        selectedPdfUri?.let { uri ->
                            viewModel.tailorResume(uri, jobDescription, selectedProvider)
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(buttonScale)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        "Tailor My Resume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Loading overlay ──
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300))
        ) {
            LoadingOverlay(
                message = if (isLoading)
                    (uiState as ResumeUiState.Loading).stepMessage
                else ""
            )
        }
    }
}

// ──────────────────────────────────────────────
// Small helpers
// ──────────────────────────────────────────────
@Composable
private fun SectionLabel(number: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                number,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AnimatedUploadCard(
    hasFile: Boolean,
    fileName: String,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (hasFile) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        label = "uploadBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (hasFile) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "uploadBg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (hasFile) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (hasFile) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(iconScale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasFile) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasFile) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = if (hasFile) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasFile) "PDF Selected" else "Choose a PDF",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasFile) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (hasFile) fileName else "Tap to browse your files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (hasFile) {
                TextButton(onClick = onClick) {
                    Text("Change", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
