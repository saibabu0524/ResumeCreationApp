package com.softsuave.resumecreationapp.feature.resume

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.api.ResumeApi
import com.softsuave.resumecreationapp.core.common.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import com.softsuave.resumecreationapp.core.domain.model.AppException

@Singleton
class ResumeRepository @Inject constructor(
    private val resumeApi: ResumeApi,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun tailorResume(
        pdfUri: Uri,
        jobDescription: String,
        provider: String
    ): Result<ByteArray> = withContext(ioDispatcher) {
        try {
            // First, copy the content from URI to a temporary file
            val tempFile = copyUriToTempFile(pdfUri)
            if (tempFile == null) {
                return@withContext Result.Error(ExceptionMapper.map(Exception("Could not read PDF file")))
            }

            val requestFile = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("resume", tempFile.name, requestFile)
            
            val jdBody = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            val provBody = provider.toRequestBody("text/plain".toMediaTypeOrNull())

            // Call API
            val responseBody = resumeApi.tailorResume(body, jdBody, provBody)
            
            // Cleanup temp file
            tempFile.delete()
            
            val bytes = responseBody.bytes()
            if (bytes.isNotEmpty()) {
                Result.Success(bytes)
            } else {
                Result.Error(ExceptionMapper.map(Exception("Received empty response from server")))
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
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Exception) {
            return null
        }
    }
}
