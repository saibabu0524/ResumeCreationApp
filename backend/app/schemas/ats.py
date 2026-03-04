"""ATS analysis Pydantic schemas."""

from __future__ import annotations

from pydantic import BaseModel, Field


class SectionScores(BaseModel):
    skills_match: int = Field(ge=0, le=100)
    experience_relevance: int = Field(ge=0, le=100)
    education_match: int = Field(ge=0, le=100)
    formatting: int = Field(ge=0, le=100)


class AtsResult(BaseModel):
    overall_score: int = Field(ge=0, le=100, description="ATS compatibility score 0-100")
    score_label: str = Field(description="Excellent | Good | Fair | Poor")
    keywords_present: list[str] = Field(default_factory=list)
    keywords_missing: list[str] = Field(default_factory=list)
    section_scores: SectionScores
    suggestions: list[str] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    summary: str = ""
