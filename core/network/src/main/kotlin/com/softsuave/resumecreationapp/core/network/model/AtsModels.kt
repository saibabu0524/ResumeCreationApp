package com.softsuave.resumecreationapp.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.softsuave.resumecreationapp.core.network.dto.ApiResponseDto

// ── ATS Network DTOs ─────────────────────────────────────────────────────────

@Serializable
data class SectionScoresDto(
    @SerialName("skills_match") val skillsMatch: Int = 0,
    @SerialName("experience_relevance") val experienceRelevance: Int = 0,
    @SerialName("education_match") val educationMatch: Int = 0,
    val formatting: Int = 0,
)

@Serializable
data class AtsResultDto(
    @SerialName("overall_score") val overallScore: Int = 0,
    @SerialName("score_label") val scoreLabel: String = "",
    @SerialName("keywords_present") val keywordsPresent: List<String> = emptyList(),
    @SerialName("keywords_missing") val keywordsMissing: List<String> = emptyList(),
    @SerialName("section_scores") val sectionScores: SectionScoresDto = SectionScoresDto(),
    val suggestions: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val summary: String = "",
)

/** Full API envelope: { data: AtsResultDto, message, success } */
typealias AtsResponse = ApiResponseDto<AtsResultDto>
