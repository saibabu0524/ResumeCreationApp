"""ATS (Applicant Tracking System) analysis service.

Analyses a resume against a job description and returns:
- Overall ATS compatibility score (0-100)
- Keywords present / missing
- Per-section scores
- Actionable improvement suggestions

Rules
-----
- No FastAPI imports here — pure service module.
- Delegates LLM calls through the shared ``call_llm`` helper.
"""

from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import HTTPException

from app.services.resume import call_llm, extract_text_from_pdf

logger = logging.getLogger(__name__)

# ── ATS Prompt ────────────────────────────────────────────────────────────────

ATS_ANALYSIS_PROMPT = """\
You are an expert ATS (Applicant Tracking System) analyst. Analyse the resume against the job description and return ONLY a valid JSON object with the following exact structure (no markdown, no explanation):

{{
  "overall_score": <integer 0-100>,
  "score_label": "<Excellent|Good|Fair|Poor>",
  "keywords_present": [<list of strings>],
  "keywords_missing": [<list of strings>],
  "section_scores": {{
    "skills_match": <integer 0-100>,
    "experience_relevance": <integer 0-100>,
    "education_match": <integer 0-100>,
    "formatting": <integer 0-100>
  }},
  "suggestions": [<list of actionable string suggestions, at least 5>],
  "strengths": [<list of 3-5 strengths>],
  "summary": "<2-3 sentence overall assessment>"
}}

Scoring:
- 85-100: Excellent — strong match, minor tweaks needed
- 70-84:  Good — decent match with a few gaps
- 50-69:  Fair — notable gaps to address
- 0-49:   Poor — significant mismatch

RESUME:
{resume_text}

JOB DESCRIPTION:
{job_description}
"""


def _extract_json(raw: str) -> dict[str, Any]:
    """Extract JSON from a potentially messy LLM response."""
    # Try direct parse first
    try:
        return json.loads(raw.strip())
    except json.JSONDecodeError:
        pass

    # Strip markdown fences
    clean = re.sub(r"```(?:json)?", "", raw, flags=re.IGNORECASE).strip("`").strip()
    try:
        return json.loads(clean)
    except json.JSONDecodeError:
        pass

    # Find first { ... } block
    match = re.search(r"\{.*\}", raw, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass

    raise HTTPException(
        status_code=500,
        detail="ATS analysis returned malformed JSON. Please try again.",
    )


def _validate_result(data: dict[str, Any]) -> dict[str, Any]:
    """Ensure all required fields exist and have sane types."""
    score = int(data.get("overall_score", 0))
    score = max(0, min(100, score))

    label = data.get("score_label", "")
    if label not in {"Excellent", "Good", "Fair", "Poor"}:
        if score >= 85:
            label = "Excellent"
        elif score >= 70:
            label = "Good"
        elif score >= 50:
            label = "Fair"
        else:
            label = "Poor"

    section_scores = data.get("section_scores", {})

    return {
        "overall_score": score,
        "score_label": label,
        "keywords_present": list(data.get("keywords_present", [])),
        "keywords_missing": list(data.get("keywords_missing", [])),
        "section_scores": {
            "skills_match": int(section_scores.get("skills_match", 0)),
            "experience_relevance": int(section_scores.get("experience_relevance", 0)),
            "education_match": int(section_scores.get("education_match", 0)),
            "formatting": int(section_scores.get("formatting", 0)),
        },
        "suggestions": list(data.get("suggestions", [])),
        "strengths": list(data.get("strengths", [])),
        "summary": str(data.get("summary", "")),
    }


async def analyse_ats(
    pdf_bytes: bytes,
    job_description: str,
    provider: str = "gemini",
) -> dict[str, Any]:
    """Run ATS analysis and return a validated result dict.

    Parameters
    ----------
    pdf_bytes:
        Raw bytes of the uploaded PDF resume.
    job_description:
        The target job description text.
    provider:
        LLM provider: ``"gemini"``, ``"ollama"``, or ``"cloud"``.

    Returns
    -------
    dict
        Validated ATS analysis result.

    Raises
    ------
    HTTPException
        On PDF extraction failure, LLM error, or JSON parse failure.
    """
    logger.info("Starting ATS analysis with provider=%s", provider)

    resume_text = extract_text_from_pdf(pdf_bytes)

    prompt = ATS_ANALYSIS_PROMPT.format(
        resume_text=resume_text[:6000],  # Guard against huge resumes
        job_description=job_description[:4000],
    )

    raw_response = await call_llm(provider, prompt)
    data = _extract_json(raw_response)
    result = _validate_result(data)

    logger.info(
        "ATS analysis complete: score=%d (%s)", result["overall_score"], result["score_label"]
    )
    return result
