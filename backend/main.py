import os
import io
import re
import uuid
import subprocess
import tempfile
import logging
from typing import Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import StreamingResponse
import pdfplumber
import fitz  # PyMuPDF
import httpx
from google import genai
from google.genai import types

from prompts import BASE_LATEX_TEMPLATE, STAGE_A_PROMPT, STAGE_B_PROMPT

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Resume Tailor API")

# --- Configuration ---
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434")

if GEMINI_API_KEY:
    gemini_client = genai.Client(api_key=GEMINI_API_KEY)
else:
    gemini_client = None

# --- Helpers ---

def extract_text_from_pdf(pdf_bytes: bytes) -> str:
    """Extract text from PDF using pdfplumber, fallback to PyMuPDF."""
    text = ""
    try:
        with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
    except Exception as e:
        logger.warning(f"pdfplumber failed: {e}. Falling back to PyMuPDF.")

    if not text.strip():
        try:
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            for page in doc:
                text += page.get_text()
        except Exception as e:
            logger.error(f"PyMuPDF failed: {e}")
            raise HTTPException(status_code=400, detail="Could not extract text from PDF.")

    if not text.strip():
        raise HTTPException(status_code=400, detail="PDF appears to be empty or unreadable.")

    return text

def extract_latex(response_text: str) -> str:
    """Extract LaTeX content from <latex> tags, or fallback to markdown blocks."""
    match = re.search(r"<latex>(.*?)</latex>", response_text, re.DOTALL | re.IGNORECASE)
    if match:
        return match.group(1).strip()
    match_md = re.search(r"```latex(.*?)```", response_text, re.DOTALL | re.IGNORECASE)
    if match_md:
        return match_md.group(1).strip()
    return response_text.strip()

async def call_llm(provider: str, prompt: str) -> str:
    """Abstract LLM call to Gemini or Ollama."""
    if provider == "gemini":
        if not gemini_client:
            raise HTTPException(status_code=503, detail="Gemini API Key not configured.")
        try:
            response = gemini_client.models.generate_content(
                model="gemini-2.5-flash",
                contents=prompt,
                config=types.GenerateContentConfig(temperature=0.2)
            )
            return response.text
        except Exception as e:
            logger.error(f"Gemini API error: {e}")
            raise HTTPException(status_code=503, detail=f"Gemini API error: {str(e)}")

    elif provider == "ollama":
        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    f"{OLLAMA_BASE_URL}/api/generate",
                    json={
                        "model": "llama3",
                        "prompt": prompt,
                        "stream": False,
                        "options": {"temperature": 0.2}
                    },
                    timeout=120.0
                )
                response.raise_for_status()
                return response.json().get("response", "")
            except Exception as e:
                logger.error(f"Ollama API error: {e}")
                raise HTTPException(status_code=503, detail=f"Ollama API error: {str(e)}")
    else:
        raise HTTPException(status_code=400, detail="Invalid provider. Choose 'gemini' or 'ollama'.")

def compile_latex_to_pdf(latex_code: str, work_dir: str) -> tuple[bool, str]:
    """
    Compile LaTeX to PDF using pdflatex.
    Returns (success: bool, pdflatex_log: str).
    """
    tex_file_path = os.path.join(work_dir, "resume.tex")
    with open(tex_file_path, "w", encoding="utf-8") as f:
        f.write(latex_code)

    log_output = ""
    try:
        # Run pdflatex twice for references/formatting
        for _ in range(2):
            result = subprocess.run(
                ["pdflatex", "-interaction=nonstopmode", "resume.tex"],
                cwd=work_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,  # merge stderr into stdout for full log
                text=True,
                timeout=30
            )
            log_output = result.stdout  # keep the last run's log

        pdf_path = os.path.join(work_dir, "resume.pdf")
        # Use PDF existence as primary success signal — pdflatex can return rc=1
        # for non-fatal warnings (e.g. undefined icon names) while still producing a valid PDF.
        if os.path.exists(pdf_path) and os.path.getsize(pdf_path) > 1024:
            if result.returncode != 0:
                logger.warning(f"pdflatex rc={result.returncode} but PDF produced — treating as success.")
            return True, log_output
        else:
            logger.error(f"pdflatex failed (rc={result.returncode}), no PDF produced:\n{log_output[-3000:]}")
            return False, log_output

    except subprocess.TimeoutExpired:
        logger.error("pdflatex compilation timed out.")
        return False, "pdflatex timed out"
    except FileNotFoundError:
        logger.error("pdflatex not found. Ensure TeX Live is installed.")
        raise HTTPException(status_code=500, detail="pdflatex not installed on server.")

