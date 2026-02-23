package com.softsuave.resumecreationapp.core.domain.usecase.user

import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.model.User
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import com.softsuave.resumecreationapp.core.domain.usecase.FlowUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the currently signed-in [User] as a [Flow].
 *
 * Emits [Result.Loading] on subscription, then [Result.Success] containing the
 * current user (or `null` when signed-out) whenever the value changes in the
 * underlying data source.
 *
 * Parameters: [Unit] — no input required.
 */
class ObserveCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher,
) : FlowUseCase<Unit, User?>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<User?>> =
        userRepository.observeCurrentUser()
}
