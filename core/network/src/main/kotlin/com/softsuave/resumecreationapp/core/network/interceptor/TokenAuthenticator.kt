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
        val refreshToken = refreshTokenProvider() ?: return null

        // Synchronization prevents multiple 401's from spamming the refresh API
        synchronized(this) {
            // First check if another thread already refreshed while we were waiting
            val previousToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = tokenProvider()

            // If the token has already been refreshed by another thread, retry with the new token
            if (previousToken != currentToken && currentToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Simple approach: execute blocking call
            val result = runBlocking {
                authApiProvider.get().refresh(RefreshRequestDto(refreshToken))
            }

            return when (result) {
                is Result.Success -> {
                    val newAccessToken = result.data.data?.accessToken
                    val newRefreshToken = result.data.data?.refreshToken
                    if (newAccessToken != null && newRefreshToken != null) {
                        onTokenRefreshed(newAccessToken, newRefreshToken)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        onLogout()
                        null
                    }
                }
                else -> {
                    // Refresh failed, user must be logged out
                    onLogout()
                    null
                }
            }
        }
    }
}
