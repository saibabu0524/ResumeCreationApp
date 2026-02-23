"""
Tests for the SecretsAgent.

Covers:
- Environment variable validation
- Base64 keystore decoding and file permissions
- Cleanup of sensitive files
- Full execute() workflow (success, missing secrets, invalid keystore)
"""

from __future__ import annotations

import base64
import os
import stat
import subprocess
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from agents.memory import PipelineMemory
from agents.secrets_agent import SecretsAgent, REQUIRED_ENV_VARS
from agents.base_agent import AgentResult


# ------------------------------------------------------------------ #
#  Helpers
# ------------------------------------------------------------------ #

# Realistic base64 payload (just enough bytes to look like a JKS header)
FAKE_KEYSTORE_BYTES = b"\xfe\xed\xfe\xed" + b"\x00" * 32
FAKE_KEYSTORE_B64 = base64.b64encode(FAKE_KEYSTORE_BYTES).decode()

# Minimal set of valid env vars
VALID_ENV: dict[str, str] = {
    "KEYSTORE_FILE_B64": FAKE_KEYSTORE_B64,
    "KEYSTORE_PASSWORD": "store_pass_123",
    "KEY_ALIAS": "release_key",
    "KEY_PASSWORD": "key_pass_456",
    "PLAY_STORE_JSON_KEY": '{"type": "service_account", "project_id": "test"}',
}


@pytest.fixture()
def memory(tmp_path: Path) -> PipelineMemory:
    """Create a PipelineMemory rooted in a temp dir with an active run."""
    mem = PipelineMemory(state_dir=str(tmp_path / ".pipeline"))
    mem.init_run(commit_sha="abc123", branch="main")
    return mem


@pytest.fixture()
def agent(memory: PipelineMemory) -> SecretsAgent:
    """Create a SecretsAgent with a fresh memory instance."""
    return SecretsAgent(memory=memory)


# ================================================================== #
#  TestSecretsAgentValidation
# ================================================================== #


