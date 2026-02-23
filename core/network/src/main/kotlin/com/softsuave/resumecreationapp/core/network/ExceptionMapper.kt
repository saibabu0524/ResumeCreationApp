package com.softsuave.resumecreationapp.core.network

import com.softsuave.resumecreationapp.core.domain.model.AppException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

/**
 * Converts raw JVM and Retrofit exceptions into typed [AppException] variants.
 *
 * This is the single place where exception translation happens for the data layer.
 * Every `catch` block in a repository or `safeApiCall` must route through here.
 */
object ExceptionMapper {

    fun map(throwable: Throwable): AppException = when (throwable) {
        // Already mapped — pass through
        is AppException -> throwable

        // Network connectivity
        is UnknownHostException -> AppException.NoInternet(cause = throwable)
        is SocketTimeoutException -> AppException.Timeout(cause = throwable)
        is IOException -> AppException.NetworkError(cause = throwable)

        // Serialization
        is SerializationException -> AppException.Unknown(
            message = "Failed to parse server response.",
            cause = throwable,
        )

        // HTTP errors
        is HttpException -> mapHttpException(throwable)

        // Everything else
        else -> AppException.Unknown(
            message = throwable.message ?: "An unexpected error occurred.",
            cause = throwable,
        )
    }

    private fun mapHttpException(e: HttpException): AppException = when (e.code()) {
        401 -> AppException.Unauthorized(cause = e)
        403 -> AppException.Forbidden(cause = e)
        404 -> AppException.NotFound(cause = e)
        in 500..599 -> AppException.ServerError(code = e.code(), cause = e)
        else -> AppException.Unknown(
            message = "HTTP ${e.code()}: ${e.message()}",
            cause = e,
        )
    }
}
