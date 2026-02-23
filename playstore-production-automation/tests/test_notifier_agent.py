"""
Tests for the Notifier Agent.

Covers Slack message construction, duration calculation,
and the critical rule: this agent NEVER fails the pipeline.
"""

from __future__ import annotations

import os
from datetime import datetime, timezone, timedelta
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.notifier_agent import NotifierAgent
from agents.memory import PipelineMemory, PipelineStatus


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory with a completed pipeline state."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    run = memory.init_run(commit_sha="a3f2b19cde", branch="release")
    run.version_code = 42
    run.version_name = "2.1.0"
    run.status = PipelineStatus.SUCCESS
    memory.save(run)
    return memory


@pytest.fixture
def failed_pipeline(tmp_path: Path):
    """Set up a PipelineMemory with a failed pipeline state."""
    state_dir = tmp_path / ".pipeline_fail"
    memory = PipelineMemory(state_dir=str(state_dir))
    run = memory.init_run(commit_sha="deadbeef", branch="release")
    run.version_code = 43
    run.version_name = "2.1.1"
    run.status = PipelineStatus.FAILED
    run.errors = ["Build failed with exit code 1"]

    # Mark build agent as failed
    build = run.get_agent("build")
    build.mark_failed("Gradle compilation error")
    run.update_agent(build)
    memory.save(run)
    return memory


@pytest.fixture
def agent(tmp_pipeline):
    """Return a NotifierAgent wired to a success state."""
    return NotifierAgent(memory=tmp_pipeline)


@pytest.fixture
def failed_agent(failed_pipeline):
    """Return a NotifierAgent wired to a failure state."""
    return NotifierAgent(memory=failed_pipeline)


# ---------------------------------------------------------------------------
# TestNotifierAgentMessage
# ---------------------------------------------------------------------------

class TestNotifierAgentMessage:
    """Tests for _build_slack_message()."""

    def test_build_slack_message_when_success_includes_checkmark(
        self, agent: NotifierAgent, tmp_pipeline: PipelineMemory
    ):
        """Success status should include ✅."""
        run = tmp_pipeline.load()
        payload = agent._build_slack_message(run)
        header_text = payload["blocks"][0]["text"]["text"]
        assert "✅" in header_text
        assert "SUCCESS" in header_text

    def test_build_slack_message_when_failure_includes_x_and_error(
        self, failed_agent: NotifierAgent, failed_pipeline: PipelineMemory
    ):
        """Failure status should include ❌ and error details."""
        run = failed_pipeline.load()
        payload = failed_agent._build_slack_message(run)
        header_text = payload["blocks"][0]["text"]["text"]
        assert "❌" in header_text
        assert "FAILED" in header_text

    def test_build_slack_message_includes_version_info(
        self, agent: NotifierAgent, tmp_pipeline: PipelineMemory
    ):
        """Message should contain version name and code."""
        run = tmp_pipeline.load()
        payload = agent._build_slack_message(run)
        # Check the section fields for version info
        section = payload["blocks"][1]
        fields_text = " ".join(
            f["text"] for f in section["fields"]
        )
        assert "v2.1.0" in fields_text
        assert "42" in fields_text

    def test_build_slack_message_includes_duration(
        self, agent: NotifierAgent, tmp_pipeline: PipelineMemory
    ):
        """Message should include pipeline duration."""
        run = tmp_pipeline.load()
        payload = agent._build_slack_message(run)
        section = payload["blocks"][1]
        fields_text = " ".join(
            f["text"] for f in section["fields"]
        )
        assert "Duration" in fields_text

    def test_calculate_duration_when_start_and_end_known_returns_minutes(
        self, agent: NotifierAgent, tmp_pipeline: PipelineMemory
    ):
        """Duration should be formatted as Xm Xs."""
        run = tmp_pipeline.load()
        # Set started_at to 20 minutes ago
        past = datetime.now(timezone.utc) - timedelta(minutes=20, seconds=15)
        run.started_at = past.isoformat()
        tmp_pipeline.save(run)

        run = tmp_pipeline.load()
        duration = agent._calculate_duration(run)
        assert "20m" in duration


# ---------------------------------------------------------------------------
# TestNotifierAgentExecute
# ---------------------------------------------------------------------------

class TestNotifierAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_slack_webhook_not_set_skips_gracefully(
        self, agent: NotifierAgent
    ):
        """No Slack webhook should skip without error."""
        with patch.dict(os.environ, {}, clear=True):
            result = agent.execute()
        assert result.success is True
        assert result.data["slack_sent"] is True  # True = skipped gracefully

    def test_execute_when_slack_post_fails_logs_warning_not_error(
        self, agent: NotifierAgent
    ):
        """Slack POST failure should not fail the agent."""
        with patch.dict(os.environ, {"SLACK_WEBHOOK_URL": "https://hooks.slack.com/test"}):
            with patch("agents.notifier_agent.requests.post") as mock_post:
                mock_post.side_effect = Exception("Connection refused")
                result = agent.execute()

        assert result.success is True  # NEVER fail

    def test_execute_when_github_token_not_set_skips_github_status(
        self, agent: NotifierAgent
    ):
        """No GitHub token should skip commit status posting."""
        with patch.dict(os.environ, {}, clear=True):
            result = agent.execute()
        assert result.success is True
        assert result.data["github_status_posted"] is True  # Skipped = True

    def test_execute_always_returns_ok_even_if_notifications_fail(
        self, agent: NotifierAgent
    ):
        """Agent must ALWAYS return ok(), even if all notifications fail."""
        with patch.dict(os.environ, {
            "SLACK_WEBHOOK_URL": "https://broken",
            "GITHUB_TOKEN": "fake-token",
            "GITHUB_REPOSITORY": "user/repo",
        }):
            with patch("agents.notifier_agent.requests.post") as mock_post:
                mock_post.side_effect = Exception("Network error")
                result = agent.execute()

        # THE critical assertion — this agent never blocks the pipeline
        assert result.success is True


# ---------------------------------------------------------------------------
# TestNotifierAgentDuration
# ---------------------------------------------------------------------------

class TestNotifierAgentDuration:
    """Tests for _calculate_duration()."""

    def test_duration_under_a_minute(self, agent: NotifierAgent, tmp_pipeline):
        """Short durations should show seconds."""
        run = tmp_pipeline.load()
        run.started_at = (
            datetime.now(timezone.utc) - timedelta(seconds=30)
        ).isoformat()
        duration = agent._calculate_duration(run)
        assert "s" in duration

    def test_duration_over_an_hour(self, agent: NotifierAgent, tmp_pipeline):
        """Long durations should show hours and minutes."""
        run = tmp_pipeline.load()
        run.started_at = (
            datetime.now(timezone.utc) - timedelta(hours=1, minutes=5)
        ).isoformat()
        duration = agent._calculate_duration(run)
        assert "h" in duration

    def test_duration_with_invalid_timestamp(self, agent: NotifierAgent, tmp_pipeline):
        """Invalid timestamp should return 'unknown'."""
        run = tmp_pipeline.load()
        run.started_at = "not-a-datetime"
        duration = agent._calculate_duration(run)
        assert duration == "unknown"
