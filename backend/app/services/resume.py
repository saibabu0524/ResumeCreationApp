"""Resume processing service.

Encapsulates all heavy-lifting logic for the /tailor endpoint:
- PDF text extraction (pdfplumber → PyMuPDF fallback)
- LaTeX extraction from LLM responses
- Multi-provider LLM calling (Gemini / Ollama / OpenAI-compatible cloud)
- pdflatex compilation with self-correction retry loop

Rules
-----
- No FastAPI ``Request``/``Depends`` imports here — this is a pure service module.
- Never access ``os.environ`` directly — always read from ``get_settings()``.
- Logging uses standard ``logging`` (not ``print``); the caller decides the log level.
"""

from __future__ import annotations

import io
import logging
import os
import re
import subprocess
from typing import Optional

import httpx
import pdfplumber
import fitz  # PyMuPDF
from fastapi import HTTPException
from google import genai
from google.genai import types

from app.core.config import get_settings
from app.core.prompts import BASE_LATEX_TEMPLATE, STAGE_A_PROMPT, STAGE_B_PROMPT

logger = logging.getLogger(__name__)


# ── Gemini client singleton ───────────────────────────────────────────────────

def _get_gemini_client() -> genai.Client | None:
    """Return a cached Gemini client, or ``None`` if the API key is not set."""
    settings = get_settings()
    if settings.GEMINI_API_KEY:
        return genai.Client(api_key=settings.GEMINI_API_KEY)
    return None


# ── PDF helpers ───────────────────────────────────────────────────────────────


def extract_text_from_pdf(pdf_bytes: bytes) -> str:
    """Extract text from a PDF using pdfplumber, with PyMuPDF as a fallback.

    Raises ``HTTPException(400)`` when the PDF is empty or completely unreadable.
    """
    text = ""
    try:
        with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
    except Exception as exc:  # noqa: BLE001
        logger.warning("pdfplumber failed: %s. Falling back to PyMuPDF.", exc)

    if not text.strip():
        try:
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            for page in doc:
                text += page.get_text()
        except Exception as exc:  # noqa: BLE001
            logger.error("PyMuPDF failed: %s", exc)
            raise HTTPException(status_code=400, detail="Could not extract text from PDF.")

    if not text.strip():
        raise HTTPException(
            status_code=400,
            detail="PDF appears to be empty or unreadable.",
        )

    return text


# ── LaTeX helpers ─────────────────────────────────────────────────────────────


def extract_latex(response_text: str) -> str:
    """Extract LaTeX content from ``<latex>...</latex>`` tags.

    Falls back to a fenced latex code block, then returns the raw text.
    """
    match = re.search(r"<latex>(.*?)</latex>", response_text, re.DOTALL | re.IGNORECASE)
    if match:
        return match.group(1).strip()
    match_md = re.search(r"```latex(.*?)```", response_text, re.DOTALL | re.IGNORECASE)
    if match_md:
        return match_md.group(1).strip()
    return response_text.strip()


def compile_latex_to_pdf(latex_code: str, work_dir: str) -> tuple[bool, str]:
    """Compile *latex_code* to PDF using ``pdflatex``.

    Runs ``pdflatex`` twice so forward references and formatting stabilise.
    Uses PDF existence as the primary success signal — pdflatex exits with
    rc=1 for non-fatal warnings (e.g. undefined icon names) even when it
    produces a valid PDF.

    Returns
    -------
    (success, log)
        *success* is ``True`` when a non-trivial PDF was produced.
        *log* is the combined stdout+stderr of the last pdflatex run.

    Raises ``HTTPException(500)`` when pdflatex is not found on PATH.
    """
    tex_path = os.path.join(work_dir, "resume.tex")
    with open(tex_path, "w", encoding="utf-8") as fh:
        fh.write(latex_code)

    log_output = ""
    try:
        result = None
        for _ in range(2):
            result = subprocess.run(
                ["pdflatex", "-interaction=nonstopmode", "resume.tex"],
                cwd=work_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                timeout=30,
            )
            log_output = result.stdout

        pdf_path = os.path.join(work_dir, "resume.pdf")
        if os.path.exists(pdf_path) and os.path.getsize(pdf_path) > 1024:
            if result and result.returncode != 0:
                logger.warning(
                    "pdflatex rc=%d but PDF produced — treating as success.",
                    result.returncode,
                )
            return True, log_output

        rc = result.returncode if result else -1
        logger.error("pdflatex failed (rc=%d), no PDF produced:\n%s", rc, log_output[-3000:])
        return False, log_output

    except subprocess.TimeoutExpired:
        logger.error("pdflatex compilation timed out.")
        return False, "pdflatex timed out"
    except FileNotFoundError:
        logger.error("pdflatex not found. Ensure TeX Live is installed.")
        raise HTTPException(status_code=500, detail="pdflatex not installed on server.")


# ── LLM helpers ───────────────────────────────────────────────────────────────


