# DEVELOPMENT.md — Code Standards, Testing Protocol & AI Code Generation Rules

> **AI AGENT READING THIS:** This file defines exactly how you must write code
> for this project. Read every section before generating any code.
> These rules are not suggestions — they are requirements.
>
> When a human says "write the metadata agent" or "fix the upload agent",
> you must follow all rules in this file. No exceptions.

---

## 1. The Golden Rule: Memory First, Code Second

Before writing ANY code for this project, you must:

```python
# Step 1: Always load state first
from agents.memory import PipelineMemory
memory = PipelineMemory()
state = memory.load()

# Step 2: Understand where you are
if state:
    print(state.summary())
else:
    print("No active run. Use memory.init_run() to start one.")

# Step 3: Leave notes so you remember next session
memory.set_ai_notes(
    "Working on MetadataAgent. Claude API call is complete. "
    "Next: validate output character count before saving to metadata.json."
)
```

Never assume you know the state. Always read it. Always write it.

---

## 2. File Generation Rules

### Every new agent file must follow this template:

```python
# agents/<name>_agent.py
#
# AGENT: <AgentName>
# PHASE: <P1|P2|P3|P4|P5>
# RUNS IN PARALLEL: <Yes|No>
# DEPENDS ON: <list of agents that must complete before this one>
# WRITES TO STATE: <list of fields this agent writes to PipelineRun>
#
# AI AGENT: Before modifying this file, read the agent's test file at
# tests/test_<name>_agent.py to understand expected behavior.
# After modifying, run: pytest tests/test_<name>_agent.py -v

import os
from pathlib import Path
from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineRun


class <Name>Agent(BaseAgent):
    """
    One paragraph describing what this agent does.
    
    AI NOTES:
    - <Any gotchas or non-obvious behavior>
    - <Known failure modes and how to handle them>
    - <Links to external docs if relevant>
    """

    def __init__(self):
        super().__init__("<name>")

    def execute(self, run: PipelineRun) -> AgentResult:
        """
        Core logic. Return AgentResult.ok() or AgentResult.fail().
        Always call self.log() for important steps.
        Always call self.memory.save(run) after modifying run state.
        """
        pass
```

### Every agent must have a corresponding test file:

```
agents/metadata_agent.py   → tests/test_metadata_agent.py
agents/upload_agent.py     → tests/test_upload_agent.py
# etc.
```

If you create a new helper function anywhere, you must add tests for it
in the corresponding test file before committing.

---

## 3. Test Writing Protocol

### Rule 1: Test every function, every condition

For every function you write, write tests for:
- ✅ The happy path (expected input → expected output)
- ❌ The failure path (bad input → expected error)
- 🔀 Edge cases (empty string, None, zero, max value)

### Rule 2: Test naming convention

```python
# Pattern: test_<function>_when_<condition>_<expected_result>

# Good
def test_read_version_when_valid_gradle_returns_code_and_name(): ...
def test_read_version_when_file_missing_raises_file_not_found(): ...
def test_bump_patch_when_non_semver_returns_unchanged(): ...

# Bad — too vague
def test_version(): ...
def test_it_works(): ...
def test_error_case(): ...
```

### Rule 3: Test structure — Arrange, Act, Assert

Every test must be readable without knowing the implementation:

```python
def test_mark_agent_failed_when_running_increments_retry_count(memory, sample_run):
    # Arrange
    memory.mark_agent_running("validator")
    
    # Act
    memory.mark_agent_failed("validator", "SDK check failed")
    
    # Assert
    loaded = memory.load()
    agent = loaded.get_agent("validator")
    assert agent.retry_count == 1
    assert agent.error == "SDK check failed"
```

### Rule 4: Use fixtures, not setup/teardown

```python
# Good — pytest fixtures are composable
@pytest.fixture
def memory(tmp_path):
    return PipelineMemory(state_dir=str(tmp_path / ".pipeline"))

@pytest.fixture
def sample_run(memory):
    return memory.init_run("abc123", "release")

# Tests can use both
def test_something(memory, sample_run):
    ...

# Bad — class-based setup
class TestSomething:
    def setUp(self):
        self.memory = PipelineMemory(...)  # Don't do this
```

### Rule 5: Mock external services, never call real APIs in tests

