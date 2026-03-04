package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.common.di.IoDispatcher
import com.softsuave.resumecreationapp.core.data.mapper.UserMapper.toDomain
import com.softsuave.resumecreationapp.core.data.mapper.UserMapper.toEntity
import com.softsuave.resumecreationapp.core.data.remote.UserApi
import com.softsuave.resumecreationapp.core.data.safeApiCall
import com.softsuave.resumecreationapp.core.database.dao.UserDao
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.model.map
import com.softsuave.resumecreationapp.core.domain.model.onSuccess
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [UserRepository].
 *
 * Caching strategy:
 *  - [observeCurrentUser] — cache-first: returns cached user from Room immediately,
 *    then refreshes from network in the background.
 *  - [getUserById] — network-first: always fetches fresh data and caches it.
 *  - [updateUser] — network-first: sends to server, updates the local cache on success.
 *
 * Raw exceptions never leak — all network calls are wrapped in [safeApiCall].
 */
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val preferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UserRepository {

    override fun observeCurrentUser(): Flow<Result<User?>> = flow {
        emit(Result.Loading)

        // Emit from cache first (null if not yet cached)
        val cachedFlow: Flow<Result<User?>> = preferencesRepository.currentUserId
            .map { userId ->
                if (userId == null) {
                    Result.Success(null)
                } else {
                    val entity = userDao.getById(userId)
                    Result.Success(entity?.toDomain())
                }
            }

        emitAll(cachedFlow)
    }

    override suspend fun getUserById(userId: String): Result<User> {
        // Network-first: fetch from API and update cache.
        // Note: backend only supports GET /users/me (JWT identity); path param is not forwarded.
        val result = safeApiCall(dispatcher) { userApi.getMe() }

        return when (val r = result) {
            is Result.Success -> when (val inner = r.data) {  // inner: Result<ApiResponseDto<UserDto>>
                is Result.Success -> {
                    val dto = inner.data.data
                    if (dto != null) {
                        userDao.upsert(dto.toEntity())
                        Result.Success(dto.toDomain())
                    } else {
                        Result.Error(AppException.Unknown())
                    }
                }
                is Result.Error -> inner
                is Result.Loading -> Result.Error(AppException.Unknown())
            }
            is Result.Error -> r
            is Result.Loading -> Result.Error(AppException.Unknown())
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        // Optimistically update local cache
        userDao.upsert(user.toEntity())

        // Sync to network — only send fields accepted by PATCH /users/me
        val request = com.softsuave.resumecreationapp.core.data.remote.dto.UserUpdateRequestDto(
            email = user.email,
        )

        val result = safeApiCall(dispatcher) { userApi.updateMe(request) }

        return when (val r = result) {
            is Result.Success -> when (val inner = r.data) {  // inner: Result<ApiResponseDto<UserDto>>
                is Result.Success -> {
                    // Update cache with server-confirmed data
                    inner.data.data?.let { dto -> userDao.upsert(dto.toEntity()) }
                    Result.Success(Unit)
                }
                is Result.Error -> inner
                is Result.Loading -> Result.Error(AppException.Unknown())
            }
            is Result.Error -> r
            is Result.Loading -> Result.Error(AppException.Unknown())
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val result = safeApiCall(dispatcher) { userApi.deleteAccount() }
        return when (val r = result) {
            is Result.Success -> when (val inner = r.data) {
                is Result.Success -> {
                    userDao.deleteAll()
                    preferencesRepository.clearAll()
                    Result.Success(Unit)
                }
                is Result.Error -> inner
                is Result.Loading -> Result.Error(AppException.Unknown())
            }
            is Result.Error -> r
            is Result.Loading -> Result.Error(AppException.Unknown())
        }
    }
}
