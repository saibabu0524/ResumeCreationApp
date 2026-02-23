"""
Tests for the Build Agent.

Covers keystore resolution, AAB output verification, size reporting,
and full execute() lifecycle with mocked subprocess calls.
"""

from __future__ import annotations

import os
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.build_agent import BuildAgent
from agents.memory import PipelineMemory


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory pointing at a temp directory."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    memory.init_run(commit_sha="abc123", branch="release")
    return memory


@pytest.fixture
def fake_project(tmp_path: Path):
    """Create a minimal fake Android project structure."""
    project = tmp_path / "project"
    project.mkdir()
    return project


@pytest.fixture
def agent(tmp_pipeline, fake_project):
    """Return a BuildAgent wired to temp dirs."""
    return BuildAgent(
        memory=tmp_pipeline,
        project_root=str(fake_project),
    )


def _create_fake_aab(project: Path, size: int = 1024) -> Path:
    """Helper: create a fake AAB file at the expected output path."""
    aab_path = project / BuildAgent.AAB_OUTPUT_PATH
    aab_path.parent.mkdir(parents=True, exist_ok=True)
    aab_path.write_bytes(b"\x00" * size)
    return aab_path


def _create_fake_keystore(memory: PipelineMemory) -> Path:
    """Helper: create a fake keystore in the artifacts directory."""
    ks = memory.artifacts_dir / "release.jks"
    ks.write_bytes(b"FAKE_KEYSTORE")
    return ks


# ---------------------------------------------------------------------------
# TestBuildAgentSetup — Keystore resolution
# ---------------------------------------------------------------------------

