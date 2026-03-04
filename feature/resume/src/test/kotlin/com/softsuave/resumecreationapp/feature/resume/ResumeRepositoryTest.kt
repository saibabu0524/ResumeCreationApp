package com.softsuave.resumecreationapp.feature.resume

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.network.api.ResumeApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeRepositoryTest {

    private lateinit var resumeApi: ResumeApi
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: ResumeRepository
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        resumeApi = mockk()
        context = mockk()
        contentResolver = mockk()

        every { context.contentResolver } returns contentResolver
        every { context.cacheDir } returns File(System.getProperty("java.io.tmpdir"))

        repository = ResumeRepository(resumeApi, context, dispatcher)
    }

    @Test
    fun `tailorResume returns Error if URI cannot be resolved`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.openInputStream(any()) } returns null // Cannot read

        // Act
        val result = repository.tailorResume(uri, "job", "provider")

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Could not read PDF file", (result as Result.Error).exception.message)
    }

    @Test
    fun `tailorResume returns Success when API returns bytes`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val mockCursor = mockk<Cursor>()
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { mockCursor.getString(0) } returns "my_resume.pdf"
        every { mockCursor.close() } returns Unit

        every { contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor
        
        val pdfContent = "fake pdf content".toByteArray()
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(pdfContent)

        val apiResponse = "response pdf bytes".toByteArray().toResponseBody()
        coEvery { resumeApi.tailorResume(any(), any(), any()) } returns apiResponse

        // Act
        val result = repository.tailorResume(uri, "job description", "OpenAI")

        // Assert
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("response pdf bytes", String(data))
    }

    @Test
    fun `tailorResume returns Error when server returns empty response`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val mockCursor = mockk<Cursor>()
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { mockCursor.getString(0) } returns "my_resume.pdf"
        every { mockCursor.close() } returns Unit

        every { contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor
        
        val pdfContent = "fake pdf content".toByteArray()
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(pdfContent)

        val apiResponse = "".toByteArray().toResponseBody()
        coEvery { resumeApi.tailorResume(any(), any(), any()) } returns apiResponse

        // Act
        val result = repository.tailorResume(uri, "job description", "OpenAI")

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Received empty response from server", (result as Result.Error).exception.message)
    }
}
