"""
Comprehensive tests for agents/base_agent.py.

Tests cover the BaseAgent lifecycle (fresh run, idempotency, dry-run),
retry behavior (retry on failure, max retries exhaustion), and error
handling (exceptions caught, state persisted on failure).
"""

from pathlib import Path
from typing import Any

import pytest

from agents.base_agent import AgentResult, BaseAgent
from agents.memory import AgentStatus, PipelineMemory


class TestableAgent(BaseAgent):
    """
    Concrete test implementation of BaseAgent.

    Allows tests to control execute() behavior via a list of results
    that are returned in sequence on each call.
    """

    def __init__(
        self,
        memory: PipelineMemory,
        results: list[AgentResult] | None = None,
        raise_exception: bool = False,
        exception_msg: str = "Boom!",
        **kwargs: Any,
    ) -> None:
        """Initialize with configurable results and exception behavior."""
        super().__init__(name="validator", memory=memory, **kwargs)
        self._results = results or [AgentResult.ok()]
        self._call_count = 0
        self._raise_exception = raise_exception
        self._exception_msg = exception_msg

    def execute(self) -> AgentResult:
        """Return the next pre-configured result or raise an exception."""
        if self._raise_exception:
            raise RuntimeError(self._exception_msg)
        result = self._results[min(self._call_count, len(self._results) - 1)]
        self._call_count += 1
        return result


# ---------------------------------------------------------------------------
# TestBaseAgentLifecycle
# ---------------------------------------------------------------------------

class TestBaseAgentLifecycle:
    """Tests for the core BaseAgent.run() lifecycle."""

    def test_run_when_fresh_agent_executes_and_marks_done(
        self, tmp_path: Path
    ) -> None:
        """A fresh agent should execute and be marked DONE on success."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            results=[AgentResult.ok({"checked": True})],
        )
        result = agent.run()

        assert result.success is True
        assert result.data == {"checked": True}

        run = memory.load()
        assert run is not None
        assert run.get_agent("validator").status == AgentStatus.DONE

    def test_run_when_already_done_skips_execution(
        self, tmp_path: Path
    ) -> None:
        """An agent that is already DONE should return early without re-executing."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        # First run — completes successfully
        agent = TestableAgent(
            memory=memory,
            results=[AgentResult.ok({"first": True})],
        )
        agent.run()

        # Second run — should skip execute() entirely
        agent2 = TestableAgent(
            memory=memory,
            results=[AgentResult.ok({"second": True})],
        )
        result = agent2.run()

        assert result.success is True
        # Should return the FIRST run's output, not the second
        assert result.data == {"first": True}

    def test_run_when_dry_run_returns_ok_without_executing(
        self, tmp_path: Path
    ) -> None:
        """A dry_run agent should return ok with dry_run flag without calling execute()."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            results=[AgentResult.fail("Should not reach this")],
            dry_run=True,
        )
        result = agent.run()

        assert result.success is True
        assert result.data == {"dry_run": True}

        run = memory.load()
        assert run is not None
        assert run.get_agent("validator").status == AgentStatus.DONE


# ---------------------------------------------------------------------------
# TestBaseAgentRetry
# ---------------------------------------------------------------------------

class TestBaseAgentRetry:
    """Tests for retry behavior in BaseAgent.run()."""

    def test_run_when_first_attempt_fails_retries_and_succeeds(
        self, tmp_path: Path
    ) -> None:
        """Agent should retry after first failure and succeed on second attempt."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            results=[
                AgentResult.fail("Transient error"),
                AgentResult.ok({"recovered": True}),
            ],
            max_retries=3,
        )
        result = agent.run()

        assert result.success is True
        assert result.data == {"recovered": True}

    def test_run_when_max_retries_exhausted_returns_failure(
        self, tmp_path: Path
    ) -> None:
        """Agent should give up after exhausting all retries."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            results=[AgentResult.fail("Persistent error")],
            max_retries=2,
        )
        result = agent.run()

        assert result.success is False
        assert "Persistent error" in (result.error or "")


# ---------------------------------------------------------------------------
# TestBaseAgentErrorHandling
# ---------------------------------------------------------------------------

class TestBaseAgentErrorHandling:
    """Tests for exception handling in BaseAgent.run()."""

    def test_run_when_execute_raises_exception_catches_it(
        self, tmp_path: Path
    ) -> None:
        """An exception in execute() should be caught and returned as a failure."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            raise_exception=True,
            exception_msg="Unexpected crash",
            max_retries=1,
        )
        result = agent.run()

        assert result.success is False
        assert "Unexpected crash" in (result.error or "")

    def test_run_when_failure_state_is_saved_to_disk(
        self, tmp_path: Path
    ) -> None:
        """Agent state should be persisted to disk even when execution fails."""
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        memory.init_run("abc", "release")

        agent = TestableAgent(
            memory=memory,
            results=[AgentResult.fail("Disk full")],
            max_retries=1,
        )
        agent.run()

        # Verify state was saved to disk
        run = memory.load()
        assert run is not None
        agent_state = run.get_agent("validator")
        assert agent_state.status == AgentStatus.FAILED
        assert agent_state.error is not None
