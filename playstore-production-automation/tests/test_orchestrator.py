"""
Tests for the Orchestrator.

Covers initialization, status display, sync points, resume logic,
and the overall pipeline coordination.
"""

from __future__ import annotations

from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.orchestrator import Orchestrator
from agents.memory import PipelineMemory, PipelineStatus, AgentStatus


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory in a temp directory."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    return memory


@pytest.fixture
def orchestrator(tmp_pipeline):
    """Return an Orchestrator wired to a temp memory."""
    return Orchestrator(memory=tmp_pipeline)


# ---------------------------------------------------------------------------
# TestOrchestratorInit
# ---------------------------------------------------------------------------

class TestOrchestratorInit:
    """Tests for init()."""

    def test_init_when_called_creates_state_with_correct_commit(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Init should create a pipeline state with the given commit."""
        orchestrator.init(commit_sha="abc123", branch="release")
        run = tmp_pipeline.load()
        assert run is not None
        assert run.commit_sha == "abc123"
        assert run.branch == "release"
        assert run.status == PipelineStatus.IN_PROGRESS

    def test_init_when_state_exists_archives_old_run(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Init with existing state should archive the old run."""
        orchestrator.init(commit_sha="first", branch="release")
        orchestrator.init(commit_sha="second", branch="release")

        run = tmp_pipeline.load()
        assert run.commit_sha == "second"

        # Check history contains the old run
        history = tmp_pipeline.get_history()
        assert len(history) >= 1


# ---------------------------------------------------------------------------
# TestOrchestratorStatus
# ---------------------------------------------------------------------------

class TestOrchestratorStatus:
    """Tests for status()."""

    def test_status_when_state_exists_prints_summary(
        self, orchestrator: Orchestrator, capsys
    ):
        """Status with active state should print summary."""
        orchestrator.init(commit_sha="abc123", branch="release")
        orchestrator.status()
        captured = capsys.readouterr()
        assert "abc123" in captured.out

    def test_status_when_no_state_prints_not_started(
        self, orchestrator: Orchestrator, capsys
    ):
        """Status with no state should print NOT STARTED."""
        orchestrator.status()
        captured = capsys.readouterr()
        assert "NOT STARTED" in captured.out


# ---------------------------------------------------------------------------
# TestOrchestratorSyncPoint
# ---------------------------------------------------------------------------

class TestOrchestratorSyncPoint:
    """Tests for _sync_point()."""

    def test_sync_point_when_all_agents_done_returns_true(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Sync point passes when all agents in phase are DONE."""
        orchestrator.init(commit_sha="abc", branch="release")
        run = tmp_pipeline.load()

        # Mark all P1 agents as done
        for name in ["validator", "version", "secrets"]:
            agent = run.get_agent(name)
            agent.mark_done(output={})
            run.update_agent(agent)
        tmp_pipeline.save(run)

        # Should not raise
        orchestrator._sync_point("P1")

    def test_sync_point_when_one_agent_failed_returns_false(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Sync point should fail when an agent has failed."""
        orchestrator.init(commit_sha="abc", branch="release")
        run = tmp_pipeline.load()

        # Mark some done, one failed
        for name in ["validator", "version"]:
            agent = run.get_agent(name)
            agent.mark_done(output={})
            run.update_agent(agent)

        secrets = run.get_agent("secrets")
        secrets.mark_failed("Missing env var")
        run.update_agent(secrets)
        tmp_pipeline.save(run)

        with pytest.raises(RuntimeError, match="sync point failed"):
            orchestrator._sync_point("P1")

    def test_sync_point_when_agent_still_running_raises(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Sync point should fail when an agent is still running."""
        orchestrator.init(commit_sha="abc", branch="release")
        run = tmp_pipeline.load()

        # Mark some done, one still running
        for name in ["validator", "version"]:
            agent = run.get_agent(name)
            agent.mark_done(output={})
            run.update_agent(agent)

        secrets = run.get_agent("secrets")
        secrets.mark_running()
        run.update_agent(secrets)
        tmp_pipeline.save(run)

        with pytest.raises(RuntimeError):
            orchestrator._sync_point("P1")


# ---------------------------------------------------------------------------
# TestOrchestratorResume
# ---------------------------------------------------------------------------

class TestOrchestratorResume:
    """Tests for resume()."""

    def test_resume_when_phase_given_resets_agents_from_that_phase(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Resume should reset agents from the given phase onward."""
        orchestrator.init(commit_sha="abc", branch="release")
        run = tmp_pipeline.load()

        # Mark all agents up to P2 as done
        for name in ["validator", "version", "secrets", "build"]:
            agent = run.get_agent(name)
            agent.mark_done(output={})
            run.update_agent(agent)

        # Mark P3 agent as failed
        qa = run.get_agent("qa")
        qa.mark_failed("Test failure")
        run.update_agent(qa)
        tmp_pipeline.save(run)

        # Resume from P3 — all agents should be mocked to avoid real execution
        with patch.object(orchestrator, "run", return_value=True):
            orchestrator.resume("P3")

        # Verify P1/P2 agents preserved, P3+ reset
        run = tmp_pipeline.load()
        assert run.get_agent("validator").is_done()
        assert run.get_agent("build").is_done()
        assert run.get_agent("qa").status == AgentStatus.PENDING

    def test_resume_when_invalid_phase_raises_value_error(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Invalid phase should raise ValueError."""
        orchestrator.init(commit_sha="abc", branch="release")

        with pytest.raises(ValueError, match="Invalid phase"):
            orchestrator.resume("P99")


# ---------------------------------------------------------------------------
# TestOrchestratorParallelExecution
# ---------------------------------------------------------------------------

class TestOrchestratorParallelExecution:
    """Tests for _run_phase_parallel()."""

    def test_parallel_execution_with_all_passing_agents(
        self, orchestrator: Orchestrator
    ):
        """All agents passing should return True."""
        mock_agent1 = MagicMock()
        mock_agent1.name = "agent1"
        mock_agent1.run.return_value = MagicMock(success=True)

        mock_agent2 = MagicMock()
        mock_agent2.name = "agent2"
        mock_agent2.run.return_value = MagicMock(success=True)

        result = orchestrator._run_phase_parallel([mock_agent1, mock_agent2])
        assert result is True

    def test_parallel_execution_with_one_failing_agent(
        self, orchestrator: Orchestrator
    ):
        """One agent failing should return False."""
        mock_agent1 = MagicMock()
        mock_agent1.name = "pass_agent"
        mock_agent1.run.return_value = MagicMock(success=True)

        mock_agent2 = MagicMock()
        mock_agent2.name = "fail_agent"
        mock_agent2.run.return_value = MagicMock(success=False, error="Boom")

        result = orchestrator._run_phase_parallel([mock_agent1, mock_agent2])
        assert result is False
