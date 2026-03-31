package com.softsuave.resumecreationapp.core.network.interceptor

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.api.AuthApi
import com.softsuave.resumecreationapp.core.network.api.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Provider

/**
 * Automatically refreshes the access token when a 401 Unauthorized response is received.
 *
 * Implements a synchronized approach to ensure only one refresh request happens at a time.
 */
class TokenAuthenticator(
    private val authApiProvider: Provider<AuthApi>,
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String?,
    private val onTokenRefreshed: (String, String) -> Unit,
    private val onLogout: () -> Unit,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Guard 1: Never attempt refresh for auth endpoints themselves.
        val path = response.request.url.encodedPath
        if (AUTH_PATHS.any { path.contains(it) }) return null

        // Guard 2: Stop after one retry to prevent infinite refresh loops.
        // responseCount == 1  → first 401, attempt refresh.
        // responseCount >= 2  → retried request also returned 401, give up.
        if (responseCount(response) >= 2) {
            onLogout()
            return null
        }

        val refreshToken = refreshTokenProvider() ?: return null

        // Synchronization prevents multiple concurrent 401 responses from each
        // spawning a separate refresh request.
        synchronized(this) {
            // Re-read current token inside the lock — another thread may have
            // already refreshed while we were waiting to acquire the lock.
            val previousToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            val currentToken = tokenProvider()

            // Another thread already refreshed — just retry with the new token.
            if (previousToken != currentToken && currentToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val result = runBlocking {
                authApiProvider.get().refresh(RefreshRequestDto(refreshToken))
            }

            return when (result) {
                is Result.Success -> {
                    val tokenData = result.data.data
                    if (tokenData != null) {
                        onTokenRefreshed(tokenData.accessToken, tokenData.refreshToken)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${tokenData.accessToken}")
                            .build()
                    } else {
                        onLogout()
                        null
                    }
                }
                is Result.Error,
                is Result.Loading -> {
                    // Refresh failed (expired or invalid refresh token) — force logout.
                    onLogout()
                    null
                }
            }
        }
    }

    /** Counts how many responses are chained (original + prior redirects/retries). */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        /** URL path segments that belong to public auth endpoints. */
        val AUTH_PATHS = setOf(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout",
        )
    }
}
