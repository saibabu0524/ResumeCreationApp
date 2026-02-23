package com.softsuave.resumecreationapp.core.work.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.softsuave.resumecreationapp.core.work.BaseCoroutineWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync worker.
 *
 * Reference implementation for the offline-first pattern:
 * 1. Write locally first → success to user immediately
 * 2. This worker syncs pending local changes to the server
 * 3. Runs periodically with network connectivity constraint
 *
 * Uses [HiltWorker] + [AssistedInject] for dependency injection.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : BaseCoroutineWorker(context, params) {

    override suspend fun doActualWork(): Result {
        Timber.d("SyncWorker: starting periodic sync")

        // Note: Inject repository and perform actual sync once API layer is complete
        // Example offline-first flow:
        // 1. val pendingChanges = localDataSource.getPendingChanges()
        // 2. pendingChanges.forEach { change ->
        //        remoteDataSource.push(change)
        //        localDataSource.markSynced(change.id)
        //    }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "sync_worker"
        private const val REPEAT_INTERVAL_HOURS = 1L

        /**
         * Enqueues the periodic sync worker.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so re-scheduling doesn't
         * restart an already-queued instance.
         */
        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
