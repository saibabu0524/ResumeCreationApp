package com.softsuave.resumecreationapp.core.network.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ResumeApi {
    @Multipart
    @POST("resume/tailor")
    suspend fun tailorResume(
        @Part resume: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
        @Part("provider") provider: RequestBody
    ): ResponseBody
}
