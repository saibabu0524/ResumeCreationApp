package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result as DomainResult
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository
import com.softsuave.resumecreationapp.core.network.api.AuthApi
import com.softsuave.resumecreationapp.core.network.api.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        val result = authApi.login(email, password)
        if (result.isSuccess) {
            val body = result.getOrNull()
            if (body != null) {
                tokenStorage.saveAccessToken(body.access_token)
                DomainResult.Success(Unit)
            } else {
                DomainResult.Error(AppException.Unknown("Empty response from server"))
            }
        } else {
            val exception = result.exceptionOrNull()
            DomainResult.Error(AppException.Unknown(exception?.message ?: "Login failed"))
        }
    }

    override suspend fun register(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        val request = RegisterRequest(email, password)
        val result = authApi.register(request)
        if (result.isSuccess) {
            // Automatically log in after registration
            login(email, password)
        } else {
            val exception = result.exceptionOrNull()
            DomainResult.Error(AppException.Unknown(exception?.message ?: "Registration failed"))
        }
    }
}
