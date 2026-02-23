"""
Base agent framework for the Android CI/CD AI Agent Pipeline.

Provides the AgentResult return type and BaseAgent abstract class that
all pipeline agents extend. Implements the full agent lifecycle: idempotency,
dry-run mode, retry loop, exception capture, and persistent state transitions.
"""

from __future__ import annotations

import traceback
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Optional

from agents.memory import PipelineMemory


@dataclass
class AgentResult:
    """Standard result type returned by all agents."""

    success: bool
    data: dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None

    @classmethod
    def ok(cls, data: dict[str, Any] | None = None) -> AgentResult:
        """Create a successful result with optional data."""
        return cls(success=True, data=data or {})

    @classmethod
    def fail(cls, error: str) -> AgentResult:
        """Create a failed result with an error message."""
        return cls(success=False, error=error)


class BaseAgent(ABC):
    """
    Abstract base class for all pipeline agents.

    Provides the run() lifecycle which handles idempotency, dry-run mode,
    retry logic, exception capture, and state persistence. Subclasses
    implement execute() with their specific logic.
    """

    def __init__(
        self,
        name: str,
        memory: PipelineMemory,
        dry_run: bool = False,
        max_retries: int = 3,
    ) -> None:
        """Initialize the agent with a name, memory instance, and configuration."""
        self.name = name
        self.memory = memory
        self.dry_run = dry_run
        self.max_retries = max_retries

    @abstractmethod
    def execute(self) -> AgentResult:
        """
        Execute the agent's core logic.

        Subclasses must implement this method. It should return an
        AgentResult indicating success or failure.
        """
        ...

    def run(self) -> AgentResult:
        """
        Run the full agent lifecycle.

        1. Load state — if already DONE, return early (idempotency)
        2. Mark as RUNNING
        3. If dry_run — return AgentResult.ok({"dry_run": True})
        4. Retry loop up to max_retries:
           - Call self.execute()
           - If success → mark DONE, return result
           - If fail → mark FAILED, if can_retry → loop again
        5. Catch all exceptions → mark FAILED with formatted traceback
        6. Always save state after each transition
        """
        try:
            # Step 1: Check if already done (idempotency)
            run = self.memory.load()
            if run is not None:
                agent_state = run.get_agent(self.name)
                if agent_state.is_done():
                    return AgentResult.ok(agent_state.output)

            # Step 2: Mark as RUNNING
            self.memory.mark_agent_running(self.name)

            # Step 3: Handle dry run
            if self.dry_run:
                output = {"dry_run": True}
                self.memory.mark_agent_done(self.name, output)
                return AgentResult.ok(output)

            # Step 4: Retry loop
            last_result: Optional[AgentResult] = None
            for _attempt in range(self.max_retries):
                try:
                    result = self.execute()

                    if result.success:
                        self.memory.mark_agent_done(self.name, result.data)
                        return result
                    else:
                        self.memory.mark_agent_failed(
                            self.name, result.error or "Unknown error"
                        )
                        last_result = result

                        # Check if we can retry
                        run = self.memory.load()
                        if run is not None:
                            agent_state = run.get_agent(self.name)
                            if not agent_state.can_retry(self.max_retries):
                                return result
                        # Reset to RUNNING for next attempt
                        self.memory.mark_agent_running(self.name)

                except Exception:
                    error_msg = traceback.format_exc()
                    self.memory.mark_agent_failed(self.name, error_msg)
                    last_result = AgentResult.fail(error_msg)

                    # Check if we can retry
                    run = self.memory.load()
                    if run is not None:
                        agent_state = run.get_agent(self.name)
                        if not agent_state.can_retry(self.max_retries):
                            return last_result
                    # Reset to RUNNING for next attempt
                    self.memory.mark_agent_running(self.name)

            # Exhausted all retries
            return last_result or AgentResult.fail("Max retries exhausted")

        except Exception:
            # Step 5: Catch-all for unexpected errors
            error_msg = traceback.format_exc()
            try:
                self.memory.mark_agent_failed(self.name, error_msg)
            except Exception:
                pass  # State save failed — nothing we can do
            return AgentResult.fail(error_msg)
