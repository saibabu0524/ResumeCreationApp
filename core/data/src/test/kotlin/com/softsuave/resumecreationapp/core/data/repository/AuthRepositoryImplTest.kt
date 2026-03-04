package com.softsuave.resumecreationapp.core.data.repository

import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import com.softsuave.resumecreationapp.core.domain.model.AppException
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto
import com.softsuave.resumecreationapp.core.network.api.AuthApi
import com.softsuave.resumecreationapp.core.network.api.TokenResponseDto
import com.softsuave.resumecreationapp.core.network.api.UserResponseDto
import com.softsuave.resumecreationapp.core.network.dto.MessageResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var authApi: AuthApi
    private lateinit var tokenStorage: TokenStorage
    private lateinit var repository: AuthRepositoryImpl
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        authApi = mockk()
        tokenStorage = mockk(relaxed = true)
        repository = AuthRepositoryImpl(authApi, tokenStorage, dispatcher)
    }

    @Test
    fun `login returns Success and saves tokens when API succeeds`() = runTest {
        val tokens = TokenResponseDto("access", "refresh")
        val apiResponse = Result.Success(ApiResponseDto(data = tokens, message = "success"))

        coEvery { authApi.login(any()) } returns apiResponse

        val result = repository.login("user@test.com", "password")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { tokenStorage.saveTokens("access", "refresh") }
    }

    @Test
    fun `login returns Error when API returns empty tokens`() = runTest {
        val apiResponse = Result.Success(ApiResponseDto<TokenResponseDto>(data = null, message = "success"))

        coEvery { authApi.login(any()) } returns apiResponse

        val result = repository.login("user@test.com", "password")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is AppException.Unknown)
        assertEquals("Empty token response from server.", result.exception.message)
    }

    @Test
    fun `register returns Success and calls login when API succeeds`() = runTest {
        val registerResponse = Result.Success(ApiResponseDto<UserResponseDto>(data = null, message = "success"))
        val tokens = TokenResponseDto("access", "refresh")
        val loginResponse = Result.Success(ApiResponseDto(data = tokens, message = "success"))

        coEvery { authApi.register(any()) } returns registerResponse
        coEvery { authApi.login(any()) } returns loginResponse

        val result = repository.register("user@test.com", "password")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authApi.login(any()) }
        coVerify(exactly = 1) { tokenStorage.saveTokens("access", "refresh") }
    }

    @Test
    fun `refreshToken returns Error if no stored refresh token`() = runTest {
        every { tokenStorage.refreshToken } returns null

        val result = repository.refreshToken()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is AppException.NotAuthenticated)
    }

    @Test
    fun `logout always clears tokens`() = runTest {
        every { tokenStorage.refreshToken } returns "refresh_token"
        coEvery { authApi.logout(any()) } returns Result.Success(MessageResponseDto(message = "success"))

        val result = repository.logout()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authApi.logout(any()) }
        coVerify(exactly = 1) { tokenStorage.clearTokens() }
    }
}
