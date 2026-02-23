"""
Comprehensive tests for agents/memory.py.

Tests cover PipelineMemory initialization, load/save with atomic writes,
agent state transitions, phase completion checks, phase-based resumption,
and human-readable pipeline summaries.
"""

import json
from pathlib import Path

import pytest

from agents.memory import (
    ALL_AGENT_NAMES,
    PHASE_AGENTS,
    AgentState,
    AgentStatus,
    PipelineMemory,
    PipelineRun,
    PipelineStatus,
)


# ---------------------------------------------------------------------------
# TestPipelineMemoryInit
# ---------------------------------------------------------------------------

class TestPipelineMemoryInit:
    """Tests for PipelineMemory constructor and init_run()."""

    def test_init_when_called_creates_required_directories(
        self, tmp_path: Path
    ) -> None:
        """PipelineMemory constructor must create state, history, locks, artifacts dirs."""
        state_dir = tmp_path / ".pipeline"
        PipelineMemory(state_dir=str(state_dir))

        assert state_dir.exists()
        assert (state_dir / "history").exists()
        assert (state_dir / "locks").exists()
        assert (state_dir / "artifacts").exists()

    def test_init_run_when_called_creates_state_file(
        self, tmp_path: Path
    ) -> None:
        """init_run() must create a valid state.json file."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc123", "release")

        state_file = state_dir / "state.json"
        assert state_file.exists()

        data = json.loads(state_file.read_text(encoding="utf-8"))
        assert data["commit_sha"] == "abc123"
        assert data["branch"] == "release"

    def test_init_run_when_called_sets_all_agents_to_pending(
        self, tmp_path: Path
    ) -> None:
        """init_run() must create all 9 agents with PENDING status."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc123", "release")

        assert len(run.agents) == len(ALL_AGENT_NAMES)
        for name in ALL_AGENT_NAMES:
            assert name in run.agents
            assert run.agents[name].status == AgentStatus.PENDING


# ---------------------------------------------------------------------------
# TestPipelineMemoryLoadSave
# ---------------------------------------------------------------------------

class TestPipelineMemoryLoadSave:
    """Tests for load() and save() including atomicity and error handling."""

    def test_load_when_no_state_file_returns_none(
        self, tmp_path: Path
    ) -> None:
        """load() must return None when no state.json exists."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))

        assert memory.load() is None

    def test_load_after_save_returns_same_data(
        self, tmp_path: Path
    ) -> None:
        """load() must return a PipelineRun matching what was saved."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        original = memory.init_run("def456", "main", triggered_by="ci")

        loaded = memory.load()
        assert loaded is not None
        assert loaded.run_id == original.run_id
        assert loaded.commit_sha == "def456"
        assert loaded.branch == "main"
        assert loaded.triggered_by == "ci"
        assert len(loaded.agents) == len(ALL_AGENT_NAMES)

    def test_save_is_atomic_when_file_exists_overwrites_cleanly(
        self, tmp_path: Path
    ) -> None:
        """save() must atomically overwrite existing state.json via tmp+rename."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("first", "release")

        # Modify and save again
        run.ai_notes = "updated notes"
        memory.save(run)

        loaded = memory.load()
        assert loaded is not None
        assert loaded.ai_notes == "updated notes"

        # Verify no leftover tmp file
        tmp_file = state_dir / "state.json.tmp"
        assert not tmp_file.exists()

    def test_load_when_state_file_corrupted_returns_none(
        self, tmp_path: Path
    ) -> None:
        """load() must return None when state.json contains invalid JSON."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))

        # Write corrupted data
        state_file = state_dir / "state.json"
        state_file.write_text("{ this is not valid json !!!", encoding="utf-8")

        assert memory.load() is None


# ---------------------------------------------------------------------------
# TestAgentStateTransitions
# ---------------------------------------------------------------------------

class TestAgentStateTransitions:
    """Tests for agent state transitions: running, done, failed, retry."""

    def test_mark_agent_running_when_pending_changes_status(
        self, tmp_path: Path
    ) -> None:
        """mark_agent_running() must change agent status from PENDING to RUNNING."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        run = memory.mark_agent_running("validator")
        assert run.get_agent("validator").status == AgentStatus.RUNNING
        assert run.get_agent("validator").started_at is not None

    def test_mark_agent_done_when_running_changes_status(
        self, tmp_path: Path
    ) -> None:
        """mark_agent_done() must change agent status to DONE with output."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")
        memory.mark_agent_running("build")

        output = {"aab_size": 12.5}
        run = memory.mark_agent_done("build", output)

        agent = run.get_agent("build")
        assert agent.status == AgentStatus.DONE
        assert agent.output == {"aab_size": 12.5}
        assert agent.completed_at is not None

    def test_mark_agent_failed_when_running_changes_status(
        self, tmp_path: Path
    ) -> None:
        """mark_agent_failed() must change agent status to FAILED with error."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")
        memory.mark_agent_running("qa")

        run = memory.mark_agent_failed("qa", "Tests failed: 3 failures")

        agent = run.get_agent("qa")
        assert agent.status == AgentStatus.FAILED
        assert agent.error == "Tests failed: 3 failures"
        assert agent.retry_count == 1

    def test_mark_agent_failed_when_failed_increments_retry_count(
        self, tmp_path: Path
    ) -> None:
        """Each call to mark_agent_failed() must increment retry_count."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        memory.mark_agent_failed("build", "Error 1")
        run = memory.mark_agent_failed("build", "Error 2")

        assert run.get_agent("build").retry_count == 2

    def test_agent_can_retry_when_failed_and_under_max(
        self, tmp_path: Path
    ) -> None:
        """can_retry() must return True when retry_count < max_retries."""
        agent = AgentState(name="test", retry_count=1)
        assert agent.can_retry(max_retries=3) is True

    def test_agent_cannot_retry_when_failed_and_at_max(
        self, tmp_path: Path
    ) -> None:
        """can_retry() must return False when retry_count >= max_retries."""
        agent = AgentState(name="test", retry_count=3)
        assert agent.can_retry(max_retries=3) is False


