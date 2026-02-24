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
import com.softsuave.resumecreationapp.feature.ats.AtsResult
import com.softsuave.resumecreationapp.feature.ats.AtsUiState
import com.softsuave.resumecreationapp.feature.ats.AtsViewModel
import kotlin.math.cos
import kotlin.math.sin

// ── Design Tokens ─────────────────────────────────────────────────────────────
private val Canvas       = Color(0xFF0E0D0B)
private val Surface      = Color(0xFF1A1814)
private val SurfaceHigh  = Color(0xFF242019)
private val Amber        = Color(0xFFD4A853)
private val AmberGlow    = Color(0xFFEFC97A)
private val AmberDim     = Color(0xFF8A6930)
private val TextPrimary  = Color(0xFFF0EAD6)
private val TextMuted    = Color(0xFF9A8E78)
private val Border       = Color(0xFF2E2A24)
private val BorderMid    = Color(0xFF4A4238)
private val ErrorRed     = Color(0xFFB04A3A)
private val ErrorDim     = Color(0xFF2D1410)
private val GreenOk      = Color(0xFF4A7C59)
private val GreenDim     = Color(0xFF1A2E20)
private val RedMiss      = Color(0xFF9A3A2A)

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
            .background(Canvas)
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
                        listOf(Amber.copy(alpha = 0.04f + glowAnim * 0.03f), Color.Transparent)
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
                                    tint = TextMuted,
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
                                    color = Amber.copy(alpha = 0.6f)
                                )
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Light)) {
                                            append("Optimise your ")
                                        }
                                        withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                                            append("chances")
                                        }
                                    },
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp
                                )
                            }
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
                            Text("ATS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Amber, letterSpacing = 1.sp)
                        }
                    }

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
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
                        HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        Spacer(Modifier.height(28.dp))

                        // Section 02: Job Description
                        AtsSectionHeader("02", "Job Description")
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
                                        "We are looking for a Senior Android Engineer…",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TextMuted.copy(alpha = 0.4f),
                                        lineHeight = 20.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp),
                                maxLines = 18,
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
                        }

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
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
                                        "⚠  ${(uiState as AtsUiState.Error).message}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ErrorRed,
                                        lineHeight = 18.sp
                                    )
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
                                containerColor = Amber,
                                disabledContainerColor = BorderMid,
                                contentColor = Canvas,
                                disabledContentColor = TextMuted
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
                Icon(Icons.Default.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(20.dp))
            }
            Text(
                "ATS ANALYSIS REPORT",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = Amber.copy(alpha = 0.7f)
            )
            TextButton(onClick = onScanAgain) {
                Text("RESCAN", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = AmberDim)
            }
        }
        HorizontalDivider(color = Border, thickness = 0.5.dp)

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
                        .border(0.5.dp, BorderMid, RoundedCornerShape(2.dp))
                        .background(Surface.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        result.summary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Section Scores ────────────────────────────────────────────────
            AtsSectionHeader("01", "Section Scores")
            Spacer(Modifier.height(14.dp))
            SectionScoreRow("Skills Match",          result.sectionScores.skillsMatch)
            SectionScoreRow("Experience Relevance",  result.sectionScores.experienceRelevance)
            SectionScoreRow("Education Match",       result.sectionScores.educationMatch)
            SectionScoreRow("Formatting",            result.sectionScores.formatting)

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Keywords ──────────────────────────────────────────────────────
            AtsSectionHeader("02", "Keywords")
            Spacer(Modifier.height(14.dp))

            if (result.keywordsPresent.isNotEmpty()) {
                KeywordChipsRow(label = "✓  FOUND", chips = result.keywordsPresent, chipColor = GreenOk, chipBg = GreenDim)
                Spacer(Modifier.height(12.dp))
            }
            if (result.keywordsMissing.isNotEmpty()) {
                KeywordChipsRow(label = "✗  MISSING", chips = result.keywordsMissing, chipColor = RedMiss, chipBg = ErrorDim)
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(24.dp))

            // ── Strengths ─────────────────────────────────────────────────────
            if (result.strengths.isNotEmpty()) {
                AtsSectionHeader("03", "Strengths")
                Spacer(Modifier.height(12.dp))
                result.strengths.forEach { strength ->
                    BulletItem(text = strength, accent = GreenOk, prefix = "✓")
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
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
            }

            Spacer(Modifier.height(32.dp))

            // Scan Again button
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(0.5.dp, AmberDim),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
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
//  Score Gauge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreGauge(score: Int, label: String) {
    val scoreColor = when {
        score >= 85 -> GreenOk
        score >= 70 -> Amber
        score >= 50 -> Color(0xFFB08A30)
        else        -> ErrorRed
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
                        color = BorderMid,
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
                                listOf(Color.Transparent, Amber.copy(0.2f), Color.Transparent)
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
                    color = TextMuted,
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
        value >= 75 -> GreenOk
        value >= 50 -> Amber
        else        -> ErrorRed
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
            Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
            Text("$value%", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = barColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(BorderMid)
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
        // Wrap chips using a FlowRow-like layout via lazy rows
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
            color = TextMuted,
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
            .border(0.5.dp, Border, RoundedCornerShape(2.dp))
            .background(Surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SurfaceHigh)
                .border(0.5.dp, AmberDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            color = TextMuted,
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
        Text(number, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, color = Amber.copy(0.5f))
        Box(Modifier.width(1.dp).height(14.dp).background(BorderMid))
        Text(title.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 3.sp, color = TextMuted)
    }
}

@Composable
private fun AtsUploadCard(hasFile: Boolean, fileName: String, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (hasFile) Amber.copy(0.6f) else BorderMid, tween(300), label = "ub")
    val bgColor     by animateColorAsState(if (hasFile) Color(0xFF1E1C14) else Surface, tween(300), label = "ubg")

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
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp,
                    color = if (hasFile) Amber else TextMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasFile) fileName.take(32) + if (fileName.length > 32) "…" else ""
                    else "Tap to browse your files",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    color = if (hasFile) TextPrimary else TextMuted.copy(0.6f)
                )
            }
            if (hasFile) {
                Text(
                    "CHANGE",
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = AmberGlow,
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
private fun AtsProviderChip(
    label: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(if (selected) Amber.copy(0.7f) else BorderMid, tween(250), label = "pc")
    val bg     by animateColorAsState(if (selected) Color(0xFF1E1C12) else Surface, tween(250), label = "pbg")

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
                        .background(if (selected) Amber else BorderMid)
                )
                Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = if (selected) Amber else TextMuted)
            }
            Text(sub, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted.copy(if (selected) 0.8f else 0.5f))
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
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        rotate(rot1) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, Amber.copy(0.3f), Amber)),
                                startAngle = 0f, sweepAngle = 280f, useCenter = false,
                                style = Stroke(8f, cap = StrokeCap.Round)
                            )
                        }
                        rotate(rot2) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, AmberDim.copy(0.6f), AmberDim)),
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
                                color = Amber.copy(alpha = 1f - i * 0.25f),
                                radius = 6f - i * 1.5f,
                                center = Offset(
                                    size.width / 2 + orbitR * cos(angle).toFloat(),
                                    size.height / 2 + orbitR * sin(angle).toFloat()
                                )
                            )
                        }
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
                Icon(Icons.Default.Analytics, null, tint = Amber, modifier = Modifier.size(32.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SCANNING YOUR RESUME",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 4.sp, color = Amber
                )
                Text(
                    message,
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextMuted,
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
//                        .alpha(textAlpha),
                        ,
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