class TestSecretsAgentValidation:
    """Tests for _validate_all_env_vars()."""

    def test_validate_env_vars_when_all_present_returns_true(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """All 5 env vars set and non-empty → (True, [])."""
        for key, value in VALID_ENV.items():
            monkeypatch.setenv(key, value)

        ok, missing = agent._validate_all_env_vars()
        assert ok is True
        assert missing == []

    def test_validate_env_vars_when_keystore_missing_returns_false(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """KEYSTORE_FILE_B64 unset → False with its name listed."""
        for key, value in VALID_ENV.items():
            if key != "KEYSTORE_FILE_B64":
                monkeypatch.setenv(key, value)
        monkeypatch.delenv("KEYSTORE_FILE_B64", raising=False)

        ok, missing = agent._validate_all_env_vars()
        assert ok is False
        assert "Missing: KEYSTORE_FILE_B64" in missing

    def test_validate_env_vars_when_play_key_missing_returns_false(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """PLAY_STORE_JSON_KEY unset → False with its name listed."""
        for key, value in VALID_ENV.items():
            if key != "PLAY_STORE_JSON_KEY":
                monkeypatch.setenv(key, value)
        monkeypatch.delenv("PLAY_STORE_JSON_KEY", raising=False)

        ok, missing = agent._validate_all_env_vars()
        assert ok is False
        assert "Missing: PLAY_STORE_JSON_KEY" in missing

    def test_validate_env_vars_when_empty_string_returns_false(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """An env var that is set to '' is treated as missing."""
        for key, value in VALID_ENV.items():
            monkeypatch.setenv(key, value)
        # Set one to empty string
        monkeypatch.setenv("KEY_PASSWORD", "")

        ok, missing = agent._validate_all_env_vars()
        assert ok is False
        assert "Missing: KEY_PASSWORD" in missing

    def test_validate_env_vars_when_legacy_keystore_name_used_returns_true(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Legacy KEYSTORE_B64 should be accepted as KEYSTORE_FILE_B64."""
        for key, value in VALID_ENV.items():
            if key != "KEYSTORE_FILE_B64":
                monkeypatch.setenv(key, value)
        monkeypatch.setenv("KEYSTORE_B64", FAKE_KEYSTORE_B64)
        monkeypatch.delenv("KEYSTORE_FILE_B64", raising=False)

        ok, missing = agent._validate_all_env_vars()
        assert ok is True
        assert missing == []


# ================================================================== #
#  TestSecretsAgentDecoding
# ================================================================== #


class TestSecretsAgentDecoding:
    """Tests for _decode_keystore()."""

    def test_decode_keystore_when_valid_b64_writes_file(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Valid base64 → file at artifacts/release.jks with correct content."""
        monkeypatch.setenv("KEYSTORE_FILE_B64", FAKE_KEYSTORE_B64)

        path = agent._decode_keystore()
        written = Path(path)

        assert written.exists()
        assert written.name == "release.jks"
        assert written.read_bytes() == FAKE_KEYSTORE_BYTES

    def test_decode_keystore_when_invalid_b64_returns_fail(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Invalid base64 → raises ValueError with expected message."""
        monkeypatch.setenv("KEYSTORE_FILE_B64", "!!!NOT_BASE64!!!")

        with pytest.raises(ValueError, match="Invalid base64 in KEYSTORE_FILE_B64"):
            agent._decode_keystore()

    def test_decode_keystore_when_written_has_correct_permissions(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Written keystore must have mode 0o600 (owner rw only)."""
        monkeypatch.setenv("KEYSTORE_FILE_B64", FAKE_KEYSTORE_B64)

        path = agent._decode_keystore()
        file_stat = os.stat(path)
        mode = stat.S_IMODE(file_stat.st_mode)

        assert mode == 0o600, f"Expected 0o600, got {oct(mode)}"


# ================================================================== #
#  TestSecretsAgentCleanup
# ================================================================== #


class TestSecretsAgentCleanup:
    """Tests for cleanup()."""

    def test_cleanup_when_files_exist_removes_them(
        self, agent: SecretsAgent
    ) -> None:
        """Cleanup deletes release.jks and play_key.json if they exist."""
        jks = agent.memory.artifacts_dir / "release.jks"
        play = agent.memory.artifacts_dir / "play_key.json"
        jks.write_bytes(b"fake")
        play.write_text("fake", encoding="utf-8")

        agent.cleanup()

        assert not jks.exists()
        assert not play.exists()

    def test_cleanup_when_files_missing_does_not_raise(
        self, agent: SecretsAgent
    ) -> None:
        """Cleanup must not raise when files are already absent."""
        # Ensure files don't exist
        jks = agent.memory.artifacts_dir / "release.jks"
        play = agent.memory.artifacts_dir / "play_key.json"
        assert not jks.exists()
        assert not play.exists()

        # Should not raise
        agent.cleanup()


# ================================================================== #
#  TestSecretsAgentExecute
# ================================================================== #


class TestSecretsAgentExecute:
    """Tests for execute()."""

    def test_execute_when_all_secrets_available_returns_ok(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Happy path: all secrets present, keytool succeeds → ok result."""
        for key, value in VALID_ENV.items():
            monkeypatch.setenv(key, value)

        mock_result = MagicMock()
        mock_result.returncode = 0
        mock_result.stderr = ""
        mock_result.stdout = "Keystore type: JKS"

        with patch("agents.secrets_agent.subprocess.run", return_value=mock_result):
            result = agent.execute()

        assert result.success is True
        assert "keystore_path" in result.data
        assert "play_key_path" in result.data
        assert result.data["keystore_path"].endswith("release.jks")
        assert result.data["play_key_path"].endswith("play_key.json")
        assert Path(result.data["keystore_path"]).exists()
        assert Path(result.data["play_key_path"]).exists()

    def test_execute_when_missing_secret_returns_fail_with_name(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Missing KEYSTORE_PASSWORD → fail result listing the missing secret."""
        for key, value in VALID_ENV.items():
            if key != "KEYSTORE_PASSWORD":
                monkeypatch.setenv(key, value)
        monkeypatch.delenv("KEYSTORE_PASSWORD", raising=False)

        result = agent.execute()

        assert result.success is False
        assert result.error is not None
        assert "KEYSTORE_PASSWORD" in result.error

    def test_execute_when_invalid_keystore_returns_fail(
        self, agent: SecretsAgent, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """keytool returns non-zero → fail result about keystore validation."""
        for key, value in VALID_ENV.items():
            monkeypatch.setenv(key, value)

        mock_result = MagicMock()
        mock_result.returncode = 1
        mock_result.stderr = "keytool error: invalid keystore"

        with patch("agents.secrets_agent.subprocess.run", return_value=mock_result):
            result = agent.execute()

        assert result.success is False
        assert result.error is not None
        assert "keystore" in result.error.lower() or "Keystore" in result.error
