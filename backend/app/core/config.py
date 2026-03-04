"""
Application configuration loaded from environment variables / .env file.

All settings are read once at import time via a cached ``get_settings()``
dependency — never access ``Settings()`` directly in route handlers.
"""

from __future__ import annotations

from functools import lru_cache
from typing import Literal

from pydantic import AnyHttpUrl, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Single source of truth for every configurable value in the app.

    Add new settings here. Never scatter ``os.environ.get(...)`` calls
    around the codebase.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # ── Application ─────────────────────────────────────────────────────────
    APP_NAME: str = "FastAPI Production Template"
    ENVIRONMENT: Literal["development", "staging", "production"] = "development"
    LOG_LEVEL: Literal["debug", "info", "warning", "error", "critical"] = "info"
    DEBUG: bool = False

    # ── Security ─────────────────────────────────────────────────────────────
    SECRET_KEY: str = "CHANGE_ME_use_openssl_rand_hex_32"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # ── Database ─────────────────────────────────────────────────────────────
    DATABASE_URL: str = "sqlite+aiosqlite:///./db.sqlite3"

    # ── CORS ─────────────────────────────────────────────────────────────────
    ALLOWED_ORIGINS: str | list[str] = ["http://localhost:3000", "http://localhost:8080"]

    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def parse_cors_origins(cls, value: str | list[str]) -> list[str]:
        """Accept either a JSON list or a comma-separated string from .env."""
        if isinstance(value, str):
            return [origin.strip() for origin in value.split(",") if origin.strip()]
        return value

    # ── File uploads ─────────────────────────────────────────────────────────
    UPLOAD_DIR: str = "uploads"
    MAX_UPLOAD_SIZE_MB: int = 10
    ALLOWED_MIME_TYPES: list[str] = [
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "application/pdf",
    ]

    # ── Rate limiting ────────────────────────────────────────────────────────
    RATE_LIMIT_DEFAULT: str = "100/minute"
    RATE_LIMIT_AUTH: str = "20/minute"

    # ── Redis / ARQ ──────────────────────────────────────────────────────────
    REDIS_URL: str = "redis://localhost:6379"

    # ── AI / LLM providers ───────────────────────────────────────────────────
    # Gemini
    GEMINI_API_KEY: str | None = None
    GEMINI_MODEL: str = "gemini-2.5-flash"

    # Ollama (local or remote)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3"
    OLLAMA_API_KEY: str | None = None

    # OpenAI-compatible cloud (e.g. Kimi / Moonshot)
    CLOUD_API_KEY: str | None = None
    CLOUD_BASE_URL: str = "https://api.moonshot.cn/v1"
    CLOUD_MODEL: str = "moonshot-v1-8k"

    # ── Resume processing ────────────────────────────────────────────────────
    LLM_RETRY_ATTEMPTS: int = 2

    # ── Derived helpers ──────────────────────────────────────────────────────
    @property
    def is_production(self) -> bool:
        return self.ENVIRONMENT == "production"

    @property
    def max_upload_bytes(self) -> int:
        return self.MAX_UPLOAD_SIZE_MB * 1024 * 1024


@lru_cache
def get_settings() -> Settings:
    """Return a cached Settings instance.

    Use as a FastAPI dependency::

        async def my_route(settings: Settings = Depends(get_settings)):
            ...
    """
    return Settings()