```python
# Always mock these:
# - subprocess calls (Gradle, lint)
# - HTTP calls (Play Console API, Slack, Claude API)
# - File system calls that would need real Android project
# - Environment variables

from unittest.mock import patch, MagicMock

def test_upload_agent_when_api_returns_403_returns_fail():
    agent = UploadAgent()
    
    mock_response = MagicMock()
    mock_response.status_code = 403
    mock_response.json.return_value = {"error": "forbidden"}
    
    with patch("requests.post", return_value=mock_response):
        result = agent._upload_aab("fake.aab", "com.example.app")
    
    assert result.success is False
    assert "403" in result.error
```

### Rule 6: Test the state machine, not just the logic

Because this system persists state, test that state transitions happen correctly:

```python
def test_build_agent_when_gradle_fails_writes_failed_to_state(memory, sample_run):
    agent = BuildAgent()
    
    with patch("subprocess.run") as mock_sub:
        mock_sub.return_value.returncode = 1
        mock_sub.return_value.stderr = "FAILED: Task ':app:bundleRelease'"
        agent.run()
    
    loaded = memory.load()
    assert loaded.get_agent("build").status == AgentStatus.FAILED
    assert "bundleRelease" in loaded.get_agent("build").error
```

---

## 4. Complete Agent Implementations

### Metadata Agent (`agents/metadata_agent.py`)

```python
# agents/metadata_agent.py
#
# AGENT: MetadataAgent
# PHASE: P3
# RUNS IN PARALLEL: Yes (with QA Agent)
# DEPENDS ON: build
# WRITES TO STATE: nothing (writes to .pipeline/artifacts/metadata.json)
#
# AI AGENT: This agent uses the Anthropic Claude API to generate release notes.
# The API key is in ANTHROPIC_API_KEY env var.
# If CHANGELOG.md exists in the project root, it is ALWAYS preferred over AI-generated notes.
# Character limit for Play Store release notes: 500 chars per locale.

import os
import json
import subprocess
import anthropic
from pathlib import Path
from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineRun


class MetadataAgent(BaseAgent):
    """
    Generates store listing metadata and release notes.
    
    Uses git commit history and Claude API to generate
    human-readable release notes automatically.
    
    AI NOTES:
    - Always check for CHANGELOG.md first — human notes > AI notes
    - Play Store limit is 500 chars. Validate before saving.
    - The Claude API prompt is configurable in config/pipeline.yaml
    - Output goes to .pipeline/artifacts/metadata.json for Upload Agent
    """

    MAX_RELEASE_NOTES_CHARS = 500
    DEFAULT_RELEASE_NOTES = "Bug fixes and performance improvements."

    def __init__(self, project_root: str = "."):
        super().__init__("metadata")
        self.project_root = Path(project_root)
        self.output_path = Path(
            os.getenv("PIPELINE_STATE_DIR", ".pipeline")
        ) / "artifacts" / "metadata.json"

    def execute(self, run: PipelineRun) -> AgentResult:
        self.log("Generating release metadata")

        release_notes = self._get_release_notes(run)
        self.log(f"Release notes ({len(release_notes)} chars): {release_notes[:80]}...")

        metadata = {
            "package_name": run.package_name,
            "version_code": run.version_code,
            "version_name": run.version_name,
            "release_notes": {"en-US": release_notes},
            "track": run.play_store_track,
        }

        self.output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.output_path, "w") as f:
            json.dump(metadata, f, indent=2)

        self.log(f"Metadata written to {self.output_path}")
        return AgentResult.ok({"metadata_path": str(self.output_path)})

    def _get_release_notes(self, run: PipelineRun) -> str:
        """
        Get release notes. Priority:
        1. CHANGELOG.md (human-written, always preferred)
        2. Claude API (AI-generated from git commits)
        3. Default fallback
        """
        # Priority 1: CHANGELOG.md
        changelog = self.project_root / "CHANGELOG.md"
        if changelog.exists():
            notes = self._parse_changelog(changelog.read_text())
            if notes:
                self.log("Using CHANGELOG.md for release notes")
                return self._truncate(notes)

        # Priority 2: AI-generated from git commits
        commits = self._get_recent_commits(run.commit_sha)
        if commits and os.getenv("ANTHROPIC_API_KEY"):
            try:
                notes = self._generate_with_claude(commits)
                if notes:
                    self.log("Using Claude API-generated release notes")
                    return self._truncate(notes)
            except Exception as e:
                self.log(f"WARNING: Claude API failed: {e}. Using fallback.")

        # Priority 3: Fallback
        self.log("Using default fallback release notes")
        return self.DEFAULT_RELEASE_NOTES

    def _parse_changelog(self, content: str) -> str:
        """Extract the most recent version section from CHANGELOG.md."""
        lines = content.split("\n")
        section_lines = []
        in_section = False

        for line in lines:
            if line.startswith("## ") and not in_section:
                in_section = True
                continue
            elif line.startswith("## ") and in_section:
                break
            elif in_section:
                section_lines.append(line)

        return "\n".join(section_lines).strip()

    def _get_recent_commits(self, since_sha: str) -> list[str]:
        """Get commit messages since the last release tag."""
        try:
            result = subprocess.run(
                ["git", "log", "--oneline", "-20", "--no-merges"],
                capture_output=True, text=True, timeout=30,
                cwd=self.project_root
            )
            if result.returncode == 0:
                return [line.strip() for line in result.stdout.strip().split("\n") if line]
        except Exception as e:
            self.log(f"Could not get git log: {e}")
        return []

    def _generate_with_claude(self, commits: list[str]) -> str:
        """Use Claude API to generate release notes from commits."""
        client = anthropic.Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))

        commits_text = "\n".join(commits)
        prompt = (
            f"You are writing release notes for the Google Play Store. "
            f"Summarize these git commits as user-facing improvements in under "
            f"{self.MAX_RELEASE_NOTES_CHARS} characters. "
            f"Be specific and friendly. Avoid technical jargon. No bullet points.\n\n"
            f"Commits:\n{commits_text}"
        )

        message = client.messages.create(
            model="claude-sonnet-4-5-20250929",
            max_tokens=256,
            messages=[{"role": "user", "content": prompt}]
        )
        return message.content[0].text.strip()

    def _truncate(self, text: str) -> str:
        """Truncate to Play Store character limit with graceful ending."""
        if len(text) <= self.MAX_RELEASE_NOTES_CHARS:
            return text
        truncated = text[:self.MAX_RELEASE_NOTES_CHARS - 3]
        last_space = truncated.rfind(" ")
        if last_space > self.MAX_RELEASE_NOTES_CHARS - 50:
            truncated = truncated[:last_space]
        return truncated + "..."
```

