package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.data.di.HistoryResumeApi
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.ResumeHistoryItem
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.ResumeHistoryRepository
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import com.softsuave.resumecreationapp.core.network.api.ResumeApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ResumeHistoryRepository].
 *
 * Network-only: always fetches fresh data from `GET /api/v1/resume/history`.
 * Uses the [@HistoryResumeApi]-qualified [ResumeApi] which is built on the authenticated
 * Retrofit client (with JSON converter + auth interceptor).
 *
 * Follows the same try/catch + [ExceptionMapper] pattern as [AtsRepositoryImpl].
 */
@Singleton
class ResumeHistoryRepositoryImpl @Inject constructor(
    @HistoryResumeApi private val resumeApi: ResumeApi,
) : ResumeHistoryRepository {

    override suspend fun getHistory(): Result<List<ResumeHistoryItem>> {
        return try {
            when (val result = resumeApi.getHistory()) {
                is Result.Success -> {
                    val items = result.data.data
                    Result.Success(
                        items?.map { dto ->
                            ResumeHistoryItem(
                                id = dto.id,
                                jobDescription = dto.jobDescription,
                                provider = dto.provider,
                                originalFilename = dto.originalFilename,
                                storedFilename = dto.storedFilename,
                                uploadedStoredFilename = dto.uploadedStoredFilename,
                                createdAt = dto.createdAt,
                            )
                        } ?: emptyList()
                    )
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(AppException.Unknown())
            }
        } catch (e: Exception) {
            Result.Error(ExceptionMapper.map(e))
        }
    }
}
