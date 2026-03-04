package com.softsuave.resumecreationapp.core.domain.repository

import com.softsuave.resumecreationapp.core.domain.model.ResumeHistoryItem
import com.softsuave.resumecreationapp.core.domain.model.Result

/**
 * Repository interface for resume history operations.
 *
 * Declared in `:core:domain` — the implementation lives in `:core:data`.
 * This interface depends only on domain types; it must never import
 * Room, Retrofit, DataStore, or any other framework.
 */
interface ResumeHistoryRepository {

    /**
     * Fetches the current user's tailored resume history from the backend.
     * Returns a list ordered newest-first.
     *
     * Returns [Result.Error] with an [com.softsuave.resumecreationapp.core.domain.model.AppException]
     * on network or server error.
     */
    suspend fun getHistory(): Result<List<ResumeHistoryItem>>
}
