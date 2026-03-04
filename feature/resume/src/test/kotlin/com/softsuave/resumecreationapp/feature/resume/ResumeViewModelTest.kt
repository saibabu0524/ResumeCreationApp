package com.softsuave.resumecreationapp.feature.resume

import android.net.Uri
import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.testing.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class ResumeViewModelTest {

    private lateinit var repository: ResumeRepository
    private lateinit var viewModel: ResumeViewModel

    @BeforeEach
    fun setup() {
        repository = mockk()
        viewModel = ResumeViewModel(repository)
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(ResumeUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `tailor emits error if job description is blank`() = runTest {
        viewModel.uiState.test {
            assertEquals(ResumeUiState.Idle, awaitItem())

            viewModel.tailorResume(mockk(), "", "provider")

            val errorState = awaitItem() as ResumeUiState.Error
            assertEquals("Job description cannot be empty.", errorState.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tailor emits Success when repository succeeds`() = runTest {
        val uri: Uri = mockk()
        val jobDescription = "Android Developer"
        val provider = "OpenAI"
        val expectedBytes = byteArrayOf(1, 2, 3)

        coEvery { repository.tailorResume(uri, jobDescription, provider) } returns Result.Success(expectedBytes)

        viewModel.uiState.test {
            assertEquals(ResumeUiState.Idle, awaitItem())

            viewModel.tailorResume(uri, jobDescription, provider)

            val loadingState = awaitItem() as ResumeUiState.Loading
            assertEquals("Processing with OpenAI... This may take a minute.", loadingState.stepMessage)

            val successState = awaitItem() as ResumeUiState.Success
            assertEquals(expectedBytes, successState.pdfBytes)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tailor emits Error when repository fails`() = runTest {
        val uri: Uri = mockk()
        val jobDescription = "Android Developer"
        val provider = "OpenAI"
        val exceptionMessage = "Network Failure"
        
        coEvery { repository.tailorResume(uri, jobDescription, provider) } returns Result.Error(AppException.Unknown(exceptionMessage))

        viewModel.uiState.test {
            assertEquals(ResumeUiState.Idle, awaitItem())

            viewModel.tailorResume(uri, jobDescription, provider)

            val loadingState = awaitItem() as ResumeUiState.Loading
            assertEquals("Processing with OpenAI... This may take a minute.", loadingState.stepMessage)

            val errorState = awaitItem() as ResumeUiState.Error
            assertEquals(exceptionMessage, errorState.message)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `reset sets state back to Idle`() = runTest {
        // Force error state first
        viewModel.tailorResume(mockk(), "", "provider")
        assertTrue(viewModel.uiState.value is ResumeUiState.Error)

        viewModel.reset()

        assertEquals(ResumeUiState.Idle, viewModel.uiState.value)
    }
}
