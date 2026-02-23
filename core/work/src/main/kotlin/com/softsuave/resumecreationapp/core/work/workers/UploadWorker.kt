package com.softsuave.resumecreationapp.core.work.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.softsuave.resumecreationapp.core.work.BaseCoroutineWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * One-shot worker for uploading files/data to the server.
 *
 * Reference implementation for:
 * - Exponential backoff via [BackoffPolicy.EXPONENTIAL]
 * - Input data passing via [Data]
 * - Queuing multiple independent uploads
 *
 * Enqueue via [enqueue] with an upload ID — the worker reads it from input data.
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : BaseCoroutineWorker(context, params) {

    override suspend fun doActualWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID)
            ?: return Result.failure()

        Timber.d("UploadWorker: processing upload id=$uploadId")

        // Note: Inject repository and perform actual upload once file storage endpoints are ready
        // Example:
        // 1. val pendingUpload = uploadRepository.getById(uploadId)
        // 2. remoteDataSource.upload(pendingUpload.data)
        // 3. uploadRepository.markCompleted(uploadId)

        return Result.success()
    }

    companion object {
        private const val KEY_UPLOAD_ID = "upload_id"
        private const val INITIAL_BACKOFF_SECONDS = 10L

        /**
         * Enqueues a one-shot upload for the given [uploadId].
         * Backoff is exponential starting at [INITIAL_BACKOFF_SECONDS].
         */
        fun enqueue(workManager: WorkManager, uploadId: String) {
            val data = Data.Builder()
                .putString(KEY_UPLOAD_ID, uploadId)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS,
                )
                .addTag("upload_$uploadId")
                .build()

            workManager.enqueue(request)
        }
    }
}
