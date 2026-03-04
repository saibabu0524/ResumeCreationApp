"""Unit tests for app/services/resume.py.

Coverage
--------
extract_text_from_pdf:
  - pdfplumber extracts text successfully.
  - pdfplumber fails → PyMuPDF fallback returns text.
  - Both extractors fail → HTTPException 400.
  - Empty PDF → HTTPException 400.

extract_latex:
  - <latex>…</latex> tags.
  - ```latex … ``` markdown fence.
  - Fallback to raw text.

compile_latex_to_pdf:
  - pdflatex succeeds, PDF produced → (True, log).
  - pdflatex fails, no PDF → (False, log).
  - pdflatex non-zero exit but PDF present → success with warning.
  - Timeout → (False, "pdflatex timed out").
  - FileNotFoundError (pdflatex not installed) → HTTPException 500.

call_llm:
  - Gemini: success.
  - Gemini: API key not configured → HTTPException 503.
  - Ollama: success.
  - Cloud: success.
  - Cloud: API key not configured → HTTPException 503.
  - Unknown provider → HTTPException 400.

process_resume_stage:
  - First attempt succeeds → returns LaTeX string.
  - First attempt fails, second attempt succeeds → returns LaTeX string.
  - All attempts fail → returns None.

tailor_resume_pipeline:
  - Full happy-path (stages A and B).
  - Stage A fails → HTTPException 500.
  - Stage B fails → HTTPException 500.
"""

from __future__ import annotations

import os
import subprocess
import tempfile
from pathlib import Path
from typing import Any
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException

from app.services.resume import (
    call_llm,
    compile_latex_to_pdf,
    extract_latex,
    extract_text_from_pdf,
    process_resume_stage,
    tailor_resume_pipeline,
)

pytestmark = pytest.mark.asyncio


# ── extract_text_from_pdf ─────────────────────────────────────────────────────


async def test_extract_text_from_pdf_pdfplumber_success() -> None:
    """pdfplumber returns text — no fallback needed."""
    mock_page = MagicMock()
    mock_page.extract_text.return_value = "Alice Engineer\nSoftware Dev"
    mock_pdf = MagicMock()
    mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
    mock_pdf.__exit__ = MagicMock(return_value=False)
    mock_pdf.pages = [mock_page]

    with patch("pdfplumber.open", return_value=mock_pdf):
        result = extract_text_from_pdf(b"fake-pdf")

    assert "Alice Engineer" in result
    assert "Software Dev" in result


async def test_extract_text_from_pdf_fallback_to_pymupdf() -> None:
    """pdfplumber raises → PyMuPDF fallback returns text."""
    mock_page = MagicMock()
    mock_page.get_text.return_value = "Fallback text from PyMuPDF"
    mock_doc = [mock_page]

    with patch("pdfplumber.open", side_effect=Exception("pdfplumber broken")):
        with patch("fitz.open", return_value=mock_doc):
            result = extract_text_from_pdf(b"fake-pdf")

    assert "Fallback text" in result


async def test_extract_text_from_pdf_both_fail() -> None:
    """Both extractors fail → HTTPException 400."""
    with patch("pdfplumber.open", side_effect=Exception("broken")):
        with patch("fitz.open", side_effect=Exception("also broken")):
            with pytest.raises(HTTPException) as exc_info:
                extract_text_from_pdf(b"bad-pdf")
    assert exc_info.value.status_code == 400


async def test_extract_text_from_pdf_empty_pdf() -> None:
    """PDF that yields no text → HTTPException 400."""
    mock_page = MagicMock()
    mock_page.extract_text.return_value = ""
    mock_pdf = MagicMock()
    mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
    mock_pdf.__exit__ = MagicMock(return_value=False)
    mock_pdf.pages = [mock_page]

    mock_fitz_page = MagicMock()
    mock_fitz_page.get_text.return_value = ""
    mock_fitz_doc = [mock_fitz_page]

    with patch("pdfplumber.open", return_value=mock_pdf):
        with patch("fitz.open", return_value=mock_fitz_doc):
            with pytest.raises(HTTPException) as exc_info:
                extract_text_from_pdf(b"empty-pdf")
    assert exc_info.value.status_code == 400


