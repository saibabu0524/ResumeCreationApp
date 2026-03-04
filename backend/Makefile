# ─────────────────────────────────────────────────────────────────────────────
# FastAPI Production Template — Makefile
#
# Usage: make <target>
# ─────────────────────────────────────────────────────────────────────────────

.PHONY: help install run worker test lint format migrate downgrade shell clean

PYTHON := python
UVICORN := uvicorn
PYTEST := pytest
RUFF := ruff
BLACK := black
ALEMBIC := alembic

# ── Help ──────────────────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "  Available targets:"
	@echo "  ------------------"
	@echo "  install     Install all runtime + dev dependencies"
	@echo "  run         Start the API server with hot-reload"
	@echo "  worker      Start the ARQ background worker"
	@echo "  test        Run the full test suite"
	@echo "  cov         Run tests with coverage report"
	@echo "  lint        Check code style (Ruff + Black — no changes)"
	@echo "  format      Auto-fix code style (Ruff + Black)"
	@echo "  migrate     Apply all pending Alembic migrations"
	@echo "  downgrade   Roll back one migration"
	@echo "  shell       Open an interactive Python shell with app context"
	@echo "  clean       Remove __pycache__ and .pytest_cache"
	@echo ""

# ── Install ───────────────────────────────────────────────────────────────────
install:
	$(PYTHON) -m pip install -e ".[dev]"

# ── Run ───────────────────────────────────────────────────────────────────────
run:
	$(UVICORN) app.main:app --reload --host 0.0.0.0 --port 8000

# ── Worker ────────────────────────────────────────────────────────────────────
worker:
	arq app.workers.arq_worker.WorkerSettings

# ── Tests ─────────────────────────────────────────────────────────────────────
test:
	$(PYTEST) -v

cov:
	$(PYTEST) --cov=app --cov-report=term-missing --cov-report=html -v

# ── Linting & Formatting ──────────────────────────────────────────────────────
lint:
	$(RUFF) check .
	$(BLACK) --check .

format:
	$(RUFF) check --fix .
	$(BLACK) .

# ── Migrations ────────────────────────────────────────────────────────────────
migrate:
	$(ALEMBIC) upgrade head

downgrade:
	$(ALEMBIC) downgrade -1

# ── Shell ─────────────────────────────────────────────────────────────────────
shell:
	$(PYTHON) -c "from app.core.config import get_settings; s = get_settings(); print(s)"

# ── Clean ─────────────────────────────────────────────────────────────────────
clean:
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "htmlcov" -exec rm -rf {} + 2>/dev/null || true
	find . -name "*.pyc" -delete 2>/dev/null || true
