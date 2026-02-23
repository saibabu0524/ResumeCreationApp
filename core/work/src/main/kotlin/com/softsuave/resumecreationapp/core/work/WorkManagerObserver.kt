package com.softsuave.resumecreationapp.core.work

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Maps [WorkInfo] state streams into the app's [Result] type so the UI
 * layer can observe background work with the same patterns used for
 * any other data stream.
 *
 * Usage:
 * ```kotlin
 * val syncState = workManagerObserver.observeWork(workRequestId)
 *     .collectAsStateWithLifecycle()
 * ```
 */
class WorkManagerObserver @Inject constructor(
    private val workManager: WorkManager,
) {

    /**
     * Observes the [WorkInfo] for a specific work request and maps it
     * to [Result].
     */
    fun observeWork(workRequestId: UUID): Flow<Result<WorkInfo.State>> {
        return workManager.getWorkInfoByIdFlow(workRequestId)
            .map { workInfo ->
                if (workInfo == null) Result.Loading else mapWorkInfoToResult(workInfo)
            }
    }

    /**
     * Observes all work with a given [tag] and maps to [Result].
     */
    fun observeWorkByTag(tag: String): Flow<Result<List<WorkInfo.State>>> {
        return workManager.getWorkInfosByTagFlow(tag)
            .map { workInfoList ->
                mapWorkInfoListToResult(workInfoList)
            }
    }

    private fun mapWorkInfoToResult(workInfo: WorkInfo): Result<WorkInfo.State> {
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> Result.Success(WorkInfo.State.SUCCEEDED)
            WorkInfo.State.FAILED -> Result.Error(
                AppException.Unknown("Background work failed"),
            )
            WorkInfo.State.CANCELLED -> Result.Error(
                AppException.Unknown("Background work was cancelled"),
            )
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
            -> Result.Loading
        }
    }

    private fun mapWorkInfoListToResult(
        workInfoList: List<WorkInfo>,
    ): Result<List<WorkInfo.State>> {
        if (workInfoList.isEmpty()) return Result.Loading

        val states = workInfoList.map { info -> info.state }
        return when {
            states.any { state -> state == WorkInfo.State.FAILED } ->
                Result.Error(AppException.Unknown("One or more work items failed"))
            states.all { state -> state == WorkInfo.State.SUCCEEDED } ->
                Result.Success(states)
            else -> Result.Loading
        }
    }
}
