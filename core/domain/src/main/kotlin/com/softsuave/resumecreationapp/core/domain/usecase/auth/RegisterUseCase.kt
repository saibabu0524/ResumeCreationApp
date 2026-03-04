package com.softsuave.resumecreationapp.core.domain.usecase.auth

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.AuthRepository
import com.softsuave.resumecreationapp.core.domain.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Registers a new user with email and password.
 *
 * Delegates to [AuthRepository.register] and propagates the [Result] directly.
 * Upon a successful registration the repository implementation automatically
 * logs the user in and persists the token pair.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<RegisterUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(parameters: Params): Result<Unit> =
        authRepository.register(
            email = parameters.email,
            password = parameters.password,
        )

    data class Params(
        val email: String,
        val password: String,
    )
}
