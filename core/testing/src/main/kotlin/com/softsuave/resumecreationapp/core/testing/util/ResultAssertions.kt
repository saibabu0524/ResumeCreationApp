package com.softsuave.resumecreationapp.core.testing.util

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Shared assertion helpers for [Result] testing.
 *
 * These utilities make test assertions on [Result] concise and readable,
 * avoiding repeated `is Result.Success` casts across the test suite.
 *
 * Usage:
 * ```kotlin
 * val result = useCase(params)
 * assertSuccess(result) { data ->
 *     assertEquals("Jane Doe", data.name)
 * }
 * ```
 */
object ResultAssertions {

    /**
     * Asserts that [result] is [Result.Success] and executes [assertions] on the data.
     */
    fun <T> assertSuccess(result: Result<T>, assertions: (T) -> Unit = {}) {
        assertInstanceOf(Result.Success::class.java, result, "Expected Result.Success but got $result")
        assertions((result as Result.Success<T>).data)
    }

    /**
     * Asserts that [result] is [Result.Error] and optionally checks the exception type.
     */
    fun assertError(
        result: Result<*>,
        expectedExceptionType: Class<out AppException>? = null,
        assertions: (AppException) -> Unit = {},
    ) {
        assertInstanceOf(Result.Error::class.java, result, "Expected Result.Error but got $result")
        val error = (result as Result.Error).exception
        if (expectedExceptionType != null) {
            assertInstanceOf(
                expectedExceptionType,
                error,
                "Expected ${expectedExceptionType.simpleName} but got ${error::class.simpleName}",
            )
        }
        assertions(error)
    }

    /**
     * Asserts that [result] is [Result.Loading].
     */
    fun assertLoading(result: Result<*>) {
        assertInstanceOf(Result.Loading::class.java, result, "Expected Result.Loading but got $result")
    }

    /**
     * Asserts that [result] is [Result.Success] and its data equals [expected].
     */
    fun <T> assertSuccessEquals(expected: T, result: Result<T>) {
        assertSuccess(result) { actual ->
            assertEquals(expected, actual)
        }
    }

    /**
     * Asserts that [result] is [Result.Success] and the data is not null.
     */
    fun <T> assertSuccessNotNull(result: Result<T?>) {
        assertSuccess(result) { data ->
            assertNotNull(data)
        }
    }

    /**
     * Asserts that [result] is [Result.Error] with an [AppException.NotFound].
     */
    fun assertNotFoundError(result: Result<*>) {
        assertError(result, AppException.NotFound::class.java)
    }

    /**
     * Asserts that [result] is [Result.Error] with an [AppException.NetworkError].
     */
    fun assertNetworkError(result: Result<*>) {
        assertError(result, AppException.NetworkError::class.java)
    }
}
