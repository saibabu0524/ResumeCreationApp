package com.softsuave.resumecreationapp.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches a Bearer token to every outgoing HTTP request.
 *
 * The token is fetched lazily from [tokenProvider] on each request, so token
 * refreshes are automatically picked up without recreating the OkHttp client.
 *
 * If [tokenProvider] returns `null` (user is signed out), the request is sent
 * without an Authorization header and the server will respond with 401, which
 * [com.softsuave.resumecreationapp.core.network.ExceptionMapper] maps to [AppException.Unauthorized].
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Safety net: never attach a Bearer token to public auth endpoints.
        // In practice these routes use the unauthenticated Retrofit client, but this
        // guard prevents accidental token attachment if routing is ever misconfigured.
        if (AUTH_PATHS.any { originalRequest.url.encodedPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider() ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        val AUTH_PATHS = setOf(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout",
        )
    }
}
