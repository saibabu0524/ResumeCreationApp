package com.softsuave.resumecreationapp.core.domain.usecase.user

import app.cash.turbine.test
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ObserveCurrentUserUseCase].
 *
 * Tests Flow emissions using Turbine — asserts exact values in exact order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCurrentUserUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: FakeUserRepository
    private lateinit var useCase: ObserveCurrentUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = FakeUserRepository()
        useCase = ObserveCurrentUserUseCase(userRepository, testDispatcher)
    }

    @Test
    fun `when no user is signed in, emits success with null`() = runTest {
        useCase(Unit).test {
            val result = awaitItem()
            assertInstanceOf(Result.Success::class.java, result)
            assertNull((result as Result.Success).data)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when user is signed in, emits success with user`() = runTest {
        // Arrange
        val expectedUser = TestUsers.user()
        userRepository.setCurrentUser(expectedUser)

        // Act & Assert
        useCase(Unit).test {
            val result = awaitItem()
            assertInstanceOf(Result.Success::class.java, result)
            assertEquals(expectedUser, (result as Result.Success).data)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when repository is configured to error, emits error`() = runTest {
        // Arrange
        val networkError = AppException.NetworkError()
        userRepository.setShouldReturnError(true, networkError)

        // Act & Assert
        useCase(Unit).test {
            val result = awaitItem()
            assertInstanceOf(Result.Error::class.java, result)
            assertEquals(networkError, (result as Result.Error).exception)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits updated user when current user changes`() = runTest {
        // Arrange
        val initialUser = TestUsers.user()
        userRepository.setCurrentUser(initialUser)

        useCase(Unit).test {
            // Initial emission
            val first = awaitItem()
            assertEquals(initialUser, (first as Result.Success).data)

            // Update user
            val updatedUser = initialUser.copy(displayName = "Updated Name")
            userRepository.setCurrentUser(updatedUser)

            val second = awaitItem()
            assertEquals(updatedUser, (second as Result.Success).data)

            cancelAndConsumeRemainingEvents()
        }
    }
}
