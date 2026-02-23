"""
Pipeline memory system for the Android CI/CD AI Agent Pipeline.

Provides persistent state management for pipeline runs, agent statuses,
file-based locking, and run history. All state is persisted to
`.pipeline/state.json` with atomic writes to survive session loss.
"""

from __future__ import annotations

import fcntl
import json
import os
import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path
from typing import Any, Optional


class AgentStatus(str, Enum):
    """Status of an individual agent in the pipeline."""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    DONE = "DONE"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"


class PipelineStatus(str, Enum):
    """Overall status of a pipeline run."""

    NOT_STARTED = "NOT_STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    NEEDS_MANUAL = "NEEDS_MANUAL"


# Phase-to-agent mapping: defines which agents belong to each phase
PHASE_AGENTS: dict[str, list[str]] = {
    "P1": ["validator", "version", "secrets"],
    "P2": ["build"],
    "P3": ["qa", "metadata"],
    "P4": ["upload"],
    "P5": ["tester", "notifier"],
}

# All agent names in pipeline order
ALL_AGENT_NAMES: list[str] = [
    "validator", "version", "secrets",
    "build",
    "qa", "metadata",
    "upload",
    "tester", "notifier",
]


@dataclass
class AgentState:
    """Tracks the state of a single agent within a pipeline run."""

    name: str
    status: AgentStatus = AgentStatus.PENDING
    result: Optional[str] = None
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    error: Optional[str] = None
    retry_count: int = 0
    output: dict[str, Any] = field(default_factory=dict)

    def mark_running(self) -> None:
        """Mark this agent as currently running and record the start time."""
        self.status = AgentStatus.RUNNING
        self.started_at = datetime.now(timezone.utc).isoformat()

    def mark_done(self, output: dict[str, Any] | None = None) -> None:
        """Mark this agent as successfully completed with optional output data."""
        self.status = AgentStatus.DONE
        self.completed_at = datetime.now(timezone.utc).isoformat()
        if output is not None:
            self.output = output

    def mark_failed(self, error: str) -> None:
        """Mark this agent as failed, record the error, and increment retry count."""
        self.status = AgentStatus.FAILED
        self.error = error
        self.completed_at = datetime.now(timezone.utc).isoformat()
        self.retry_count += 1

    def is_done(self) -> bool:
        """Return True if this agent has completed successfully."""
        return self.status == AgentStatus.DONE

    def can_retry(self, max_retries: int = 3) -> bool:
        """Return True if this agent can be retried (under max retry limit)."""
        return self.retry_count < max_retries

    def to_dict(self) -> dict[str, Any]:
        """Serialize this agent state to a dictionary."""
        return {
            "name": self.name,
            "status": self.status.value,
            "result": self.result,
            "started_at": self.started_at,
            "completed_at": self.completed_at,
            "error": self.error,
            "retry_count": self.retry_count,
            "output": self.output,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> AgentState:
        """Deserialize an agent state from a dictionary."""
        return cls(
            name=data["name"],
            status=AgentStatus(data.get("status", "PENDING")),
            result=data.get("result"),
            started_at=data.get("started_at"),
            completed_at=data.get("completed_at"),
            error=data.get("error"),
            retry_count=data.get("retry_count", 0),
            output=data.get("output", {}),
        )


@dataclass
class PipelineRun:
    """Represents a complete pipeline run with all agent states and metadata."""

    run_id: str
    triggered_by: str
    commit_sha: str
    branch: str
    started_at: str
    status: PipelineStatus = PipelineStatus.IN_PROGRESS
    current_phase: str = "P1"
    agents: dict[str, AgentState] = field(default_factory=dict)
    version_code: Optional[int] = None
    version_name: Optional[str] = None
    aab_path: Optional[str] = None
    package_name: Optional[str] = None
    errors: list[str] = field(default_factory=list)
    ai_notes: str = ""
    dry_run: bool = False

    def get_agent(self, name: str) -> AgentState:
        """Get the state of a specific agent by name."""
        if name not in self.agents:
            raise KeyError(f"Unknown agent: {name}")
        return self.agents[name]

    def update_agent(self, agent: AgentState) -> None:
        """Write an updated agent state back to the agents dict."""
        self.agents[agent.name] = agent

    def summary(self) -> str:
        """Generate a human-readable multi-line summary of the pipeline run."""
        lines: list[str] = [
            f"Pipeline Run: {self.run_id}",
            f"Status: {self.status.value}",
            f"Branch: {self.branch} @ {self.commit_sha[:8]}",
            f"Phase: {self.current_phase}",
            f"Started: {self.started_at}",
            "",
            "Agents:",
        ]
        for name in ALL_AGENT_NAMES:
            agent = self.agents.get(name)
            if agent:
                line = f"  {agent.name}: {agent.status.value}"
                if agent.error:
                    line += f" (error: {agent.error})"
                lines.append(line)

        if self.errors:
            lines.append("")
            lines.append("Errors:")
            for err in self.errors:
                lines.append(f"  - {err}")

        if self.ai_notes:
            lines.append("")
            lines.append(f"AI Notes: {self.ai_notes}")

        return "\n".join(lines)

    def phase_is_complete(self, phase: str) -> bool:
        """Return True if all agents in the given phase are DONE."""
        agent_names = PHASE_AGENTS.get(phase, [])
        return all(
            self.agents.get(name, AgentState(name=name)).is_done()
            for name in agent_names
        )

    def to_dict(self) -> dict[str, Any]:
        """Serialize the full pipeline run to a dictionary."""
        return {
            "run_id": self.run_id,
            "triggered_by": self.triggered_by,
            "commit_sha": self.commit_sha,
            "branch": self.branch,
            "started_at": self.started_at,
            "status": self.status.value,
            "current_phase": self.current_phase,
            "agents": {
                name: agent.to_dict()
                for name, agent in self.agents.items()
            },
            "version_code": self.version_code,
            "version_name": self.version_name,
            "aab_path": self.aab_path,
            "package_name": self.package_name,
            "errors": self.errors,
            "ai_notes": self.ai_notes,
            "dry_run": self.dry_run,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> PipelineRun:
        """Deserialize a pipeline run from a dictionary."""
        agents = {
            name: AgentState.from_dict(agent_data)
            for name, agent_data in data.get("agents", {}).items()
        }
        return cls(
            run_id=data["run_id"],
            triggered_by=data["triggered_by"],
            commit_sha=data["commit_sha"],
            branch=data["branch"],
            started_at=data["started_at"],
            status=PipelineStatus(data.get("status", "IN_PROGRESS")),
            current_phase=data.get("current_phase", "P1"),
            agents=agents,
            version_code=data.get("version_code"),
            version_name=data.get("version_name"),
            aab_path=data.get("aab_path"),
            package_name=data.get("package_name"),
            errors=data.get("errors", []),
            ai_notes=data.get("ai_notes", ""),
            dry_run=data.get("dry_run", False),
        )


class PipelineMemory:
    """
    Persistent state manager for the CI/CD pipeline.

    Manages pipeline run state via `.pipeline/state.json`, supports atomic
    writes, file-based locking, run archival, and phase-based resumption.
    """

    def __init__(self, state_dir: str = ".pipeline") -> None:
        """Initialize PipelineMemory with the given state directory."""
        self.state_dir = Path(state_dir)
        self.state_file = self.state_dir / "state.json"
        self.history_dir = self.state_dir / "history"
        self.locks_dir = self.state_dir / "locks"
        self.artifacts_dir = self.state_dir / "artifacts"

        # Create all required directories
        self.state_dir.mkdir(parents=True, exist_ok=True)
        self.history_dir.mkdir(parents=True, exist_ok=True)
        self.locks_dir.mkdir(parents=True, exist_ok=True)
        self.artifacts_dir.mkdir(parents=True, exist_ok=True)

        # Track open lock file handles
        self._lock_handles: dict[str, Any] = {}

        # Thread-safe state file access (RLock allows re-entrant locking
        # so mark_agent_* can hold the lock and still call save())
        self._state_lock = threading.RLock()

    def init_run(
        self,
        commit_sha: str,
        branch: str,
        triggered_by: str = "manual",
        package_name: Optional[str] = None,
    ) -> PipelineRun:
        """
        Initialize a new pipeline run.

        Archives any existing state file to history, then creates a fresh
        PipelineRun with all agents set to PENDING.
        """
        # Archive existing state if present
        existing = self.load()
        if existing is not None:
            archive_path = self.history_dir / f"{existing.run_id}.json"
            archive_path.write_text(
                json.dumps(existing.to_dict(), indent=2), encoding="utf-8"
            )

        # Create fresh run with all agents PENDING
        run_id = str(uuid.uuid4())[:8]
        now = datetime.now(timezone.utc).isoformat()

        agents: dict[str, AgentState] = {
            name: AgentState(name=name)
            for name in ALL_AGENT_NAMES
        }

        run = PipelineRun(
            run_id=run_id,
            triggered_by=triggered_by,
            commit_sha=commit_sha,
            branch=branch,
            started_at=now,
            agents=agents,
            package_name=package_name,
        )

        self.save(run)
        return run

    def load(self) -> Optional[PipelineRun]:
        """
        Load the current pipeline run from state.json.

        Returns None if no state file exists or if the file is corrupted.
        """
        if not self.state_file.exists():
            return None
        try:
            data = json.loads(self.state_file.read_text(encoding="utf-8"))
            return PipelineRun.from_dict(data)
        except (json.JSONDecodeError, KeyError, ValueError, TypeError):
            return None

    def save(self, run: PipelineRun) -> None:
        """
        Save the pipeline run to state.json atomically.

        Writes to a uniquely-named temporary file, then renames to the
        target path. Each thread gets a unique tmp filename to prevent
        race conditions during parallel agent execution.
        """
        with self._state_lock:
            suffix = f"{os.getpid()}.{threading.get_ident()}"
            tmp_file = self.state_dir / f"state.{suffix}.tmp"
            tmp_file.write_text(
                json.dumps(run.to_dict(), indent=2), encoding="utf-8"
            )
            os.rename(str(tmp_file), str(self.state_file))

    def mark_agent_running(self, agent_name: str) -> PipelineRun:
        """Mark an agent as RUNNING and persist the state."""
        with self._state_lock:
            run = self.load()
            if run is None:
                raise RuntimeError("No active pipeline run")
            agent = run.get_agent(agent_name)
            agent.mark_running()
            run.update_agent(agent)
            self.save(run)
            return run

    def mark_agent_done(
        self, agent_name: str, output: dict[str, Any] | None = None
    ) -> PipelineRun:
        """Mark an agent as DONE with optional output data and persist the state."""
        with self._state_lock:
            run = self.load()
            if run is None:
                raise RuntimeError("No active pipeline run")
            agent = run.get_agent(agent_name)
            agent.mark_done(output)
            run.update_agent(agent)
            self.save(run)
            return run

    def mark_agent_failed(self, agent_name: str, error: str) -> PipelineRun:
        """Mark an agent as FAILED with an error message and persist the state."""
        with self._state_lock:
            run = self.load()
            if run is None:
                raise RuntimeError("No active pipeline run")
            agent = run.get_agent(agent_name)
            agent.mark_failed(error)
            run.update_agent(agent)
            self.save(run)
            return run

    def set_ai_notes(self, notes: str) -> None:
        """Set AI-generated notes on the current pipeline run."""
        run = self.load()
        if run is None:
            raise RuntimeError("No active pipeline run")
        run.ai_notes = notes
        self.save(run)

    def acquire_lock(self, lock_name: str) -> bool:
        """
        Acquire a file-based lock using fcntl.

        Returns True if the lock was acquired, False otherwise.
        """
        lock_path = self.locks_dir / f"{lock_name}.lock"
        try:
            fh = open(lock_path, "w")  # noqa: SIM115
            fcntl.flock(fh.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            self._lock_handles[lock_name] = fh
            return True
        except (OSError, IOError):
            return False

    def release_lock(self, lock_name: str) -> None:
        """Release a previously acquired file-based lock."""
        fh = self._lock_handles.pop(lock_name, None)
        if fh is not None:
            try:
                fcntl.flock(fh.fileno(), fcntl.LOCK_UN)
                fh.close()
            except (OSError, IOError):
                pass

    def resume_from_phase(self, phase: str) -> Optional[PipelineRun]:
        """
        Resume pipeline from a given phase by resetting agents in that
        phase and all subsequent phases to PENDING.

        Returns None if no existing state is found.
        """
        run = self.load()
        if run is None:
            return None

        phase_order = ["P1", "P2", "P3", "P4", "P5"]
        try:
            start_idx = phase_order.index(phase)
        except ValueError:
            return None

        # Reset agents from the specified phase onward
        phases_to_reset = phase_order[start_idx:]
        for p in phases_to_reset:
            for agent_name in PHASE_AGENTS.get(p, []):
                agent = run.agents.get(agent_name)
                if agent:
                    agent.status = AgentStatus.PENDING
                    agent.error = None
                    agent.result = None
                    agent.started_at = None
                    agent.completed_at = None
                    agent.output = {}

        run.current_phase = phase
        run.status = PipelineStatus.IN_PROGRESS
        self.save(run)
        return run

    def get_history(self) -> list[dict[str, Any]]:
        """Read all archived pipeline runs from the history directory."""
        history: list[dict[str, Any]] = []
        if not self.history_dir.exists():
            return history
        for history_file in sorted(self.history_dir.glob("*.json")):
            try:
                data = json.loads(
                    history_file.read_text(encoding="utf-8")
                )
                history.append(data)
            except (json.JSONDecodeError, OSError):
                continue
        return history