class TestBuildAgentSetup:
    """Tests for _resolve_keystore_path()."""

    def test_resolve_keystore_when_secrets_agent_ran_returns_path(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Keystore written by Secrets Agent should be found."""
        ks = _create_fake_keystore(tmp_pipeline)
        result = agent._resolve_keystore_path()
        assert result == str(ks.resolve())
        assert Path(result).exists()

    def test_resolve_keystore_when_file_missing_raises_error(
        self, agent: BuildAgent
    ):
        """Should raise FileNotFoundError when keystore doesn't exist."""
        with pytest.raises(FileNotFoundError, match="Keystore not found"):
            agent._resolve_keystore_path()


# ---------------------------------------------------------------------------
# TestBuildAgentOutput — AAB verification & size
# ---------------------------------------------------------------------------

class TestBuildAgentOutput:
    """Tests for _verify_output() and _get_aab_size_mb()."""

    def test_verify_output_when_aab_exists_returns_true(
        self, agent: BuildAgent, fake_project: Path
    ):
        """A valid, non-empty AAB should pass verification."""
        _create_fake_aab(fake_project)
        assert agent._verify_output() is True

    def test_verify_output_when_aab_missing_returns_false(
        self, agent: BuildAgent
    ):
        """Missing AAB file should fail verification."""
        assert agent._verify_output() is False

    def test_verify_output_when_aab_empty_returns_false(
        self, agent: BuildAgent, fake_project: Path
    ):
        """An empty AAB file should fail verification."""
        _create_fake_aab(fake_project, size=0)
        assert agent._verify_output() is False

    def test_get_aab_size_when_file_exists_returns_float(
        self, agent: BuildAgent, fake_project: Path
    ):
        """Should return file size in MB as a float."""
        _create_fake_aab(fake_project, size=2 * 1024 * 1024)  # 2 MB
        size = agent._get_aab_size_mb()
        assert isinstance(size, float)
        assert size == pytest.approx(2.0, abs=0.01)

    def test_get_aab_size_when_file_missing_returns_zero(
        self, agent: BuildAgent
    ):
        """Should return 0.0 when the AAB doesn't exist."""
        assert agent._get_aab_size_mb() == 0.0


# ---------------------------------------------------------------------------
# TestBuildAgentExecute — Full lifecycle
# ---------------------------------------------------------------------------

class TestBuildAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_gradle_succeeds_returns_ok_with_path(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory,
        fake_project: Path
    ):
        """Successful build should return ok with aab_path and size_mb."""
        _create_fake_keystore(tmp_pipeline)
        _create_fake_aab(fake_project, size=5 * 1024 * 1024)

        with patch.object(agent, "_run_build") as mock_build:
            mock_build.return_value = (True, "BUILD SUCCESSFUL")
            result = agent.execute()

        assert result.success is True
        assert "aab_path" in result.data
        assert "size_mb" in result.data
        assert result.data["size_mb"] == pytest.approx(5.0, abs=0.01)

    def test_execute_when_gradle_fails_returns_fail_with_error(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Failed build should return fail with the error message."""
        _create_fake_keystore(tmp_pipeline)

        with patch.object(agent, "_run_build") as mock_build:
            mock_build.return_value = (False, "FAILURE: Build failed")
            result = agent.execute()

        assert result.success is False
        assert "Build failed" in result.error

    def test_execute_when_aab_not_produced_returns_fail(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Build may 'succeed' but produce no AAB — should still fail."""
        _create_fake_keystore(tmp_pipeline)
        # No fake AAB created → _verify_output will return False

        with patch.object(agent, "_run_build") as mock_build:
            mock_build.return_value = (True, "BUILD SUCCESSFUL")
            result = agent.execute()

        assert result.success is False
        assert "AAB not found" in result.error

    def test_execute_when_keystore_missing_returns_fail(
        self, agent: BuildAgent
    ):
        """Missing keystore should fail immediately."""
        result = agent.execute()
        assert result.success is False
        assert "Keystore not found" in result.error

    def test_execute_saves_aab_path_to_pipeline_state(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory,
        fake_project: Path
    ):
        """After successful build, aab_path should be in pipeline state."""
        _create_fake_keystore(tmp_pipeline)
        _create_fake_aab(fake_project)

        with patch.object(agent, "_run_build") as mock_build:
            mock_build.return_value = (True, "BUILD SUCCESSFUL")
            agent.execute()

        run = tmp_pipeline.load()
        assert run is not None
        assert run.aab_path is not None
        assert "app-prod-release.aab" in run.aab_path


# ---------------------------------------------------------------------------
# TestBuildAgentRunBuild — subprocess interactions
# ---------------------------------------------------------------------------

class TestBuildAgentRunBuild:
    """Tests for _run_build() subprocess handling."""

    def test_run_build_when_gradle_succeeds_returns_true(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Gradle exit code 0 should return (True, stdout)."""
        ks_path = str(_create_fake_keystore(tmp_pipeline))

        mock_result = MagicMock()
        mock_result.returncode = 0
        mock_result.stdout = "BUILD SUCCESSFUL"
        mock_result.stderr = ""

        with patch("subprocess.run", return_value=mock_result):
            success, output = agent._run_build(ks_path)

        assert success is True
        assert "BUILD SUCCESSFUL" in output

    def test_run_build_when_gradle_fails_returns_false(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Gradle non-zero exit code should return (False, stderr)."""
        ks_path = str(_create_fake_keystore(tmp_pipeline))

        mock_result = MagicMock()
        mock_result.returncode = 1
        mock_result.stdout = ""
        mock_result.stderr = "FAILURE: Build failed with exception"

        with patch("subprocess.run", return_value=mock_result):
            success, output = agent._run_build(ks_path)

        assert success is False
        assert "FAILURE" in output

    def test_run_build_when_gradle_wrapper_not_found_returns_false(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Missing gradlew should return (False, message)."""
        ks_path = str(_create_fake_keystore(tmp_pipeline))

        with patch("subprocess.run", side_effect=FileNotFoundError):
            success, output = agent._run_build(ks_path)

        assert success is False
        assert "gradlew not found" in output

    def test_run_build_when_timeout_returns_false(
        self, agent: BuildAgent, tmp_pipeline: PipelineMemory
    ):
        """Build timeout should return (False, message)."""
        ks_path = str(_create_fake_keystore(tmp_pipeline))

        with patch("subprocess.run", side_effect=subprocess.TimeoutExpired(
            cmd="./gradlew", timeout=1200
        )):
            success, output = agent._run_build(ks_path)

        assert success is False
        assert "timed out" in output