async def process_resume_stage(
    provider: str,
    base_prompt: str,
    work_dir: str,
    retries: int = 2
) -> Optional[str]:
    """
    Run LLM → compile loop with retry.
    On failure, feeds the pdflatex error log back to the LLM for self-correction.
    """
    prompt = base_prompt
    for attempt in range(retries):
        logger.info(f"LLM compilation attempt {attempt + 1}/{retries}")
        raw_response = await call_llm(provider, prompt)
        latex_code = extract_latex(raw_response)

        success, log = compile_latex_to_pdf(latex_code, work_dir)
        if success:
            return latex_code

        # Feed the actual error log back so the model can self-correct
        # Extract relevant error lines (lines starting with '!')
        error_lines = "\n".join(
            line for line in log.splitlines()
            if line.startswith("!") or "Error" in line or "Undefined" in line
        )
        prompt = (
            base_prompt
            + f"\n\nIMPORTANT: Your previous LaTeX caused compilation errors. "
            f"Fix ALL issues before responding.\n\nCompiler errors:\n{error_lines or log[-1500:]}\n\n"
            f"Broken LaTeX (for reference):\n```latex\n{latex_code[:3000]}\n```"
        )

    return None

def cleanup_temp_dir(dir_path: str):
    import shutil
    try:
        shutil.rmtree(dir_path)
    except Exception as e:
        logger.error(f"Failed to cleanup {dir_path}: {e}")

# --- Endpoints ---

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/tailor")
async def tailor_resume(
    background_tasks: BackgroundTasks,
    resume: UploadFile = File(...),
    job_description: str = Form(...),
    provider: str = Form("gemini")
):
    if not resume.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Upload must be a PDF file.")

    request_id = str(uuid.uuid4())
    work_dir = tempfile.mkdtemp(prefix=f"resume_{request_id}_")
    background_tasks.add_task(cleanup_temp_dir, work_dir)

    try:
        # 1. Read & extract PDF
        logger.info(f"[{request_id}] 1. Extracting PDF...")
        pdf_bytes = await resume.read()
        raw_text = extract_text_from_pdf(pdf_bytes)

        # 2. Stage A: Structure the resume into the LaTeX template
        logger.info(f"[{request_id}] 2. Stage A — Structuring...")
        # Use replace() instead of .format() to avoid KeyError on LaTeX curly braces like {5pt}
        stage_a_prompt = (
            STAGE_A_PROMPT
            .replace("{latex_template}", BASE_LATEX_TEMPLATE)
            .replace("{resume_text}", raw_text)
        )
        structured_latex = await process_resume_stage(provider, stage_a_prompt, work_dir)
        if not structured_latex:
            raise HTTPException(
                status_code=500,
                detail="Failed to generate valid structured LaTeX from resume after retries."
            )

        # 3. Stage B: Tailor the structured resume to the job description
        logger.info(f"[{request_id}] 3. Stage B — Tailoring...")
        stage_b_prompt = (
            STAGE_B_PROMPT
            .replace("{job_description}", job_description)
            .replace("{latex_resume}", structured_latex)
        )
        tailored_latex = await process_resume_stage(provider, stage_b_prompt, work_dir)
        if not tailored_latex:
            raise HTTPException(
                status_code=500,
                detail="Failed to compile tailored LaTeX resume after retries."
            )

        # 4. Stream back the compiled PDF
        pdf_path = os.path.join(work_dir, "resume.pdf")
        if not os.path.exists(pdf_path):
            raise HTTPException(status_code=500, detail="PDF not found after compilation.")

        logger.info(f"[{request_id}] Done. Returning PDF.")
        with open(pdf_path, "rb") as f:
            pdf_data = f.read()

        safe_filename = re.sub(r"[^\w.\-]", "_", resume.filename)
        return StreamingResponse(
            io.BytesIO(pdf_data),
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="tailored_{safe_filename}"'}
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[{request_id}] Unexpected error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal server error during processing.")