package com.softsuave.resumecreationapp.core.domain.model

/**
 * Domain model representing a single entry in the user's tailored-resume history.
 *
 * Pure Kotlin — zero Android imports.
 * Mapped from [com.softsuave.resumecreationapp.core.network.api.ResumeHistoryItemDto].
 */
data class ResumeHistoryItem(
    val id: String,
    /** The full job description that was used to tailor the resume. */
    val jobDescription: String,
    /** AI provider used: "gemini" or "ollama". */
    val provider: String,
    /** The user's original uploaded filename (e.g. "my_resume.pdf"). */
    val originalFilename: String,
    /** UUID-based server filename for the tailored output PDF. */
    val storedFilename: String,
    /** UUID-based server filename for the original uploaded PDF (nullable). */
    val uploadedStoredFilename: String?,
    /** ISO-8601 creation timestamp string, e.g. "2026-02-25T09:00:00". */
    val createdAt: String,
)
