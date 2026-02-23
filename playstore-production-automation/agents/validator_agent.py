"""
Validator Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 1 (parallel with Version and Secrets agents).
Validates the Android project by checking SDK versions, manifest permissions,
running lint, and verifying required files exist. Collects ALL issues before
deciding pass/fail — never stops at the first failure.
"""

from __future__ import annotations

import os
import re
import subprocess
from pathlib import Path
from typing import Any

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory


# Dangerous permissions that trigger a blocking issue
DANGEROUS_PERMISSIONS: list[str] = [
    "SEND_SMS",
    "READ_SMS",
    "RECEIVE_SMS",
    "READ_CALL_LOG",
    "WRITE_CALL_LOG",
    "PROCESS_OUTGOING_CALLS",
    "READ_CONTACTS",
    "QUERY_ALL_PACKAGES",
    "REQUEST_INSTALL_PACKAGES",
]


class ValidatorAgent(BaseAgent):
    """
    Validates an Android project before building.

    Checks performed:
      1. SDK version requirements (targetSdkVersion >= 34)
      2. AndroidManifest.xml for dangerous permissions
      3. Gradle lint for errors
      4. Required files existence
    """

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        project_root: str = ".",
    ) -> None:
        """Initialize ValidatorAgent with project root and pipeline memory."""
        super().__init__(name="validator", memory=memory, dry_run=dry_run)
        self.project_root = Path(project_root)

    def execute(self) -> AgentResult:
        """
        Run ALL validation checks, collect ALL issues, then decide pass/fail.

        Returns:
            AgentResult.ok  — if no blocking issues found
            AgentResult.fail — if one or more blocking issues found
        """
        all_issues: list[str] = []
        checks_passed = 0

        # --- Run each check, catch exceptions per-check ---
        for check_fn in (
            self._check_files_exist,
            self._check_sdk_versions,
            self._check_manifest,
            self._run_lint,
        ):
            try:
                issues = check_fn()
                if not issues:
                    checks_passed += 1
                else:
                    all_issues.extend(issues)
            except Exception as exc:
                all_issues.append(f"{check_fn.__name__} failed: {exc}")

        # --- Decide result ---
        if all_issues:
            summary = "; ".join(all_issues)
            return AgentResult.fail(
                f"{len(all_issues)} blocking issues found: {summary}"
            )

        return AgentResult.ok({
            "issues": [],
            "checks_passed": checks_passed,
        })

    # ------------------------------------------------------------------
    # Individual validation checks
    # ------------------------------------------------------------------

    def _check_sdk_versions(self) -> list[str]:
        """
        Verify targetSdkVersion >= 34 in build.gradle(.kts).

        Searches for build.gradle or build.gradle.kts at project root
        and in the app/ subdirectory.
        """
        issues: list[str] = []

        gradle_file = self._find_gradle_file()
        if gradle_file is None:
            return ["build.gradle not found"]

        try:
            content = gradle_file.read_text(encoding="utf-8")
        except OSError:
            return ["build.gradle not found"]

        # Match: targetSdkVersion 34  /  targetSdkVersion = 34
        #        targetSdk 34         /  targetSdk = 34
        pattern = r"targetSdk(?:Version)?\s*(?:=\s*)?(\d+)"
        match = re.search(pattern, content)

        if match is None:
            return ["targetSdkVersion not specified"]

        target_sdk = int(match.group(1))
        if target_sdk < 34:
            issues.append(
                f"targetSdkVersion is {target_sdk}, must be >= 34"
            )

        return issues

    def _check_manifest(self) -> list[str]:
        """
        Check AndroidManifest.xml for dangerous permissions.

        Looks for the manifest at app/src/main/AndroidManifest.xml
        relative to the project root.
        """
        manifest_path = self.project_root / "app" / "src" / "main" / "AndroidManifest.xml"

        if not manifest_path.exists():
            return ["AndroidManifest.xml not found"]

        try:
            content = manifest_path.read_text(encoding="utf-8")
        except OSError:
            return ["AndroidManifest.xml not found"]

        issues: list[str] = []
        for perm in DANGEROUS_PERMISSIONS:
            # Match android.permission.PERM_NAME in uses-permission tags
            if f"android.permission.{perm}" in content:
                issues.append(
                    f"Dangerous permission found: {perm}"
                )

        return issues

    def _run_lint(self) -> list[str]:
        """
        Run Gradle lint and report any errors.

        Executes ./gradlew lint with a 600-second timeout.
        Only lint errors (not warnings) are considered blocking.
        """
        gradlew_path = self.project_root / "gradlew"

        if not gradlew_path.exists():
            return ["gradlew not found, skipping lint"]

        try:
            result = subprocess.run(
                [str(gradlew_path), "lint"],
                capture_output=True,
                text=True,
                timeout=600,
                cwd=str(self.project_root),
            )
        except subprocess.TimeoutExpired:
            return ["Lint timed out after 600 seconds"]
        except FileNotFoundError:
            return ["gradlew not found, skipping lint"]
        except OSError as exc:
            return [f"Lint execution failed: {exc}"]

        # Combine stdout and stderr for analysis
        output = (result.stdout or "") + (result.stderr or "")

        if result.returncode != 0:
            # Try to extract error count from lint output
            error_match = re.search(r"(\d+)\s+error", output, re.IGNORECASE)
            if error_match:
                error_count = int(error_match.group(1))
                return [f"Lint found {error_count} errors"]
            return ["Lint found errors"]

        return []

    def _check_files_exist(self) -> list[str]:
        """
        Verify that required project files exist.

        Checks for:
          - build.gradle (or build.gradle.kts) at project root or app/
          - AndroidManifest.xml at app/src/main/
        """
        issues: list[str] = []

        if self._find_gradle_file() is None:
            issues.append("build.gradle not found")

        manifest_path = self.project_root / "app" / "src" / "main" / "AndroidManifest.xml"
        if not manifest_path.exists():
            issues.append("AndroidManifest.xml not found")

        return issues

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _find_gradle_file(self) -> Path | None:
        """
        Locate build.gradle or build.gradle.kts.

        Searches project_root first, then project_root/app/.
        Returns the Path if found, None otherwise.
        """
        candidates = [
            self.project_root / "app" / "build.gradle",
            self.project_root / "app" / "build.gradle.kts",
            self.project_root / "build.gradle",
            self.project_root / "build.gradle.kts",
            self.project_root / "build-logic" / "convention" / "src" / "main" / "kotlin" / "AndroidApplicationConventionPlugin.kt",
        ]
        for candidate in candidates:
            if candidate.exists():
                return candidate
        return None