### Test: `tests/test_metadata_agent.py`

```python
# tests/test_metadata_agent.py

import json
import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock
from agents.metadata_agent import MetadataAgent
from agents.memory import PipelineRun


@pytest.fixture
def mock_run():
    run = MagicMock(spec=PipelineRun)
    run.package_name = "com.example.app"
    run.version_code = 42
    run.version_name = "2.1.0"
    run.play_store_track = "internal"
    run.commit_sha = "abc123"
    return run


@pytest.fixture
def agent(tmp_path):
    a = MetadataAgent(project_root=str(tmp_path))
    a.output_path = tmp_path / ".pipeline" / "artifacts" / "metadata.json"
    return a, tmp_path


class TestParseChangelog:
    """Tests for CHANGELOG.md parsing."""

    def test_parse_changelog_when_standard_format_returns_latest_section(
        self, agent
    ):
        a, _ = agent
        content = """# Changelog

## 2.1.0
Fixed login screen bug.
Improved performance on older devices.

## 2.0.0
Initial release.
"""
        result = a._parse_changelog(content)
        assert "Fixed login screen bug" in result
        assert "2.0.0" not in result
        assert "Initial release" not in result

    def test_parse_changelog_when_empty_file_returns_empty_string(self, agent):
        a, _ = agent
        result = a._parse_changelog("")
        assert result == ""

    def test_parse_changelog_when_no_sections_returns_empty_string(self, agent):
        a, _ = agent
        result = a._parse_changelog("Just some random text without headers")
        assert result == ""

    def test_parse_changelog_when_single_section_returns_all_content(self, agent):
        a, _ = agent
        content = "## 1.0.0\nFirst release\nWith multiple lines"
        result = a._parse_changelog(content)
        assert "First release" in result
        assert "multiple lines" in result


class TestTruncate:
    """Tests for release notes character truncation."""

    def test_truncate_when_under_limit_returns_unchanged(self, agent):
        a, _ = agent
        short_text = "This is a short release note."
        assert a._truncate(short_text) == short_text

    def test_truncate_when_over_limit_returns_within_limit(self, agent):
        a, _ = agent
        long_text = "word " * 200  # Way over 500 chars
        result = a._truncate(long_text)
        assert len(result) <= a.MAX_RELEASE_NOTES_CHARS

    def test_truncate_when_over_limit_ends_with_ellipsis(self, agent):
        a, _ = agent
        long_text = "a " * 300
        result = a._truncate(long_text)
        assert result.endswith("...")

    def test_truncate_when_exactly_at_limit_returns_unchanged(self, agent):
        a, _ = agent
        exact = "a" * a.MAX_RELEASE_NOTES_CHARS
        result = a._truncate(exact)
        assert result == exact


class TestGetReleaseNotes:
    """Tests for the release notes priority chain."""

    def test_get_release_notes_when_changelog_exists_uses_changelog(
        self, agent, mock_run
    ):
        a, tmp_path = agent
        changelog = tmp_path / "CHANGELOG.md"
        changelog.write_text("## 2.1.0\nFixed bug in login flow.")

        result = a._get_release_notes(mock_run)

        assert "Fixed bug in login flow" in result

    def test_get_release_notes_when_no_changelog_and_no_api_key_uses_default(
        self, agent, mock_run
    ):
        a, _ = agent

        with patch.dict("os.environ", {}, clear=True):
            with patch.object(a, "_get_recent_commits", return_value=["abc fix bug"]):
                result = a._get_release_notes(mock_run)

        assert result == a.DEFAULT_RELEASE_NOTES

    def test_get_release_notes_when_claude_api_fails_uses_default(
        self, agent, mock_run
    ):
        a, _ = agent

        with patch.dict("os.environ", {"ANTHROPIC_API_KEY": "test-key"}):
            with patch.object(a, "_get_recent_commits", return_value=["abc fix"]):
                with patch.object(a, "_generate_with_claude", side_effect=Exception("API error")):
                    result = a._get_release_notes(mock_run)

        assert result == a.DEFAULT_RELEASE_NOTES

    def test_get_release_notes_when_claude_api_succeeds_uses_ai_notes(
        self, agent, mock_run
    ):
        a, _ = agent

        with patch.dict("os.environ", {"ANTHROPIC_API_KEY": "test-key"}):
            with patch.object(a, "_get_recent_commits", return_value=["abc123 fix login"]):
                with patch.object(a, "_generate_with_claude", return_value="Fixed login issue"):
                    result = a._get_release_notes(mock_run)

        assert result == "Fixed login issue"


class TestMetadataAgentExecute:
    """Integration tests for the full execute() flow."""

    def test_execute_when_successful_writes_metadata_json(
        self, agent, mock_run
    ):
        a, tmp_path = agent

        with patch.object(a, "_get_release_notes", return_value="Bug fixes"):
            result = a.execute(mock_run)

        assert result.success is True
        assert a.output_path.exists()

        with open(a.output_path) as f:
            metadata = json.load(f)

        assert metadata["package_name"] == "com.example.app"
        assert metadata["version_code"] == 42
        assert metadata["release_notes"]["en-US"] == "Bug fixes"

    def test_execute_when_output_dir_missing_creates_it(
        self, agent, mock_run
    ):
        a, tmp_path = agent
        # Ensure directory doesn't exist yet
        assert not a.output_path.parent.exists()

        with patch.object(a, "_get_release_notes", return_value="Update"):
            a.execute(mock_run)

        assert a.output_path.parent.exists()
```