# ── extract_latex ─────────────────────────────────────────────────────────────


async def test_extract_latex_from_tags() -> None:
    raw = "Some intro\n<latex>\\documentclass{article}\\begin{document}Hi\\end{document}</latex>\nTrailing"
    assert extract_latex(raw) == r"\documentclass{article}\begin{document}Hi\end{document}"


async def test_extract_latex_from_markdown_fence() -> None:
    raw = "Here is the result:\n```latex\n\\begin{document}Hello\\end{document}\n```"
    assert "Hello" in extract_latex(raw)


async def test_extract_latex_fallback_raw() -> None:
    raw = "   Just raw latex code   "
    assert extract_latex(raw) == "Just raw latex code"


# ── compile_latex_to_pdf ──────────────────────────────────────────────────────


async def test_compile_latex_success() -> None:
    """pdflatex exits cleanly and produces a non-trivial PDF."""
    tex = r"\documentclass{article}\begin{document}Hello\end{document}"

    with tempfile.TemporaryDirectory() as tmpdir:
        fake_pdf = Path(tmpdir) / "resume.pdf"
        fake_pdf.write_bytes(b"%PDF-1.4" + b"0" * 2000)  # > 1024 bytes

        mock_result = MagicMock()
        mock_result.returncode = 0
        mock_result.stdout = "This is pdflatex output"

        with patch("subprocess.run", return_value=mock_result):
            success, log = compile_latex_to_pdf(tex, tmpdir)

    assert success is True
    assert "pdflatex" in log.lower() or isinstance(log, str)


async def test_compile_latex_no_pdf_produced() -> None:
    """pdflatex runs but no PDF is produced → (False, log)."""
    tex = r"\documentclass{article}\begin{document}Bad\end{document}"

    with tempfile.TemporaryDirectory() as tmpdir:
        mock_result = MagicMock()
        mock_result.returncode = 1
        mock_result.stdout = "! Undefined control sequence."

        with patch("subprocess.run", return_value=mock_result):
            success, log = compile_latex_to_pdf(tex, tmpdir)

    assert success is False
    assert "Undefined" in log


async def test_compile_latex_nonzero_rc_but_pdf_present() -> None:
    """rc != 0 but PDF is present → treated as success (non-fatal warnings)."""
    tex = r"\documentclass{article}\begin{document}Warn\end{document}"

    with tempfile.TemporaryDirectory() as tmpdir:
        fake_pdf = Path(tmpdir) / "resume.pdf"
        fake_pdf.write_bytes(b"%PDF-1.4" + b"x" * 2000)

        mock_result = MagicMock()
        mock_result.returncode = 1  # Non-fatal warning
        mock_result.stdout = "LaTeX Warning: something minor"

        with patch("subprocess.run", return_value=mock_result):
            success, log = compile_latex_to_pdf(tex, tmpdir)

    assert success is True


async def test_compile_latex_timeout() -> None:
    """Subprocess times out → (False, timeout message)."""
    tex = r"\documentclass{article}\begin{document}Slow\end{document}"

    with tempfile.TemporaryDirectory() as tmpdir:
        with patch("subprocess.run", side_effect=subprocess.TimeoutExpired("pdflatex", 30)):
            success, log = compile_latex_to_pdf(tex, tmpdir)

    assert success is False
    assert "timed out" in log


async def test_compile_latex_pdflatex_not_installed() -> None:
    """pdflatex binary missing → HTTPException 500."""
    tex = r"\documentclass{article}\begin{document}Test\end{document}"

    with tempfile.TemporaryDirectory() as tmpdir:
        with patch("subprocess.run", side_effect=FileNotFoundError):
            with pytest.raises(HTTPException) as exc_info:
                compile_latex_to_pdf(tex, tmpdir)

    assert exc_info.value.status_code == 500


# ── call_llm ──────────────────────────────────────────────────────────────────


