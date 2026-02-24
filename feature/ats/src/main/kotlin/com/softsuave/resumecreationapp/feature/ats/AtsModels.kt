package com.softsuave.resumecreationapp.feature.ats

data class SectionScores(
    val skillsMatch: Int,
    val experienceRelevance: Int,
    val educationMatch: Int,
    val formatting: Int,
)

data class AtsResult(
    val overallScore: Int,
    val scoreLabel: String,          // Excellent | Good | Fair | Poor
    val keywordsPresent: List<String>,
    val keywordsMissing: List<String>,
    val sectionScores: SectionScores,
    val suggestions: List<String>,
    val strengths: List<String>,
    val summary: String,
)
