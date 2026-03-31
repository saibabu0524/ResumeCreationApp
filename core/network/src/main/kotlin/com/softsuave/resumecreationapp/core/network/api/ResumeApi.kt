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
import retrofit2.http.Path

// ── DTOs ──────────────────────────────────────────────────────────────────────

/**
 * Response body from POST /resume/tailor (202 Accepted).
 * The heavy pipeline runs in the background — clients poll [getJobStatus].
 */
@Serializable
data class TailorJobResponseDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
)

/**
 * Response body from GET /resume/jobs/{job_id}.
 * [status] is one of: queued | processing | completed | failed.
 */
@Serializable
data class JobStatusDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val provider: String? = null,
    @SerialName("original_filename") val originalFilename: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val error: String? = null,
)

/**
 * DTO representing a single history entry from GET /api/v1/resume/history.
 * Matches the backend TailoredResume model JSON shape.
 */
@Serializable
data class ResumeHistoryItemDto(
    val id: String,
    val status: String = "completed",
    @SerialName("job_description") val jobDescription: String = "",
    val provider: String = "",
    @SerialName("original_filename") val originalFilename: String = "",
    @SerialName("stored_filename") val storedFilename: String? = null,
    @SerialName("uploaded_stored_filename") val uploadedStoredFilename: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

// ── API interface ─────────────────────────────────────────────────────────────

interface ResumeApi {

    /**
     * POST /api/v1/resume/tailor — enqueue a tailoring job.
     * Returns 202 Accepted with a [TailorJobResponseDto] containing the job ID.
     * Poll [getJobStatus] with the returned job ID to track progress.
     */
    @Multipart
    @POST("resume/tailor")
    suspend fun tailorResume(
        @Part resume: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
        @Part("provider") provider: RequestBody,
    ): ApiResponseDto<TailorJobResponseDto>

    /**
     * GET /api/v1/resume/jobs/{job_id} — poll job status.
     * Returns queued | processing | completed | failed.
     * When completed, [JobStatusDto.downloadUrl] is populated.
     */
    @GET("resume/jobs/{job_id}")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String,
    ): ApiResponseDto<JobStatusDto>

    /**
     * GET /api/v1/resume/jobs/{job_id}/download — stream the tailored PDF.
     * Only valid when the job status is "completed".
     */
    @GET("resume/jobs/{job_id}/download")
    suspend fun downloadTailoredResume(
        @Path("job_id") jobId: String,
    ): ResponseBody

    /** GET /api/v1/resume/history — returns the current user's tailored resume history. */
    @GET("resume/history")
    suspend fun getHistory(): Result<ApiResponseDto<List<ResumeHistoryItemDto>>>
}

