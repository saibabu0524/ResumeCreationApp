package com.softsuave.resumecreationapp.feature.ats

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.SectionScores
import com.softsuave.resumecreationapp.core.domain.usecase.ats.AnalyseAtsUseCase
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class AtsViewModelTest {

    private lateinit var analyseAtsUseCase: AnalyseAtsUseCase
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var viewModel: AtsViewModel

    @BeforeEach
    fun setup() {
        analyseAtsUseCase = mockk()
        context = mockk()
        contentResolver = mockk()

        every { context.contentResolver } returns contentResolver

        viewModel = AtsViewModel(analyseAtsUseCase, context)
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(AtsUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `analyse emits error if job description is blank`() = runTest {
        viewModel.uiState.test {
            assertEquals(AtsUiState.Idle, awaitItem())

            viewModel.analyse(mockk(), "", "provider")

            val errorState = awaitItem() as AtsUiState.Error
            assertEquals("Job description cannot be empty.", errorState.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `reset sets state back to Idle`() = runTest {
        // Force error state first
        viewModel.analyse(mockk(), "", "provider")
        assertTrue(viewModel.uiState.value is AtsUiState.Error)

        viewModel.reset()

        assertEquals(AtsUiState.Idle, viewModel.uiState.value)
    }

    // ─── URI resolution helpers ───────────────────────────────────────────────

    /** Configures the mocked ContentResolver to successfully resolve a URI. */
    private fun givenUriResolves(bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every {
            contentResolver.openInputStream(any())
        } returns ByteArrayInputStream(bytes)
    }

    /** Configures the mocked ContentResolver so openInputStream returns null. */
    private fun givenUriUnresolvable() {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.openInputStream(any()) } returns null
    }

    // ─── Success path ─────────────────────────────────────────────────────────

    @Test
    fun `analyse success emits Success state with AtsResult`() = runTest {
        val expected = AtsResult(
            overallScore = 85,
            scoreLabel = "Excellent",
            keywordsPresent = listOf("Kotlin"),
            keywordsMissing = emptyList(),
            sectionScores = SectionScores(90, 85, 80, 95),
            suggestions = emptyList(),
            strengths = listOf("Strong Kotlin"),
            summary = "Great match.",
        )
        givenUriResolves()
        coEvery { analyseAtsUseCase(any()) } returns Result.Success(expected)

        viewModel.uiState.test {
            assertEquals(AtsUiState.Idle, awaitItem())

            viewModel.analyse(mockk(), "Senior Android Engineer", "gemini")

            // Loading
            assertTrue(awaitItem() is AtsUiState.Loading)
            // Success
            val success = awaitItem() as AtsUiState.Success
            assertEquals(expected, success.result)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Error paths ──────────────────────────────────────────────────────────

    @Test
    fun `analyse use case error emits Error state`() = runTest {
        givenUriResolves()
        coEvery { analyseAtsUseCase(any()) } returns Result.Error(
            AppException.ServerError(message = "API down"),
        )

        viewModel.uiState.test {
            assertEquals(AtsUiState.Idle, awaitItem())

            viewModel.analyse(mockk(), "Senior Android Engineer", "gemini")

            assertTrue(awaitItem() is AtsUiState.Loading)
            val error = awaitItem() as AtsUiState.Error
            assertEquals("API down", error.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `analyse unresolvable URI emits Error without calling use case`() = runTest {
        givenUriUnresolvable()

        viewModel.uiState.test {
            assertEquals(AtsUiState.Idle, awaitItem())

            viewModel.analyse(mockk(), "Senior Android Engineer", "gemini")

            // Loading emitted before URI resolution
            assertTrue(awaitItem() is AtsUiState.Loading)
            val error = awaitItem() as AtsUiState.Error
            assertEquals("Could not read the PDF file.", error.message)
            cancelAndConsumeRemainingEvents()
        }
    }
}
