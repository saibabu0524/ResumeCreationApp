package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.SectionScores
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import com.softsuave.resumecreationapp.core.network.api.AtsApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AtsRepository].
 *
 * Converts raw [ByteArray] into a multipart request and delegates to [AtsApi].
 * Maps network DTOs to domain models.
 */
@Singleton
class AtsRepositoryImpl @Inject constructor(
    private val atsApi: AtsApi,
) : AtsRepository {

    override suspend fun analyseAts(
        pdfBytes: ByteArray,
        fileName: String,
        jobDescription: String,
        provider: String,
    ): Result<AtsResult> {
        return try {
            val requestFile = pdfBytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("resume", fileName, requestFile)
            val jdBody = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            val provBody = provider.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = atsApi.analyseAts(body, jdBody, provBody)

            val dto = response.data
                ?: return Result.Error(
                    ExceptionMapper.map(Exception("Empty response from ATS API"))
                )

            Result.Success(
                AtsResult(
                    overallScore = dto.overallScore,
                    scoreLabel = dto.scoreLabel,
                    keywordsPresent = dto.keywordsPresent,
                    keywordsMissing = dto.keywordsMissing,
                    sectionScores = SectionScores(
                        skillsMatch = dto.sectionScores.skillsMatch,
                        experienceRelevance = dto.sectionScores.experienceRelevance,
                        educationMatch = dto.sectionScores.educationMatch,
                        formatting = dto.sectionScores.formatting,
                    ),
                    suggestions = dto.suggestions,
                    strengths = dto.strengths,
                    summary = dto.summary,
                )
            )
        } catch (e: Exception) {
            Result.Error(ExceptionMapper.map(e))
        }
    }
}
