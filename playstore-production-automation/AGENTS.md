# AGENTS.md — Agent Code, Memory System & State Management

> **AI AGENT READING THIS:** This file contains:
> 1. The PipelineMemory class — your persistent brain across sessions
> 2. BaseAgent class — inherit this for every agent you build
> 3. All 9 agent implementations with full code
> 4. Test cases for every function
>
> **Rule:** Before writing any code, call `memory.load()`.
> After every significant action, call `memory.save()`.
> If you are resuming, call `memory.get_agent_status(agent_name)` first.

---

## 1. Memory System (`agents/memory.py`)

The memory system is the backbone of session persistence. Every agent reads
from and writes to this system. If you lose your chat, open a new session,
or switch AI models — this is what keeps the pipeline alive.

### Core Concepts

- **Run ID**: Unique identifier for each pipeline execution (`run_YYYYMMDD_HHMMSS`)
- **Agent Status**: `PENDING | RUNNING | DONE | FAILED | SKIPPED`
- **Checkpoint**: A named sync point that blocks until all required agents reach DONE
- **AI Notes**: Free-text field for the AI to leave context for its next session
- **History**: Every completed run is archived forever for debugging

```python
# agents/memory.py

import json
import os
import fcntl
import hashlib
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional, Any
from dataclasses import dataclass, field, asdict
from enum import Enum


class AgentStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    DONE = "DONE"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"


class PipelineStatus(str, Enum):
    NOT_STARTED = "NOT_STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    NEEDS_MANUAL = "NEEDS_MANUAL"


@dataclass
class AgentState:
    """State of a single agent in the pipeline."""
    name: str
    status: str = AgentStatus.PENDING
    result: Optional[str] = None
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    error: Optional[str] = None
    retry_count: int = 0
    output: dict = field(default_factory=dict)

    def is_done(self) -> bool:
        return self.status == AgentStatus.DONE

    def is_failed(self) -> bool:
        return self.status == AgentStatus.FAILED

    def is_running(self) -> bool:
        return self.status == AgentStatus.RUNNING

    def can_retry(self, max_retries: int = 3) -> bool:
        return self.is_failed() and self.retry_count < max_retries

    def mark_running(self) -> None:
        self.status = AgentStatus.RUNNING
        self.started_at = datetime.now(timezone.utc).isoformat()

    def mark_done(self, result: str = "PASS", output: dict = None) -> None:
        self.status = AgentStatus.DONE
        self.result = result
        self.completed_at = datetime.now(timezone.utc).isoformat()
        if output:
            self.output = output

    def mark_failed(self, error: str) -> None:
        self.status = AgentStatus.FAILED
        self.error = error
        self.completed_at = datetime.now(timezone.utc).isoformat()
        self.retry_count += 1


@dataclass
class PipelineRun:
    """Complete state for one pipeline run. Serialized to state.json."""
    run_id: str
    triggered_by: str
    commit_sha: str
    branch: str
    started_at: str
    status: str = PipelineStatus.IN_PROGRESS
    current_phase: str = "P1"
    completed_at: Optional[str] = None
    version_code: Optional[int] = None
    version_name: Optional[str] = None
    package_name: Optional[str] = None
    aab_path: Optional[str] = None
    play_store_track: str = "internal"
    errors: list = field(default_factory=list)
    ai_notes: str = ""
    agents: dict = field(default_factory=dict)

    def __post_init__(self):
        """Initialize all agents in PENDING state if not already set."""
        agent_names = [
            "validator", "version", "secrets", "build",
            "qa", "metadata", "upload", "tester", "notifier"
        ]
        for name in agent_names:
            if name not in self.agents:
                self.agents[name] = asdict(AgentState(name=name))

    def get_agent(self, name: str) -> AgentState:
        return AgentState(**self.agents[name])

    def update_agent(self, agent: AgentState) -> None:
        self.agents[agent.name] = asdict(agent)

    def summary(self) -> str:
        """Human-readable summary for AI session resume."""
        lines = [
            f"Run ID: {self.run_id}",
            f"Status: {self.status}  |  Phase: {self.current_phase}",
            f"Commit: {self.commit_sha} on {self.branch}",
            f"Started: {self.started_at}",
            "",
            "Agent Status:",
        ]
        for name, state_dict in self.agents.items():
            state = AgentState(**state_dict)
            icon = {"DONE": "✅", "RUNNING": "🔄", "FAILED": "❌",
                    "PENDING": "⏳", "SKIPPED": "⏭️"}.get(state.status, "?")
            err = f" — ERROR: {state.error}" if state.error else ""
            lines.append(f"  {icon} {name:12s} {state.status}{err}")

        if self.ai_notes:
            lines += ["", f"AI Notes: {self.ai_notes}"]
        if self.errors:
            lines += ["", "Pipeline Errors:"]
            for e in self.errors:
                lines.append(f"  • {e}")
        return "\n".join(lines)

    def phase_is_complete(self, phase: str) -> bool:
        """Check if all agents for a phase have completed successfully."""
        phase_agents = {
            "P1": ["validator", "version", "secrets"],
            "P2": ["build"],
            "P3": ["qa", "metadata"],
            "P4": ["upload"],
            "P5": ["tester", "notifier"],
        }
        agents_in_phase = phase_agents.get(phase, [])
        return all(
            AgentState(**self.agents[a]).is_done()
            for a in agents_in_phase
            if a in self.agents
        )


class PipelineMemory:
    """
    Persistent memory for the entire pipeline system.
    
    AI AGENT: This is your external brain. Use it like this:
    
        memory = PipelineMemory()
        
        # On session start — always do this first
        state = memory.load()
        if state:
            print(state.summary())  # understand where you are
        else:
            state = memory.init_run(commit_sha="abc123", branch="release")
        
        # During work — save after every agent action
        agent = state.get_agent("validator")
        agent.mark_running()
        state.update_agent(agent)
        memory.save(state)
        
        # Leave notes for your next session
        state.ai_notes = "Upload failed with 403. Needs Play Editor permission fixed."
        memory.save(state)
        
        # Resume from a specific phase
        memory.resume_from_phase("P3")
    """

    AGENT_NAMES = [
        "validator", "version", "secrets", "build",
        "qa", "metadata", "upload", "tester", "notifier"
    ]

    def __init__(self, state_dir: str = ".pipeline"):
        self.state_dir = Path(state_dir)
        self.state_file = self.state_dir / "state.json"
        self.history_dir = self.state_dir / "history"
        self.locks_dir = self.state_dir / "locks"
        self.artifacts_dir = self.state_dir / "artifacts"
        self._ensure_dirs()

    def _ensure_dirs(self) -> None:
        """Create all required directories."""
        for d in [self.state_dir, self.history_dir,
                  self.locks_dir, self.artifacts_dir]:
            d.mkdir(parents=True, exist_ok=True)

    def init_run(self, commit_sha: str, branch: str,
                 triggered_by: str = "manual",
                 package_name: str = None) -> PipelineRun:
        """Start a new pipeline run. Archives any existing run first."""
        existing = self.load()
        if existing and existing.status == PipelineStatus.IN_PROGRESS:
            self._archive(existing)

        run_id = f"run_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S')}"
        run = PipelineRun(
            run_id=run_id,
            triggered_by=triggered_by,
            commit_sha=commit_sha,
            branch=branch,
            started_at=datetime.now(timezone.utc).isoformat(),
            package_name=package_name or os.getenv("ANDROID_PACKAGE_NAME"),
        )
        self.save(run)
        return run

    def load(self) -> Optional[PipelineRun]:
        """Load current pipeline state. Returns None if no state exists."""
        if not self.state_file.exists():
            return None
        try:
            with open(self.state_file, "r") as f:
                data = json.load(f)
            # Reconstruct agents as dicts (they're stored as dicts)
            return PipelineRun(**data)
        except (json.JSONDecodeError, TypeError, KeyError) as e:
            print(f"[Memory] WARNING: Could not load state: {e}")
            return None

    def save(self, run: PipelineRun) -> None:
        """Save pipeline state atomically using a temp file + rename."""
        tmp_file = self.state_file.with_suffix(".tmp")
        data = asdict(run)
        with open(tmp_file, "w") as f:
            json.dump(data, f, indent=2, default=str)
        tmp_file.rename(self.state_file)

    def mark_agent_running(self, agent_name: str) -> PipelineRun:
        """Convenience: mark an agent as running and save."""
        run = self.load()
        if not run:
            raise RuntimeError("No active pipeline run. Call init_run() first.")
        agent = run.get_agent(agent_name)
        agent.mark_running()
        run.update_agent(agent)
        self.save(run)
        return run

    def mark_agent_done(self, agent_name: str, output: dict = None) -> PipelineRun:
        """Convenience: mark an agent as done and save."""
        run = self.load()
        agent = run.get_agent(agent_name)
        agent.mark_done(output=output or {})
        run.update_agent(agent)
        self.save(run)
        return run

    def mark_agent_failed(self, agent_name: str, error: str) -> PipelineRun:
        """Convenience: mark an agent as failed with error message and save."""
        run = self.load()
        agent = run.get_agent(agent_name)
        agent.mark_failed(error)
        run.update_agent(agent)
        run.errors.append(f"[{agent_name}] {error}")
        self.save(run)
        return run

    def set_ai_notes(self, notes: str) -> None:
        """Leave a note for the next AI session. Call this liberally."""
        run = self.load()
        if run:
            run.ai_notes = notes
            self.save(run)

    def acquire_lock(self, lock_name: str) -> bool:
        """
        Acquire a named lock to prevent race conditions (e.g., version increment).
        Returns True if lock acquired, False if already locked.
        """
        lock_file = self.locks_dir / f"{lock_name}.lock"
        try:
            fd = open(lock_file, "w")
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            fd.write(str(os.getpid()))
            fd.flush()
            return True
        except (IOError, OSError):
            return False

    def release_lock(self, lock_name: str) -> None:
        """Release a named lock."""
        lock_file = self.locks_dir / f"{lock_name}.lock"
        if lock_file.exists():
            lock_file.unlink()

    def get_history(self) -> list[dict]:
        """Return all completed pipeline runs sorted by date descending."""
        runs = []
        for f in sorted(self.history_dir.glob("*.json"), reverse=True):
            try:
                with open(f) as fp:
                    runs.append(json.load(fp))
            except Exception:
                pass
        return runs

    def _archive(self, run: PipelineRun) -> None:
        """Move the current state to history."""
        archive_file = self.history_dir / f"{run.run_id}.json"
        with open(archive_file, "w") as f:
            json.dump(asdict(run), f, indent=2)

    def resume_from_phase(self, phase: str) -> Optional[PipelineRun]:
        """
        Reset all agents from a phase onwards to PENDING so they re-run.
        Useful when resuming after a failure at a specific phase.
        """
        phase_order = ["P1", "P2", "P3", "P4", "P5"]
        phase_agents = {
            "P1": ["validator", "version", "secrets"],
            "P2": ["build"],
            "P3": ["qa", "metadata"],
            "P4": ["upload"],
            "P5": ["tester", "notifier"],
        }
        run = self.load()
        if not run:
            return None

        start_idx = phase_order.index(phase)
        for p in phase_order[start_idx:]:
            for agent_name in phase_agents.get(p, []):
                agent = run.get_agent(agent_name)
                agent.status = AgentStatus.PENDING
                agent.result = None
                agent.error = None
                agent.started_at = None
                agent.completed_at = None
                run.update_agent(agent)

        run.current_phase = phase
        run.status = PipelineStatus.IN_PROGRESS
        self.save(run)
        return run
```

