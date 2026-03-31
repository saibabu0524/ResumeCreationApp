/** Shared type definitions for the Chrome extension. */

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
}

export interface UserPublic {
  id: string;
  email: string;
  is_active: boolean;
  is_superuser: boolean;
}

export interface ApiResponse<T> {
  data: T | null;
  message: string;
  success: boolean;
}

export interface MessageResponse {
  message: string;
  success: boolean;
}

export interface StoredResume {
  id: string;
  original_filename: string;
  file_size_bytes: number;
  created_at: string;
}

export interface SectionScores {
  skills_match: number;
  experience_relevance: number;
  education_match: number;
  formatting: number;
}

export interface AtsResult {
  overall_score: number;
  score_label: string;
  keywords_present: string[];
  keywords_missing: string[];
  section_scores: SectionScores;
  suggestions: string[];
  strengths: string[];
  summary: string;
}

export interface ExtractionResult {
  source: string;
  jobDescription: string | null;
  url: string;
  title: string;
}
