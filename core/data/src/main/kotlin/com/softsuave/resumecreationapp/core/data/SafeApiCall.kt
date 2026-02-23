package com.softsuave.resumecreationapp.core.data

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Wraps a suspending API call in a `try/catch` and maps the result to [Result].
 *
 * All repository functions that call a network API must use this wrapper.
 * It guarantees:
 *  - [Result.Success] is returned only when the call succeeds.
 *  - [Result.Error] with a typed [AppException] is returned for every failure.
 *  - Raw exceptions NEVER propagate past the data layer.
 *
 * ```kotlin
 * suspend fun getUser(id: String): Result<UserDto> = safeApiCall(ioDispatcher) {
 *     api.getUser(id)
 * }
 * ```
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T,
): Result<T> = withContext(dispatcher) {
    try {
        Result.Success(apiCall())
    } catch (throwable: Throwable) {
        Result.Error(ExceptionMapper.map(throwable))
    }
}
