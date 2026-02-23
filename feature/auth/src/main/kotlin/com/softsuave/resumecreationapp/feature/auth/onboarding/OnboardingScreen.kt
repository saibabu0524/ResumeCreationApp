package com.softsuave.resumecreationapp.feature.auth.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.softsuave.resumecreationapp.core.ui.component.AppButton
import com.softsuave.resumecreationapp.core.ui.component.AppButtonVariant
import com.softsuave.resumecreationapp.core.ui.theme.LocalSpacing
import com.softsuave.resumecreationapp.feature.auth.R
import kotlinx.coroutines.launch

/**
 * Route-level composable for the Onboarding flow.
 * No ViewModel needed — purely UI-driven pager flow.
 */
@Composable
fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
) {
    OnboardingScreen(onOnboardingComplete = onOnboardingComplete)
}

@Immutable
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Explore,
            title = stringResource(R.string.auth_onboarding_page1_title),
            description = stringResource(R.string.auth_onboarding_page1_desc),
        ),
        OnboardingPage(
            icon = Icons.Default.Security,
            title = stringResource(R.string.auth_onboarding_page2_title),
            description = stringResource(R.string.auth_onboarding_page2_desc),
        ),
        OnboardingPage(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.auth_onboarding_page3_title),
            description = stringResource(R.string.auth_onboarding_page3_desc),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // ─── Page Indicators ─────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.padding(spacing.medium),
        ) {
            repeat(pages.size) { index ->
                Surface(
                    modifier = Modifier.size(if (index == pagerState.currentPage) 12.dp else 8.dp),
                    shape = CircleShape,
                    color = if (index == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(spacing.large))

        // ─── Actions ─────────────────────────────────────────────────
        AppButton(
            text = stringResource(
                if (isLastPage) R.string.auth_onboarding_get_started
                else R.string.core_ui_next,
            ),
            onClick = {
                if (isLastPage) {
                    onOnboardingComplete()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isLastPage) {
            Spacer(modifier = Modifier.height(spacing.small))
            AppButton(
                text = stringResource(R.string.auth_onboarding_skip),
                onClick = onOnboardingComplete,
                variant = AppButtonVariant.Text,
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(spacing.extraLarge))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.large),
            )
        }
    }
}
