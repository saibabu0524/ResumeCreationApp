"""
Comprehensive tests for agents/version_agent.py.

Tests cover version reading/writing from Gradle files (Groovy & KTS),
semantic version patch bumping, and the full execute() lifecycle including
lock acquisition, failure modes, and state persistence.
"""

from __future__ import annotations

import os
from pathlib import Path
from unittest.mock import patch

import pytest

from agents.base_agent import AgentResult
from agents.memory import PipelineMemory
from agents.version_agent import VersionAgent


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

SAMPLE_GRADLE = """\
android {
    compileSdkVersion 34

    defaultConfig {
        applicationId "com.example.app"
        minSdkVersion 21
        targetSdkVersion 34
        versionCode 42
        versionName "1.2.3"
    }
}
"""

SAMPLE_GRADLE_KTS = """\
android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 21
        targetSdk = 34
        versionCode = 42
        versionName = "1.2.3"
    }
}
"""


@pytest.fixture()
def gradle_file(tmp_path: Path) -> Path:
    """Create a temporary Groovy build.gradle file."""
    app_dir = tmp_path / "app"
    app_dir.mkdir()
    gradle = app_dir / "build.gradle"
    gradle.write_text(SAMPLE_GRADLE, encoding="utf-8")
    return gradle


@pytest.fixture()
def gradle_kts_file(tmp_path: Path) -> Path:
    """Create a temporary Kotlin DSL build.gradle.kts file."""
    app_dir = tmp_path / "app"
    app_dir.mkdir(exist_ok=True)
    gradle = app_dir / "build.gradle.kts"
    gradle.write_text(SAMPLE_GRADLE_KTS, encoding="utf-8")
    return gradle


@pytest.fixture()
def memory(tmp_path: Path) -> PipelineMemory:
    """Create a PipelineMemory instance in a temp directory."""
    state_dir = tmp_path / ".pipeline"
    mem = PipelineMemory(state_dir=str(state_dir))
    mem.init_run("abc123", "main")
    return mem


@pytest.fixture()
def agent(memory: PipelineMemory, tmp_path: Path) -> VersionAgent:
    """Create a VersionAgent pointing at the temp project root."""
    return VersionAgent(memory=memory, project_root=str(tmp_path))


# ---------------------------------------------------------------------------
# TestReadVersionFromGradle
# ---------------------------------------------------------------------------


