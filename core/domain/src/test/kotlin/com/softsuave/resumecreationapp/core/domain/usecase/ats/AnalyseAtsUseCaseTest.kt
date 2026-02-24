package com.softsuave.resumecreationapp.core.domain.usecase.ats

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.SectionScores
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyseAtsUseCaseTest {

    private lateinit var atsRepository: AtsRepository
    private lateinit var useCase: AnalyseAtsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        atsRepository = mockk()
        useCase = AnalyseAtsUseCase(atsRepository, testDispatcher)
    }

    @Test
    fun `execute returns success when repository succeeds`() = runTest {
        val expectedResult = AtsResult(
            overallScore = 80,
            scoreLabel = "Good",
            keywordsPresent = listOf("Kotlin"),
            keywordsMissing = listOf("CI/CD"),
            sectionScores = SectionScores(
                skillsMatch = 80,
                experienceRelevance = 75,
                educationMatch = 70,
                formatting = 90,
            ),
            suggestions = listOf("Add CI/CD experience"),
            strengths = listOf("Strong Kotlin skills"),
            summary = "Good match overall.",
        )
        val params = AnalyseAtsUseCase.Params(
            pdfBytes = byteArrayOf(1, 2, 3),
            fileName = "resume.pdf",
            jobDescription = "Developer",
            provider = "gemini",
        )
        coEvery {
            atsRepository.analyseAts(any(), any(), any(), any())
        } returns Result.Success(expectedResult)

        val result = useCase(params)

        assertTrue(result is Result.Success)
        assertEquals(expectedResult, (result as Result.Success).data)
        coVerify(exactly = 1) {
            atsRepository.analyseAts(
                pdfBytes = params.pdfBytes,
                fileName = params.fileName,
                jobDescription = params.jobDescription,
                provider = params.provider,
            )
        }
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        val exception = AppException.ServerError(message = "Server error")
        val params = AnalyseAtsUseCase.Params(
            pdfBytes = byteArrayOf(1),
            fileName = "resume.pdf",
            jobDescription = "job",
            provider = "provider",
        )
        coEvery {
            atsRepository.analyseAts(any(), any(), any(), any())
        } returns Result.Error(exception)

        val result = useCase(params)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
