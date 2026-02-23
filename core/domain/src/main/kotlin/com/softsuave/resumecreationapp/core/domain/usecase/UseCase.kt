package com.softsuave.resumecreationapp.core.domain.usecase

import com.softsuave.resumecreationapp.core.domain.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Base class for a use case that performs a single suspending operation and returns [Result].
 *
 * @param P Parameter type accepted by [invoke]. Use [Unit] for no parameters.
 * @param R Return type wrapped inside [Result].
 *
 * Usage:
 * ```kotlin
 * class GetUserProfileUseCase @Inject constructor(
 *     private val userRepository: UserRepository,
 *     @IoDispatcher dispatcher: CoroutineDispatcher,
 * ) : UseCase<GetUserProfileUseCase.Params, UserProfile>(dispatcher) {
 *
 *     override suspend fun execute(parameters: Params): Result<UserProfile> =
 *         userRepository.getUserProfile(parameters.userId)
 *
 *     data class Params(val userId: String)
 * }
 * ```
 */
abstract class UseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher,
) {
    /**
     * Executes [execute] on the provided [dispatcher] and returns the result.
     * Callers should always call the operator, never [execute] directly.
     */
    suspend operator fun invoke(parameters: P): Result<R> =
        withContext(dispatcher) { execute(parameters) }

    /**
     * Override to implement the business logic. Runs on [dispatcher].
     */
    protected abstract suspend fun execute(parameters: P): Result<R>
}

/**
 * Base class for a use case that returns a [Flow] of [Result] values.
 *
 * @param P Parameter type accepted by [invoke]. Use [Unit] for no parameters.
 * @param R The type of individual items emitted inside [Result.Success].
 *
 * Usage:
 * ```kotlin
 * class ObserveUsersUseCase @Inject constructor(
 *     private val userRepository: UserRepository,
 *     @IoDispatcher dispatcher: CoroutineDispatcher,
 * ) : FlowUseCase<Unit, List<User>>(dispatcher) {
 *
 *     override fun execute(parameters: Unit): Flow<Result<List<User>>> =
 *         userRepository.getUsers()
 * }
 * ```
 */
abstract class FlowUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher,
) {
    /**
     * Returns a [Flow] that emits [Result] values on [dispatcher].
     * Callers should always call the operator, never [execute] directly.
     */
    operator fun invoke(parameters: P): Flow<Result<R>> =
        execute(parameters).flowOn(dispatcher)

    /**
     * Override to implement the reactive business logic.
     */
    protected abstract fun execute(parameters: P): Flow<Result<R>>
}
