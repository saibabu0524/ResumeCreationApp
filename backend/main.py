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
                text += page.extract_text() + "\n"
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
    """Extract LaTeX content from <latex> tags, or return raw text if omitted (fallback)."""
    match = re.search(r"<latex>(.*?)</latex>", response_text, re.DOTALL | re.IGNORECASE)
    if match:
        return match.group(1).strip()
    # Fallback to markdown blocks if they messed up the tags
    match_md = re.search(r"```latex(.*?)```", response_text, re.DOTALL | re.IGNORECASE)
    if match_md:
       return match_md.group(1).strip()
    return response_text.strip()

async def call_llm(provider: str, prompt: str) -> str:
    """Absract LLM call to Gemini or Ollama."""
    if provider == "gemini":
        if not gemini_client:
            raise HTTPException(status_code=503, detail="Gemini API Key not configured.")
        try:
            # Using flash-2.0 or 1.5-flash
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

def compile_latex_to_pdf(latex_code: str, work_dir: str) -> bool:
    """Compile LaTeX to PDF using pdflatex. Returns True if successful."""
    tex_file_path = os.path.join(work_dir, "resume.tex")
    with open(tex_file_path, "w") as f:
        f.write(latex_code)

    try:
        # Run pdflatex twice for references/formatting
        for _ in range(2):
            result = subprocess.run(
                ["pdflatex", "-interaction=nonstopmode", "resume.tex"],
                cwd=work_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=30
            )

        pdf_path = os.path.join(work_dir, "resume.pdf")
        if result.returncode == 0 and os.path.exists(pdf_path):
            return True
        else:
            logger.error(f"pdflatex compilation failed. Return code: {result.returncode}")
            logger.debug(f"pdflatex stdout:\n{result.stdout}")
            return False
    except subprocess.TimeoutExpired:
        logger.error("pdflatex compilation timed out.")
        return False
    except FileNotFoundError:
        logger.error("pdflatex command not found. Ensure TeX Live is installed.")
        raise HTTPException(status_code=500, detail="pdflatex not installed on server.")

async def process_resume_stage(provider: str, prompt: str, work_dir: str, retries: int = 2) -> Optional[str]:
    """Run LLM and attempt pdflatex compilation with retry loop."""
    for attempt in range(retries):
        logger.info(f"LLM compilation attempt {attempt + 1}/{retries}")
        raw_llm_response = await call_llm(provider, prompt)
        latex_code = extract_latex(raw_llm_response)

        if compile_latex_to_pdf(latex_code, work_dir):
            return latex_code
        else:
             # Compile error - if we have another attempt, we could theoretically feed the log back.
             # For simplicity in this implementation, we just retry the generation with a stronger warning.
             prompt += "\n\nVERY IMPORTANT: Your previous response caused a LaTeX compilation error. Make sure you are escaping all special characters (e.g. \\%, \\&, \\$, \\#) properly and providing VALID formatting."
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
    if not resume.filename.endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Upload must be a PDF file.")

    request_id = str(uuid.uuid4())
    work_dir = tempfile.mkdtemp(prefix=f"resume_{request_id}_")
    background_tasks.add_task(cleanup_temp_dir, work_dir)

    try:
        # 1. Read PDF
        logger.info(f"[{request_id}] 1. Extracting PDF...")
        pdf_bytes = await resume.read()
        raw_text = extract_text_from_pdf(pdf_bytes)

        # 2. Stage A: Structure
        logger.info(f"[{request_id}] 2. Stage A (Structuring)...")
        stage_a_prompt = STAGE_A_PROMPT.format(
            latex_template=BASE_LATEX_TEMPLATE,
            resume_text=raw_text
        )
        structured_latex = await process_resume_stage(provider, stage_a_prompt, work_dir)
        if not structured_latex:
            raise HTTPException(status_code=500, detail="Failed to generate valid structured LaTeX from resume.")

        # 3. Stage B: Tailor
        logger.info(f"[{request_id}] 3. Stage B (Tailoring)...")
        stage_b_prompt = STAGE_B_PROMPT.format(
            job_description=job_description,
            latex_resume=structured_latex
        )
        tailored_latex = await process_resume_stage(provider, stage_b_prompt, work_dir)
        if not tailored_latex:
             raise HTTPException(status_code=500, detail="Failed to compile tailored LaTeX resume.")

        # 4. Return the compiled PDF
        pdf_path = os.path.join(work_dir, "resume.pdf")
        if not os.path.exists(pdf_path):
             raise HTTPException(status_code=500, detail="PDF was not found after compilation.")

        logger.info(f"[{request_id}] Success. Returning PDF.")
        with open(pdf_path, "rb") as f:
            pdf_data = f.read()

        return StreamingResponse(
            io.BytesIO(pdf_data),
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="tailored_{resume.filename}"'}
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[{request_id}] Unexpected error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal server error during processing.")