---

### Upload Agent (`agents/upload_agent.py`)

```python
# agents/upload_agent.py
#
# AGENT: UploadAgent
# PHASE: P4
# RUNS IN PARALLEL: No
# DEPENDS ON: qa, metadata
# WRITES TO STATE: nothing (upload result logged to pipeline state)
#
# AI AGENT CRITICAL NOTES:
# - The first ever upload for a NEW app must be done manually in Play Console.
#   This agent handles UPDATES only (re-uploads to existing app).
# - If you get 403: service account needs "Release manager" permission in Play Console.
# - If you get "Version code already used": Version Agent didn't run. Check state.
# - Uses Fastlane Supply under the hood — see fastlane/Fastfile for lane config.
# - Reads AAB path from .pipeline/artifacts/ or from GitHub Actions artifact.

import os
import json
import subprocess
from pathlib import Path
from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineRun


class UploadAgent(BaseAgent):
    """
    Uploads the signed AAB to Google Play Console.
    
    Handles the case where the app doesn't exist yet (first-time upload
    scenario) with a clear error message rather than a cryptic API failure.
    """

    def __init__(self):
        super().__init__("upload")
        self.aab_path = Path(
            os.getenv("AAB_PATH",
                       "app/build/outputs/bundle/release/app-release.aab")
        )
        self.metadata_path = Path(
            os.getenv("PIPELINE_STATE_DIR", ".pipeline")
        ) / "artifacts" / "metadata.json"

    def execute(self, run: PipelineRun) -> AgentResult:
        self.log("Starting Play Store upload")

        # Validate AAB exists
        if not self.aab_path.exists():
            return AgentResult.fail(
                f"AAB not found at {self.aab_path}. "
                "Did the Build Agent complete successfully?"
            )

        # Load metadata
        metadata = self._load_metadata()
        if not metadata:
            return AgentResult.fail(
                f"metadata.json not found at {self.metadata_path}. "
                "Did the Metadata Agent complete successfully?"
            )

        # Write Play Store JSON key to temp file
        json_key_path = self._write_json_key()
        if not json_key_path:
            return AgentResult.fail(
                "PLAY_STORE_JSON_KEY environment variable is not set. "
                "Add it to GitHub Secrets."
            )

        try:
            result = self._run_fastlane_supply(
                package_name=run.package_name or metadata.get("package_name"),
                track=metadata.get("track", "internal"),
                release_notes=metadata.get("release_notes", {}),
                json_key_path=json_key_path,
            )
            return result
        finally:
            # Always clean up the key file
            Path(json_key_path).unlink(missing_ok=True)

    def _load_metadata(self) -> dict:
        """Load metadata.json written by Metadata Agent."""
        if not self.metadata_path.exists():
            return {}
        try:
            with open(self.metadata_path) as f:
                return json.load(f)
        except json.JSONDecodeError:
            return {}

    def _write_json_key(self) -> str:
        """Write Play Store JSON key from env var to a temp file."""
        key_content = os.getenv("PLAY_STORE_JSON_KEY")
        if not key_content:
            return None

        key_path = "/tmp/play_store_key.json"
        # Handle both inline JSON and file path
        if key_content.startswith("{"):
            with open(key_path, "w") as f:
                f.write(key_content)
        else:
            # It's a file path
            return key_content

        return key_path

    def _run_fastlane_supply(self, package_name: str, track: str,
                              release_notes: dict, json_key_path: str) -> AgentResult:
        """Execute Fastlane supply to upload the AAB."""
        env = {
            **os.environ,
            "SUPPLY_AAB": str(self.aab_path),
            "SUPPLY_PACKAGE_NAME": package_name,
            "SUPPLY_TRACK": track,
            "SUPPLY_JSON_KEY": json_key_path,
        }

        # Write release notes to fastlane metadata directory
        self._write_fastlane_release_notes(release_notes)

        result = subprocess.run(
            ["fastlane", "supply",
             "--aab", str(self.aab_path),
             "--package_name", package_name,
             "--track", track,
             "--json_key", json_key_path,
             "--skip_upload_apk"],
            capture_output=True, text=True, timeout=300, env=env
        )

        if result.returncode == 0:
            self.log("Upload successful")
            return AgentResult.ok({
                "track": track,
                "package_name": package_name,
                "aab_path": str(self.aab_path),
            })

        error = self._parse_fastlane_error(result.stderr + result.stdout)
        return AgentResult.fail(error)

    def _write_fastlane_release_notes(self, release_notes: dict) -> None:
        """Write release notes to Fastlane metadata directory."""
        for locale, notes in release_notes.items():
            notes_dir = Path("fastlane/metadata/android") / locale
            notes_dir.mkdir(parents=True, exist_ok=True)
            (notes_dir / "changelogs" / "default.txt").parent.mkdir(
                parents=True, exist_ok=True
            )
            (notes_dir / "changelogs" / "default.txt").write_text(notes)

    def _parse_fastlane_error(self, output: str) -> str:
        """Parse Fastlane/Play Store error output into a human-readable message."""
        if "Version code has already been used" in output:
            return (
                "Version code already used. The Version Agent may not have run. "
                "Resume from P1: python -m agents.orchestrator --resume --from-phase P1"
            )
        if "403" in output or "Forbidden" in output:
            return (
                "403 Forbidden. Service account is missing Play Console permissions. "
                "Go to Play Console → Users & Permissions → Service Accounts "
                "→ grant 'Release manager' role."
            )
        if "Package not found" in output or "does not exist" in output:
            return (
                "App not found in Play Console. The first upload must be done manually. "
                "See README.md section 'Known Limitations'."
            )
        if "401" in output or "Unauthorized" in output:
            return (
                "401 Unauthorized. The PLAY_STORE_JSON_KEY may be invalid or expired. "
                "Regenerate the service account key in Google Cloud Console."
            )
        # Return first 500 chars of raw output as fallback
        return f"Upload failed: {output[:500]}"

    def check_permissions(self) -> bool:
        """Verify Play Console API access. Call before first real run."""
        json_key_path = self._write_json_key()
        if not json_key_path:
            print("ERROR: PLAY_STORE_JSON_KEY not set")
            return False

        result = subprocess.run(
            ["fastlane", "supply", "--json_key", json_key_path,
             "--package_name",
             os.getenv("ANDROID_PACKAGE_NAME", "com.example.app"),
             "--list"],
            capture_output=True, text=True, timeout=60
        )
        Path(json_key_path).unlink(missing_ok=True)
        return result.returncode == 0
```

