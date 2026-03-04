package com.softsuave.resumecreationapp.core.domain.usecase.auth

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository
import com.softsuave.resumecreationapp.core.domain.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Authenticates a user with email and password.
 *
 * Delegates to [AuthRepository.login] and propagates the [Result] directly.
 * The dispatcher ensures the repository call runs off the main thread.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<LoginUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(parameters: Params): Result<Unit> =
        authRepository.login(
            email = parameters.email,
            password = parameters.password,
        )

    data class Params(
        val email: String,
        val password: String,
    )
}
