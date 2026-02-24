package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.common.di.IoDispatcher
import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result as DomainResult
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository
import com.softsuave.resumecreationapp.core.network.api.AuthApi
import com.softsuave.resumecreationapp.core.network.api.LoginRequestDto
import com.softsuave.resumecreationapp.core.network.api.LogoutRequestDto
import com.softsuave.resumecreationapp.core.network.api.RefreshRequestDto
import com.softsuave.resumecreationapp.core.network.api.RegisterRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [AuthRepository].
 *
 * The [AuthApi] uses the unauthenticated Retrofit client — no Bearer token is attached.
 * Responses arrive already wrapped in [DomainResult] via [ApiResultCallAdapterFactory].
 *
 * All dispatching is performed via the injected [ioDispatcher] per project conventions
 * (never `Dispatchers.IO` directly).
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(email: String, password: String): DomainResult<Unit> =
        withContext(ioDispatcher) {
            when (val result = authApi.login(LoginRequestDto(email, password))) {
                is DomainResult.Success -> {
                    val tokens = result.data.data
                    if (tokens != null) {
                        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                        DomainResult.Success(Unit)
                    } else {
                        DomainResult.Error(AppException.Unknown("Empty token response from server."))
                    }
                }
                is DomainResult.Error -> result
                is DomainResult.Loading -> DomainResult.Error(AppException.Unknown())
            }
        }

    override suspend fun register(email: String, password: String): DomainResult<Unit> =
        withContext(ioDispatcher) {
            when (val result = authApi.register(RegisterRequestDto(email, password))) {
                is DomainResult.Success -> {
                    // Registration succeeded — automatically log in to obtain tokens.
                    login(email, password)
                }
                is DomainResult.Error -> result
                is DomainResult.Loading -> DomainResult.Error(AppException.Unknown())
            }
        }

    override suspend fun refreshToken(): DomainResult<Unit> =
        withContext(ioDispatcher) {
            val storedRefreshToken = tokenStorage.refreshToken
                ?: return@withContext DomainResult.Error(
                    AppException.NotAuthenticated("No refresh token found. Please sign in again.")
                )

            when (val result = authApi.refresh(RefreshRequestDto(storedRefreshToken))) {
                is DomainResult.Success -> {
                    val tokens = result.data.data
                    if (tokens != null) {
                        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                        DomainResult.Success(Unit)
                    } else {
                        DomainResult.Error(AppException.Unknown("Empty token response during refresh."))
                    }
                }
                is DomainResult.Error -> result
                is DomainResult.Loading -> DomainResult.Error(AppException.Unknown())
            }
        }

    override suspend fun logout(): DomainResult<Unit> =
        withContext(ioDispatcher) {
            val storedRefreshToken = tokenStorage.refreshToken
            if (storedRefreshToken != null) {
                // Best-effort — revoke the token on the server; ignore errors.
                // The server is idempotent (always returns 200) so failures are transient.
                authApi.logout(LogoutRequestDto(storedRefreshToken))
            }
            // Always clear local tokens regardless of server response.
            tokenStorage.clearTokens()
            DomainResult.Success(Unit)
        }
}
