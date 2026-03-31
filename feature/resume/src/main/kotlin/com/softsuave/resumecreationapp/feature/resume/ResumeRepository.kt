package com.softsuave.resumecreationapp.feature.resume

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import com.softsuave.resumecreationapp.core.network.api.JobStatusDto
import com.softsuave.resumecreationapp.core.network.api.ResumeApi
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
class ResumeRepository @Inject constructor(
    private val resumeApi: ResumeApi,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Submit a resume-tailoring job to the server.
     * Returns the [jobId] string on success (used for polling and download).
     */
    suspend fun submitTailorJob(
        pdfUri: Uri,
        jobDescription: String,
        provider: String,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val tempFile = copyUriToTempFile(pdfUri)
                ?: return@withContext Result.Error(ExceptionMapper.map(Exception("Could not read PDF file")))

            val requestFile = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("resume", tempFile.name, requestFile)
            val jdBody = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            val provBody = provider.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = resumeApi.tailorResume(body, jdBody, provBody)
            tempFile.delete()

            val jobId = response.data?.jobId
            if (jobId.isNullOrBlank()) {
                Result.Error(ExceptionMapper.map(Exception("Server did not return a job ID")))
            } else {
                Result.Success(jobId)
            }
        } catch (e: Exception) {
            Result.Error(ExceptionMapper.map(e))
        }
    }

    /**
     * Poll the status of a previously submitted tailoring job.
     * Returns a [JobStatusDto] whose [JobStatusDto.status] is one of:
     * queued | processing | completed | failed.
     */
    suspend fun getJobStatus(jobId: String): Result<JobStatusDto> = withContext(ioDispatcher) {
        try {
            val response = resumeApi.getJobStatus(jobId)
            val data = response.data
                ?: return@withContext Result.Error(AppException.Unknown(message = "Empty status response"))
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(ExceptionMapper.map(e))
        }
    }

    /**
     * Download the tailored PDF for a completed job.
     * Only call this after [getJobStatus] returns status == "completed".
     */
    suspend fun downloadTailoredResume(jobId: String): Result<ByteArray> = withContext(ioDispatcher) {
        try {
            val bytes = resumeApi.downloadTailoredResume(jobId).bytes()
            if (bytes.isNotEmpty()) {
                Result.Success(bytes)
            } else {
                Result.Error(ExceptionMapper.map(Exception("Received empty PDF from server")))
            }
        } catch (e: Exception) {
            Result.Error(ExceptionMapper.map(e))
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        var fileName = "temp_resume.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        val tempFile = File(context.cacheDir, "upload_$fileName")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}

