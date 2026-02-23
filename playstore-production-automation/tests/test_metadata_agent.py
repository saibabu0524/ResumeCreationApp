"""
Tests for the Metadata Agent.

Covers CHANGELOG parsing, text truncation, release notes priority chain,
and the full execute() lifecycle.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.metadata_agent import MetadataAgent
from agents.memory import PipelineMemory


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory in a temp directory."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    run = memory.init_run(commit_sha="abc123", branch="release")
    # Set version info on the run
    run.version_code = 42
    run.version_name = "2.1.0"
    run.package_name = "com.example.testapp"
    memory.save(run)
    return memory


@pytest.fixture
def fake_project(tmp_path: Path):
    """Create a minimal fake project structure."""
    project = tmp_path / "project"
    project.mkdir()
    return project


@pytest.fixture
def agent(tmp_pipeline, fake_project):
    """Return a MetadataAgent wired to temp dirs."""
    return MetadataAgent(
        memory=tmp_pipeline,
        project_root=str(fake_project),
    )


# ---------------------------------------------------------------------------
# TestParseChangelog
# ---------------------------------------------------------------------------

CHANGELOG_STANDARD = """\
# Changelog

## 2.1.0

- Fixed crash on login screen
- Improved performance on older devices
- Added dark mode support

## 2.0.0

- Complete UI redesign
- New onboarding flow
"""

CHANGELOG_SINGLE_SECTION = """\
## 1.0.0

- Initial release
- Basic functionality
"""


class TestParseChangelog:
    """Tests for _parse_changelog()."""

    def test_parse_changelog_when_standard_format_returns_latest_section(
        self, agent: MetadataAgent
    ):
        """Should extract only the latest version section."""
        result = agent._parse_changelog(CHANGELOG_STANDARD)
        assert "Fixed crash on login screen" in result
        assert "dark mode support" in result
        # Should NOT contain content from older version
        assert "Complete UI redesign" not in result

    def test_parse_changelog_when_empty_file_returns_empty_string(
        self, agent: MetadataAgent
    ):
        """Empty content should return empty string."""
        assert agent._parse_changelog("") == ""
        assert agent._parse_changelog("   ") == ""

    def test_parse_changelog_when_no_sections_returns_empty_string(
        self, agent: MetadataAgent
    ):
        """Content without ## headings should return empty string."""
        result = agent._parse_changelog("Just some text without headings.\nMore text.")
        assert result == ""

    def test_parse_changelog_when_single_section_returns_all_content(
        self, agent: MetadataAgent
    ):
        """Single section should return all its content."""
        result = agent._parse_changelog(CHANGELOG_SINGLE_SECTION)
        assert "Initial release" in result
        assert "Basic functionality" in result


# ---------------------------------------------------------------------------
# TestTruncate
# ---------------------------------------------------------------------------

class TestTruncate:
    """Tests for _truncate()."""

    def test_truncate_when_under_limit_returns_unchanged(
        self, agent: MetadataAgent
    ):
        """Short text should be returned as-is."""
        text = "Short text"
        assert agent._truncate(text) == text

    def test_truncate_when_over_limit_returns_within_limit(
        self, agent: MetadataAgent
    ):
        """Long text should be truncated to within the limit."""
        text = "word " * 200  # ~1000 chars
        result = agent._truncate(text, max_length=100)
        assert len(result) <= 100

    def test_truncate_when_over_limit_ends_with_ellipsis(
        self, agent: MetadataAgent
    ):
        """Truncated text should end with '...'."""
        text = "word " * 200
        result = agent._truncate(text, max_length=50)
        assert result.endswith("...")

    def test_truncate_when_exactly_at_limit_returns_unchanged(
        self, agent: MetadataAgent
    ):
        """Text exactly at the limit should not be modified."""
        text = "x" * 500
        assert agent._truncate(text) == text


# ---------------------------------------------------------------------------
# TestGetReleaseNotes
# ---------------------------------------------------------------------------

