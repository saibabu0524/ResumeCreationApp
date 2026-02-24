package com.softsuave.resumecreationapp.core.network.api

import com.softsuave.resumecreationapp.core.network.model.AtsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AtsApi {
    @Multipart
    @POST("ats/analyse")
    suspend fun analyseAts(
        @Part resume: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
        @Part("provider") provider: RequestBody,
    ): AtsResponse
}
