package com.softsuave.resumecreationapp.core.domain.usecase.user

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.domain.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Updates mutable fields of the authenticated [User].
 *
 * The full updated [User] domain model is provided as a parameter.
 * The repository is responsible for persisting changes to remote and local sources.
 */
class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<UpdateUserUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(parameters: Params): Result<Unit> =
        userRepository.updateUser(parameters.user)

    data class Params(val user: User)
}
