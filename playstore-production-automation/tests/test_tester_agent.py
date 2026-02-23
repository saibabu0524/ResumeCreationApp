"""
Tests for the Tester Agent.

Covers YAML config loading, track config lookup, promotion rules,
and the full execute() lifecycle.
"""

from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.tester_agent import TesterAgent
from agents.memory import PipelineMemory, AgentStatus


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

TESTER_CONFIG_YAML = """\
groups:
  internal:
    track: "internal"
    emails:
      - dev@company.com
      - qa@company.com
    auto_promote_to: null
  alpha:
    track: "alpha"
    emails:
      - beta@company.com
    auto_promote_to: "production"
"""


@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory with a post-upload state."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    run = memory.init_run(commit_sha="abc123", branch="release")
    run.package_name = "com.example.testapp"

    # Mark QA as done
    qa_state = run.get_agent("qa")
    qa_state.mark_done(output={"passed": 10})
    run.update_agent(qa_state)
    memory.save(run)

    # Write metadata.json
    metadata = {"track": "internal"}
    meta_path = memory.artifacts_dir / "metadata.json"
    meta_path.write_text(json.dumps(metadata), encoding="utf-8")

    return memory


@pytest.fixture
def config_file(tmp_path: Path):
    """Write a tester config YAML file."""
    config_path = tmp_path / "tester-groups.yaml"
    config_path.write_text(TESTER_CONFIG_YAML, encoding="utf-8")
    return config_path


@pytest.fixture
def agent(tmp_pipeline, config_file):
    """Return a TesterAgent wired to temp dirs."""
    return TesterAgent(
        memory=tmp_pipeline,
        config_path=str(config_file),
        dry_run=True,  # Don't call real APIs in tests
    )


# ---------------------------------------------------------------------------
# TestTesterAgentConfig
# ---------------------------------------------------------------------------

class TestTesterAgentConfig:
    """Tests for config loading."""

    def test_load_config_when_file_exists_returns_dict(
        self, agent: TesterAgent
    ):
        """YAML config should be parsed correctly."""
        config = agent._load_tester_config()
        assert "groups" in config
        assert "internal" in config["groups"]

    def test_load_config_when_file_missing_returns_empty_config(
        self, tmp_pipeline: PipelineMemory
    ):
        """Missing config file should return empty groups."""
        a = TesterAgent(
            memory=tmp_pipeline,
            config_path="/nonexistent/path.yaml",
        )
        config = a._load_tester_config()
        assert config == {"groups": {}}

    def test_get_track_config_when_track_exists_returns_config(
        self, agent: TesterAgent
    ):
        """Should return config for a known track."""
        config = agent._get_track_config("internal")
        assert config is not None
        assert "dev@company.com" in config["emails"]

    def test_get_track_config_when_track_missing_returns_none(
        self, agent: TesterAgent
    ):
        """Should return None for an unknown track."""
        config = agent._get_track_config("production")
        assert config is None


# ---------------------------------------------------------------------------
# TestTesterAgentPromotion
# ---------------------------------------------------------------------------

class TestTesterAgentPromotion:
    """Tests for _should_promote()."""

    def test_should_promote_when_qa_passed_and_rule_exists_returns_true(
        self, agent: TesterAgent, tmp_pipeline: PipelineMemory
    ):
        """Should promote when QA passed and auto_promote_to is set."""
        run = tmp_pipeline.load()
        config = {"auto_promote_to": "production"}
        assert agent._should_promote(run, config) is True

    def test_should_promote_when_qa_failed_returns_false(
        self, agent: TesterAgent, tmp_pipeline: PipelineMemory
    ):
        """Should not promote when QA didn't pass."""
        run = tmp_pipeline.load()
        # Reset QA to failed
        qa = run.get_agent("qa")
        qa.status = AgentStatus.FAILED
        run.update_agent(qa)
        tmp_pipeline.save(run)

        run = tmp_pipeline.load()
        config = {"auto_promote_to": "production"}
        assert agent._should_promote(run, config) is False

    def test_should_promote_when_no_rule_returns_false(
        self, agent: TesterAgent, tmp_pipeline: PipelineMemory
    ):
        """Should not promote when auto_promote_to is null."""
        run = tmp_pipeline.load()
        config = {"auto_promote_to": None}
        assert agent._should_promote(run, config) is False

    def test_should_promote_when_manual_required_returns_false(
        self, agent: TesterAgent, tmp_pipeline: PipelineMemory
    ):
        """Should not promote when there's no auto_promote_to key."""
        run = tmp_pipeline.load()
        config = {}
        assert agent._should_promote(run, config) is False


# ---------------------------------------------------------------------------
# TestTesterAgentExecute
# ---------------------------------------------------------------------------

class TestTesterAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_testers_assigned_returns_ok(
        self, agent: TesterAgent
    ):
        """Successful dry-run execution should return ok."""
        result = agent.execute()
        assert result.success is True
        assert result.data["testers_assigned"] == 2  # dev + qa
        assert result.data["track"] == "internal"

    def test_execute_when_no_config_for_track_skips_gracefully(
        self, tmp_pipeline: PipelineMemory, tmp_path: Path
    ):
        """Unknown track should skip tester assignment gracefully."""
        # Change metadata to an unknown track
        metadata = {"track": "unknown_track"}
        meta_path = tmp_pipeline.artifacts_dir / "metadata.json"
        meta_path.write_text(json.dumps(metadata), encoding="utf-8")

        config_path = tmp_path / "config.yaml"
        config_path.write_text(TESTER_CONFIG_YAML, encoding="utf-8")

        a = TesterAgent(
            memory=tmp_pipeline,
            config_path=str(config_path),
            dry_run=True,
        )
        result = a.execute()
        assert result.success is True
        assert result.data["testers_assigned"] == 0

    def test_execute_when_api_fails_returns_fail(
        self, tmp_pipeline: PipelineMemory, tmp_path: Path
    ):
        """API failure should return fail."""
        config_path = tmp_path / "config.yaml"
        config_path.write_text(TESTER_CONFIG_YAML, encoding="utf-8")

        a = TesterAgent(
            memory=tmp_pipeline,
            config_path=str(config_path),
            dry_run=False,  # Real mode
        )

        with patch.object(a, "_assign_testers", return_value=False):
            result = a.execute()

        assert result.success is False
        assert "Failed to assign testers" in result.error