### Test: `tests/test_upload_agent.py`

```python
# tests/test_upload_agent.py

import json
import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock
from agents.upload_agent import UploadAgent
from agents.memory import PipelineRun


@pytest.fixture
def agent(tmp_path):
    a = UploadAgent()
    a.aab_path = tmp_path / "app-release.aab"
    a.metadata_path = tmp_path / "metadata.json"
    return a, tmp_path


@pytest.fixture
def mock_run():
    run = MagicMock(spec=PipelineRun)
    run.package_name = "com.example.app"
    run.play_store_track = "internal"
    return run


@pytest.fixture
def valid_aab(agent):
    a, tmp_path = agent
    a.aab_path.write_bytes(b"fake aab content")
    return a, tmp_path


@pytest.fixture
def valid_metadata(agent):
    a, tmp_path = agent
    metadata = {
        "package_name": "com.example.app",
        "track": "internal",
        "release_notes": {"en-US": "Bug fixes"},
        "version_code": 42,
    }
    a.metadata_path.write_text(json.dumps(metadata))
    return a, tmp_path


class TestLoadMetadata:
    """Tests for loading metadata.json."""

    def test_load_metadata_when_file_exists_returns_dict(self, valid_metadata):
        a, _ = valid_metadata
        result = a._load_metadata()
        assert result["package_name"] == "com.example.app"
        assert result["track"] == "internal"

    def test_load_metadata_when_file_missing_returns_empty_dict(self, agent):
        a, _ = agent
        result = a._load_metadata()
        assert result == {}

    def test_load_metadata_when_invalid_json_returns_empty_dict(self, agent):
        a, _ = agent
        a.metadata_path.write_text("{{invalid")
        result = a._load_metadata()
        assert result == {}


class TestParseFastlaneError:
    """Tests for Play Store error message parsing."""

    def test_parse_error_when_version_code_used_returns_clear_message(self, agent):
        a, _ = agent
        output = "Version code has already been used"
        result = a._parse_fastlane_error(output)
        assert "Version code already used" in result
        assert "Version Agent" in result

    def test_parse_error_when_403_returns_permission_message(self, agent):
        a, _ = agent
        output = "HTTPError 403 Forbidden"
        result = a._parse_fastlane_error(output)
        assert "403 Forbidden" in result
        assert "Release manager" in result

    def test_parse_error_when_package_not_found_returns_manual_upload_message(
        self, agent
    ):
        a, _ = agent
        output = "Package not found in Google Play"
        result = a._parse_fastlane_error(output)
        assert "first upload must be done manually" in result

    def test_parse_error_when_401_returns_key_error_message(self, agent):
        a, _ = agent
        output = "HTTPError 401 Unauthorized"
        result = a._parse_fastlane_error(output)
        assert "401 Unauthorized" in result
        assert "service account key" in result.lower()

    def test_parse_error_when_unknown_error_returns_truncated_output(self, agent):
        a, _ = agent
        long_output = "x" * 1000
        result = a._parse_fastlane_error(long_output)
        assert len(result) <= 520  # "Upload failed: " + 500 chars


class TestExecute:
    """Integration tests for the full execute() flow."""

    def test_execute_when_aab_missing_returns_fail_with_clear_message(
        self, agent, mock_run
    ):
        a, _ = agent
        # Don't create the AAB file
        result = a.execute(mock_run)
        assert result.success is False
        assert "AAB not found" in result.error
        assert "Build Agent" in result.error

    def test_execute_when_metadata_missing_returns_fail_with_clear_message(
        self, valid_aab, mock_run
    ):
        a, _ = valid_aab
        # AAB exists but no metadata
        result = a.execute(mock_run)
        assert result.success is False
        assert "metadata.json not found" in result.error

    def test_execute_when_json_key_missing_returns_fail_with_clear_message(
        self, valid_aab, valid_metadata, mock_run, monkeypatch
    ):
        a, _ = valid_aab
        # Merge fixtures
        a.metadata_path = valid_metadata[0].metadata_path
        a.metadata_path.write_text(
            (valid_metadata[1] / "metadata.json").read_text()
            if (valid_metadata[1] / "metadata.json").exists()
            else '{"package_name":"com.example","track":"internal","release_notes":{}}'
        )

        monkeypatch.delenv("PLAY_STORE_JSON_KEY", raising=False)
        result = a.execute(mock_run)
        assert result.success is False
        assert "PLAY_STORE_JSON_KEY" in result.error

    def test_execute_when_fastlane_succeeds_returns_ok(
        self, tmp_path, mock_run
    ):
        a = UploadAgent()
        a.aab_path = tmp_path / "app.aab"
        a.aab_path.write_bytes(b"fake")
        a.metadata_path = tmp_path / "metadata.json"
        a.metadata_path.write_text(json.dumps({
            "package_name": "com.example.app",
            "track": "internal",
            "release_notes": {"en-US": "Fixes"},
        }))

        mock_result = MagicMock()
        mock_result.returncode = 0
        mock_result.stdout = "Upload Complete"

        with patch.dict("os.environ", {"PLAY_STORE_JSON_KEY": '{"type":"service_account"}'}):
            with patch("subprocess.run", return_value=mock_result):
                with patch.object(a, "_write_fastlane_release_notes"):
                    result = a.execute(mock_run)

        assert result.success is True
        assert result.data["track"] == "internal"
```