class TestReadVersionFromGradle:
    """Tests for VersionAgent.read_version_from_gradle()."""

    def test_read_version_when_valid_gradle_returns_code_and_name(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Should parse versionCode and versionName from a valid Groovy gradle file."""
        code, name = agent.read_version_from_gradle(str(gradle_file))
        assert code == 42
        assert name == "1.2.3"

    def test_read_version_when_valid_kts_gradle_returns_code_and_name(
        self, agent: VersionAgent, gradle_kts_file: Path
    ) -> None:
        """Should parse versionCode and versionName from a valid KTS gradle file."""
        code, name = agent.read_version_from_gradle(str(gradle_kts_file))
        assert code == 42
        assert name == "1.2.3"

    def test_read_version_when_file_missing_raises_file_not_found(
        self, agent: VersionAgent
    ) -> None:
        """Should raise FileNotFoundError when the gradle file doesn't exist."""
        with pytest.raises(FileNotFoundError):
            agent.read_version_from_gradle("/nonexistent/build.gradle")

    def test_read_version_when_no_version_code_raises_value_error(
        self, agent: VersionAgent, tmp_path: Path
    ) -> None:
        """Should raise ValueError when versionCode is missing from gradle."""
        gradle = tmp_path / "build.gradle"
        gradle.write_text('versionName "1.0.0"\n', encoding="utf-8")
        with pytest.raises(ValueError, match="versionCode not found"):
            agent.read_version_from_gradle(str(gradle))

    def test_read_version_when_no_version_name_raises_value_error(
        self, agent: VersionAgent, tmp_path: Path
    ) -> None:
        """Should raise ValueError when versionName is missing from gradle."""
        gradle = tmp_path / "build.gradle"
        gradle.write_text("versionCode 42\n", encoding="utf-8")
        with pytest.raises(ValueError, match="versionName not found"):
            agent.read_version_from_gradle(str(gradle))


# ---------------------------------------------------------------------------
# TestWriteVersionToGradle
# ---------------------------------------------------------------------------


class TestWriteVersionToGradle:
    """Tests for VersionAgent.write_version_to_gradle()."""

    def test_write_version_when_called_updates_version_code(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Should update the versionCode in the gradle file."""
        agent.write_version_to_gradle(str(gradle_file), 43, "1.2.4")
        code, _ = agent.read_version_from_gradle(str(gradle_file))
        assert code == 43

    def test_write_version_when_called_updates_version_name(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Should update the versionName in the gradle file."""
        agent.write_version_to_gradle(str(gradle_file), 43, "1.2.4")
        _, name = agent.read_version_from_gradle(str(gradle_file))
        assert name == "1.2.4"

    def test_write_version_when_called_preserves_other_gradle_content(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Should preserve all non-version content in the gradle file."""
        agent.write_version_to_gradle(str(gradle_file), 43, "1.2.4")
        content = gradle_file.read_text(encoding="utf-8")
        # Original non-version lines should still be present
        assert 'applicationId "com.example.app"' in content
        assert "compileSdkVersion 34" in content
        assert "minSdkVersion 21" in content
        assert "targetSdkVersion 34" in content

    def test_write_version_when_kts_format_updates_correctly(
        self, agent: VersionAgent, gradle_kts_file: Path
    ) -> None:
        """Should correctly update versions in KTS format."""
        agent.write_version_to_gradle(str(gradle_kts_file), 100, "2.0.0")
        code, name = agent.read_version_from_gradle(str(gradle_kts_file))
        assert code == 100
        assert name == "2.0.0"


# ---------------------------------------------------------------------------
# TestBumpPatch
# ---------------------------------------------------------------------------


class TestBumpPatch:
    """Tests for VersionAgent._bump_patch()."""

    def test_bump_patch_when_valid_semver_increments_patch(
        self, agent: VersionAgent
    ) -> None:
        """Should increment the patch number for valid semver."""
        assert agent._bump_patch("1.2.3") == "1.2.4"

    def test_bump_patch_when_patch_is_9_carries_correctly(
        self, agent: VersionAgent
    ) -> None:
        """Should carry over 9 → 10 without affecting major/minor."""
        assert agent._bump_patch("1.2.9") == "1.2.10"

    def test_bump_patch_when_large_numbers(
        self, agent: VersionAgent
    ) -> None:
        """Should handle large version numbers correctly."""
        assert agent._bump_patch("10.20.99") == "10.20.100"

    def test_bump_patch_when_non_semver_returns_unchanged(
        self, agent: VersionAgent
    ) -> None:
        """Should return the input unchanged if not valid 3-part semver."""
        assert agent._bump_patch("not-a-version") == "not-a-version"

    def test_bump_patch_when_two_part_version_returns_unchanged(
        self, agent: VersionAgent
    ) -> None:
        """Should return a 2-part version unchanged (not semver)."""
        assert agent._bump_patch("1.2") == "1.2"

    def test_bump_patch_when_four_parts_returns_unchanged(
        self, agent: VersionAgent
    ) -> None:
        """Should return a 4-part version unchanged (not semver)."""
        assert agent._bump_patch("1.2.3.4") == "1.2.3.4"

    def test_bump_patch_when_zero_patch(
        self, agent: VersionAgent
    ) -> None:
        """Should increment 0 patch to 1."""
        assert agent._bump_patch("1.0.0") == "1.0.1"


# ---------------------------------------------------------------------------
# TestVersionAgentExecute
# ---------------------------------------------------------------------------


class TestVersionAgentExecute:
    """Tests for VersionAgent.execute() lifecycle."""

    def test_execute_when_successful_increments_version_code(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Should bump version code and name, and return old/new values."""
        result = agent.execute()

        assert result.success is True
        assert result.data["old_version_code"] == 42
        assert result.data["new_version_code"] == 43
        assert result.data["old_version_name"] == "1.2.3"
        assert result.data["new_version_name"] == "1.2.4"

        # Verify the gradle file was actually updated
        code, name = agent.read_version_from_gradle(str(gradle_file))
        assert code == 43
        assert name == "1.2.4"

    def test_execute_when_successful_saves_version_to_pipeline_state(
        self, agent: VersionAgent, gradle_file: Path, memory: PipelineMemory
    ) -> None:
        """Should persist the new version in the pipeline run state."""
        agent.execute()

        run = memory.load()
        assert run is not None
        assert run.version_code == 43
        assert run.version_name == "1.2.4"

    def test_execute_when_lock_unavailable_returns_fail(
        self, memory: PipelineMemory, tmp_path: Path
    ) -> None:
        """Should return failure when the version lock cannot be acquired."""
        # Create the gradle file so we don't fail at the "file not found" step
        app_dir = tmp_path / "app"
        app_dir.mkdir(exist_ok=True)
        gradle = app_dir / "build.gradle"
        gradle.write_text(SAMPLE_GRADLE, encoding="utf-8")

        agent = VersionAgent(
            memory=memory, project_root=str(tmp_path)
        )

        with patch.object(memory, "acquire_lock", return_value=False):
            # Patch time.time to immediately exceed the 30s timeout
            call_count = 0
            original_time = __import__("time").time

            def fake_time() -> float:
                nonlocal call_count
                call_count += 1
                # First call returns 0, second returns 31 (past deadline)
                if call_count <= 1:
                    return 0.0
                return 31.0

            with patch("agents.version_agent.time.time", side_effect=fake_time):
                with patch("agents.version_agent.time.sleep"):
                    result = agent.execute()

        assert result.success is False
        assert "Could not acquire version lock" in (result.error or "")

    def test_execute_when_gradle_missing_returns_fail(
        self, memory: PipelineMemory, tmp_path: Path
    ) -> None:
        """Should return failure when no build.gradle exists in project."""
        # Use a temp dir with no gradle files at all
        empty_project = tmp_path / "empty_project"
        empty_project.mkdir()

        agent = VersionAgent(
            memory=memory, project_root=str(empty_project)
        )
        result = agent.execute()

        assert result.success is False
        assert "No build.gradle found" in (result.error or "")

    def test_execute_when_lock_released_on_success(
        self, agent: VersionAgent, gradle_file: Path, memory: PipelineMemory
    ) -> None:
        """Should release the version lock after successful execution."""
        with patch.object(memory, "release_lock") as mock_release:
            agent.execute()
            mock_release.assert_called_once_with("version")

    def test_execute_when_lock_released_on_failure(
        self, memory: PipelineMemory, tmp_path: Path
    ) -> None:
        """Should release the version lock even when execution fails."""
        # Create gradle without versionName to force a ValueError
        app_dir = tmp_path / "app"
        app_dir.mkdir(exist_ok=True)
        gradle = app_dir / "build.gradle"
        gradle.write_text("versionCode 1\n", encoding="utf-8")

        agent = VersionAgent(memory=memory, project_root=str(tmp_path))

        with patch.object(memory, "release_lock") as mock_release:
            result = agent.execute()
            assert result.success is False
            mock_release.assert_called_once_with("version")

    def test_execute_when_kts_gradle_increments_correctly(
        self, memory: PipelineMemory, tmp_path: Path, gradle_kts_file: Path
    ) -> None:
        """Should handle .gradle.kts files correctly during execution."""
        agent = VersionAgent(memory=memory, project_root=str(tmp_path))
        result = agent.execute()

        assert result.success is True
        assert result.data["new_version_code"] == 43
        assert result.data["new_version_name"] == "1.2.4"

    def test_execute_when_version_code_override_uses_override(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """VERSION_CODE_OVERRIDE should set exact next versionCode."""
        with patch.dict(os.environ, {"VERSION_CODE_OVERRIDE": "100"}):
            result = agent.execute()

        assert result.success is True
        assert result.data["old_version_code"] == 42
        assert result.data["new_version_code"] == 100

    def test_execute_when_version_code_override_not_greater_returns_fail(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Override must be greater than current versionCode."""
        with patch.dict(os.environ, {"VERSION_CODE_OVERRIDE": "42"}):
            result = agent.execute()

        assert result.success is False
        assert "must be greater than current versionCode" in (result.error or "")

    def test_execute_when_version_code_override_invalid_returns_fail(
        self, agent: VersionAgent, gradle_file: Path
    ) -> None:
        """Non-numeric override should fail fast."""
        with patch.dict(os.environ, {"VERSION_CODE_OVERRIDE": "abc"}):
            result = agent.execute()

        assert result.success is False
        assert "positive integer" in (result.error or "")
