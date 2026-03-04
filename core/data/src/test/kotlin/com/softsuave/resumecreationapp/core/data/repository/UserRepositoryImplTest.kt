package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.data.remote.UserApi
import com.softsuave.resumecreationapp.core.data.remote.dto.UserDto
import com.softsuave.resumecreationapp.core.database.dao.UserDao
import com.softsuave.resumecreationapp.core.database.entity.UserEntity
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.dto.MessageResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private lateinit var userApi: UserApi
    private lateinit var userDao: UserDao
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var repository: UserRepositoryImpl
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        userApi = mockk()
        userDao = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        repository = UserRepositoryImpl(userApi, userDao, preferencesRepository, dispatcher)
    }

    @Test
    fun `observeCurrentUser emits Loading then cached user`() = runTest {
        // Arrange
        val userId = "123"
        every { preferencesRepository.currentUserId } returns flowOf(userId)
        
        val userEntity = UserEntity(
            id = userId,
            displayName = "Test User",
            email = "test@example.com",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        coEvery { userDao.getById(userId) } returns userEntity

        // Act
        val results = repository.observeCurrentUser().toList()

        // Assert
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        
        val successResult = results[1] as Result.Success
        val user = successResult.data
        assertEquals(userId, user?.id)
        assertEquals("Test User", user?.displayName)
    }

    @Test
    fun `getUserById fetches from API and caches it`() = runTest {
        // Arrange
        val dto = UserDto(
            id = "123",
            displayName = "Test User",
            email = "test@example.com",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        val apiResponse = Result.Success(ApiResponseDto(data = dto, message = "success"))
        coEvery { userApi.getMe() } returns apiResponse

        // Act
        val result = repository.getUserById("123") // The parameter is actually ignored by the impl

        // Assert
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("123", user.id)
        
        coVerify(exactly = 1) { userDao.upsert(any()) }
    }

    @Test
    fun `deleteAccount clears DB and preferences`() = runTest {
        // Arrange
        val apiResponse = Result.Success(MessageResponseDto(message = "success"))
        coEvery { userApi.deleteAccount() } returns apiResponse

        // Act
        val result = repository.deleteAccount()

        // Assert
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { userDao.deleteAll() }
        coVerify(exactly = 1) { preferencesRepository.clearAll() }
    }
}
