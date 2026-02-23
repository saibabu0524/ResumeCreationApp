package com.softsuave.resumecreationapp.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import timber.log.Timber
import kotlin.math.pow

/**
 * Base [CoroutineWorker] providing standard error reporting, retry logic
 * with exponential backoff, and progress tracking.
 *
 * Concrete workers extend this and implement [doActualWork].
 * This class handles generic error reporting so workers focus on their task.
 *
 * Retry policy:
 * - Returns [Result.retry] on transient failures (up to [MAX_RETRIES])
 * - Returns [Result.failure] on non-retryable errors or after max retries
 * - Backoff delay = `BASE_BACKOFF_MS * 2^runAttemptCount`
 */
abstract class BaseCoroutineWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /**
     * Implement the actual work here. Throwing an exception will be caught
     * by [doWork] and converted to a retry or failure result.
     *
     * @return [Result.success] on success, or throw for automatic retry handling.
     */
    abstract suspend fun doActualWork(): Result

    final override suspend fun doWork(): Result {
        return try {
            Timber.d("${this::class.simpleName} started (attempt ${runAttemptCount + 1})")
            val result = doActualWork()
            Timber.d("${this::class.simpleName} completed with $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "${this::class.simpleName} failed on attempt ${runAttemptCount + 1}")
            handleError(e)
        }
    }

    /**
     * Determines whether to retry or fail based on attempt count
     * and the type of exception.
     */
    private fun handleError(exception: Exception): Result {
        return if (runAttemptCount < MAX_RETRIES && isRetryable(exception)) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    /**
     * Override to control which exceptions trigger retries.
     * By default, all exceptions are retryable.
     */
    protected open fun isRetryable(exception: Exception): Boolean = true

    /**
     * Calculates exponential backoff delay for the current attempt.
     * Useful for manual delay inside [doActualWork] if needed.
     */
    protected fun calculateBackoffDelay(): Long {
        return BASE_BACKOFF_MS * BACKOFF_MULTIPLIER.pow(runAttemptCount).toLong()
    }

    companion object {
        const val MAX_RETRIES = 3
        private const val BASE_BACKOFF_MS = 1_000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }
}
