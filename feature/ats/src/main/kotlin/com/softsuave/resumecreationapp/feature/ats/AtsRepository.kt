package com.softsuave.resumecreationapp.feature.ats

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import com.softsuave.resumecreationapp.core.network.api.AtsApi
import com.softsuave.resumecreationapp.core.common.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtsRepository @Inject constructor(
    private val atsApi: AtsApi,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun analyseAts(
        pdfUri: Uri,
        jobDescription: String,
        provider: String,
    ): Result<AtsResult> = withContext(ioDispatcher) {
        try {
            val tempFile = copyUriToTempFile(pdfUri)
                ?: return@withContext Result.Error(
                    ExceptionMapper.map(Exception("Could not read PDF file"))
                )

            val requestFile = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("resume", tempFile.name, requestFile)
            val jdBody = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            val provBody = provider.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = atsApi.analyseAts(body, jdBody, provBody)
            tempFile.delete()

            val dto = response.data
                ?: return@withContext Result.Error(
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

    private fun copyUriToTempFile(uri: Uri): File? {
        var fileName = "temp_resume.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) fileName = cursor.getString(idx)
            }
        }
        val tempFile = File(context.cacheDir, "ats_upload_$fileName")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