async def test_call_llm_gemini_success() -> None:
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = "<latex>\\documentclass{article}</latex>"
    mock_client.models.generate_content.return_value = mock_response

    with patch("app.services.resume._get_gemini_client", return_value=mock_client):
        result = await call_llm("gemini", "Format this resume")

    assert "documentclass" in result


async def test_call_llm_gemini_not_configured() -> None:
    """No Gemini API key → HTTPException 503."""
    with patch("app.services.resume._get_gemini_client", return_value=None):
        with pytest.raises(HTTPException) as exc_info:
            await call_llm("gemini", "Format this resume")
    assert exc_info.value.status_code == 503


async def test_call_llm_gemini_api_error() -> None:
    """Gemini client raises → HTTPException 503."""
    mock_client = MagicMock()
    mock_client.models.generate_content.side_effect = RuntimeError("quota exceeded")

    with patch("app.services.resume._get_gemini_client", return_value=mock_client):
        with pytest.raises(HTTPException) as exc_info:
            await call_llm("gemini", "prompt")
    assert exc_info.value.status_code == 503


async def test_call_llm_ollama_success() -> None:
    mock_response = MagicMock()
    mock_response.json.return_value = {"response": "<latex>\\begin{document}\\end{document}</latex>"}
    mock_response.raise_for_status = MagicMock()

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.post = AsyncMock(return_value=mock_response)

    with patch("httpx.AsyncClient", return_value=mock_client):
        result = await call_llm("ollama", "Format this resume")

    assert "begin{document}" in result


async def test_call_llm_cloud_success() -> None:
    mock_response = MagicMock()
    mock_response.json.return_value = {
        "choices": [{"message": {"content": "<latex>\\begin{document}\\end{document}</latex>"}}]
    }
    mock_response.raise_for_status = MagicMock()

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.post = AsyncMock(return_value=mock_response)

    with patch("httpx.AsyncClient", return_value=mock_client):
        with patch("app.services.resume.get_settings") as mock_settings:
            settings = MagicMock()
            settings.CLOUD_API_KEY = "test-key"
            settings.CLOUD_BASE_URL = "https://api.example.com/v1"
            settings.CLOUD_MODEL = "test-model"
            mock_settings.return_value = settings
            result = await call_llm("cloud", "Format this resume")

    assert "begin{document}" in result


async def test_call_llm_cloud_not_configured() -> None:
    """No CLOUD_API_KEY → HTTPException 503."""
    with patch("app.services.resume.get_settings") as mock_settings:
        settings = MagicMock()
        settings.CLOUD_API_KEY = None
        mock_settings.return_value = settings
        with pytest.raises(HTTPException) as exc_info:
            await call_llm("cloud", "prompt")
    assert exc_info.value.status_code == 503


async def test_call_llm_unknown_provider() -> None:
    with pytest.raises(HTTPException) as exc_info:
        await call_llm("openai", "prompt")
    assert exc_info.value.status_code == 400


# ── process_resume_stage ──────────────────────────────────────────────────────


async def test_process_resume_stage_first_attempt_succeeds() -> None:
    _LATEX = r"\documentclass{article}\begin{document}OK\end{document}"
    wrapped = f"<latex>{_LATEX}</latex>"

    with patch("app.services.resume.call_llm", new=AsyncMock(return_value=wrapped)):
        with patch("app.services.resume.compile_latex_to_pdf", return_value=(True, "all good")):
            with tempfile.TemporaryDirectory() as tmpdir:
                result = await process_resume_stage("gemini", "prompt", tmpdir, retries=2)

    assert result is not None
    assert "documentclass" in result


async def test_process_resume_stage_retries_on_failure() -> None:
    """First attempt fails compilation, second attempt succeeds."""
    _LATEX = r"\documentclass{article}\begin{document}Fixed\end{document}"
    wrapped = f"<latex>{_LATEX}</latex>"

    compile_results = [(False, "! Error on line 5."), (True, "success")]
    compile_iter = iter(compile_results)

    with patch("app.services.resume.call_llm", new=AsyncMock(return_value=wrapped)):
        with patch("app.services.resume.compile_latex_to_pdf", side_effect=lambda *_: next(compile_iter)):
            with tempfile.TemporaryDirectory() as tmpdir:
                result = await process_resume_stage("gemini", "prompt", tmpdir, retries=2)

    assert result is not None