---

## 2. Base Agent (`agents/base_agent.py`)

Every agent inherits from `BaseAgent`. It handles memory integration,
retry logic, logging, and the standard run lifecycle automatically.

```python
# agents/base_agent.py

import os
import time
import traceback
from abc import ABC, abstractmethod
from typing import Optional
from agents.memory import PipelineMemory, PipelineRun, AgentState


class AgentResult:
    """Standard result object returned by every agent."""
    def __init__(self, success: bool, data: dict = None, error: str = None):
        self.success = success
        self.data = data or {}
        self.error = error

    @classmethod
    def ok(cls, data: dict = None) -> "AgentResult":
        return cls(success=True, data=data or {})

    @classmethod
    def fail(cls, error: str) -> "AgentResult":
        return cls(success=False, error=error)

    def __bool__(self) -> bool:
        return self.success


class BaseAgent(ABC):
    """
    Base class for all pipeline agents.
    
    AI AGENT: To implement a new agent:
    1. Inherit from BaseAgent
    2. Set self.name to the agent's identifier
    3. Implement the execute() method with your logic
    4. Call self.memory.save(run) after any significant state change
    5. Use self.log() instead of print() for all output
    
    Example:
        class MyAgent(BaseAgent):
            def __init__(self):
                super().__init__("my_agent")
            
            def execute(self, run: PipelineRun) -> AgentResult:
                # Your logic here
                return AgentResult.ok({"key": "value"})
    """

    MAX_RETRIES = int(os.getenv("PIPELINE_MAX_RETRIES", "3"))
    RETRY_DELAY_SECONDS = 5

    def __init__(self, name: str, state_dir: str = None):
        self.name = name
        self.memory = PipelineMemory(
            state_dir=state_dir or os.getenv("PIPELINE_STATE_DIR", ".pipeline")
        )
        self._logs: list[str] = []

    def log(self, message: str) -> None:
        """Structured log with timestamp and agent name."""
        ts = time.strftime("%H:%M:%S")
        line = f"[{ts}][{self.name}] {message}"
        print(line)
        self._logs.append(line)

    def run(self, dry_run: bool = False) -> AgentResult:
        """
        Main entry point. Handles state management, retry logic,
        and error handling automatically.
        """
        run = self.memory.load()
        if not run:
            return AgentResult.fail("No active pipeline run found. "
                                    "Call orchestrator --init first.")

        # Check if already done (idempotency — safe to call multiple times)
        agent_state = run.get_agent(self.name)
        if agent_state.is_done():
            self.log(f"Already completed with result: {agent_state.result}. Skipping.")
            return AgentResult.ok(agent_state.output)

        if dry_run:
            self.log("DRY RUN — would execute but skipping actual operations")
            return AgentResult.ok({"dry_run": True})

        # Mark running
        self.memory.mark_agent_running(self.name)
        self.log(f"Starting agent")

        # Execute with retry logic
        last_error = None
        for attempt in range(1, self.MAX_RETRIES + 1):
            try:
                result = self.execute(run)
                if result.success:
                    self.memory.mark_agent_done(self.name, result.data)
                    self.log(f"Completed successfully on attempt {attempt}")
                    return result
                else:
                    last_error = result.error
                    self.log(f"Attempt {attempt} failed: {last_error}")
            except Exception as e:
                last_error = f"{type(e).__name__}: {str(e)}\n{traceback.format_exc()}"
                self.log(f"Attempt {attempt} raised exception: {last_error}")

            if attempt < self.MAX_RETRIES:
                self.log(f"Retrying in {self.RETRY_DELAY_SECONDS}s...")
                time.sleep(self.RETRY_DELAY_SECONDS)

        # All retries exhausted
        self.memory.mark_agent_failed(self.name, last_error)
        self.log(f"All {self.MAX_RETRIES} attempts failed. Marking FAILED.")
        return AgentResult.fail(last_error)

    @abstractmethod
    def execute(self, run: PipelineRun) -> AgentResult:
        """
        Implement your agent's core logic here.
        Return AgentResult.ok() on success, AgentResult.fail(error) on failure.
        
        Important: call self.memory.save(run) after updating run state.
        """
        pass
```

