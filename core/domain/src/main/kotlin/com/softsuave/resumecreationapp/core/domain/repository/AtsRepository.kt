package com.softsuave.resumecreationapp.core.domain.repository

import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result

/**
 * Contract for ATS (Applicant Tracking System) analysis.
 *
 * Accepts platform-agnostic types only ([ByteArray] instead of
 * `android.net.Uri`) so this interface can live in the pure-JVM domain layer.
 * The feature-module ViewModel is responsible for resolving a content URI
 * into raw bytes before calling this repository.
 */
interface AtsRepository {

    /**
     * Analyses a PDF resume against a job description and returns a scored result.
     *
     * @param pdfBytes  Raw PDF bytes of the resume.
     * @param fileName  Original file name (used for the multipart upload).
     * @param jobDescription  The target job description text.
     * @param provider  LLM provider key (e.g. `"gemini"`, `"ollama"`).
     */
    suspend fun analyseAts(
        pdfBytes: ByteArray,
        fileName: String,
        jobDescription: String,
        provider: String,
    ): Result<AtsResult>
}