async def test_process_resume_stage_all_retries_exhausted() -> None:
    """All compile attempts fail → returns None."""
    wrapped = "<latex>\\bad latex{</latex>"

    with patch("app.services.resume.call_llm", new=AsyncMock(return_value=wrapped)):
        with patch("app.services.resume.compile_latex_to_pdf", return_value=(False, "! Fatal error.")):
            with tempfile.TemporaryDirectory() as tmpdir:
                result = await process_resume_stage("gemini", "prompt", tmpdir, retries=3)

    assert result is None


# ── tailor_resume_pipeline ────────────────────────────────────────────────────


async def test_tailor_resume_pipeline_happy_path() -> None:
    """Both stages succeed — returns the final tailored LaTeX."""
    _STAGE_A = r"\documentclass{article}\begin{document}Structured\end{document}"
    _STAGE_B = r"\documentclass{article}\begin{document}Tailored\end{document}"

    call_count = 0

    async def _fake_stage(provider: str, prompt: str, work_dir: str, retries: Any = None) -> str:
        nonlocal call_count
        call_count += 1
        return _STAGE_A if call_count == 1 else _STAGE_B

    mock_extract = MagicMock(return_value="Alice Engineer\nPython Developer")

    with patch("app.services.resume.extract_text_from_pdf", mock_extract):
        with patch("app.services.resume.process_resume_stage", side_effect=_fake_stage):
            with tempfile.TemporaryDirectory() as tmpdir:
                result = await tailor_resume_pipeline(
                    pdf_bytes=b"fake-pdf",
                    job_description="Senior Python Engineer role",
                    provider="gemini",
                    work_dir=tmpdir,
                    request_id="test-123",
                )

    assert "Tailored" in result


async def test_tailor_resume_pipeline_stage_a_fails() -> None:
    """Stage A pipeline returns None → HTTPException 500."""
    mock_extract = MagicMock(return_value="Some resume text")

    with patch("app.services.resume.extract_text_from_pdf", mock_extract):
        with patch("app.services.resume.process_resume_stage", new=AsyncMock(return_value=None)):
            with tempfile.TemporaryDirectory() as tmpdir:
                with pytest.raises(HTTPException) as exc_info:
                    await tailor_resume_pipeline(
                        pdf_bytes=b"fake-pdf",
                        job_description="Engineer role",
                        provider="gemini",
                        work_dir=tmpdir,
                        request_id="test-456",
                    )

    assert exc_info.value.status_code == 500
    assert "structured" in exc_info.value.detail.lower()


async def test_tailor_resume_pipeline_stage_b_fails() -> None:
    """Stage A succeeds, Stage B returns None → HTTPException 500."""
    _STAGE_A = r"\documentclass{article}\begin{document}Structured\end{document}"

    call_count = 0

    async def _fake_stage(provider: str, prompt: str, work_dir: str, retries: Any = None) -> str | None:
        nonlocal call_count
        call_count += 1
        return _STAGE_A if call_count == 1 else None

    mock_extract = MagicMock(return_value="Some resume text")

    with patch("app.services.resume.extract_text_from_pdf", mock_extract):
        with patch("app.services.resume.process_resume_stage", side_effect=_fake_stage):
            with tempfile.TemporaryDirectory() as tmpdir:
                with pytest.raises(HTTPException) as exc_info:
                    await tailor_resume_pipeline(
                        pdf_bytes=b"fake-pdf",
                        job_description="Engineer role",
                        provider="gemini",
                        work_dir=tmpdir,
                        request_id="test-789",
                    )

    assert exc_info.value.status_code == 500
    assert "tailored" in exc_info.value.detail.lower()