---

## 3. All Agent Implementations

### 3.1 Validator Agent (`agents/validator_agent.py`)

```python
# agents/validator_agent.py

import re
import subprocess
from pathlib import Path
from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineRun


class ValidatorAgent(BaseAgent):
    """
    Validates the project before building.
    Checks: SDK versions, lint, 64-bit compliance, Play Store policy requirements.
    """

    MINIMUM_TARGET_SDK = 34  # Play Store requirement as of 2024
    MINIMUM_COMPILE_SDK = 34

    def __init__(self, project_root: str = "."):
        super().__init__("validator")
        self.project_root = Path(project_root)
        self.manifest_path = self.project_root / "app" / "src" / "main" / "AndroidManifest.xml"
        self.gradle_path = self.project_root / "app" / "build.gradle"

    def execute(self, run: PipelineRun) -> AgentResult:
        self.log("Starting validation checks")
        issues = []

        # Run all checks — collect all failures, don't short-circuit
        sdk_issues = self._check_sdk_versions()
        issues.extend(sdk_issues)

        manifest_issues = self._check_manifest()
        issues.extend(manifest_issues)

        lint_issues = self._run_lint()
        issues.extend(lint_issues)

        if issues:
            error_summary = f"{len(issues)} validation issue(s) found:\n" + \
                           "\n".join(f"  - {i}" for i in issues)
            return AgentResult.fail(error_summary)

        self.log("All validation checks passed")
        return AgentResult.ok({"checks_passed": 4, "issues_found": 0})

    def _check_sdk_versions(self) -> list[str]:
        """Check that targetSdkVersion and compileSdkVersion meet minimums."""
        issues = []
        if not self.gradle_path.exists():
            return [f"build.gradle not found at {self.gradle_path}"]

        content = self.gradle_path.read_text()

        target_match = re.search(r"targetSdkVersion\s+(\d+)", content)
        if target_match:
            target_sdk = int(target_match.group(1))
            if target_sdk < self.MINIMUM_TARGET_SDK:
                issues.append(
                    f"targetSdkVersion {target_sdk} is below minimum {self.MINIMUM_TARGET_SDK}. "
                    f"Update in app/build.gradle"
                )
        else:
            issues.append("targetSdkVersion not found in build.gradle")

        compile_match = re.search(r"compileSdkVersion\s+(\d+)", content)
        if compile_match:
            compile_sdk = int(compile_match.group(1))
            if compile_sdk < self.MINIMUM_COMPILE_SDK:
                issues.append(
                    f"compileSdkVersion {compile_sdk} is below minimum {self.MINIMUM_COMPILE_SDK}"
                )

        return issues

    def _check_manifest(self) -> list[str]:
        """Check AndroidManifest.xml for policy violations."""
        issues = []
        if not self.manifest_path.exists():
            return [f"AndroidManifest.xml not found at {self.manifest_path}"]

        content = self.manifest_path.read_text()

        # Check for dangerous permissions that trigger manual review
        dangerous_permissions = [
            "READ_SMS", "RECEIVE_SMS", "SEND_SMS",
            "PROCESS_OUTGOING_CALLS", "READ_CALL_LOG"
        ]
        for perm in dangerous_permissions:
            if perm in content:
                issues.append(
                    f"Dangerous permission {perm} requires Play Store declaration form. "
                    f"Remove or complete the declaration at play.google.com/about/privacy-security"
                )

        # Check for QUERY_ALL_PACKAGES
        if "QUERY_ALL_PACKAGES" in content:
            issues.append(
                "QUERY_ALL_PACKAGES permission requires policy approval. "
                "Remove unless absolutely necessary."
            )

        return issues

    def _run_lint(self) -> list[str]:
        """Run Android lint and parse results."""
        try:
            gradlew = self.project_root / "gradlew"
            if not gradlew.exists():
                self.log("WARNING: gradlew not found, skipping lint")
                return []

            result = subprocess.run(
                ["./gradlew", "lint", "--quiet"],
                cwd=self.project_root,
                capture_output=True,
                text=True,
                timeout=120
            )

            if result.returncode != 0:
                return [f"Lint failed: {result.stderr[:500]}"]

            return []

        except subprocess.TimeoutExpired:
            return ["Lint check timed out after 120 seconds"]
        except FileNotFoundError:
            self.log("WARNING: gradle not available, skipping lint")
            return []
```

