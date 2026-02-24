package com.softsuave.resumecreationapp.feature.resume.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softsuave.resumecreationapp.feature.resume.ResumeUiState
import com.softsuave.resumecreationapp.feature.resume.ResumeViewModel
import com.softsuave.resumecreationapp.core.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Production entry-point: wires up the [ResumeViewModel] via Hilt and delegates to
 * the stateless [HomeScreenContent] overload.
 */
@Composable
fun HomeScreen(
    viewModel: ResumeViewModel = hiltViewModel(),
    onNavigateToResult: (ByteArray) -> Unit,
    onNavigateToAts: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ResumeUiState.Success) {
            onNavigateToResult((uiState as ResumeUiState.Success).pdfBytes)
            viewModel.reset()
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onTailorResume = { uri, jd, provider -> viewModel.tailorResume(uri, jd, provider) },
        onNavigateToAts = onNavigateToAts,
    )
}

/**
 * Stateless home screen composable — accepts [uiState] and callbacks so it can be
 * rendered in Compose Previews and Robolectric unit tests without Hilt.
 */
@Composable
fun HomeScreenContent(
    uiState: ResumeUiState = ResumeUiState.Idle,
    onTailorResume: (Uri, String, String) -> Unit = { _, _, _ -> },
    onNavigateToAts: () -> Unit = {},
) {
    var selectedPdfUri by remember<MutableState<Uri?>> { mutableStateOf(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("gemini") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedPdfUri = uri
        selectedPdfName = uri?.lastPathSegment?.substringAfterLast("/") ?: "resume.pdf"
    }

    val isLoading = uiState is ResumeUiState.Loading

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        // Ambient glow
        val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
            label = "ga"
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Amber.copy(alpha = 0.04f + glowAnim * 0.03f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "RESUME TAILOR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = Amber.copy(alpha = 0.6f)
                    )
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Light)) { append("Craft your ") }
                            withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append("story") }
                        },
                        fontFamily = FontFamily.Serif,
                        fontSize = 26.sp
                    )
                }
                // Logo mark
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .drawBehind {
                            drawCircle(color = Amber.copy(alpha = 0.2f), style = Stroke(1f))
                            drawCircle(
                                brush = Brush.radialGradient(listOf(Color(0xFF2A2218), Color(0xFF151210))),
                                radius = size.minDimension / 2 - 3
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("RT", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Amber, letterSpacing = 1.sp)
                }
            }

            HorizontalDivider(color = Border, thickness = 0.5.dp)

            // ── Scrollable Content ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                // ── Section 01: Upload ───────────────────────────────────────
                SectionHeader(number = "01", title = "Upload Resume")
                Spacer(Modifier.height(12.dp))

                UploadCard(
                    hasFile = selectedPdfUri != null,
                    fileName = selectedPdfName,
                    onClick = { pdfPickerLauncher.launch("application/pdf") }
                )

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(
                    color = Border,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(28.dp))

                // ── Section 02: Job Description ──────────────────────────────
                SectionHeader(number = "02", title = "Job Description")
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, BorderMid, RoundedCornerShape(2.dp))
                        .background(Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 0.dp, color = Color.Transparent)
                            .background(SurfaceHigh)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Work, null, tint = Amber, modifier = Modifier.size(14.dp))
                        Text("PASTE JOB DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, color = TextMuted)
                    }
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        placeholder = {
                            Text(
                                "We are looking for a Senior Android Engineer...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextMuted.copy(alpha = 0.4f),
                                lineHeight = 20.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        maxLines = 20,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Amber,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                    )
                    if (jobDescription.isNotEmpty()) {
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                        Text(
                            "${jobDescription.length} chars",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(28.dp))

                // ── Section 03: AI Provider ──────────────────────────────────
                SectionHeader(number = "03", title = "AI Provider")
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProviderChip(
                        label = "GEMINI",
                        sub = "Cloud · Fast",
                        value = "gemini",
                        selected = selectedProvider == "gemini",
                        onClick = { selectedProvider = "gemini" },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        label = "OLLAMA",
                        sub = "Local · Private",
                        value = "ollama",
                        selected = selectedProvider == "ollama",
                        onClick = { selectedProvider = "ollama" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Error ────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState is ResumeUiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (uiState is ResumeUiState.Error) {
                        Spacer(Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(ErrorDim)
                                .border(0.5.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                "⚠  ${(uiState as ResumeUiState.Error).message}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ErrorRed,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Submit Button ─────────────────────────────────────────────
                val canSubmit = selectedPdfUri != null && jobDescription.isNotBlank()
                Button(
                    onClick = {
                        selectedPdfUri?.let { uri ->
                            onTailorResume(uri, jobDescription, selectedProvider)
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        disabledContainerColor = BorderMid,
                        contentColor = Canvas,
                        disabledContentColor = TextMuted
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        "TAILOR MY RESUME →",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── ATS Scanner shortcut ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, BorderMid, RoundedCornerShape(2.dp))
                        .background(Surface)
                        .clickable(onClick = onNavigateToAts)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SurfaceHigh)
                                .border(0.5.dp, AmberDim, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Amber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ATS SCANNER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = Amber
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Check your resume score before tailoring",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextMuted.copy(0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AmberDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Loading Overlay ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            EditorialLoadingOverlay(
                message = if (isLoading) (uiState as ResumeUiState.Loading).stepMessage else ""
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Loading Overlay
// ─────────────────────────────────────────────────────────────
@Composable
private fun EditorialLoadingOverlay(message: String) {
    val rot1 by rememberInfiniteTransition(label = "r1").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot1"
    )
    val rot2 by rememberInfiniteTransition(label = "r2").animateFloat(
        360f, 0f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "rot2"
    )
    val orbitAngle by rememberInfiniteTransition(label = "orb").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "orb"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.85f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p"
    )
    val textAlpha by rememberInfiniteTransition(label = "ta").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "ta"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // Spinner
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        // Outer sweep arc
                        rotate(rot1) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, Amber.copy(0.3f), Amber)
                                ),
                                startAngle = 0f, sweepAngle = 280f, useCenter = false,
                                style = Stroke(8f, cap = StrokeCap.Round)
                            )
                        }
                        // Inner arc
                        rotate(rot2) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, AmberDim.copy(0.6f), AmberDim)
                                ),
                                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                                topLeft = Offset(22f, 22f),
                                size = androidx.compose.ui.geometry.Size(size.width - 44f, size.height - 44f),
                                style = Stroke(4f, cap = StrokeCap.Round)
                            )
                        }
                        // Orbiting dots
                        val orbitR = size.width / 2 - 8f
                        for (i in 0..2) {
                            val angle = Math.toRadians((orbitAngle + i * 120.0))
                            val alpha = 1f - i * 0.25f
                            val dotR = (6f - i * 1.5f)
                            drawCircle(
                                color = Amber.copy(alpha = alpha),
                                radius = dotR,
                                center = Offset(
                                    size.width / 2 + orbitR * cos(angle).toFloat(),
                                    size.height / 2 + orbitR * sin(angle).toFloat()
                                )
                            )
                        }
                        // Centre circle
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFF2A2218), Color(0xFF111109))),
                            radius = size.minDimension * 0.28f * pulse
                        )
                        drawCircle(
                            color = Amber.copy(0.25f),
                            radius = size.minDimension * 0.28f * pulse,
                            style = Stroke(1f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    null,
                    tint = Amber,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Text block
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TAILORING YOUR RESUME",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 4.sp,
                    color = Amber
                )
                Text(
                    message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .alpha(textAlpha),
                    lineHeight = 20.sp
                )
            }

            // Animated step pills
            LoadingSteps()
        }
    }
}

