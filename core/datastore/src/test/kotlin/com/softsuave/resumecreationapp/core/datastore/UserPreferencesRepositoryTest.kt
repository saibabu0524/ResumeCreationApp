package com.softsuave.resumecreationapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @TempDir
    lateinit var tempFolder: File

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository
    
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @BeforeEach
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder, "user_prefs.preferences_pb") }
        )
        repository = UserPreferencesRepository(dataStore)
    }

    @Test
    fun `default values are correct`() = testScope.runTest {
        assertFalse(repository.isDarkMode.first())
        assertFalse(repository.isOnboardingCompleted.first())
        assertTrue(repository.isPushNotificationsEnabled.first())
        assertNull(repository.currentUserId.first())
    }

    @Test
    fun `setDarkMode updates value`() = testScope.runTest {
        repository.setDarkMode(true)
        assertTrue(repository.isDarkMode.first())

        repository.setDarkMode(false)
        assertFalse(repository.isDarkMode.first())
    }

    @Test
    fun `setCurrentUserId updates value`() = testScope.runTest {
        repository.setCurrentUserId("user_123")
        assertEquals("user_123", repository.currentUserId.first())

        repository.setCurrentUserId(null)
        assertNull(repository.currentUserId.first())
    }

    @Test
    fun `clearAll removes all data`() = testScope.runTest {
        // Arrange
        repository.setDarkMode(true)
        repository.setOnboardingCompleted(true)
        repository.setPushNotificationsEnabled(false)
        repository.setCurrentUserId("user_123")

        // Act
        repository.clearAll()

        // Assert
        assertFalse(repository.isDarkMode.first())
        assertFalse(repository.isOnboardingCompleted.first())
        assertTrue(repository.isPushNotificationsEnabled.first())
        assertNull(repository.currentUserId.first())
    }
}