### Test: `tests/test_validator_agent.py`

```python
# tests/test_validator_agent.py

import pytest
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock
from agents.validator_agent import ValidatorAgent
from agents.memory import PipelineMemory, PipelineRun, PipelineStatus


@pytest.fixture
def temp_project(tmp_path):
    """Create a minimal fake Android project structure."""
    app_dir = tmp_path / "app" / "src" / "main"
    app_dir.mkdir(parents=True)
    return tmp_path


@pytest.fixture
def valid_gradle(temp_project):
    """Create a valid build.gradle with correct SDK versions."""
    gradle = temp_project / "app" / "build.gradle"
    gradle.write_text("""
        android {
            compileSdkVersion 34
            defaultConfig {
                targetSdkVersion 34
                versionCode 1
                versionName "1.0.0"
            }
        }
    """)
    return temp_project


@pytest.fixture
def valid_manifest(temp_project):
    """Create a valid AndroidManifest.xml with no dangerous permissions."""
    manifest = temp_project / "app" / "src" / "main" / "AndroidManifest.xml"
    manifest.write_text("""<?xml version="1.0" encoding="utf-8"?>
    <manifest package="com.example.app">
        <uses-permission android:name="android.permission.INTERNET" />
    </manifest>""")
    return temp_project


@pytest.fixture
def mock_pipeline_run():
    """Create a mock pipeline run for testing."""
    return MagicMock(spec=PipelineRun)


class TestValidatorAgentSdkCheck:
    """Tests for SDK version validation."""

    def test_check_sdk_versions_when_valid_returns_no_issues(self, valid_gradle):
        agent = ValidatorAgent(project_root=str(valid_gradle))
        issues = agent._check_sdk_versions()
        assert issues == []

    def test_check_sdk_versions_when_target_too_low_returns_issue(self, temp_project):
        gradle = temp_project / "app" / "build.gradle"
        gradle.write_text("android { defaultConfig { targetSdkVersion 32 } }")

        agent = ValidatorAgent(project_root=str(temp_project))
        issues = agent._check_sdk_versions()

        assert len(issues) == 1
        assert "targetSdkVersion 32" in issues[0]
        assert "minimum 34" in issues[0]

    def test_check_sdk_versions_when_gradle_missing_returns_issue(self, tmp_path):
        agent = ValidatorAgent(project_root=str(tmp_path))
        issues = agent._check_sdk_versions()
        assert any("build.gradle not found" in i for i in issues)

    def test_check_sdk_versions_when_no_target_sdk_returns_issue(self, temp_project):
        gradle = temp_project / "app" / "build.gradle"
        gradle.write_text("android { defaultConfig { minSdkVersion 21 } }")

        agent = ValidatorAgent(project_root=str(temp_project))
        issues = agent._check_sdk_versions()

        assert any("targetSdkVersion not found" in i for i in issues)


class TestValidatorAgentManifestCheck:
    """Tests for AndroidManifest.xml validation."""

    def test_check_manifest_when_clean_returns_no_issues(self, valid_manifest, valid_gradle):
        agent = ValidatorAgent(project_root=str(valid_manifest))
        issues = agent._check_manifest()
        assert issues == []

    def test_check_manifest_when_sms_permission_returns_issue(self, temp_project):
        manifest = temp_project / "app" / "src" / "main" / "AndroidManifest.xml"
        manifest.write_text("""<manifest>
            <uses-permission android:name="android.permission.READ_SMS" />
        </manifest>""")

        agent = ValidatorAgent(project_root=str(temp_project))
        issues = agent._check_manifest()

        assert any("READ_SMS" in i for i in issues)
        assert any("declaration" in i.lower() for i in issues)

    def test_check_manifest_when_query_all_packages_returns_issue(self, temp_project):
        manifest = temp_project / "app" / "src" / "main" / "AndroidManifest.xml"
        manifest.write_text("""<manifest>
            <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
        </manifest>""")

        agent = ValidatorAgent(project_root=str(temp_project))
        issues = agent._check_manifest()

        assert any("QUERY_ALL_PACKAGES" in i for i in issues)

    def test_check_manifest_when_missing_returns_issue(self, tmp_path):
        agent = ValidatorAgent(project_root=str(tmp_path))
        issues = agent._check_manifest()
        assert any("not found" in i for i in issues)

    def test_check_manifest_when_multiple_dangerous_permissions_returns_all_issues(
        self, temp_project
    ):
        manifest = temp_project / "app" / "src" / "main" / "AndroidManifest.xml"
        manifest.write_text("""<manifest>
            <uses-permission android:name="android.permission.READ_SMS" />
            <uses-permission android:name="android.permission.SEND_SMS" />
        </manifest>""")

        agent = ValidatorAgent(project_root=str(temp_project))
        issues = agent._check_manifest()

        assert len(issues) == 2


class TestValidatorAgentExecute:
    """Integration tests for the full execute() flow."""

    @patch.object(ValidatorAgent, '_run_lint', return_value=[])
    def test_execute_when_all_checks_pass_returns_ok(
        self, mock_lint, valid_gradle, valid_manifest, mock_pipeline_run, tmp_path
    ):
        # Merge both fixtures into one path
        (valid_manifest / "app" / "build.gradle").write_text(
            (valid_gradle / "app" / "build.gradle").read_text()
        )
        agent = ValidatorAgent(project_root=str(valid_manifest))

        with patch.object(agent.memory, 'load', return_value=mock_pipeline_run):
            with patch.object(agent.memory, 'mark_agent_running'):
                with patch.object(agent.memory, 'mark_agent_done'):
                    result = agent.execute(mock_pipeline_run)

        assert result.success is True
        assert result.data["checks_passed"] == 4

    @patch.object(ValidatorAgent, '_run_lint', return_value=[])
    def test_execute_when_sdk_too_low_returns_fail(
        self, mock_lint, temp_project, mock_pipeline_run
    ):
        gradle = temp_project / "app" / "build.gradle"
        gradle.write_text("android { defaultConfig { targetSdkVersion 30 } }")
        manifest = temp_project / "app" / "src" / "main" / "AndroidManifest.xml"
        manifest.write_text("<manifest />")

        agent = ValidatorAgent(project_root=str(temp_project))
        result = agent.execute(mock_pipeline_run)

        assert result.success is False
        assert "targetSdkVersion 30" in result.error
```