class TestGetReleaseNotes:
    """Tests for _get_release_notes()."""

    def test_get_release_notes_when_changelog_exists_uses_changelog(
        self, agent: MetadataAgent, fake_project: Path, tmp_pipeline: PipelineMemory
    ):
        """CHANGELOG.md should be the highest priority source."""
        changelog = fake_project / "CHANGELOG.md"
        changelog.write_text(CHANGELOG_STANDARD, encoding="utf-8")

        run = tmp_pipeline.load()
        result = agent._get_release_notes(run)

        assert "Fixed crash on login screen" in result

    def test_get_release_notes_when_no_changelog_and_no_api_key_uses_default(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """No CHANGELOG and no API key should fall back to default text."""
        run = tmp_pipeline.load()

        with patch.dict(os.environ, {}, clear=True):
            with patch.object(agent, "_get_recent_commits", return_value=[]):
                result = agent._get_release_notes(run)

        assert result == MetadataAgent.DEFAULT_RELEASE_NOTES

    def test_get_release_notes_when_claude_api_fails_uses_default(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """Claude API failure should fall back to default."""
        run = tmp_pipeline.load()

        with patch.object(agent, "_get_recent_commits", return_value=["fix: login"]):
            with patch.object(agent, "_generate_with_claude", return_value=""):
                result = agent._get_release_notes(run)

        assert result == MetadataAgent.DEFAULT_RELEASE_NOTES

    def test_get_release_notes_when_claude_api_succeeds_uses_ai_notes(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """Claude-generated notes should be used when available."""
        run = tmp_pipeline.load()
        ai_notes = "• Improved login flow\n• Fixed crash on older devices"

        with patch.object(agent, "_get_recent_commits", return_value=["fix: login"]):
            with patch.object(agent, "_generate_with_claude", return_value=ai_notes):
                result = agent._get_release_notes(run)

        assert "Improved login flow" in result


# ---------------------------------------------------------------------------
# TestMetadataAgentExecute
# ---------------------------------------------------------------------------

class TestMetadataAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_successful_writes_metadata_json(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """Successful execution should write metadata.json."""
        with patch.object(agent, "_get_release_notes", return_value="Test notes"):
            result = agent.execute()

        assert result.success is True

        metadata_path = tmp_pipeline.artifacts_dir / "metadata.json"
        assert metadata_path.exists()

        data = json.loads(metadata_path.read_text())
        assert data["package_name"] == "com.example.testapp"
        assert data["version_code"] == 42
        assert data["version_name"] == "2.1.0"
        assert data["track"] == "internal"
        assert data["release_notes"]["en-US"] == "Test notes"

    def test_execute_when_play_track_env_set_uses_custom_track(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """PLAY_TRACK should override default internal track."""
        with patch.dict(os.environ, {"PLAY_TRACK": "closed-testers"}):
            with patch.object(agent, "_get_release_notes", return_value="Test notes"):
                result = agent.execute()

        assert result.success is True
        assert result.data["track"] == "closed-testers"

    def test_execute_when_output_dir_missing_creates_it(
        self, tmp_path: Path
    ):
        """Should create the artifacts directory if it doesn't exist."""
        state_dir = tmp_path / ".pipe_new"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run(commit_sha="def456", branch="release")
        run.version_code = 1
        run.version_name = "1.0.0"
        memory.save(run)

        new_agent = MetadataAgent(memory=memory, project_root=str(tmp_path))

        with patch.object(new_agent, "_get_release_notes", return_value="Notes"):
            result = new_agent.execute()

        assert result.success is True
        assert (memory.artifacts_dir / "metadata.json").exists()

    def test_execute_when_metadata_has_correct_structure(
        self, agent: MetadataAgent, tmp_pipeline: PipelineMemory
    ):
        """Metadata should contain all required fields."""
        with patch.object(
            agent, "_get_release_notes", return_value="Release notes"
        ):
            result = agent.execute()

        assert result.success is True
        required_keys = {
            "package_name", "version_code", "version_name",
            "track", "release_notes",
        }
        assert required_keys.issubset(result.data.keys())
        assert "en-US" in result.data["release_notes"]

    def test_execute_when_no_state_returns_fail(self, tmp_path: Path):
        """Should fail gracefully if no pipeline state exists."""
        state_dir = tmp_path / ".pipe_empty"
        memory = PipelineMemory(state_dir=str(state_dir))
        # Do NOT init_run — no state file

        empty_agent = MetadataAgent(memory=memory, project_root=str(tmp_path))
        result = empty_agent.execute()

        assert result.success is False
        assert "No pipeline state" in result.error


# ---------------------------------------------------------------------------
# TestGenerateWithClaude
# ---------------------------------------------------------------------------

class TestGenerateWithClaude:
    """Tests for _generate_with_claude()."""

    def test_generate_with_claude_when_no_api_key_returns_empty(
        self, agent: MetadataAgent
    ):
        """No API key should return empty string without error."""
        with patch.dict(os.environ, {}, clear=True):
            result = agent._generate_with_claude(["fix: something"])
        assert result == ""

    def test_generate_with_claude_when_import_fails_returns_empty(
        self, agent: MetadataAgent
    ):
        """Missing anthropic package should return empty string."""
        with patch.dict(os.environ, {"ANTHROPIC_API_KEY": "test-key"}):
            with patch.dict("sys.modules", {"anthropic": None}):
                result = agent._generate_with_claude(["fix: something"])
        assert result == ""