# ---------------------------------------------------------------------------
# TestPhaseCompletion
# ---------------------------------------------------------------------------

class TestPhaseCompletion:
    """Tests for PipelineRun.phase_is_complete()."""

    def test_phase_is_complete_when_all_phase_agents_done_returns_true(
        self, tmp_path: Path
    ) -> None:
        """phase_is_complete() returns True when every agent in the phase is DONE."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")

        # Mark all P1 agents as done
        for name in PHASE_AGENTS["P1"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name, {})

        run = memory.load()
        assert run is not None
        assert run.phase_is_complete("P1") is True

    def test_phase_is_complete_when_one_agent_not_done_returns_false(
        self, tmp_path: Path
    ) -> None:
        """phase_is_complete() returns False when any agent is still PENDING."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")

        # Mark only 2 of 3 P1 agents as done
        memory.mark_agent_running("validator")
        memory.mark_agent_done("validator", {})
        memory.mark_agent_running("version")
        memory.mark_agent_done("version", {})
        # secrets is still PENDING

        run = memory.load()
        assert run is not None
        assert run.phase_is_complete("P1") is False

    def test_phase_is_complete_when_agent_failed_returns_false(
        self, tmp_path: Path
    ) -> None:
        """phase_is_complete() returns False when any agent is FAILED."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")

        memory.mark_agent_running("validator")
        memory.mark_agent_done("validator", {})
        memory.mark_agent_running("version")
        memory.mark_agent_done("version", {})
        memory.mark_agent_running("secrets")
        memory.mark_agent_failed("secrets", "Missing keystore")

        run = memory.load()
        assert run is not None
        assert run.phase_is_complete("P1") is False


# ---------------------------------------------------------------------------
# TestResumeFromPhase
# ---------------------------------------------------------------------------

class TestResumeFromPhase:
    """Tests for PipelineMemory.resume_from_phase()."""

    def test_resume_from_phase_when_p3_resets_p3_agents(
        self, tmp_path: Path
    ) -> None:
        """resume_from_phase('P3') must reset P3/P4/P5 agents to PENDING."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")

        # Complete P1 and P2
        for name in PHASE_AGENTS["P1"] + PHASE_AGENTS["P2"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name, {})

        # Mark P3 agents as failed
        for name in PHASE_AGENTS["P3"]:
            memory.mark_agent_running(name)
            memory.mark_agent_failed(name, "some error")

        # Resume from P3
        run = memory.resume_from_phase("P3")
        assert run is not None

        # P3, P4, P5 agents should all be PENDING
        for phase in ["P3", "P4", "P5"]:
            for name in PHASE_AGENTS[phase]:
                assert run.get_agent(name).status == AgentStatus.PENDING

    def test_resume_from_phase_when_resumed_preserves_earlier_phases(
        self, tmp_path: Path
    ) -> None:
        """resume_from_phase('P3') must NOT reset P1/P2 agents."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        # Complete P1 and P2
        for name in PHASE_AGENTS["P1"] + PHASE_AGENTS["P2"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name, {})

        run = memory.resume_from_phase("P3")
        assert run is not None

        # P1 and P2 agents must still be DONE
        for phase in ["P1", "P2"]:
            for name in PHASE_AGENTS[phase]:
                assert run.get_agent(name).status == AgentStatus.DONE

    def test_resume_from_phase_when_no_state_returns_none(
        self, tmp_path: Path
    ) -> None:
        """resume_from_phase() must return None when no state.json exists."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))

        result = memory.resume_from_phase("P1")
        assert result is None


# ---------------------------------------------------------------------------
# TestRunSummary
# ---------------------------------------------------------------------------

class TestRunSummary:
    """Tests for PipelineRun.summary()."""

    def test_summary_when_pipeline_running_contains_status(
        self, tmp_path: Path
    ) -> None:
        """summary() must include the pipeline status string."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")

        text = run.summary()
        assert "IN_PROGRESS" in text
        assert "release" in text

    def test_summary_when_agent_failed_shows_error(
        self, tmp_path: Path
    ) -> None:
        """summary() must show the error message when an agent has failed."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        memory.mark_agent_running("build")
        run = memory.mark_agent_failed("build", "Gradle OOM")

        text = run.summary()
        assert "Gradle OOM" in text
        assert "FAILED" in text

    def test_summary_when_ai_notes_present_includes_them(
        self, tmp_path: Path
    ) -> None:
        """summary() must include the ai_notes field when set."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        run = memory.init_run("abc", "release")
        run.ai_notes = "Consider upgrading Gradle wrapper"
        memory.save(run)

        loaded = memory.load()
        assert loaded is not None
        text = loaded.summary()
        assert "Consider upgrading Gradle wrapper" in text