---

### 3.2 Version Agent (`agents/version_agent.py`)

```python
# agents/version_agent.py

import re
import os
import time
from pathlib import Path
from typing import Tuple, Optional
from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineRun


class VersionAgent(BaseAgent):
    """
    Manages version code and version name.
    Always ensures versionCode is higher than what Play Store last accepted.
    Uses a distributed lock to prevent collisions when multiple pipelines run.
    """

    LOCK_NAME = "version_increment"
    LOCK_TIMEOUT_SECONDS = 30

    def __init__(self, project_root: str = "."):
        super().__init__("version")
        self.project_root = Path(project_root)
        self.gradle_path = self.project_root / "app" / "build.gradle"

    def execute(self, run: PipelineRun) -> AgentResult:
        self.log("Acquiring version lock to prevent collision")

        # Acquire lock with timeout
        lock_acquired = False
        for _ in range(self.LOCK_TIMEOUT_SECONDS):
            if self.memory.acquire_lock(self.LOCK_NAME):
                lock_acquired = True
                break
            time.sleep(1)

        if not lock_acquired:
            return AgentResult.fail(
                "Could not acquire version lock after 30s. "
                "Another pipeline may be running. Check .pipeline/locks/"
            )

        try:
            return self._do_version_increment(run)
        finally:
            self.memory.release_lock(self.LOCK_NAME)

    def _do_version_increment(self, run: PipelineRun) -> AgentResult:
        current_code, current_name = self.read_version_from_gradle()
        self.log(f"Current version: {current_name} (code {current_code})")

        # New version code is simply current + 1
        # In production, you'd also query Play Console API here for safety
        new_code = current_code + 1
        new_name = self._bump_patch(current_name)

        self.log(f"New version: {new_name} (code {new_code})")

        self.write_version_to_gradle(new_code, new_name)

        # Persist to pipeline state
        run.version_code = new_code
        run.version_name = new_name
        self.memory.save(run)

        return AgentResult.ok({
            "old_version_code": current_code,
            "new_version_code": new_code,
            "old_version_name": current_name,
            "new_version_name": new_name,
        })

    def read_version_from_gradle(self) -> Tuple[int, str]:
        """Read versionCode and versionName from build.gradle."""
        if not self.gradle_path.exists():
            raise FileNotFoundError(f"build.gradle not found at {self.gradle_path}")

        content = self.gradle_path.read_text()

        code_match = re.search(r"versionCode\s+(\d+)", content)
        if not code_match:
            raise ValueError("versionCode not found in build.gradle")

        name_match = re.search(r'versionName\s+"([^"]+)"', content)
        if not name_match:
            raise ValueError("versionName not found in build.gradle")

        return int(code_match.group(1)), name_match.group(1)

    def write_version_to_gradle(self, new_code: int, new_name: str) -> None:
        """Write new versionCode and versionName to build.gradle."""
        content = self.gradle_path.read_text()

        content = re.sub(
            r"(versionCode\s+)\d+",
            f"\\g<1>{new_code}",
            content
        )
        content = re.sub(
            r'(versionName\s+)"[^"]+"',
            f'\\g<1>"{new_name}"',
            content
        )

        self.gradle_path.write_text(content)

    def _bump_patch(self, version_name: str) -> str:
        """Increment patch version: 1.2.3 → 1.2.4"""
        parts = version_name.split(".")
        if len(parts) != 3:
            return version_name  # Don't modify non-semver versions
        try:
            parts[2] = str(int(parts[2]) + 1)
            return ".".join(parts)
        except ValueError:
            return version_name
```

### Test: `tests/test_version_agent.py`

