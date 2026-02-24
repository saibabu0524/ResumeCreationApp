package com.softsuave.resumecreationapp.core.domain.model

/**
 * ATS (Applicant Tracking System) analysis result.
 *
 * Pure domain model — no Android or framework dependencies.
 */
data class AtsResult(
    val overallScore: Int,
    val scoreLabel: String,
    val keywordsPresent: List<String>,
    val keywordsMissing: List<String>,
    val sectionScores: SectionScores,
    val suggestions: List<String>,
    val strengths: List<String>,
    val summary: String,
)

data class SectionScores(
    val skillsMatch: Int,
    val experienceRelevance: Int,
    val educationMatch: Int,
    val formatting: Int,
)
