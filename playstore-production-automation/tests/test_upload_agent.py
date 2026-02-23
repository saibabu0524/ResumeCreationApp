"""
Tests for the Upload Agent.

Covers metadata loading, Fastlane error parsing, JSON key handling,
and the full execute() lifecycle with security cleanup verification.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.upload_agent import UploadAgent
from agents.memory import PipelineMemory


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory with a build-ready state."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    run = memory.init_run(commit_sha="abc123", branch="release")

    # Simulate Build Agent having completed
    aab_path = tmp_path / "app" / "build" / "outputs" / "bundle" / "release"
    aab_path.mkdir(parents=True, exist_ok=True)
    aab_file = aab_path / "app-release.aab"
    aab_file.write_bytes(b"\x00" * 1024)

    run.aab_path = str(aab_file)
    run.version_code = 42
    run.version_name = "2.1.0"
    run.package_name = "com.example.testapp"
    memory.save(run)

    # Write metadata.json (simulating Metadata Agent)
    metadata = {
        "package_name": "com.example.testapp",
        "version_code": 42,
        "version_name": "2.1.0",
        "track": "internal",
        "release_notes": {
            "en-US": "Bug fixes and improvements.",
        },
    }
    metadata_path = memory.artifacts_dir / "metadata.json"
    metadata_path.write_text(json.dumps(metadata), encoding="utf-8")

    return memory


@pytest.fixture
def fake_project(tmp_path: Path):
    """Create a minimal fake project."""
    project = tmp_path / "project"
    project.mkdir()
    return project


@pytest.fixture
def agent(tmp_pipeline, tmp_path):
    """Return an UploadAgent wired to temp dirs."""
    a = UploadAgent(
        memory=tmp_pipeline,
        project_root=str(tmp_path),
    )
    # Override the tmp key path so we don't write to real /tmp
    a.JSON_KEY_TMP_PATH = str(tmp_path / "play_store_key.json")
    return a


# ---------------------------------------------------------------------------
# TestLoadMetadata
# ---------------------------------------------------------------------------

class TestLoadMetadata:
    """Tests for _load_metadata()."""

    def test_load_metadata_when_file_exists_returns_dict(
        self, agent: UploadAgent
    ):
        """Should successfully load and parse metadata.json."""
        result = agent._load_metadata()
        assert isinstance(result, dict)
        assert result["package_name"] == "com.example.testapp"
        assert result["version_code"] == 42

    def test_load_metadata_when_file_missing_returns_empty_dict(
        self, tmp_path: Path
    ):
        """Missing metadata.json should return empty dict."""
        state_dir = tmp_path / ".pipe_empty"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run(commit_sha="x", branch="main")
        a = UploadAgent(memory=memory, project_root=str(tmp_path))
        result = a._load_metadata()
        assert result == {}

    def test_load_metadata_when_invalid_json_returns_empty_dict(
        self, agent: UploadAgent, tmp_pipeline: PipelineMemory
    ):
        """Corrupted metadata.json should return empty dict."""
        meta_path = tmp_pipeline.artifacts_dir / "metadata.json"
        meta_path.write_text("NOT VALID JSON {{{{", encoding="utf-8")
        result = agent._load_metadata()
        assert result == {}


class TestFastlaneCommand:
    """Tests for fastlane command selection."""

    def test_get_fastlane_cmd_with_gemfile_uses_bundle_exec(
        self, tmp_pipeline: PipelineMemory, tmp_path: Path
    ):
        """Gemfile present -> run fastlane through Bundler."""
        (tmp_path / "Gemfile").write_text(
            'source "https://rubygems.org"\n',
            encoding="utf-8",
        )
        a = UploadAgent(memory=tmp_pipeline, project_root=str(tmp_path))
        assert a._get_fastlane_cmd() == ["bundle", "exec", "fastlane"]

    def test_get_fastlane_cmd_without_gemfile_uses_global_fastlane(
        self, tmp_pipeline: PipelineMemory, tmp_path: Path
    ):
        """No Gemfile -> call global fastlane binary."""
        a = UploadAgent(memory=tmp_pipeline, project_root=str(tmp_path))
        assert a._get_fastlane_cmd() == ["fastlane"]


# ---------------------------------------------------------------------------
# TestParseFastlaneError
# ---------------------------------------------------------------------------

class TestParseFastlaneError:
    """Tests for _parse_fastlane_error()."""

    def test_parse_error_when_version_code_used_returns_clear_message(
        self, agent: UploadAgent
    ):
        """Should suggest re-running the version agent."""
        result = agent._parse_fastlane_error(
            "Error: Version code has already been used"
        )
        assert "Version Agent" in result

    def test_parse_error_when_403_returns_permission_message(
        self, agent: UploadAgent
    ):
        """Should suggest granting release manager role."""
        result = agent._parse_fastlane_error(
            "Google API returned 403 Forbidden"
        )
        assert "Release manager" in result

    def test_parse_error_when_caller_no_permission_returns_actionable_message(
        self, agent: UploadAgent
    ):
        """Should guide app-level Play Console permission checks."""
        result = agent._parse_fastlane_error(
            "Google Api Error: Invalid request - The caller does not have permission"
        )
        assert "service account" in result.lower()

    def test_parse_error_when_package_not_found_returns_manual_upload_message(
        self, agent: UploadAgent
    ):
        """Should explain the first upload must be manual."""
        result = agent._parse_fastlane_error("Package not found in Play Store")
        assert "manually" in result.lower()

    def test_parse_error_when_missing_title_returns_listing_message(
        self, agent: UploadAgent
    ):
        """Should explain missing store listing title for en-US."""
        result = agent._parse_fastlane_error(
            "This app has no title for language en-US."
        )
        assert "title" in result.lower()
        assert "en-us" in result.lower()

    def test_parse_error_when_401_returns_key_error_message(
        self, agent: UploadAgent
    ):
        """Should suggest regenerating the JSON key."""
        result = agent._parse_fastlane_error("HTTP 401 Unauthorized")
        assert "expired" in result.lower() or "invalid" in result.lower()

    def test_parse_error_when_unknown_error_returns_truncated_output(
        self, agent: UploadAgent
    ):
        """Unknown errors should return truncated raw output."""
        long_error = "x" * 1000
        result = agent._parse_fastlane_error(long_error)
        assert len(result) <= 500


# ---------------------------------------------------------------------------
# TestUploadAgentExecute
# ---------------------------------------------------------------------------

class TestUploadAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_aab_missing_returns_fail_with_clear_message(
        self, tmp_path: Path
    ):
        """Should fail clearly if no AAB was produced."""
        state_dir = tmp_path / ".pipe_no_aab"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run(commit_sha="x", branch="release")
        run.aab_path = "/nonexistent/path.aab"
        memory.save(run)

        a = UploadAgent(memory=memory, project_root=str(tmp_path))
        result = a.execute()
        assert result.success is False
        assert "No AAB found" in result.error

    def test_execute_when_metadata_missing_returns_fail_with_clear_message(
        self, tmp_path: Path
    ):
        """Should fail clearly if metadata.json is missing."""
        state_dir = tmp_path / ".pipe_no_meta"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run(commit_sha="x", branch="release")

        aab_file = tmp_path / "test.aab"
        aab_file.write_bytes(b"\x00")
        run.aab_path = str(aab_file)
        memory.save(run)

        a = UploadAgent(memory=memory, project_root=str(tmp_path))
        result = a.execute()
        assert result.success is False
        assert "metadata" in result.error.lower()

    def test_execute_when_json_key_missing_returns_fail_with_clear_message(
        self, agent: UploadAgent
    ):
        """Should fail if PLAY_STORE_JSON_KEY env var is not set."""
        with patch.dict(os.environ, {}, clear=True):
            result = agent.execute()
        assert result.success is False
        assert "PLAY_STORE_JSON_KEY" in result.error

    def test_execute_when_fastlane_succeeds_returns_ok(
        self, agent: UploadAgent
    ):
        """Successful upload should return ok with track and version."""
        with patch.dict(os.environ, {"PLAY_STORE_JSON_KEY": '{"type":"service_account"}'}):
            with patch.object(agent, "_run_fastlane_supply") as mock_supply:
                mock_supply.return_value = (True, "Upload successful")
                result = agent.execute()

        assert result.success is True
        assert result.data["track"] == "internal"
        assert result.data["uploaded"] is True

    def test_execute_when_fastlane_fails_parses_error_message(
        self, agent: UploadAgent
    ):
        """Failed upload should return parsed, actionable error."""
        with patch.dict(os.environ, {"PLAY_STORE_JSON_KEY": '{"type":"sa"}'}):
            with patch.object(agent, "_run_fastlane_supply") as mock_supply:
                mock_supply.return_value = (
                    False,
                    "Version code has already been used",
                )
                result = agent.execute()

        assert result.success is False
        assert "Version Agent" in result.error

    def test_execute_always_deletes_key_file_on_success(
        self, agent: UploadAgent, tmp_path: Path
    ):
        """JSON key file must be deleted after successful upload."""
        key_path = Path(agent.JSON_KEY_TMP_PATH)

        with patch.dict(os.environ, {"PLAY_STORE_JSON_KEY": '{"type":"sa"}'}):
            with patch.object(agent, "_run_fastlane_supply") as mock_supply:
                mock_supply.return_value = (True, "OK")
                agent.execute()

        assert not key_path.exists()

    def test_execute_always_deletes_key_file_on_failure(
        self, agent: UploadAgent, tmp_path: Path
    ):
        """JSON key file must be deleted even when upload fails."""
        key_path = Path(agent.JSON_KEY_TMP_PATH)

        with patch.dict(os.environ, {"PLAY_STORE_JSON_KEY": '{"type":"sa"}'}):
            with patch.object(agent, "_run_fastlane_supply") as mock_supply:
                mock_supply.return_value = (False, "Some error")
                agent.execute()

        assert not key_path.exists()


# ---------------------------------------------------------------------------
# TestWriteJsonKey
# ---------------------------------------------------------------------------

class TestWriteJsonKey:
    """Tests for _write_json_key()."""

    def test_write_json_key_when_env_set_writes_file(
        self, agent: UploadAgent
    ):
        """Should write the key content to the temp path."""
        with patch.dict(os.environ, {"PLAY_STORE_JSON_KEY": '{"key":"value"}'}):
            path = agent._write_json_key()
        assert Path(path).exists()
        assert Path(path).read_text() == '{"key":"value"}'

    def test_write_json_key_when_env_missing_raises(self, agent: UploadAgent):
        """Should raise ValueError if env var is not set."""
        with patch.dict(os.environ, {}, clear=True):
            with pytest.raises(ValueError, match="PLAY_STORE_JSON_KEY"):
                agent._write_json_key()


# ---------------------------------------------------------------------------
# TestRunFastlaneSupplyEnv
# ---------------------------------------------------------------------------

class TestRunFastlaneSupplyEnv:
    """Tests that _run_fastlane_supply sets the correct env vars for Fastlane auth."""

    def test_supply_json_key_env_var_is_set_when_fastlane_called(
        self, agent: UploadAgent, tmp_path: Path
    ):
        """SUPPLY_JSON_KEY must be set to the key file path — Fastlane reads THIS for auth."""
        captured_env: dict = {}

        def capture_env(*args, **kwargs):
            captured_env.update(kwargs.get("env", {}))
            mock = MagicMock()
            mock.returncode = 0
            mock.stdout = "success"
            mock.stderr = ""
            return mock

        with patch("agents.upload_agent.subprocess.run", side_effect=capture_env):
            agent._run_fastlane_supply(
                aab_path="/fake/app.aab",
                track="internal",
                json_key_path="/tmp/key.json",
                package_name="com.example.app",
            )

        assert captured_env.get("SUPPLY_JSON_KEY") == "/tmp/key.json", (
            "SUPPLY_JSON_KEY must be set to the key file path for Fastlane auth"
        )
        assert captured_env.get("PLAY_STORE_JSON_KEY") == "/tmp/key.json"
        assert captured_env.get("SUPPLY_AAB") == "/fake/app.aab"
        assert captured_env.get("SUPPLY_TRACK") == "internal"

    def test_supply_package_name_env_var_is_set_when_package_name_provided(
        self, agent: UploadAgent
    ):
        """SUPPLY_PACKAGE_NAME should be set from the explicit package_name arg."""
        captured_env: dict = {}

        def capture_env(*args, **kwargs):
            captured_env.update(kwargs.get("env", {}))
            mock = MagicMock()
            mock.returncode = 0
            mock.stdout = "success"
            mock.stderr = ""
            return mock

        with patch("agents.upload_agent.subprocess.run", side_effect=capture_env):
            agent._run_fastlane_supply(
                aab_path="/fake/app.aab",
                track="alpha",
                json_key_path="/tmp/key.json",
                package_name="com.example.app",
            )

        assert captured_env.get("SUPPLY_PACKAGE_NAME") == "com.example.app"
