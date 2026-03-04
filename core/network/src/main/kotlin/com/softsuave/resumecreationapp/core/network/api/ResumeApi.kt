package com.softsuave.resumecreationapp.core.network.api

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * DTO representing a single history entry from `GET /api/v1/resume/history`.
 * Matches the backend `TailoredResume` model JSON shape.
 */
@Serializable
data class ResumeHistoryItemDto(
    val id: String,
    @SerialName("job_description") val jobDescription: String,
    val provider: String,
    @SerialName("original_filename") val originalFilename: String,
    @SerialName("stored_filename") val storedFilename: String,
    @SerialName("uploaded_stored_filename") val uploadedStoredFilename: String? = null,
    @SerialName("created_at") val createdAt: String,
)

interface ResumeApi {

    /** POST /api/v1/resume/tailor — upload a PDF and job description, returns a tailored PDF. */
    @Multipart
    @POST("resume/tailor")
    suspend fun tailorResume(
        @Part resume: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
        @Part("provider") provider: RequestBody,
    ): ResponseBody

    /** GET /api/v1/resume/history — returns the current user's tailored resume history. */
    @GET("resume/history")
    suspend fun getHistory(): Result<ApiResponseDto<List<ResumeHistoryItemDto>>>
}