async def call_llm(provider: str, prompt: str) -> str:
    """Dispatch an LLM request to the configured provider.

    Parameters
    ----------
    provider:
        One of ``"gemini"``, ``"ollama"``, or ``"cloud"``.
    prompt:
        Full text prompt to send.

    Returns the raw text response from the model.

    Raises ``HTTPException(503)`` on provider errors or missing configuration.
    Raises ``HTTPException(400)`` for unknown provider strings.
    """
    settings = get_settings()

    if provider == "gemini":
        gemini_client = _get_gemini_client()
        if not gemini_client:
            raise HTTPException(status_code=503, detail="GEMINI_API_KEY not configured.")
        try:
            response = gemini_client.models.generate_content(
                model=settings.GEMINI_MODEL,
                contents=prompt,
                config=types.GenerateContentConfig(temperature=0.2),
            )
            return response.text
        except Exception as exc:  # noqa: BLE001
            logger.error("Gemini API error: %s", exc)
            raise HTTPException(status_code=503, detail=f"Gemini API error: {exc}")

    if provider == "ollama":
        headers: dict[str, str] = {}
        if settings.OLLAMA_API_KEY:
            headers["Authorization"] = f"Bearer {settings.OLLAMA_API_KEY}"
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.post(
                    f"{settings.OLLAMA_BASE_URL}/api/generate",
                    headers=headers,
                    json={
                        "model": settings.OLLAMA_MODEL,
                        "prompt": prompt,
                        "stream": False,
                        "options": {"temperature": 0.2},
                    },
                    timeout=120.0,
                )
                resp.raise_for_status()
                return resp.json().get("response", "")
            except Exception as exc:  # noqa: BLE001
                logger.error("Ollama API error: %s", exc)
                raise HTTPException(status_code=503, detail=f"Ollama API error: {exc}")

    if provider == "cloud":
        if not settings.CLOUD_API_KEY:
            raise HTTPException(
                status_code=503,
                detail="CLOUD_API_KEY not configured. Set it to use cloud models like Kimi.",
            )
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.post(
                    f"{settings.CLOUD_BASE_URL}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {settings.CLOUD_API_KEY}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": settings.CLOUD_MODEL,
                        "messages": [{"role": "user", "content": prompt}],
                        "temperature": 0.2,
                    },
                    timeout=120.0,
                )
                resp.raise_for_status()
                return resp.json()["choices"][0]["message"]["content"]
            except Exception as exc:  # noqa: BLE001
                logger.error("Cloud API error: %s", exc)
                raise HTTPException(status_code=503, detail=f"Cloud API error: {exc}")

    raise HTTPException(
        status_code=400,
        detail="Invalid provider. Choose 'gemini', 'ollama', or 'cloud'.",
    )


# ── Resume pipeline ───────────────────────────────────────────────────────────


async def process_resume_stage(
    provider: str,
    base_prompt: str,
    work_dir: str,
    retries: int | None = None,
) -> Optional[str]:
    """Run the LLM → compile loop with self-correction retry.

    On each failed compilation the pdflatex error log is appended to the
    prompt so the model can self-correct before the next attempt.

    Returns the LaTeX string on success, or ``None`` after all retries fail.
    """
    settings = get_settings()
    max_retries = retries if retries is not None else settings.LLM_RETRY_ATTEMPTS

    prompt = base_prompt
    for attempt in range(max_retries):
        logger.info("LLM compilation attempt %d/%d", attempt + 1, max_retries)
        raw_response = await call_llm(provider, prompt)
        latex_code = extract_latex(raw_response)

        success, log = compile_latex_to_pdf(latex_code, work_dir)
        if success:
            return latex_code

        # Inject compiler errors back into the prompt for self-correction.
        error_lines = "\n".join(
            line
            for line in log.splitlines()
            if line.startswith("!") or "Error" in line or "Undefined" in line
        )
        prompt = (
            base_prompt
            + "\n\nIMPORTANT: Your previous LaTeX caused compilation errors. "
            "Fix ALL issues before responding.\n\n"
            f"Compiler errors:\n{error_lines or log[-1500:]}\n\n"
            f"Broken LaTeX (for reference):\n```latex\n{latex_code[:3000]}\n```"
        )

    return None


async def tailor_resume_pipeline(
    pdf_bytes: bytes,
    job_description: str,
    provider: str,
    work_dir: str,
    request_id: str,
) -> str:
    """Full two-stage pipeline: structure → tailor.

    Returns the final tailored LaTeX string (which has already been compiled
    and its PDF written to ``{work_dir}/resume.pdf``).

    Raises ``HTTPException`` on any unrecoverable error.
    """
    # ── Stage A: structure raw PDF text into the LaTeX template ──────────────
    logger.info("[%s] Stage A — extracting and structuring PDF...", request_id)
    raw_text = extract_text_from_pdf(pdf_bytes)

    stage_a_prompt = (
        STAGE_A_PROMPT
        .replace("{latex_template}", BASE_LATEX_TEMPLATE)
        .replace("{resume_text}", raw_text)
    )
    structured_latex = await process_resume_stage(provider, stage_a_prompt, work_dir)
    if not structured_latex:
        raise HTTPException(
            status_code=500,
            detail="Failed to generate valid structured LaTeX from resume after retries.",
        )

    # ── Stage B: tailor the structured resume to the job description ─────────
    logger.info("[%s] Stage B — tailoring to job description...", request_id)
    stage_b_prompt = (
        STAGE_B_PROMPT
        .replace("{job_description}", job_description)
        .replace("{latex_resume}", structured_latex)
    )
    tailored_latex = await process_resume_stage(provider, stage_b_prompt, work_dir)
    if not tailored_latex:
        raise HTTPException(
            status_code=500,
            detail="Failed to compile tailored LaTeX resume after retries.",
        )

    return tailored_latex
