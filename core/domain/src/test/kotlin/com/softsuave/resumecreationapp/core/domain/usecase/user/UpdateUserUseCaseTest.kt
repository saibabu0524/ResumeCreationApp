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
 * Unit tests for [UpdateUserUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateUserUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: FakeUserRepository
    private lateinit var useCase: UpdateUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = FakeUserRepository()
        useCase = UpdateUserUseCase(userRepository, testDispatcher)
    }

    @Test
    fun `when user exists, updates successfully`() = runTest {
        // Arrange
        val existingUser = TestUsers.user()
        userRepository.addUser(existingUser)
        val updatedUser = existingUser.copy(displayName = "New Name")

        // Act
        val result = useCase(UpdateUserUseCase.Params(updatedUser))

        // Assert
        assertInstanceOf(Result.Success::class.java, result)

        // Verify user was actually updated in repository
        val fetchResult = userRepository.getUserById(existingUser.id)
        assertInstanceOf(Result.Success::class.java, fetchResult)
        assertEquals("New Name", (fetchResult as Result.Success).data.displayName)
    }

    @Test
    fun `when user does not exist, returns not found error`() = runTest {
        // Arrange
        val nonExistentUser = TestUsers.user(id = "nonexistent_id")

        // Act
        val result = useCase(UpdateUserUseCase.Params(nonExistentUser))

        // Assert
        assertInstanceOf(Result.Error::class.java, result)
        assertInstanceOf(
            AppException.NotFound::class.java,
            (result as Result.Error).exception,
        )
    }

    @Test
    fun `when repository returns error, propagates error`() = runTest {
        // Arrange
        val serverError = AppException.ServerError()
        userRepository.setShouldReturnError(true, serverError)

        // Act
        val result = useCase(UpdateUserUseCase.Params(TestUsers.user()))

        // Assert
        assertInstanceOf(Result.Error::class.java, result)
        assertInstanceOf(
            AppException.ServerError::class.java,
            (result as Result.Error).exception,
        )
    }

    @Test
    fun `updates only the specified fields`() = runTest {
        // Arrange
        val existingUser = TestUsers.user(
            displayName = "Old Name",
            email = "original@example.com",
        )
        userRepository.addUser(existingUser)
        val updatedUser = existingUser.copy(displayName = "New Name")

        // Act
        useCase(UpdateUserUseCase.Params(updatedUser))

        // Assert — email should remain unchanged
        val fetchResult = userRepository.getUserById(existingUser.id)
        assertInstanceOf(Result.Success::class.java, fetchResult)
        val savedUser = (fetchResult as Result.Success).data
        assertEquals("New Name", savedUser.displayName)
        assertEquals("original@example.com", savedUser.email)
    }
}
