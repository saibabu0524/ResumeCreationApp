// =============================================================================
// ResumeTailor — Shared API Types
// All API response / request types live here. Never define them inline.
// Matches the live API at http://resumetailor.in/openapi.json
// =============================================================================

// ── Generic API wrapper ───────────────────────────────────────────────────────

export interface ApiWrapper<T> {
    data: T | null;
    message: string;
    success: boolean;
}

// ── Auth ─────────────────────────────────────────────────────────────────────

/** POST /auth/login & /auth/register response */
export interface TokenResponse {
    access_token: string;
    refresh_token: string;
    token_type: "bearer";
}

/** GET /users/me response */
export interface UserPublic {
    id: string;           // UUID
    email: string;
    is_active: boolean;
    is_superuser: boolean;
}

export interface LoginRequest {
    email: string;
    password: string;
}

/** Register — no name field in this API */
export interface RegisterRequest {
    email: string;
    password: string;
}

export interface RefreshRequest {
    refresh_token: string;
}

// ── Resume ────────────────────────────────────────────────────────────────────

export interface ResumeHistoryItem {
    id: string;
    original_filename: string;
    tailored_filename: string;
    job_description_preview: string;
    ats_score: number | null;
    created_at: string;
    download_url: string;
}

// ── ATS ──────────────────────────────────────────────────────────────────────

export interface SectionScores {
    skills_match: number;        // 0-100
    experience_relevance: number; // 0-100
    education_match: number;      // 0-100
    formatting: number;           // 0-100
}

export interface ATSAnalysisResponse {
    overall_score: number;        // 0-100
    score_label: string;          // "Excellent" | "Good" | "Fair" | "Poor"
    keywords_present: string[];
    keywords_missing: string[];
    section_scores: SectionScores;
    suggestions: string[];
    strengths: string[];
    summary: string;
}

// ── Errors ────────────────────────────────────────────────────────────────────

export interface ApiError {
    detail: string;
    status_code?: number;
}