@Composable
private fun LoadingSteps() {
    val steps = listOf("PARSE PDF", "ANALYSE JD", "TAILOR", "COMPILE")
    var active by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1600)
            active = (active + 1) % steps.size
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { idx, step ->
            val isPast = idx < active
            val isCurrent = idx == active
            val bg by animateColorAsState(
                when { isCurrent -> Amber; isPast -> AmberDim; else -> Surface },
                tween(300), label = "sbg"
            )
            val tc by animateColorAsState(
                when { isCurrent -> Canvas; isPast -> TextMuted; else -> TextMuted.copy(0.4f) },
                tween(300), label = "stc"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(bg)
                    .border(0.5.dp, if (isCurrent) Amber else BorderMid, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(step, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = tc, letterSpacing = 1.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(number: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            number,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = Amber.copy(0.5f)
        )
        Box(Modifier.width(1.dp).height(14.dp).background(BorderMid))
        Text(
            title.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun UploadCard(hasFile: Boolean, fileName: String, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        if (hasFile) Amber.copy(0.6f) else BorderMid,
        tween(300), label = "ub"
    )
    val bgColor by animateColorAsState(
        if (hasFile) Color(0xFF1E1C14) else Surface,
        tween(300), label = "ubg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(2.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (hasFile) AmberDim.copy(0.3f) else SurfaceHigh)
                    .border(0.5.dp, if (hasFile) Amber.copy(0.3f) else BorderMid, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasFile) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    null,
                    tint = if (hasFile) Amber else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasFile) "PDF READY" else "SELECT PDF",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = if (hasFile) Amber else TextMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasFile) fileName.take(32) + if (fileName.length > 32) "…" else ""
                    else "Tap to browse your files",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = if (hasFile) TextPrimary else TextMuted.copy(0.6f)
                )
            }

            if (hasFile) {
                Text(
                    "CHANGE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = AmberGlow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, AmberDim, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    sub: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(if (selected) Amber.copy(0.7f) else BorderMid, tween(250), label = "pc")
    val bg by animateColorAsState(if (selected) Color(0xFF1E1C12) else Surface, tween(250), label = "pbg")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = border,
                shape = RoundedCornerShape(2.dp)
            )
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Amber else BorderMid)
                )
                Text(
                    label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Amber else TextMuted
                )
            }
            Text(
                sub,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextMuted.copy(if (selected) 0.8f else 0.5f)
            )
        }
    }
}
