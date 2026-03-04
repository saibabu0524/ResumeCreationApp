package com.softsuave.resumecreationapp.feature.ats.ui

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.ui.theme.*
import com.softsuave.resumecreationapp.feature.ats.AtsUiState
import com.softsuave.resumecreationapp.feature.ats.AtsViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AtsRoute(
    onNavigateBack: () -> Unit,
    viewModel: AtsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    AtsScreen(
        uiState = uiState,
        onAnalyse = { uri, jd, prov -> viewModel.analyse(uri, jd, prov) },
        onReset = viewModel::reset,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AtsScreen(
    uiState: AtsUiState,
    onAnalyse: (Uri, String, String) -> Unit,
    onReset: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedPdfUri    by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName   by remember { mutableStateOf("") }
    var jobDescription    by remember { mutableStateOf("") }
    var selectedProvider  by remember { mutableStateOf("gemini") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedPdfUri  = uri
        selectedPdfName = uri?.lastPathSegment?.substringAfterLast("/") ?: "resume.pdf"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient glow
        val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(7000), RepeatMode.Reverse),
            label = "ga"
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f + glowAnim * 0.03f), Color.Transparent)
                    )
                )
        )

        when (uiState) {
            is AtsUiState.Success -> {
                AtsResultScreen(
                    result = uiState.result,
                    onScanAgain = onReset,
                    onNavigateBack = onNavigateBack,
                )
            }

            is AtsUiState.Loading -> {
                AtsLoadingOverlay(message = uiState.stepMessage)
            }

            else -> {
                // Idle or Error → show input form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // ── Top Bar ───────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "ATS SCANNER",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 3.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light)) {
                                            append("Optimise your ")
                                        }
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                                            append("chances")
                                        }
                                    },
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp
                                )
                            }
                        }
                        // Logo mark
                        val primary = MaterialTheme.colorScheme.primary
                        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .drawBehind {
                                    drawCircle(color = primary.copy(alpha = 0.2f), style = Stroke(1f))
                                    drawCircle(
                                        brush = Brush.radialGradient(listOf(primaryContainer, surfaceVariant)),
                                        radius = size.minDimension / 2 - 3
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ATS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        Spacer(Modifier.height(28.dp))

                        // Section 01: Upload
                        AtsSectionHeader("01", "Upload Resume")
                        Spacer(Modifier.height(12.dp))
                        AtsUploadCard(
                            hasFile = selectedPdfUri != null,
                            fileName = selectedPdfName,
                            onClick = { pdfPickerLauncher.launch("application/pdf") }
                        )

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        Spacer(Modifier.height(28.dp))

                        // Section 02: Job Description
                        AtsSectionHeader("02", "Job Description")
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Work, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text("PASTE JOB DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            OutlinedTextField(
                                value = jobDescription,
                                onValueChange = { jobDescription = it },
                                placeholder = {
                                    Text(
                                        "We are looking for a Senior Android Engineer…",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        lineHeight = 20.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp),
                                maxLines = 18,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        Spacer(Modifier.height(28.dp))

                        // Section 03: AI Provider
                        AtsSectionHeader("03", "AI Provider")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("gemini" to "Cloud · Fast", "ollama" to "Local · Private").forEach { (value, sub) ->
                                AtsProviderChip(
                                    label = value.uppercase(),
                                    sub = sub,
                                    selected = selectedProvider == value,
                                    onClick = { selectedProvider = value },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Error
                        AnimatedVisibility(
                            visible = uiState is AtsUiState.Error,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (uiState is AtsUiState.Error) {
                                Column {
                                    Spacer(Modifier.height(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(SemanticErrorDim)
                                            .border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            "⚠  ${(uiState as AtsUiState.Error).message}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Submit button
                        val canSubmit = selectedPdfUri != null && jobDescription.isNotBlank()
                        Button(
                            onClick = {
                                selectedPdfUri?.let { uri ->
                                    onAnalyse(uri, jobDescription, selectedProvider)
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.outline,
                                contentColor = MaterialTheme.colorScheme.background,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Default.Analytics, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "SCAN MY RESUME →",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ATS Result Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AtsResultScreen(
    result: AtsResult,
    onScanAgain: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(
                "ATS ANALYSIS REPORT",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            TextButton(onClick = onScanAgain) {
                Text("RESCAN", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = Amber40)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Score Gauge ───────────────────────────────────────────────────
            ScoreGauge(score = result.overallScore, label = result.scoreLabel)

            Spacer(Modifier.height(20.dp))

            // Summary
            if (result.summary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        result.summary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Section Scores ────────────────────────────────────────────────
            AtsSectionHeader("01", "Section Scores")
            Spacer(Modifier.height(14.dp))
            SectionScoreRow("Skills Match",          result.sectionScores.skillsMatch)
            SectionScoreRow("Experience Relevance",  result.sectionScores.experienceRelevance)
            SectionScoreRow("Education Match",       result.sectionScores.educationMatch)
            SectionScoreRow("Formatting",            result.sectionScores.formatting)

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Keywords ──────────────────────────────────────────────────────
            AtsSectionHeader("02", "Keywords")
            Spacer(Modifier.height(14.dp))

            if (result.keywordsPresent.isNotEmpty()) {
                KeywordChipsRow(label = "✓  FOUND", chips = result.keywordsPresent, chipColor = MaterialTheme.colorScheme.tertiary, chipBg = SemanticSuccessDim)
                Spacer(Modifier.height(12.dp))
            }
            if (result.keywordsMissing.isNotEmpty()) {
                KeywordChipsRow(label = "✗  MISSING", chips = result.keywordsMissing, chipColor = SemanticMiss, chipBg = SemanticErrorDim)
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Strengths ─────────────────────────────────────────────────────
            if (result.strengths.isNotEmpty()) {
                AtsSectionHeader("03", "Strengths")
                Spacer(Modifier.height(12.dp))
                result.strengths.forEach { strength ->
                    BulletItem(text = strength, accent = MaterialTheme.colorScheme.tertiary, prefix = "✓")
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(24.dp))
            }

            // ── Suggestions ───────────────────────────────────────────────────
            if (result.suggestions.isNotEmpty()) {
                AtsSectionHeader("04", "Suggestions")
                Spacer(Modifier.height(12.dp))
                result.suggestions.forEachIndexed { idx, suggestion ->
                    SuggestionItem(number = "${idx + 1}", text = suggestion)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(24.dp))
            }

            // ── Resume Diff View ──────────────────────────────────────────────
            if (result.suggestions.isNotEmpty()) {
                AtsSectionHeader("05", "Improvement Preview")
                Spacer(Modifier.height(12.dp))
                ResumeDiffSection(suggestions = result.suggestions)
                Spacer(Modifier.height(28.dp))
            }

            // Scan Again button
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(0.5.dp, Amber40),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("SCAN AGAIN", fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Resume Diff View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResumeDiffSection(suggestions: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header explanation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                .padding(12.dp)
        ) {
            Text(
                "Based on the analysis, here's how each suggestion maps to improvements in your resume:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        suggestions.forEachIndexed { idx, suggestion ->
            DiffCard(index = idx + 1, suggestion = suggestion)
        }
    }
}

@Composable
private fun DiffCard(index: Int, suggestion: String) {
    // Parse suggestion into a "before" (issue) and "after" (improvement) view
    val parts = suggestion.split(".", limit = 2)
    val title = parts.firstOrNull()?.trim() ?: suggestion
    val detail = parts.getOrNull(1)?.trim() ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
    ) {
        // Diff header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(0.5.dp, Amber40, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "CHANGE #$index",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // "Before" row (current issue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticErrorDim.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "−",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CURRENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // "After" row (suggested improvement)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticSuccessDim.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "+",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SUGGESTED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (detail.isNotEmpty()) detail else "Apply: $title",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Score Gauge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreGauge(score: Int, label: String) {
    val scoreColor = when {
        score >= 85 -> MaterialTheme.colorScheme.tertiary
        score >= 70 -> MaterialTheme.colorScheme.primary
        score >= 50 -> MaterialTheme.colorScheme.primary
        else        -> MaterialTheme.colorScheme.error
    }

    // Animate score arc
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "score"
    )
    val rotation by rememberInfiniteTransition(label = "rot").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "r"
    )

    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawBehind {
                    // Background track
                    drawArc(
                        color = outline,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(12f, cap = StrokeCap.Round)
                    )
                    // Score arc
                    drawArc(
                        brush = Brush.sweepGradient(listOf(scoreColor.copy(0.4f), scoreColor)),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedScore,
                        useCenter = false,
                        style = Stroke(12f, cap = StrokeCap.Round)
                    )
                    // Outer decorative ring
                    rotate(rotation) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, primary.copy(0.2f), Color.Transparent)
                            ),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(1.5f)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    fontFamily = FontFamily.Serif,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    "/ 100",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(scoreColor.copy(alpha = 0.1f))
                .border(0.5.dp, scoreColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionScoreRow(label: String, value: Int) {
    val barColor = when {
        value >= 75 -> MaterialTheme.colorScheme.tertiary
        value >= 50 -> MaterialTheme.colorScheme.primary
        else        -> MaterialTheme.colorScheme.error
    }
    val animatedWidth by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "sw"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$value%", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = barColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun KeywordChipsRow(label: String, chips: List<String>, chipColor: Color, chipBg: Color) {
    Column {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = chipColor)
        Spacer(Modifier.height(8.dp))
        var startIndex = 0
        val chipsPerRow = 3
        while (startIndex < chips.size) {
            val rowChips = chips.subList(startIndex, minOf(startIndex + chipsPerRow, chips.size))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                rowChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(chipBg)
                            .border(0.5.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            chip,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = chipColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            startIndex += chipsPerRow
        }
    }
}

@Composable
private fun BulletItem(text: String, accent: Color, prefix: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(prefix, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = accent)
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SuggestionItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, Amber40, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AtsSectionHeader(number: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(number, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary.copy(0.5f))
        Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outline))
        Text(title.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AtsUploadCard(hasFile: Boolean, fileName: String, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (hasFile) MaterialTheme.colorScheme.primary.copy(0.6f) else MaterialTheme.colorScheme.outline, tween(300), label = "ub")
    val bgColor     by animateColorAsState(if (hasFile) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, tween(300), label = "ubg")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(2.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (hasFile) Amber40.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, if (hasFile) MaterialTheme.colorScheme.primary.copy(0.3f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasFile) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    null,
                    tint = if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasFile) "PDF READY" else "SELECT PDF",
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp,
                    color = if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasFile) fileName.take(32) + if (fileName.length > 32) "…" else ""
                    else "Tap to browse your files",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    color = if (hasFile) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
            if (hasFile) {
                Text(
                    "CHANGE",
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = Amber95,
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, Amber40, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AtsProviderChip(
    label: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary.copy(0.7f) else MaterialTheme.colorScheme.outline, tween(250), label = "pc")
    val bg     by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, tween(250), label = "pbg")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(if (selected) 1.dp else 0.5.dp, border, RoundedCornerShape(2.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
                Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(sub, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(if (selected) 0.8f else 0.5f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Loading Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AtsLoadingOverlay(message: String) {
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

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        rotate(rot1) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, primary.copy(0.3f), primary)),
                                startAngle = 0f, sweepAngle = 280f, useCenter = false,
                                style = Stroke(8f, cap = StrokeCap.Round)
                            )
                        }
                        rotate(rot2) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, Amber40.copy(0.6f), Amber40)),
                                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                                topLeft = Offset(22f, 22f),
                                size = androidx.compose.ui.geometry.Size(size.width - 44f, size.height - 44f),
                                style = Stroke(4f, cap = StrokeCap.Round)
                            )
                        }
                        val orbitR = size.width / 2 - 8f
                        for (i in 0..2) {
                            val angle = Math.toRadians((orbitAngle + i * 120.0))
                            drawCircle(
                                color = primary.copy(alpha = 1f - i * 0.25f),
                                radius = 6f - i * 1.5f,
                                center = Offset(
                                    size.width / 2 + orbitR * cos(angle).toFloat(),
                                    size.height / 2 + orbitR * sin(angle).toFloat()
                                )
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(listOf(primaryContainer, surfaceVariant)),
                            radius = size.minDimension * 0.28f * pulse
                        )
                        drawCircle(
                            color = primary.copy(0.25f),
                            radius = size.minDimension * 0.28f * pulse,
                            style = Stroke(1f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SCANNING YOUR RESUME",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.primary
                )
                Text(
                    message,
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 48.dp),
                    lineHeight = 20.sp
                )
            }

            // Step pills
            AtsLoadingSteps()
        }
    }
}

@Composable
private fun AtsLoadingSteps() {
    val steps = listOf("EXTRACT", "ANALYSE", "SCORE", "REPORT")
    var active by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            active = (active + 1) % steps.size
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { idx, step ->
            val isPast    = idx < active
            val isCurrent = idx == active
            val bg by animateColorAsState(
                when { isCurrent -> MaterialTheme.colorScheme.primary; isPast -> Amber40; else -> MaterialTheme.colorScheme.surfaceVariant },
                tween(300), label = "sbg"
            )
            val tc by animateColorAsState(
                when { isCurrent -> MaterialTheme.colorScheme.background; isPast -> MaterialTheme.colorScheme.onSurfaceVariant; else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f) },
                tween(300), label = "stc"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(bg)
                    .border(0.5.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(step, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = tc, letterSpacing = 1.sp)
            }
        }
    }
}
