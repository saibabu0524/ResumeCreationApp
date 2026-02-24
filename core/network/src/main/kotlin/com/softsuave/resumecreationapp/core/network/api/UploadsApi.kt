package com.softsuave.resumecreationapp.core.network.api

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.dto.MessageResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// ─── Response DTO ─────────────────────────────────────────────────────────────

/**
 * Metadata returned by `POST /api/v1/uploads/` after a successful file upload.
 */
@Serializable
data class UploadResponseDto(
    @SerialName("original_filename") val originalFilename: String? = null,
    @SerialName("stored_filename") val storedFilename: String,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
)

// ─── API Interface ────────────────────────────────────────────────────────────

/**
 * Retrofit API service for file upload/download endpoints.
 *
 * POST /api/v1/uploads/          — Upload a file (authenticated).
 * GET  /api/v1/uploads/{filename} — Download a previously uploaded file.
 *
 * All functions return [Result] via [com.softsuave.resumecreationapp.core.network.adapter.ApiResultCallAdapterFactory].
 */
interface UploadsApi {

    /**
     * Upload a file. The server validates MIME type and size.
     * Returns metadata about the stored file including its UUID-based stored filename.
     */
    @Multipart
    @POST("uploads/")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
    ): Result<ApiResponseDto<UploadResponseDto>>

    /**
     * Download a previously uploaded file by its stored (UUID-based) filename.
     * Returns the raw bytes as a [ResponseBody] — callers must close it after reading.
     */
    @GET("uploads/{filename}")
    suspend fun downloadFile(
        @Path("filename") filename: String,
    ): Result<ResponseBody>
}
