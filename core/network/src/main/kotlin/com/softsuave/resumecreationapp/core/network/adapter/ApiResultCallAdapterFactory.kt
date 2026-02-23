package com.softsuave.resumecreationapp.core.network.adapter

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.ExceptionMapper
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Retrofit [Call] wrapper that automatically converts every API response into [Result].
 *
 * With this adapter installed, API service functions return `Result<T>` directly:
 * ```kotlin
 * interface UserApi {
 *     @GET("users/{id}")
 *     suspend fun getUser(@Path("id") id: String): Result<UserDto>
 * }
 * ```
 * No try-catch is needed at the call site — success maps to [Result.Success],
 * any HTTP error or network exception maps to [Result.Error] via [ExceptionMapper].
 */
class ApiResultCall<T : Any>(
    private val delegate: Call<T>,
) : Call<Result<T>> {

    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val result = if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.Success(body)
                    } else {
                        Result.Error(
                            AppException.Unknown(message = "Response body was null for ${response.code()}")
                        )
                    }
                } else {
                    Result.Error(ExceptionMapper.map(retrofit2.HttpException(response)))
                }
                callback.onResponse(this@ApiResultCall, Response.success(result))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                val result = Result.Error(ExceptionMapper.map(t))
                callback.onResponse(this@ApiResultCall, Response.success(result))
            }
        })
    }

    override fun clone(): Call<Result<T>> = ApiResultCall(delegate.clone())
    override fun execute(): Response<Result<T>> = throw UnsupportedOperationException("ApiResultCall does not support synchronous execution.")
    override fun isExecuted(): Boolean = delegate.isExecuted
    override fun cancel() = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}

/**
 * [CallAdapter] that produces [ApiResultCall] for return types of `Result<T>`.
 */
class ApiResultCallAdapter<T : Any>(
    private val responseType: Type,
) : CallAdapter<T, Call<Result<T>>> {

    override fun responseType(): Type = responseType
    override fun adapt(call: Call<T>): Call<Result<T>> = ApiResultCall(call)
}

/**
 * [CallAdapter.Factory] for [ApiResultCallAdapter].
 *
 * Install via `Retrofit.Builder.addCallAdapterFactory(ApiResultCallAdapterFactory())`.
 */
class ApiResultCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        // Must be a parameterized Call<Result<T>>
        if (getRawType(returnType) != Call::class.java) return null
        if (returnType !is ParameterizedType) return null

        val callInnerType = getParameterUpperBound(0, returnType)
        if (getRawType(callInnerType) != Result::class.java) return null
        if (callInnerType !is ParameterizedType) return null

        val resultInnerType = getParameterUpperBound(0, callInnerType)
        return ApiResultCallAdapter<Any>(resultInnerType)
    }
}
