# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Builder: install dependencies into a virtual environment
# ─────────────────────────────────────────────────────────────────────────────
FROM python:3.12-slim AS builder

WORKDIR /app

# Install build tools
RUN pip install --no-cache-dir --upgrade pip setuptools wheel

# Copy only the dependency manifest first (better layer caching)
COPY pyproject.toml .

# Install runtime dependencies into an isolated venv
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir -e "."

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime: lean production image
# ─────────────────────────────────────────────────────────────────────────────
FROM python:3.12-slim AS runtime

# Install TeX Live (pdflatex) for compiling resumes
RUN apt-get update && apt-get install -y --no-install-recommends \
    texlive-latex-base \
    texlive-latex-extra \
    texlive-fonts-recommended \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Copy the venv from the builder stage
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Copy the application source
COPY app/ ./app/
COPY alembic/ ./alembic/
COPY alembic.ini .

# Ensure the uploads directory exists with correct permissions
RUN mkdir -p /app/uploads && chown appuser:appgroup /app/uploads

USER appuser

EXPOSE 8000

# Run with Uvicorn.  Port and log level can be overridden via environment.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