---

## 5. Requirements (`requirements.txt`)

```
# Core
anthropic>=0.34.0
requests>=2.31.0
pyyaml>=6.0.1

# Testing
pytest>=7.4.0
pytest-cov>=4.1.0
pytest-mock>=3.11.0

# Dev utilities
python-dotenv>=1.0.0
```

---

## 6. `.gitignore` entries for pipeline state

```gitignore
# Pipeline runtime state — never commit
.pipeline/
*.jks
*.keystore
play_store_key.json
/tmp/play_store_key.json

# But DO commit config
!config/
!config/pipeline.yaml
!config/tester-groups.yaml
```

---

## 7. AI Agent Self-Check Protocol

Before finishing any coding session, an AI agent must:

```python
# 1. Run all tests
# pytest tests/ -v
# → All tests must pass. Fix failures before ending session.

# 2. Update ai_notes with what you did and what's next
memory = PipelineMemory()
memory.set_ai_notes(
    "Completed: MetadataAgent and UploadAgent with full test coverage. "
    "Next session should: implement TesterAgent (config/tester-groups.yaml) "
    "and NotifierAgent (Slack Block Kit format). "
    "Known issue: _write_fastlane_release_notes path needs project_root prefix."
)

# 3. Check that state.json is consistent
state = memory.load()
print(state.summary())

# 4. List any TODOs for the next session
print("""
NEXT SESSION TODO:
- [ ] Implement TesterAgent (agents/tester_agent.py)
- [ ] Implement NotifierAgent (agents/notifier_agent.py)
- [ ] Write tests for both
- [ ] Wire up Orchestrator to call all agents
- [ ] End-to-end dry-run test
""")
```

This ensures the next AI agent (or next session) can pick up exactly where you left off.
