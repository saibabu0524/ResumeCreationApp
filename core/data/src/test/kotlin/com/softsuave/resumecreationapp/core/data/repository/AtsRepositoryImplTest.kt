package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.api.AtsApi
import com.softsuave.resumecreationapp.core.network.model.AtsResultDto
import com.softsuave.resumecreationapp.core.network.model.SectionScoresDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AtsRepositoryImplTest {

    private lateinit var atsApi: AtsApi
    private lateinit var repository: AtsRepositoryImpl

    @BeforeEach
    fun setup() {
        atsApi = mockk()
        repository = AtsRepositoryImpl(atsApi)
    }

    @Test
    fun `analyseAts returns Success when API succeeds`() = runTest {
        // Arrange
        val dto = AtsResultDto(
            overallScore = 85,
            scoreLabel = "Good",
            keywordsPresent = listOf("Kotlin"),
            keywordsMissing = listOf("Java"),
            sectionScores = SectionScoresDto(
                skillsMatch = 90,
                experienceRelevance = 80,
                educationMatch = 100,
                formatting = 70
            ),
            suggestions = emptyList(),
            strengths = emptyList(),
            summary = "Good match"
        )
        val apiResponse = ApiResponseDto(data = dto, message = "success")

        coEvery { atsApi.analyseAts(any(), any(), any()) } returns apiResponse

        // Act
        val result = repository.analyseAts(
            pdfBytes = byteArrayOf(1, 2, 3),
            fileName = "resume.pdf",
            jobDescription = "Developer",
            provider = "OpenAI"
        )

        // Assert
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(85, data.overallScore)
        assertEquals("Good", data.scoreLabel)
        assertEquals(90, data.sectionScores.skillsMatch)
    }

    @Test
    fun `analyseAts returns Error when API returns empty data`() = runTest {
        // Arrange
        val apiResponse = ApiResponseDto<AtsResultDto>(data = null, message = "success")
        coEvery { atsApi.analyseAts(any(), any(), any()) } returns apiResponse

        // Act
        val result = repository.analyseAts(
            pdfBytes = byteArrayOf(1),
            fileName = "resume.pdf",
            jobDescription = "job",
            provider = "provider"
        )

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Empty response from ATS API", (result as Result.Error).exception.message)
    }

    @Test
    fun `analyseAts returns Error on exception`() = runTest {
        // Arrange
        coEvery { atsApi.analyseAts(any(), any(), any()) } throws RuntimeException("Network Error")

        // Act
        val result = repository.analyseAts(
            pdfBytes = byteArrayOf(1),
            fileName = "resume.pdf",
            jobDescription = "job",
            provider = "provider"
        )

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Network Error", (result as Result.Error).exception.message)
    }
}
