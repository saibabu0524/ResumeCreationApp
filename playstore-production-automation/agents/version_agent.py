"""
Version Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 1 (parallel with Validator and Secrets agents).
Auto-increments the versionCode and versionName in build.gradle,
supporting both Groovy (.gradle) and Kotlin DSL (.gradle.kts) formats.
"""

from __future__ import annotations

import os
import re
import time
from pathlib import Path
from typing import Optional

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory


class VersionAgent(BaseAgent):
    """
    Agent that auto-increments Android app version code and name.

    Reads the current versionCode and versionName from build.gradle,
    increments them (code +1, name patch bump), writes them back,
    and stores the new values in the pipeline state.
    """

    # Regex patterns for Groovy build.gradle
    _RE_VERSION_CODE_GROOVY = re.compile(r'versionCode\s+(\d+)')
    _RE_VERSION_NAME_GROOVY = re.compile(r'versionName\s+"([^"]+)"')

    # Regex patterns for Kotlin DSL build.gradle.kts
    _RE_VERSION_CODE_KTS = re.compile(r'versionCode\s*=\s*(\d+)')
    _RE_VERSION_NAME_KTS = re.compile(r'versionName\s*=\s*"([^"]+)"')

    def __init__(
        self,
        memory: PipelineMemory,
        project_root: str = ".",
        dry_run: bool = False,
    ) -> None:
        """Initialize the VersionAgent.

        Args:
            memory: PipelineMemory instance for state persistence.
            project_root: Root directory of the Android project.
            dry_run: If True, skip actual execution.
        """
        super().__init__(name="version", memory=memory, dry_run=dry_run)
        self.project_root = project_root

    def read_version_from_gradle(
        self, gradle_path: str
    ) -> tuple[int, str]:
        """Parse build.gradle for versionCode and versionName.

        Supports both Groovy (.gradle) and Kotlin DSL (.gradle.kts) formats.

        Args:
            gradle_path: Path to the build.gradle or build.gradle.kts file.

        Returns:
            A tuple of (versionCode, versionName).

        Raises:
            FileNotFoundError: If the gradle file does not exist.
            ValueError: If versionCode or versionName cannot be found.
        """
        path = Path(gradle_path)
        if not path.exists():
            raise FileNotFoundError(f"Gradle file not found: {gradle_path}")

        content = path.read_text(encoding="utf-8")

        # Try Groovy format first, then KTS format
        code_match = self._RE_VERSION_CODE_GROOVY.search(content)
        if code_match is None:
            code_match = self._RE_VERSION_CODE_KTS.search(content)
        if code_match is None:
            raise ValueError(
                f"versionCode not found in {gradle_path}"
            )

        name_match = self._RE_VERSION_NAME_GROOVY.search(content)
        if name_match is None:
            name_match = self._RE_VERSION_NAME_KTS.search(content)
        if name_match is None:
            raise ValueError(
                f"versionName not found in {gradle_path}"
            )

        version_code = int(code_match.group(1))
        version_name = name_match.group(1)

        return version_code, version_name

    def write_version_to_gradle(
        self,
        gradle_path: str,
        new_code: int,
        new_name: str,
    ) -> None:
        """Write updated versionCode and versionName to build.gradle.

        Reads the file, performs regex replacement, writes back, and
        verifies the changes by re-reading the file.

        Args:
            gradle_path: Path to the build.gradle or build.gradle.kts file.
            new_code: The new versionCode to write.
            new_name: The new versionName to write.

        Raises:
            RuntimeError: If verification after write fails.
        """
        path = Path(gradle_path)
        content = path.read_text(encoding="utf-8")

        # Replace versionCode — try Groovy format first, then KTS
        if self._RE_VERSION_CODE_GROOVY.search(content):
            content = self._RE_VERSION_CODE_GROOVY.sub(
                f"versionCode {new_code}", content
            )
        else:
            content = self._RE_VERSION_CODE_KTS.sub(
                f"versionCode = {new_code}", content
            )

        # Replace versionName — try Groovy format first, then KTS
        if self._RE_VERSION_NAME_GROOVY.search(content):
            content = self._RE_VERSION_NAME_GROOVY.sub(
                f'versionName "{new_name}"', content
            )
        else:
            content = self._RE_VERSION_NAME_KTS.sub(
                f'versionName = "{new_name}"', content
            )

        path.write_text(content, encoding="utf-8")

        # Verify the write was successful
        verify_code, verify_name = self.read_version_from_gradle(gradle_path)
        if verify_code != new_code or verify_name != new_name:
            raise RuntimeError(
                f"Version verification failed: expected ({new_code}, "
                f"'{new_name}'), got ({verify_code}, '{verify_name}')"
            )

    def _bump_patch(self, version_name: str) -> str:
        """Increment the patch component of a semver version string.

        Args:
            version_name: A version string in "X.Y.Z" format.

        Returns:
            The bumped version string, e.g. "1.2.3" → "1.2.4".
            Returns the input unchanged if not a valid 3-part semver.
        """
        parts = version_name.split(".")
        if len(parts) != 3:
            return version_name

        try:
            major, minor, patch = int(parts[0]), int(parts[1]), int(parts[2])
        except ValueError:
            return version_name

        patch += 1
        return f"{major}.{minor}.{patch}"

    def _find_gradle_file(self) -> Optional[str]:
        """Locate the build.gradle file in the project.

        Checks in order:
          1. {project_root}/app/build.gradle
          2. {project_root}/app/build.gradle.kts
          3. {project_root}/build.gradle
          4. {project_root}/build.gradle.kts

        Returns:
            The path to the first existing gradle file, or None.
        """
        root = Path(self.project_root)
        candidates = [
            root / "app" / "build.gradle",
            root / "app" / "build.gradle.kts",
            root / "build.gradle",
            root / "build.gradle.kts",
            root / "build-logic" / "convention" / "src" / "main" / "kotlin" / "AndroidApplicationConventionPlugin.kt",
        ]
        for candidate in candidates:
            if candidate.exists():
                return str(candidate)
        return None

    def _get_version_code_override(self) -> int | None:
        """
        Read VERSION_CODE_OVERRIDE from environment if provided.

        Returns:
            Parsed positive integer override, or None if unset/empty.

        Raises:
            ValueError: If the override is not a positive integer.
        """
        raw = os.getenv("VERSION_CODE_OVERRIDE", "").strip()
        if not raw:
            return None

        if not raw.isdigit():
            raise ValueError("VERSION_CODE_OVERRIDE must be a positive integer")

        value = int(raw)
        if value <= 0:
            raise ValueError("VERSION_CODE_OVERRIDE must be a positive integer")

        return value

    def execute(self) -> AgentResult:
        """Execute the version bump workflow.

        1. Find build.gradle in the project
        2. Acquire the version lock
        3. Read current versionCode and versionName
        4. Increment both values
        5. Write new values back to gradle
        6. Store new values in pipeline state
        7. Always release the lock in the finally block

        Returns:
            AgentResult with old and new version information.
        """
        # Step 1: Find build.gradle
        gradle_path = self._find_gradle_file()
        if gradle_path is None:
            return AgentResult.fail(
                f"No build.gradle found in {self.project_root}"
            )

        lock_acquired = False
        try:
            # Step 2: Acquire version lock with 30s timeout
            deadline = time.time() + 30
            while time.time() < deadline:
                if self.memory.acquire_lock("version"):
                    lock_acquired = True
                    break
                time.sleep(0.5)

            if not lock_acquired:
                return AgentResult.fail("Could not acquire version lock")

            # Step 3: Read current version
            old_code, old_name = self.read_version_from_gradle(gradle_path)

            # Step 4: Determine next version code (auto-bump or CI override)
            override_code = self._get_version_code_override()
            if override_code is not None:
                if override_code <= old_code:
                    return AgentResult.fail(
                        "VERSION_CODE_OVERRIDE must be greater than current "
                        f"versionCode ({old_code}). Got {override_code}."
                    )
                new_code = override_code
            else:
                new_code = old_code + 1
            new_name = self._bump_patch(old_name)

            # Step 5: Write new version
            self.write_version_to_gradle(gradle_path, new_code, new_name)

            # Step 6: Save to pipeline state
            run = self.memory.load()
            if run is not None:
                run.version_code = new_code
                run.version_name = new_name
                self.memory.save(run)

            # Step 8: Return success
            return AgentResult.ok({
                "old_version_code": old_code,
                "new_version_code": new_code,
                "old_version_name": old_name,
                "new_version_name": new_name,
                "gradle_path": gradle_path,
            })

        except FileNotFoundError as exc:
            return AgentResult.fail(str(exc))
        except ValueError as exc:
            return AgentResult.fail(str(exc))
        except Exception as exc:
            return AgentResult.fail(f"Version bump failed: {exc}")
        finally:
            # Step 7: ALWAYS release lock
            if lock_acquired:
                self.memory.release_lock("version")