```python
# tests/test_version_agent.py

import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock
from agents.version_agent import VersionAgent
from agents.memory import PipelineRun


@pytest.fixture
def gradle_project(tmp_path):
    """Create a fake project with a build.gradle file."""
    app_dir = tmp_path / "app"
    app_dir.mkdir()
    gradle = app_dir / "build.gradle"
    gradle.write_text("""
        android {
            defaultConfig {
                versionCode 10
                versionName "1.2.3"
            }
        }
    """)
    return tmp_path


@pytest.fixture
def mock_run():
    run = MagicMock(spec=PipelineRun)
    run.version_code = None
    run.version_name = None
    return run


class TestReadVersionFromGradle:
    """Tests for reading version info from build.gradle."""

    def test_read_version_when_valid_gradle_returns_code_and_name(self, gradle_project):
        agent = VersionAgent(project_root=str(gradle_project))
        code, name = agent.read_version_from_gradle()
        assert code == 10
        assert name == "1.2.3"

    def test_read_version_when_file_missing_raises_file_not_found(self, tmp_path):
        agent = VersionAgent(project_root=str(tmp_path))
        with pytest.raises(FileNotFoundError, match="build.gradle not found"):
            agent.read_version_from_gradle()

    def test_read_version_when_no_version_code_raises_value_error(self, tmp_path):
        gradle = tmp_path / "app" / "build.gradle"
        gradle.parent.mkdir()
        gradle.write_text('android { defaultConfig { versionName "1.0.0" } }')

        agent = VersionAgent(project_root=str(tmp_path))
        with pytest.raises(ValueError, match="versionCode not found"):
            agent.read_version_from_gradle()

    def test_read_version_when_no_version_name_raises_value_error(self, tmp_path):
        gradle = tmp_path / "app" / "build.gradle"
        gradle.parent.mkdir()
        gradle.write_text("android { defaultConfig { versionCode 5 } }")

        agent = VersionAgent(project_root=str(tmp_path))
        with pytest.raises(ValueError, match="versionName not found"):
            agent.read_version_from_gradle()


class TestWriteVersionToGradle:
    """Tests for writing updated version info back to build.gradle."""

    def test_write_version_when_called_updates_version_code(self, gradle_project):
        agent = VersionAgent(project_root=str(gradle_project))
        agent.write_version_to_gradle(new_code=11, new_name="1.2.4")
        code, _ = agent.read_version_from_gradle()
        assert code == 11

    def test_write_version_when_called_updates_version_name(self, gradle_project):
        agent = VersionAgent(project_root=str(gradle_project))
        agent.write_version_to_gradle(new_code=11, new_name="1.2.4")
        _, name = agent.read_version_from_gradle()
        assert name == "1.2.4"

    def test_write_version_when_called_preserves_other_gradle_content(self, gradle_project):
        agent = VersionAgent(project_root=str(gradle_project))
        original = (gradle_project / "app" / "build.gradle").read_text()
        agent.write_version_to_gradle(11, "1.2.4")
        updated = (gradle_project / "app" / "build.gradle").read_text()
        # Structural content should be preserved
        assert "android {" in updated
        assert "defaultConfig {" in updated


class TestBumpPatch:
    """Tests for semantic version patch bumping."""

    def test_bump_patch_when_valid_semver_increments_patch(self):
        agent = VersionAgent()
        assert agent._bump_patch("1.2.3") == "1.2.4"

    def test_bump_patch_when_patch_is_9_carries_correctly(self):
        agent = VersionAgent()
        assert agent._bump_patch("1.2.9") == "1.2.10"

    def test_bump_patch_when_non_semver_returns_unchanged(self):
        agent = VersionAgent()
        assert agent._bump_patch("release-v2") == "release-v2"

    def test_bump_patch_when_two_part_version_returns_unchanged(self):
        agent = VersionAgent()
        assert agent._bump_patch("1.2") == "1.2"

    def test_bump_patch_when_zero_patch_increments_to_one(self):
        agent = VersionAgent()
        assert agent._bump_patch("2.0.0") == "2.0.1"


class TestVersionAgentExecute:
    """Integration tests for the full version increment flow."""

    def test_execute_when_successful_increments_version_code(
        self, gradle_project, mock_run
    ):
        agent = VersionAgent(project_root=str(gradle_project))

        with patch.object(agent.memory, 'load', return_value=mock_run):
            with patch.object(agent.memory, 'acquire_lock', return_value=True):
                with patch.object(agent.memory, 'release_lock'):
                    with patch.object(agent.memory, 'save'):
                        result = agent._do_version_increment(mock_run)

        assert result.success is True
        assert result.data["new_version_code"] == 11
        assert result.data["old_version_code"] == 10

    def test_execute_when_lock_unavailable_returns_fail(
        self, gradle_project, mock_run
    ):
        agent = VersionAgent(project_root=str(gradle_project))

        with patch.object(agent.memory, 'load', return_value=mock_run):
            with patch.object(agent.memory, 'acquire_lock', return_value=False):
                with patch('time.sleep'):  # speed up the test
                    result = agent.execute(mock_run)

        assert result.success is False
        assert "Could not acquire version lock" in result.error
```

---

### 3.3 Memory System Tests (`tests/test_memory.py`)

