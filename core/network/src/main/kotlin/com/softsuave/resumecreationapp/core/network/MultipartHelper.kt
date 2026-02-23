package com.softsuave.resumecreationapp.core.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Utility for building multipart/form-data request bodies.
 *
 * File and image uploads are painful to add retroactively.
 * This helper encapsulates the OkHttp multipart boilerplate in
 * one reusable place.
 *
 * Usage in a repository or data source:
 * ```kotlin
 * val part = MultipartHelper.filePart("avatar", file, "image/jpeg")
 * val response = api.uploadAvatar(part)
 * ```
 */
object MultipartHelper {

    /**
     * Creates a [MultipartBody.Part] from a [File].
     *
     * @param name The form field name expected by the server.
     * @param file The file to upload.
     * @param mimeType The MIME type (e.g., `"image/jpeg"`, `"application/pdf"`).
     */
    fun filePart(
        name: String,
        file: File,
        mimeType: String,
    ): MultipartBody.Part {
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, file.name, requestBody)
    }

    /**
     * Creates a [MultipartBody.Part] from a [ByteArray].
     *
     * Useful when the file is already in memory (e.g., cropped bitmap data).
     *
     * @param name The form field name expected by the server.
     * @param fileName The file name to report in the Content-Disposition header.
     * @param bytes The raw bytes to upload.
     * @param mimeType The MIME type (e.g., `"image/png"`).
     */
    fun bytesPart(
        name: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
    ): MultipartBody.Part {
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, fileName, requestBody)
    }

    /**
     * Creates a plain text [RequestBody] for a multipart text field.
     */
    fun textPart(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaTypeOrNull())
}
