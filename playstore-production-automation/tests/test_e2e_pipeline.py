"""
End-to-end integration tests for the Android CI/CD Pipeline.

These tests verify that the entire pipeline runs correctly in dry-run
mode, testing agent coordination, state transitions, and the orchestrator.
They do NOT call external services — everything is mocked or dry-run.
"""

from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from agents.base_agent import AgentResult
from agents.memory import (
    PipelineMemory,
    PipelineStatus,
    AgentStatus,
    ALL_AGENT_NAMES,
    PHASE_AGENTS,
)
from agents.orchestrator import Orchestrator


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a fresh PipelineMemory."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    return memory


@pytest.fixture
def orchestrator(tmp_pipeline):
    """Return an Orchestrator wired to temp memory."""
    return Orchestrator(memory=tmp_pipeline)


# ---------------------------------------------------------------------------
# Test: Full pipeline dry-run
# ---------------------------------------------------------------------------

class TestPipelineDryRun:
    """Verify the full pipeline succeeds in dry-run mode."""

    def test_dry_run_completes_with_all_agents_done(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """
        A full dry-run should:
        1. Mark all 9 agents as DONE
        2. Mark pipeline status as SUCCESS
        """
        orchestrator.init(commit_sha="abc123", branch="release")
        success = orchestrator.run(dry_run=True)

        assert success is True

        run = tmp_pipeline.load()
        assert run is not None
        assert run.status == PipelineStatus.SUCCESS

        # Every agent should be DONE
        for name in ALL_AGENT_NAMES:
            agent_state = run.get_agent(name)
            assert agent_state.is_done(), (
                f"Agent '{name}' should be DONE but is {agent_state.status}"
            )

    def test_dry_run_agents_have_dry_run_output(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Dry-run agents should have dry_run=True in their output."""
        orchestrator.init(commit_sha="abc123", branch="release")
        orchestrator.run(dry_run=True)

        run = tmp_pipeline.load()
        for name in ALL_AGENT_NAMES:
            agent_state = run.get_agent(name)
            assert agent_state.output.get("dry_run") is True, (
                f"Agent '{name}' missing dry_run=True in output"
            )

    def test_dry_run_archives_previous_run_on_reinit(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Running init twice should archive the first run."""
        orchestrator.init(commit_sha="first", branch="main")
        orchestrator.run(dry_run=True)

        orchestrator.init(commit_sha="second", branch="release")
        orchestrator.run(dry_run=True)

        history = tmp_pipeline.get_history()
        assert len(history) >= 1

        # Current run should be the second one
        run = tmp_pipeline.load()
        assert run.commit_sha == "second"


# ---------------------------------------------------------------------------
# Test: State persistence across operations
# ---------------------------------------------------------------------------

class TestStatePersistence:
    """Verify state is correctly persisted and can be read back."""

    def test_state_file_exists_after_init(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """state.json should exist after init."""
        orchestrator.init(commit_sha="abc123", branch="release")
        state_file = tmp_pipeline.state_dir / "state.json"
        assert state_file.exists()

    def test_state_json_is_valid_json(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """state.json should be valid, parseable JSON."""
        orchestrator.init(commit_sha="abc123", branch="release")
        state_file = tmp_pipeline.state_dir / "state.json"
        data = json.loads(state_file.read_text())
        assert "run_id" in data
        assert "agents" in data
        assert len(data["agents"]) == 9

    def test_state_roundtrip_preserves_all_fields(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Save → Load should preserve all PipelineRun fields."""
        orchestrator.init(commit_sha="abc123", branch="release")

        run1 = tmp_pipeline.load()
        run1.version_code = 42
        run1.version_name = "2.1.0"
        run1.aab_path = "/path/to/app.aab"
        run1.package_name = "com.test.app"
        tmp_pipeline.save(run1)

        run2 = tmp_pipeline.load()
        assert run2.version_code == 42
        assert run2.version_name == "2.1.0"
        assert run2.aab_path == "/path/to/app.aab"
        assert run2.package_name == "com.test.app"


# ---------------------------------------------------------------------------
# Test: Phase ordering and sync points
# ---------------------------------------------------------------------------

class TestPhaseExecution:
    """Verify phases execute in correct order with sync points."""

    def test_phases_have_correct_agent_mapping(self):
        """PHASE_AGENTS should map correctly to all 9 agents."""
        all_from_phases = []
        for phase, agents in PHASE_AGENTS.items():
            all_from_phases.extend(agents)
        assert sorted(all_from_phases) == sorted(ALL_AGENT_NAMES)

    def test_phase_count_is_five(self):
        """Pipeline should have exactly 5 phases."""
        assert len(PHASE_AGENTS) == 5
        assert list(PHASE_AGENTS.keys()) == ["P1", "P2", "P3", "P4", "P5"]

    def test_p1_has_three_parallel_agents(self):
        """P1 should run validator, version, secrets in parallel."""
        assert set(PHASE_AGENTS["P1"]) == {"validator", "version", "secrets"}

    def test_p2_has_single_build_agent(self):
        """P2 should run only the build agent."""
        assert PHASE_AGENTS["P2"] == ["build"]

    def test_p3_has_two_parallel_agents(self):
        """P3 should run qa and metadata in parallel."""
        assert set(PHASE_AGENTS["P3"]) == {"qa", "metadata"}

    def test_p4_has_single_upload_agent(self):
        """P4 should run only the upload agent."""
        assert PHASE_AGENTS["P4"] == ["upload"]

    def test_p5_has_two_parallel_agents(self):
        """P5 should run tester and notifier in parallel."""
        assert set(PHASE_AGENTS["P5"]) == {"tester", "notifier"}


# ---------------------------------------------------------------------------
# Test: Resume from phase  
# ---------------------------------------------------------------------------

class TestResumeFromPhase:
    """Verify resume correctly resets agents from a given phase."""

    def test_resume_from_p3_preserves_p1_p2_resets_p3_to_p5(
        self, tmp_pipeline: PipelineMemory
    ):
        """Resume from P3 should keep P1+P2 done, reset P3-P5."""
        run = tmp_pipeline.init_run(commit_sha="abc", branch="release")

        # Mark P1 and P2 as done
        for name in ["validator", "version", "secrets", "build"]:
            agent = run.get_agent(name)
            agent.mark_done(output={"result": "ok"})
            run.update_agent(agent)

        # Mark P3 agent as failed
        qa = run.get_agent("qa")
        qa.mark_failed("test error")
        run.update_agent(qa)
        tmp_pipeline.save(run)

        # Resume from P3
        result = tmp_pipeline.resume_from_phase("P3")
        assert result is not None

        # P1+P2 should still be done
        assert result.get_agent("validator").is_done()
        assert result.get_agent("version").is_done()
        assert result.get_agent("secrets").is_done()
        assert result.get_agent("build").is_done()

        # P3, P4, P5 should be reset to PENDING
        for name in ["qa", "metadata", "upload", "tester", "notifier"]:
            assert result.get_agent(name).status == AgentStatus.PENDING, (
                f"Agent {name} should be PENDING after resume"
            )

    def test_resume_from_p1_resets_everything(
        self, tmp_pipeline: PipelineMemory
    ):
        """Resume from P1 should reset all agents."""
        run = tmp_pipeline.init_run(commit_sha="abc", branch="release")

        # Mark all as done
        for name in ALL_AGENT_NAMES:
            agent = run.get_agent(name)
            agent.mark_done(output={})
            run.update_agent(agent)
        tmp_pipeline.save(run)

        result = tmp_pipeline.resume_from_phase("P1")
        assert result is not None

        for name in ALL_AGENT_NAMES:
            assert result.get_agent(name).status == AgentStatus.PENDING


# ---------------------------------------------------------------------------
# Test: Idempotency
# ---------------------------------------------------------------------------

class TestIdempotency:
    """Verify agents are idempotent — running twice returns same result."""

    def test_running_pipeline_twice_in_dry_run_is_idempotent(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Running the pipeline a second time should be a no-op."""
        orchestrator.init(commit_sha="abc123", branch="release")

        # First run
        success1 = orchestrator.run(dry_run=True)
        assert success1 is True

        run1 = tmp_pipeline.load()
        agents_after_first = {
            name: run1.get_agent(name).status for name in ALL_AGENT_NAMES
        }

        # Second run — agents should already be DONE
        success2 = orchestrator.run(dry_run=True)
        assert success2 is True

        run2 = tmp_pipeline.load()
        for name in ALL_AGENT_NAMES:
            assert run2.get_agent(name).is_done()


# ---------------------------------------------------------------------------
# Test: Error handling at pipeline level
# ---------------------------------------------------------------------------

class TestPipelineErrorHandling:
    """Verify pipeline handles agent failures gracefully."""

    def test_pipeline_marks_failed_when_sync_point_fails(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """If a sync point fails, pipeline should be marked FAILED."""
        orchestrator.init(commit_sha="abc123", branch="release")

        # Mock P1 agents to have one fail
        with patch.object(orchestrator, "_create_agents") as mock_create:
            mock_agents = {}
            for name in ALL_AGENT_NAMES:
                mock_agent = MagicMock()
                mock_agent.name = name
                if name == "secrets":
                    mock_agent.run.return_value = AgentResult.fail("No secrets")
                else:
                    mock_agent.run.return_value = AgentResult.ok({"done": True})
                mock_agents[name] = mock_agent
            mock_create.return_value = mock_agents

            success = orchestrator.run()

        assert success is False

        run = tmp_pipeline.load()
        assert run.status == PipelineStatus.FAILED

    def test_notifier_runs_even_on_pipeline_failure(
        self, orchestrator: Orchestrator, tmp_pipeline: PipelineMemory
    ):
        """Notifier agent should run even if the pipeline fails."""
        orchestrator.init(commit_sha="abc123", branch="release")

        notifier_mock = MagicMock()
        notifier_mock.name = "notifier"
        notifier_mock.run.return_value = AgentResult.ok({})

        with patch.object(orchestrator, "_create_agents") as mock_create:
            mock_agents = {}
            for name in ALL_AGENT_NAMES:
                mock_agent = MagicMock()
                mock_agent.name = name
                if name == "validator":
                    mock_agent.run.return_value = AgentResult.fail("Invalid")
                elif name == "notifier":
                    mock_agents[name] = notifier_mock
                    continue
                else:
                    mock_agent.run.return_value = AgentResult.ok({})
                mock_agents[name] = mock_agent
            mock_create.return_value = mock_agents

            orchestrator.run()

        # Notifier should have been called
        notifier_mock.run.assert_called()


# ---------------------------------------------------------------------------
# Test: Agent import validation
# ---------------------------------------------------------------------------

class TestAgentImports:
    """Verify all agent modules can be imported successfully."""

    def test_all_agents_importable(self):
        """Every agent module should be importable without errors."""
        from agents.memory import PipelineMemory
        from agents.base_agent import BaseAgent, AgentResult
        from agents.validator_agent import ValidatorAgent
        from agents.version_agent import VersionAgent
        from agents.secrets_agent import SecretsAgent
        from agents.build_agent import BuildAgent
        from agents.qa_agent import QAAgent
        from agents.metadata_agent import MetadataAgent
        from agents.upload_agent import UploadAgent
        from agents.tester_agent import TesterAgent
        from agents.notifier_agent import NotifierAgent
        from agents.orchestrator import Orchestrator

        # All should be classes
        assert callable(PipelineMemory)
        assert callable(BaseAgent)
        assert callable(ValidatorAgent)
        assert callable(VersionAgent)
        assert callable(SecretsAgent)
        assert callable(BuildAgent)
        assert callable(QAAgent)
        assert callable(MetadataAgent)
        assert callable(UploadAgent)
        assert callable(TesterAgent)
        assert callable(NotifierAgent)
        assert callable(Orchestrator)

    def test_all_agents_extend_base_agent(self):
        """Every pipeline agent should extend BaseAgent."""
        from agents.base_agent import BaseAgent
        from agents.validator_agent import ValidatorAgent
        from agents.version_agent import VersionAgent
        from agents.secrets_agent import SecretsAgent
        from agents.build_agent import BuildAgent
        from agents.qa_agent import QAAgent
        from agents.metadata_agent import MetadataAgent
        from agents.upload_agent import UploadAgent
        from agents.tester_agent import TesterAgent
        from agents.notifier_agent import NotifierAgent

        for cls in [
            ValidatorAgent, VersionAgent, SecretsAgent,
            BuildAgent, QAAgent, MetadataAgent,
            UploadAgent, TesterAgent, NotifierAgent,
        ]:
            assert issubclass(cls, BaseAgent), (
                f"{cls.__name__} must extend BaseAgent"
            )


# ---------------------------------------------------------------------------
# Test: CLI interface
# ---------------------------------------------------------------------------

class TestCLI:
    """Verify orchestrator CLI works."""

    def test_status_without_state_shows_not_started(
        self, orchestrator: Orchestrator, capsys
    ):
        """--status with no state should print NOT STARTED."""
        orchestrator.status()
        captured = capsys.readouterr()
        assert "NOT STARTED" in captured.out

    def test_history_without_runs_shows_no_history(
        self, orchestrator: Orchestrator, capsys
    ):
        """--history with no previous runs should print message."""
        orchestrator.history()
        captured = capsys.readouterr()
        assert "No pipeline history" in captured.out

    def test_status_after_init_shows_agent_list(
        self, orchestrator: Orchestrator, capsys
    ):
        """--status after init should list all 9 agents."""
        orchestrator.init(commit_sha="abc", branch="release")
        orchestrator.status()
        captured = capsys.readouterr()

        for name in ALL_AGENT_NAMES:
            assert name in captured.out, (
                f"Agent '{name}' should appear in status output"
            )