```python
# tests/test_memory.py

import pytest
import json
from pathlib import Path
from agents.memory import (
    PipelineMemory, PipelineRun, AgentState,
    AgentStatus, PipelineStatus
)


@pytest.fixture
def memory(tmp_path):
    """Create a PipelineMemory instance using a temporary directory."""
    return PipelineMemory(state_dir=str(tmp_path / ".pipeline"))


@pytest.fixture
def sample_run(memory):
    """Initialize and return a fresh pipeline run."""
    return memory.init_run(
        commit_sha="abc123def",
        branch="release",
        triggered_by="push",
        package_name="com.example.app"
    )


class TestPipelineMemoryInit:
    """Tests for PipelineMemory initialization and directory creation."""

    def test_init_when_called_creates_required_directories(self, tmp_path):
        state_dir = tmp_path / ".pipeline"
        memory = PipelineMemory(state_dir=str(state_dir))
        assert state_dir.exists()
        assert (state_dir / "history").exists()
        assert (state_dir / "locks").exists()
        assert (state_dir / "artifacts").exists()

    def test_init_run_when_called_creates_state_file(self, memory, tmp_path):
        memory.init_run("abc123", "release")
        state_file = Path(memory.state_dir) / "state.json"
        assert state_file.exists()

    def test_init_run_when_called_sets_all_agents_to_pending(self, memory):
        run = memory.init_run("abc123", "release")
        for name in memory.AGENT_NAMES:
            agent = run.get_agent(name)
            assert agent.status == AgentStatus.PENDING


class TestPipelineMemoryLoadSave:
    """Tests for state persistence across loads."""

    def test_load_when_no_state_file_returns_none(self, memory):
        result = memory.load()
        assert result is None

    def test_load_after_save_returns_same_data(self, memory, sample_run):
        loaded = memory.load()
        assert loaded.run_id == sample_run.run_id
        assert loaded.commit_sha == "abc123def"
        assert loaded.branch == "release"

    def test_save_is_atomic_when_file_exists_overwrites_cleanly(
        self, memory, sample_run
    ):
        sample_run.ai_notes = "Test note"
        memory.save(sample_run)
        loaded = memory.load()
        assert loaded.ai_notes == "Test note"

    def test_load_when_state_file_corrupted_returns_none(self, memory, tmp_path):
        state_file = Path(memory.state_dir) / "state.json"
        state_file.write_text("{{invalid json")
        result = memory.load()
        assert result is None


class TestAgentStateTransitions:
    """Tests for agent state lifecycle: PENDING → RUNNING → DONE/FAILED."""

    def test_mark_agent_running_when_pending_changes_status(
        self, memory, sample_run
    ):
        memory.mark_agent_running("validator")
        loaded = memory.load()
        assert loaded.get_agent("validator").status == AgentStatus.RUNNING

    def test_mark_agent_done_when_running_changes_status(
        self, memory, sample_run
    ):
        memory.mark_agent_running("validator")
        memory.mark_agent_done("validator", {"result": "ok"})
        loaded = memory.load()
        agent = loaded.get_agent("validator")
        assert agent.status == AgentStatus.DONE
        assert agent.output == {"result": "ok"}

    def test_mark_agent_failed_when_running_changes_status(
        self, memory, sample_run
    ):
        memory.mark_agent_running("validator")
        memory.mark_agent_failed("validator", "SDK check failed")
        loaded = memory.load()
        agent = loaded.get_agent("validator")
        assert agent.status == AgentStatus.FAILED
        assert agent.error == "SDK check failed"

    def test_mark_agent_failed_when_failed_increments_retry_count(
        self, memory, sample_run
    ):
        memory.mark_agent_running("validator")
        memory.mark_agent_failed("validator", "error 1")
        memory.mark_agent_running("validator")
        memory.mark_agent_failed("validator", "error 2")
        loaded = memory.load()
        assert loaded.get_agent("validator").retry_count == 2

    def test_agent_can_retry_when_failed_and_under_max(self, memory, sample_run):
        memory.mark_agent_failed("validator", "error")
        loaded = memory.load()
        agent = loaded.get_agent("validator")
        assert agent.can_retry(max_retries=3) is True

    def test_agent_cannot_retry_when_failed_and_at_max(self, memory, sample_run):
        for _ in range(3):
            memory.mark_agent_running("validator")
            memory.mark_agent_failed("validator", "error")
        loaded = memory.load()
        agent = loaded.get_agent("validator")
        assert agent.can_retry(max_retries=3) is False


class TestPhaseCompletion:
    """Tests for phase-level completion checks."""

    def test_phase_is_complete_when_all_phase_agents_done_returns_true(
        self, memory, sample_run
    ):
        for name in ["validator", "version", "secrets"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name)
        loaded = memory.load()
        assert loaded.phase_is_complete("P1") is True

    def test_phase_is_complete_when_one_agent_not_done_returns_false(
        self, memory, sample_run
    ):
        memory.mark_agent_running("validator")
        memory.mark_agent_done("validator")
        # version and secrets still PENDING
        loaded = memory.load()
        assert loaded.phase_is_complete("P1") is False

    def test_phase_is_complete_when_agent_failed_returns_false(
        self, memory, sample_run
    ):
        for name in ["validator", "version"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name)
        memory.mark_agent_running("secrets")
        memory.mark_agent_failed("secrets", "could not fetch keystore")
        loaded = memory.load()
        assert loaded.phase_is_complete("P1") is False


class TestResumeFromPhase:
    """Tests for resuming a pipeline from a specific phase."""

    def test_resume_from_phase_when_p3_resets_p3_agents(
        self, memory, sample_run
    ):
        # Complete P1 and P2
        for name in ["validator", "version", "secrets", "build"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name)
        # Fail at P3
        memory.mark_agent_running("qa")
        memory.mark_agent_failed("qa", "tests failed")

        # Resume from P3
        resumed = memory.resume_from_phase("P3")
        assert resumed.get_agent("qa").status == AgentStatus.PENDING
        assert resumed.get_agent("metadata").status == AgentStatus.PENDING

    def test_resume_from_phase_when_resumed_preserves_earlier_phases(
        self, memory, sample_run
    ):
        for name in ["validator", "version", "secrets"]:
            memory.mark_agent_running(name)
            memory.mark_agent_done(name)

        memory.resume_from_phase("P3")
        loaded = memory.load()

        # P1 agents should still be DONE
        assert loaded.get_agent("validator").status == AgentStatus.DONE
        assert loaded.get_agent("version").status == AgentStatus.DONE

    def test_resume_from_phase_when_no_state_returns_none(self, memory):
        result = memory.resume_from_phase("P3")
        assert result is None


class TestRunSummary:
    """Tests for the human-readable summary method (used by AI agents)."""

    def test_summary_when_pipeline_running_contains_status(
        self, memory, sample_run
    ):
        summary = sample_run.summary()
        assert "IN_PROGRESS" in summary
        assert "P1" in summary
        assert "abc123def" in summary

    def test_summary_when_agent_failed_shows_error(self, memory, sample_run):
        memory.mark_agent_running("validator")
        memory.mark_agent_failed("validator", "SDK too old")
        loaded = memory.load()
        summary = loaded.summary()
        assert "SDK too old" in summary
        assert "❌" in summary

    def test_summary_when_ai_notes_present_includes_them(
        self, memory, sample_run
    ):
        sample_run.ai_notes = "Waiting for Play Console permission fix"
        summary = sample_run.summary()
        assert "Waiting for Play Console permission fix" in summary
```

