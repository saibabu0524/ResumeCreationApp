package com.softsuave.resumecreationapp.core.domain.model

/**
 * Universal return type for any operation that can succeed, fail, or be loading.
 *
 * Rules:
 *  - Every repository function returns `Flow<Result<T>>` or `Result<T>`.
 *  - [Error] wraps [AppException] — raw JVM exceptions never reach the UI.
 *  - [Loading] is emitted before the first data emission in a [Flow] pipeline.
 */
sealed class Result<out T> {

    /** Operation completed successfully with [data]. */
    data class Success<T>(val data: T) : Result<T>()

    /** Operation failed with a typed [exception]. */
    data class Error(val exception: AppException) : Result<Nothing>()

    /** Operation is in-flight (used as initial state in Flow pipelines). */
    data object Loading : Result<Nothing>()

    // ─── Helpers ─────────────────────────────────────────────────────────────

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    /** Returns the data if this is [Success], otherwise `null`. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the exception if this is [Error], otherwise `null`. */
    fun exceptionOrNull(): AppException? = (this as? Error)?.exception
}

// ─── Extension functions ──────────────────────────────────────────────────────

/**
 * Executes [action] if this is [Result.Success]. Always returns `this` for chaining.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Executes [action] if this is [Result.Error]. Always returns `this` for chaining.
 */
inline fun <T> Result<T>.onError(action: (AppException) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

/**
 * Executes [action] if this is [Result.Loading]. Always returns `this` for chaining.
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

/**
 * Maps a [Result.Success] value using [transform], leaving [Result.Error] and
 * [Result.Loading] unchanged.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Returns [default] when this is not [Result.Success].
 */
fun <T> Result<T>.getOrDefault(default: T): T = (this as? Result.Success)?.data ?: default
