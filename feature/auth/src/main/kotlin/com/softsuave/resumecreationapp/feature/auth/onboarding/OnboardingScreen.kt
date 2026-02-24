package com.softsuave.resumecreationapp.feature.auth.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softsuave.resumecreationapp.feature.auth.R
import kotlinx.coroutines.launch

// ── Design Tokens ────────────────────────────────────────────────────────────
private val Canvas      = Color(0xFF0E0D0B)
private val Surface     = Color(0xFF1A1814)
private val SurfaceHigh = Color(0xFF242019)
private val Amber       = Color(0xFFD4A853)
private val AmberGlow   = Color(0xFFEFC97A)
private val AmberDim    = Color(0xFF8A6930)
private val TextPrimary = Color(0xFFF0EAD6)
private val TextMuted   = Color(0xFF9A8E78)
private val Border      = Color(0xFF2E2A24)
private val BorderMid   = Color(0xFF4A4238)

@Immutable
private data class OnboardingPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val italic: String,
    val description: String,
)

@Composable
fun OnboardingRoute(onOnboardingComplete: () -> Unit) {
    OnboardingScreen(onOnboardingComplete = onOnboardingComplete)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Explore,
            eyebrow = "STEP 01",
            title = "Upload &\nAnalyse",
            italic = "Your Resume",
            description = stringResource(R.string.auth_onboarding_page1_desc),
        ),
        OnboardingPage(
            icon = Icons.Default.Security,
            eyebrow = "STEP 02",
            title = "AI-Powered",
            italic = "Tailoring",
            description = stringResource(R.string.auth_onboarding_page2_desc),
        ),
        OnboardingPage(
            icon = Icons.Default.Notifications,
            eyebrow = "STEP 03",
            title = "Download &",
            italic = "Get Hired",
            description = stringResource(R.string.auth_onboarding_page3_desc),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    // Ambient rotation for icon rings
    val rotation by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "rot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        // Background glow that shifts per page
        val glowX by animateFloatAsState(
            targetValue = pagerState.currentPage / (pages.size - 1f),
            animationSpec = tween(600),
            label = "glowX"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Amber.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(glowX * 1000f + 100f, 400f),
                        radius = 700f
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(pages.size) { idx ->
                        val targetWidth = if (idx <= pagerState.currentPage) 1f else 0f
                        val progress by animateFloatAsState(
                            targetValue = targetWidth,
                            animationSpec = tween(400),
                            label = "bar$idx"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BorderMid)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(Amber)
                            )
                        }
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(
                    page = pages[pageIndex],
                    rotation = rotation,
                    isActive = pageIndex == pagerState.currentPage
                )
            }

            // Bottom actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { idx ->
                        val isActive = idx == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isActive) 24.dp else 6.dp,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "dotW"
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isActive) Amber else BorderMid)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Primary button
                Button(
                    onClick = {
                        if (isLastPage) onOnboardingComplete()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Canvas
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        if (isLastPage) "GET STARTED →" else "NEXT →",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Skip link
                AnimatedVisibility(visible = !isLastPage) {
                    TextButton(onClick = onOnboardingComplete) {
                        Text(
                            "Skip for now",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    rotation: Float,
    isActive: Boolean,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )
    val contentOffset by animateIntOffsetAsState(
        targetValue = if (isActive) androidx.compose.ui.unit.IntOffset.Zero
        else androidx.compose.ui.unit.IntOffset(0, 40),
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "contentOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Rotating icon ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawBehind {
                    // Outer dashed-effect ring
                    rotate(rotation) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, Amber.copy(alpha = 0.5f), Amber, Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                        )
                    }
                    // Counter-rotating inner ring
                    rotate(-rotation * 0.6f) {
                        drawArc(
                            color = Amber.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 200f,
                            useCenter = false,
                            style = Stroke(width = 1f)
                        )
                    }
                    // Solid center circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF2A2218), Color(0xFF151210))
                        ),
                        radius = size.minDimension * 0.35f
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.2f),
                        radius = size.minDimension * 0.35f,
                        style = Stroke(width = 0.5f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = Amber
            )
        }

        Spacer(Modifier.height(40.dp))

        // Eyebrow label
        Text(
            page.eyebrow,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = Amber.copy(alpha = 0.7f),
            modifier = Modifier.offset(y = contentOffset.y.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Title
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Light)) {
                    append(page.title + "\n")
                }
                withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append(page.italic)
                }
            },
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = contentOffset.y.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Description
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Border, RoundedCornerShape(2.dp))
                .background(Surface.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                page.description,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )
        }
    }
}
