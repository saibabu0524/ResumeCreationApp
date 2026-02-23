package com.softsuave.resumecreationapp.core.domain.usecase.user

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.domain.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Retrieves a [User] by their unique identifier.
 *
 * Returns [Result.Error] with [com.softsuave.resumecreationapp.core.domain.model.AppException.NotFound]
 * if no matching user exists.
 */
class GetUserByIdUseCase @Inject constructor(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<GetUserByIdUseCase.Params, User>(dispatcher) {

    override suspend fun execute(parameters: Params): Result<User> =
        userRepository.getUserById(parameters.userId)

    data class Params(val userId: String)
}
