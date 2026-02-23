"""
Comprehensive tests for agents/validator_agent.py.

Uses pytest with tmp_path for creating fake project structures.
Mocks subprocess.run for lint checks to avoid needing a real Gradle setup.
"""

from __future__ import annotations

import subprocess
from pathlib import Path
from typing import Any
from unittest.mock import MagicMock, patch

import pytest

from agents.base_agent import AgentResult
from agents.memory import PipelineMemory
from agents.validator_agent import ValidatorAgent


# ---------------------------------------------------------------------------
# Helpers — create fake Android project structures in tmp_path
# ---------------------------------------------------------------------------

def _create_build_gradle(
    project_root: Path,
    content: str,
    kts: bool = False,
    in_app: bool = False,
) -> Path:
    """Write a build.gradle(.kts) file with the given content."""
    parent = project_root / "app" if in_app else project_root
    parent.mkdir(parents=True, exist_ok=True)
    name = "build.gradle.kts" if kts else "build.gradle"
    gradle_file = parent / name
    gradle_file.write_text(content, encoding="utf-8")
    return gradle_file


def _create_manifest(
    project_root: Path,
    permissions: list[str] | None = None,
) -> Path:
    """Write a minimal AndroidManifest.xml with optional permissions."""
    manifest_dir = project_root / "app" / "src" / "main"
    manifest_dir.mkdir(parents=True, exist_ok=True)
    manifest_file = manifest_dir / "AndroidManifest.xml"

    perm_lines = ""
    if permissions:
        perm_lines = "\n".join(
            f'    <uses-permission android:name="android.permission.{p}" />'
            for p in permissions
        )

    manifest_file.write_text(
        f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.testapp">
{perm_lines}
    <application android:label="TestApp" />
</manifest>
""",
        encoding="utf-8",
    )
    return manifest_file


def _create_gradlew(project_root: Path) -> Path:
    """Create a fake gradlew script."""
    project_root.mkdir(parents=True, exist_ok=True)
    gradlew = project_root / "gradlew"
    gradlew.write_text("#!/bin/bash\necho 'fake gradlew'", encoding="utf-8")
    gradlew.chmod(0o755)
    return gradlew


def _make_memory(tmp_path: Path) -> PipelineMemory:
    """Create a PipelineMemory instance with a fresh run initialized."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    memory.init_run("abc123", "main")
    return memory


# ===========================================================================
# TestValidatorAgentSdkCheck
# ===========================================================================

class TestValidatorAgentSdkCheck:
    """Tests for _check_sdk_versions()."""

    def test_check_sdk_versions_when_valid_returns_no_issues(
        self, tmp_path: Path
    ) -> None:
        """A project with targetSdkVersion 34 should pass validation."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    targetSdkVersion 34\n}\n",
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert issues == []

    def test_check_sdk_versions_when_target_too_low_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """targetSdkVersion 33 should produce a blocking issue."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    targetSdkVersion 33\n}\n",
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert len(issues) == 1
        assert "targetSdkVersion is 33" in issues[0]
        assert "must be >= 34" in issues[0]

    def test_check_sdk_versions_when_gradle_missing_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """Missing build.gradle should produce a blocking issue."""
        project = tmp_path / "project"
        project.mkdir(parents=True, exist_ok=True)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert len(issues) == 1
        assert "build.gradle not found" in issues[0]

    def test_check_sdk_versions_when_no_target_sdk_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """A build.gradle without targetSdkVersion should produce a blocking issue."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    compileSdkVersion 34\n}\n",
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert len(issues) == 1
        assert "targetSdkVersion not specified" in issues[0]

    def test_check_sdk_versions_kts_syntax(
        self, tmp_path: Path
    ) -> None:
        """build.gradle.kts with targetSdk = 34 should pass validation."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    targetSdk = 34\n}\n",
            kts=True,
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert issues == []

    def test_check_sdk_versions_app_subdir(
        self, tmp_path: Path
    ) -> None:
        """build.gradle in app/ subdirectory should be found."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    targetSdkVersion 34\n}\n",
            in_app=True,
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert issues == []

    def test_check_sdk_versions_prefers_app_gradle_over_root(
        self, tmp_path: Path
    ) -> None:
        """When both exist, app/build.gradle should be used for targetSdk."""
        project = tmp_path / "project"
        _create_build_gradle(
            project,
            "android {\n    compileSdkVersion 34\n}\n",
            in_app=False,
        )
        _create_build_gradle(
            project,
            "android {\n    targetSdkVersion 34\n}\n",
            in_app=True,
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_sdk_versions()

        assert issues == []


# ===========================================================================
# TestValidatorAgentManifestCheck
# ===========================================================================

class TestValidatorAgentManifestCheck:
    """Tests for _check_manifest()."""

    def test_check_manifest_when_clean_returns_no_issues(
        self, tmp_path: Path
    ) -> None:
        """A manifest with no dangerous permissions should pass."""
        project = tmp_path / "project"
        _create_manifest(project, permissions=["INTERNET", "ACCESS_NETWORK_STATE"])
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_manifest()

        assert issues == []

    def test_check_manifest_when_sms_permission_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """SEND_SMS permission should trigger a blocking issue."""
        project = tmp_path / "project"
        _create_manifest(project, permissions=["INTERNET", "SEND_SMS"])
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_manifest()

        assert len(issues) == 1
        assert "SEND_SMS" in issues[0]

    def test_check_manifest_when_query_all_packages_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """QUERY_ALL_PACKAGES permission should trigger a blocking issue."""
        project = tmp_path / "project"
        _create_manifest(project, permissions=["QUERY_ALL_PACKAGES"])
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_manifest()

        assert len(issues) == 1
        assert "QUERY_ALL_PACKAGES" in issues[0]

    def test_check_manifest_when_missing_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """Missing AndroidManifest.xml should produce a blocking issue."""
        project = tmp_path / "project"
        project.mkdir(parents=True, exist_ok=True)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_manifest()

        assert len(issues) == 1
        assert "AndroidManifest.xml not found" in issues[0]

    def test_check_manifest_when_multiple_permissions_returns_all_issues(
        self, tmp_path: Path
    ) -> None:
        """Multiple dangerous permissions should each produce a separate issue."""
        project = tmp_path / "project"
        _create_manifest(
            project,
            permissions=["SEND_SMS", "READ_SMS", "QUERY_ALL_PACKAGES"],
        )
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_manifest()

        assert len(issues) == 3
        permission_names = " ".join(issues)
        assert "SEND_SMS" in permission_names
        assert "READ_SMS" in permission_names
        assert "QUERY_ALL_PACKAGES" in permission_names


# ===========================================================================
# TestValidatorAgentLintCheck
# ===========================================================================

class TestValidatorAgentLintCheck:
    """Tests for _run_lint()."""

    def test_run_lint_when_gradlew_missing_returns_issue(
        self, tmp_path: Path
    ) -> None:
        """Missing gradlew should produce a skip message."""
        project = tmp_path / "project"
        project.mkdir(parents=True, exist_ok=True)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._run_lint()

        assert len(issues) == 1
        assert "gradlew not found" in issues[0]

    @patch("agents.validator_agent.subprocess.run")
    def test_run_lint_when_passes_returns_no_issues(
        self, mock_run: MagicMock, tmp_path: Path
    ) -> None:
        """A successful lint run should return no issues."""
        project = tmp_path / "project"
        _create_gradlew(project)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        mock_run.return_value = subprocess.CompletedProcess(
            args=["./gradlew", "lint"],
            returncode=0,
            stdout="BUILD SUCCESSFUL\n",
            stderr="",
        )

        issues = agent._run_lint()

        assert issues == []

    @patch("agents.validator_agent.subprocess.run")
    def test_run_lint_when_errors_found_returns_issue(
        self, mock_run: MagicMock, tmp_path: Path
    ) -> None:
        """Lint errors should produce a blocking issue with the error count."""
        project = tmp_path / "project"
        _create_gradlew(project)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        mock_run.return_value = subprocess.CompletedProcess(
            args=["./gradlew", "lint"],
            returncode=1,
            stdout="Lint found 5 errors and 3 warnings\n",
            stderr="",
        )

        issues = agent._run_lint()

        assert len(issues) == 1
        assert "Lint found 5 errors" in issues[0]

    @patch("agents.validator_agent.subprocess.run")
    def test_run_lint_when_timeout_returns_issue(
        self, mock_run: MagicMock, tmp_path: Path
    ) -> None:
        """A lint timeout should produce a blocking issue."""
        project = tmp_path / "project"
        _create_gradlew(project)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        mock_run.side_effect = subprocess.TimeoutExpired(
            cmd=["./gradlew", "lint"], timeout=600
        )

        issues = agent._run_lint()

        assert len(issues) == 1
        assert "Lint timed out after 600 seconds" in issues[0]


# ===========================================================================
# TestValidatorAgentFilesExist
# ===========================================================================

class TestValidatorAgentFilesExist:
    """Tests for _check_files_exist()."""

    def test_check_files_exist_when_all_present(
        self, tmp_path: Path
    ) -> None:
        """No issues when both build.gradle and AndroidManifest.xml exist."""
        project = tmp_path / "project"
        _create_build_gradle(project, "android {}")
        _create_manifest(project)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_files_exist()

        assert issues == []

    def test_check_files_exist_when_both_missing(
        self, tmp_path: Path
    ) -> None:
        """Both missing files should produce two issues."""
        project = tmp_path / "project"
        project.mkdir(parents=True, exist_ok=True)
        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        issues = agent._check_files_exist()

        assert len(issues) == 2
        issue_text = " ".join(issues)
        assert "build.gradle not found" in issue_text
        assert "AndroidManifest.xml not found" in issue_text


# ===========================================================================
# TestValidatorAgentExecute
# ===========================================================================

class TestValidatorAgentExecute:
    """Tests for the full execute() flow."""

    def test_execute_when_all_checks_pass_returns_ok(
        self, tmp_path: Path
    ) -> None:
        """A valid project should produce AgentResult.ok with zero issues."""
        project = tmp_path / "project"
        _create_build_gradle(project, "android {\n    targetSdkVersion 34\n}\n")
        _create_manifest(project, permissions=["INTERNET"])
        # No gradlew — lint will be skipped (non-blocking skip message
        # is returned by _run_lint, but it's actually an issue).
        # To make all checks pass, we need gradlew too with a mocked subprocess.
        _create_gradlew(project)

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        with patch("agents.validator_agent.subprocess.run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=["./gradlew", "lint"],
                returncode=0,
                stdout="BUILD SUCCESSFUL\n",
                stderr="",
            )
            result = agent.execute()

        assert result.success is True
        assert result.data["issues"] == []
        assert result.data["checks_passed"] == 4

    def test_execute_when_sdk_too_low_returns_fail(
        self, tmp_path: Path
    ) -> None:
        """A project with targetSdkVersion < 34 should fail."""
        project = tmp_path / "project"
        _create_build_gradle(project, "android {\n    targetSdkVersion 30\n}\n")
        _create_manifest(project, permissions=["INTERNET"])
        _create_gradlew(project)

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        with patch("agents.validator_agent.subprocess.run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=["./gradlew", "lint"],
                returncode=0,
                stdout="BUILD SUCCESSFUL\n",
                stderr="",
            )
            result = agent.execute()

        assert result.success is False
        assert "blocking issues found" in (result.error or "")
        assert "targetSdkVersion is 30" in (result.error or "")

    def test_execute_when_lint_fails_returns_fail(
        self, tmp_path: Path
    ) -> None:
        """Lint errors should cause execute() to return a failure."""
        project = tmp_path / "project"
        _create_build_gradle(project, "android {\n    targetSdkVersion 34\n}\n")
        _create_manifest(project, permissions=["INTERNET"])
        _create_gradlew(project)

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        with patch("agents.validator_agent.subprocess.run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=["./gradlew", "lint"],
                returncode=1,
                stdout="Lint found 3 errors\n",
                stderr="",
            )
            result = agent.execute()

        assert result.success is False
        assert "blocking issues found" in (result.error or "")
        assert "Lint found 3 errors" in (result.error or "")

    def test_execute_collects_all_issues_not_just_first(
        self, tmp_path: Path
    ) -> None:
        """execute() should collect issues from ALL checks, not stop early."""
        project = tmp_path / "project"
        # SDK too low
        _create_build_gradle(project, "android {\n    targetSdkVersion 30\n}\n")
        # Dangerous permission
        _create_manifest(project, permissions=["SEND_SMS"])
        _create_gradlew(project)

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        with patch("agents.validator_agent.subprocess.run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=["./gradlew", "lint"],
                returncode=1,
                stdout="Lint found 2 errors\n",
                stderr="",
            )
            result = agent.execute()

        assert result.success is False
        error_msg = result.error or ""
        # All three types of issues should be present
        assert "targetSdkVersion is 30" in error_msg
        assert "SEND_SMS" in error_msg
        assert "Lint found 2 errors" in error_msg

    def test_execute_never_crashes_on_exception(
        self, tmp_path: Path
    ) -> None:
        """execute() should catch per-check exceptions and return a fail result."""
        project = tmp_path / "project"
        project.mkdir(parents=True, exist_ok=True)
        # No files at all — every check will find issues but shouldn't crash

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        result = agent.execute()

        # Should not crash — returns a failure with collected issues
        assert result.success is False
        assert "blocking issues found" in (result.error or "")

    def test_execute_via_run_integrates_with_base_agent(
        self, tmp_path: Path
    ) -> None:
        """Full integration: run() → execute() → state persisted."""
        project = tmp_path / "project"
        _create_build_gradle(project, "android {\n    targetSdkVersion 34\n}\n")
        _create_manifest(project, permissions=["INTERNET"])
        _create_gradlew(project)

        memory = _make_memory(tmp_path)
        agent = ValidatorAgent(memory=memory, project_root=str(project))

        with patch("agents.validator_agent.subprocess.run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=["./gradlew", "lint"],
                returncode=0,
                stdout="BUILD SUCCESSFUL\n",
                stderr="",
            )
            result = agent.run()

        assert result.success is True

        # Verify state was persisted
        run = memory.load()
        assert run is not None
        agent_state = run.get_agent("validator")
        assert agent_state.is_done()
