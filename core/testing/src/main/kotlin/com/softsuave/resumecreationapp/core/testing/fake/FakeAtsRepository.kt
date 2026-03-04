package com.softsuave.resumecreationapp.core.testing.fake

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.SectionScores
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository

/**
 * Fake implementation of [AtsRepository] for testing.
 *
 * Configure behaviour via:
 *  - [setResult] — override what [analyseAts] returns
 *  - [setShouldReturnError] — make all calls return an error
 *
 * A sensible default [AtsResult] is pre-configured so most tests need
 * no setup at all.
 */
class FakeAtsRepository : AtsRepository {

    /** Default stub result returned unless overridden. */
    val defaultResult: AtsResult = AtsResult(
        overallScore = 78,
        scoreLabel = "Good",
        keywordsPresent = listOf("Kotlin", "Compose"),
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

    private var result: Result<AtsResult> = Result.Success(defaultResult)
    private var shouldReturnError = false
    private var errorToReturn: AppException = AppException.Unknown()

    var analyseCallCount: Int = 0
        private set

    // ─── Test Helpers ─────────────────────────────────────────────────────────

    fun setResult(atsResult: AtsResult) {
        result = Result.Success(atsResult)
    }

    fun setShouldReturnError(
        shouldError: Boolean,
        error: AppException = AppException.Unknown(),
    ) {
        shouldReturnError = shouldError
        errorToReturn = error
    }

    fun reset() {
        result = Result.Success(defaultResult)
        shouldReturnError = false
        errorToReturn = AppException.Unknown()
        analyseCallCount = 0
    }

    // ─── AtsRepository Implementation ────────────────────────────────────────

    override suspend fun analyseAts(
        pdfBytes: ByteArray,
        fileName: String,
        jobDescription: String,
        provider: String,
    ): Result<AtsResult> {
        analyseCallCount++
        return if (shouldReturnError) Result.Error(errorToReturn) else result
    }
}