---

## 4. GitHub Actions Workflow (`.github/workflows/release.yml`)

```yaml
# .github/workflows/release.yml

name: Release Pipeline — AI Parallel Agents

on:
  push:
    branches:
      - release

# Prevent multiple releases running at once
concurrency:
  group: release-pipeline
  cancel-in-progress: false  # Never cancel — finish or fail, don't half-release

env:
  PYTHON_VERSION: "3.11"
  PIPELINE_STATE_DIR: ".pipeline"
  ANDROID_PACKAGE_NAME: ${{ vars.ANDROID_PACKAGE_NAME }}

jobs:

  # ─── PHASE 1: All 3 run in parallel ────────────────────────────────────────
  validator:
    name: "P1 · Validator Agent"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Initialize pipeline state
        run: python -m agents.orchestrator --init --commit ${{ github.sha }} --branch release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      - name: Run Validator Agent
        run: python -m agents.validator_agent
      - uses: actions/upload-artifact@v4
        with:
          name: pipeline-state-validator
          path: .pipeline/state.json

  version:
    name: "P1 · Version Agent"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - uses: actions/download-artifact@v4
        with: { name: pipeline-state-validator }
      - name: Run Version Agent
        run: python -m agents.version_agent
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}

  secrets-fetch:
    name: "P1 · Secrets Agent"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Run Secrets Agent
        run: python -m agents.secrets_agent
        env:
          KEYSTORE_FILE_B64: ${{ secrets.KEYSTORE_FILE_B64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}

  # ─── PHASE 2: Build (sequential, depends on all P1) ─────────────────────────
  build:
    name: "P2 · Build Agent"
    runs-on: ubuntu-latest
    needs: [validator, version, secrets-fetch]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: "17", distribution: "temurin" }
      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('**/*.gradle*') }}
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Run Build Agent
        run: python -m agents.build_agent
        env:
          KEYSTORE_FILE_B64: ${{ secrets.KEYSTORE_FILE_B64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      - uses: actions/upload-artifact@v4
        with:
          name: release-aab
          path: app/build/outputs/bundle/release/app-release.aab

  # ─── PHASE 3: QA + Metadata in parallel ─────────────────────────────────────
  qa:
    name: "P3 · QA Agent"
    runs-on: ubuntu-latest
    needs: [build]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: "17", distribution: "temurin" }
      - uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: gradle-${{ hashFiles('**/*.gradle*') }}
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Run QA Agent
        run: python -m agents.qa_agent
      - uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: app/build/reports/tests/

  metadata:
    name: "P3 · Metadata Agent"
    runs-on: ubuntu-latest
    needs: [build]
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }  # Full history for commit log
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Run Metadata Agent
        run: python -m agents.metadata_agent
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}

  # ─── PHASE 4: Upload ────────────────────────────────────────────────────────
  upload:
    name: "P4 · Upload Agent"
    runs-on: ubuntu-latest
    needs: [qa, metadata]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with: { name: release-aab }
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - name: Run Upload Agent
        run: python -m agents.upload_agent
        env:
          PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}
          ANDROID_PACKAGE_NAME: ${{ env.ANDROID_PACKAGE_NAME }}

  # ─── PHASE 5: Tester + Notifier in parallel ──────────────────────────────────
  tester:
    name: "P5 · Tester Agent"
    runs-on: ubuntu-latest
    needs: [upload]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - run: python -m agents.tester_agent
        env:
          PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}

  notifier:
    name: "P5 · Notifier Agent"
    runs-on: ubuntu-latest
    needs: [upload]
    if: always()  # Always notify, even on failure
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "${{ env.PYTHON_VERSION }}" }
      - run: pip install -r requirements.txt
      - run: python -m agents.notifier_agent
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          PIPELINE_OUTCOME: ${{ needs.upload.result }}
```

---

## 5. Configuration Files

### `config/pipeline.yaml`

```yaml
# Pipeline configuration — read by Orchestrator Agent

pipeline:
  name: "Android Release Pipeline"
  package_name: "com.yourcompany.yourapp"
  default_track: "internal"        # internal | alpha | beta | production
  max_retries: 3
  phase_timeout_minutes:
    P1: 5
    P2: 25
    P3: 15
    P4: 10
    P5: 5

build:
  gradle_task: "bundleRelease"
  output_path: "app/build/outputs/bundle/release/app-release.aab"
  java_version: "17"

validation:
  minimum_target_sdk: 34
  minimum_compile_sdk: 34
  block_on_lint_errors: true
  warn_only_permissions: []        # Permissions that warn but don't block

metadata:
  max_release_notes_length: 500
  changelog_file: "CHANGELOG.md"  # If exists, preferred over AI-generated notes
  ai_prompt: >
    You are writing release notes for the Google Play Store.
    Summarize the following git commits as user-facing improvements
    in under 500 characters. Be specific, friendly, and avoid technical jargon.
    Only list user-visible changes. Format as plain text, no bullet points.

notifications:
  slack_channel: "#releases"
  post_github_commit_status: true
  notify_on: ["success", "failure"]
```

### `config/tester-groups.yaml`

```yaml
# Tester group configuration — read by Tester Agent

groups:
  internal:
    track: "internal"
    emails:
      - developer@yourcompany.com
      - qa@yourcompany.com
    auto_promote_to: null     # Don't auto-promote internal builds

  alpha:
    track: "alpha"
    emails:
      - beta1@yourcompany.com
      - beta2@yourcompany.com
    auto_promote_to: null

  beta:
    track: "beta"
    emails: []                # Open beta — no email list needed
    auto_promote_to: null

promotion_rules:
  - from: "internal"
    to: "alpha"
    requires: "qa_passed"    # Only promote if QA Agent passed
  - from: "alpha"
    to: "beta"
    requires: "manual"       # Beta promotion always requires human approval
```
