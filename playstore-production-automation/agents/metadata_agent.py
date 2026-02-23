"""
Metadata Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 3 (parallel with QA Agent), after Build succeeds.
Generates release notes and metadata for the Google Play Store upload
using a priority chain: CHANGELOG.md → Claude AI → default fallback.
"""

from __future__ import annotations

import json
import logging
import os
import subprocess
from pathlib import Path
from typing import Any, Optional

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory

logger = logging.getLogger(__name__)


class MetadataAgent(BaseAgent):
    """
    Agent that generates Play Store release metadata.

    Builds a metadata.json file containing package name, version info,
    track, and release notes. Release notes are determined via a priority
    chain: CHANGELOG.md → Claude API → default text.
    """

    MAX_RELEASE_NOTES_LENGTH = 500
    DEFAULT_RELEASE_NOTES = "Bug fixes and performance improvements."
    DEFAULT_TRACK = "internal"

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        project_root: str = ".",
        max_retries: int = 3,
    ) -> None:
        """Initialize the Metadata Agent."""
        super().__init__(
            name="metadata",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )
        self.project_root = Path(project_root).resolve()

    def _parse_changelog(self, content: str) -> str:
        """
        Extract the latest version section from CHANGELOG.md content.

        Looks for the first ``## `` heading and returns all content up to
        the next ``## `` heading or end of file.

        Args:
            content: Raw CHANGELOG.md text.

        Returns:
            The text of the latest version section, or empty string.
        """
        if not content or not content.strip():
            return ""

        lines = content.strip().split("\n")
        section_lines: list[str] = []
        in_section = False

        for line in lines:
            if line.startswith("## "):
                if in_section:
                    # Hit the next section — stop
                    break
                in_section = True
                continue  # Skip the heading itself

            if in_section:
                section_lines.append(line)

        return "\n".join(section_lines).strip()

    def _get_recent_commits(self, since_sha: str | None = None) -> list[str]:
        """
        Retrieve recent commit messages from git history.

        Args:
            since_sha: If provided, only get commits after this SHA.

        Returns:
            List of one-line commit messages (max 20).
        """
        cmd = ["git", "log", "--oneline", "-20", "--no-merges"]
        if since_sha:
            cmd = ["git", "log", "--oneline", f"{since_sha}..HEAD", "--no-merges"]

        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=30,
                cwd=str(self.project_root),
            )
            if result.returncode == 0 and result.stdout.strip():
                return result.stdout.strip().split("\n")
            return []
        except (FileNotFoundError, subprocess.TimeoutExpired):
            logger.warning("Could not retrieve git log.")
            return []

    def _generate_with_claude(self, commits: list[str]) -> str:
        """
        Generate release notes using the Claude AI API.

        Args:
            commits: List of recent commit messages.

        Returns:
            AI-generated release notes, or empty string on failure.
        """
        api_key = os.getenv("ANTHROPIC_API_KEY")
        if not api_key:
            logger.info("ANTHROPIC_API_KEY not set, skipping AI generation.")
            return ""

        try:
            import anthropic

            client = anthropic.Anthropic(api_key=api_key)
            commit_text = "\n".join(commits)

            message = client.messages.create(
                model="claude-sonnet-4-20250514",
                max_tokens=300,
                messages=[
                    {
                        "role": "user",
                        "content": (
                            "Generate concise Google Play Store release notes "
                            "(max 400 characters) from these git commits. "
                            "Use bullet points, user-facing language, no "
                            "technical jargon. Do not include version numbers.\n\n"
                            f"Commits:\n{commit_text}"
                        ),
                    }
                ],
            )

            text = message.content[0].text.strip()
            logger.info("Claude generated release notes (%d chars).", len(text))
            return text

        except ImportError:
            logger.warning("anthropic package not installed.")
            return ""
        except Exception as exc:
            logger.warning("Claude API call failed: %s", exc)
            return ""

    def _truncate(self, text: str, max_length: int = 500) -> str:
        """
        Truncate text to a maximum character length at a word boundary.

        Args:
            text: Text to truncate.
            max_length: Maximum allowed characters (default 500).

        Returns:
            Truncated text ending with "..." if it was shortened,
            or the original text if within the limit.
        """
        if len(text) <= max_length:
            return text

        # Leave room for "..."
        truncated = text[: max_length - 3]
        # Break at last space to avoid cutting a word
        last_space = truncated.rfind(" ")
        if last_space > 0:
            truncated = truncated[:last_space]

        return truncated + "..."

    def _get_release_notes(self, run: Any) -> str:
        """
        Determine release notes using the priority chain.

        Priority:
            1. CHANGELOG.md → parse latest section
            2. Claude AI API → generate from commits
            3. Default fallback text

        Args:
            run: The current PipelineRun instance.

        Returns:
            Release notes string (≤500 characters).
        """
        # Priority 1: CHANGELOG.md
        changelog_path = self.project_root / "CHANGELOG.md"
        if changelog_path.exists():
            try:
                content = changelog_path.read_text(encoding="utf-8")
                notes = self._parse_changelog(content)
                if notes:
                    logger.info("Using release notes from CHANGELOG.md.")
                    return self._truncate(notes)
            except Exception as exc:
                logger.warning("Error reading CHANGELOG.md: %s", exc)

        # Priority 2: Claude AI
        commits = self._get_recent_commits()
        if commits:
            notes = self._generate_with_claude(commits)
            if notes:
                logger.info("Using AI-generated release notes.")
                return self._truncate(notes)

        # Priority 3: Default
        logger.info("Using default release notes.")
        return self.DEFAULT_RELEASE_NOTES

    def execute(self) -> AgentResult:
        """
        Execute metadata generation.

        1. Load pipeline state for version info
        2. Determine release notes via priority chain
        3. Build metadata dictionary
        4. Write to .pipeline/artifacts/metadata.json
        5. Return result with metadata
        """
        # Step 1: Load pipeline state
        run = self.memory.load()
        if run is None:
            return AgentResult.fail("No pipeline state found.")

        package_name = run.package_name or os.getenv(
            "ANDROID_PACKAGE_NAME", "com.example.app"
        )
        version_code = run.version_code or 1
        version_name = run.version_name or "1.0.0"

        # Step 2: Get release notes
        release_notes = self._get_release_notes(run)

        # Step 3: Build metadata
        # Allow CI to override target Play track (e.g., closed-testing track name).
        # Falls back to "internal" for backward compatibility.
        track = os.getenv("PLAY_TRACK", self.DEFAULT_TRACK).strip() or self.DEFAULT_TRACK
        metadata: dict[str, Any] = {
            "package_name": package_name,
            "version_code": version_code,
            "version_name": version_name,
            "track": track,
            "release_notes": {
                "en-US": release_notes,
            },
        }

        # Step 4: Write to artifacts
        output_path = self.memory.artifacts_dir / "metadata.json"
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)

        logger.info("Metadata written to %s", output_path)

        # Step 5: Return result
        return AgentResult.ok(metadata)
