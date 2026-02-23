package com.softsuave.resumecreationapp.core.domain.usecase.user

import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GetUserByIdUseCase].
 *
 * Uses [FakeUserRepository] — no mocks. Tests are resilient to
 * repository implementation changes (ADR-005).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetUserByIdUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: FakeUserRepository
    private lateinit var useCase: GetUserByIdUseCase

    @BeforeEach
    fun setup() {
        userRepository = FakeUserRepository()
        useCase = GetUserByIdUseCase(userRepository, testDispatcher)
    }

    @Test
    fun `when user exists, returns success with user`() = runTest {
        // Arrange
        val expectedUser = TestUsers.user()
        userRepository.addUser(expectedUser)

        // Act
        val result = useCase(GetUserByIdUseCase.Params(TestUsers.DEFAULT_USER_ID))

        // Assert
        assertInstanceOf(Result.Success::class.java, result)
        val user = (result as Result.Success).data
        assertEquals(expectedUser.id, user.id)
        assertEquals(expectedUser.displayName, user.displayName)
        assertEquals(expectedUser.email, user.email)
    }

    @Test
    fun `when user does not exist, returns not found error`() = runTest {
        // Act
        val result = useCase(GetUserByIdUseCase.Params("nonexistent_id"))

        // Assert
        assertInstanceOf(Result.Error::class.java, result)
        assertInstanceOf(
            AppException.NotFound::class.java,
            (result as Result.Error).exception,
        )
    }

    @Test
    fun `when repository returns error, returns error`() = runTest {
        // Arrange
        val networkError = AppException.NetworkError()
        userRepository.setShouldReturnError(true, networkError)

        // Act
        val result = useCase(GetUserByIdUseCase.Params(TestUsers.DEFAULT_USER_ID))

        // Assert
        assertInstanceOf(Result.Error::class.java, result)
        assertEquals(networkError, (result as Result.Error).exception)
    }

    @Test
    fun `fetches the correct user by id when multiple users exist`() = runTest {
        // Arrange
        val users = TestUsers.users(count = 5)
        users.forEach { userRepository.addUser(it) }
        val targetUser = users[2]

        // Act
        val result = useCase(GetUserByIdUseCase.Params(targetUser.id))

        // Assert
        assertInstanceOf(Result.Success::class.java, result)
        val user = (result as Result.Success).data
        assertEquals(targetUser.id, user.id)
        assertEquals(targetUser.displayName, user.displayName)
    }
}
