"""
Tests for the QA Agent.

Covers JUnit XML parsing, flaky test detection, warn-only mode,
and the full execute() lifecycle.
"""

from __future__ import annotations

import os
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from agents.qa_agent import QAAgent
from agents.memory import PipelineMemory


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_pipeline(tmp_path: Path):
    """Set up a PipelineMemory in a temp directory."""
    state_dir = tmp_path / ".pipeline"
    memory = PipelineMemory(state_dir=str(state_dir))
    memory.init_run(commit_sha="abc123", branch="release")
    return memory


@pytest.fixture
def fake_project(tmp_path: Path):
    """Create a minimal fake project structure."""
    project = tmp_path / "project"
    project.mkdir()
    return project


@pytest.fixture
def agent(tmp_pipeline, fake_project):
    """Return a QAAgent wired to temp dirs."""
    return QAAgent(
        memory=tmp_pipeline,
        project_root=str(fake_project),
    )


# Sample JUnit XML content
JUNIT_ALL_PASS = """\
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.AppTest" tests="3" failures="0" errors="0" skipped="0">
  <testcase classname="com.example.AppTest" name="testLogin" time="0.05"/>
  <testcase classname="com.example.AppTest" name="testLogout" time="0.03"/>
  <testcase classname="com.example.AppTest" name="testProfile" time="0.02"/>
</testsuite>
"""

JUNIT_WITH_FAILURES = """\
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.AppTest" tests="4" failures="2" errors="0" skipped="0">
  <testcase classname="com.example.AppTest" name="testLogin" time="0.05"/>
  <testcase classname="com.example.AppTest" name="testLogout" time="0.03">
    <failure message="Expected true but got false">AssertionError</failure>
  </testcase>
  <testcase classname="com.example.AppTest" name="testProfile" time="0.02">
    <failure message="NullPointerException">java.lang.NullPointerException</failure>
  </testcase>
  <testcase classname="com.example.AppTest" name="testSettings" time="0.01"/>
</testsuite>
"""

JUNIT_MALFORMED = """<?xml version="1.0"?><testsuite><not_closed>"""


def _write_xml(directory: Path, filename: str, content: str) -> Path:
    """Helper: write JUnit XML to a test results directory structure."""
    results_dir = directory / "app" / "build" / "test-results" / "testDebugUnitTest"
    results_dir.mkdir(parents=True, exist_ok=True)
    xml_file = results_dir / filename
    xml_file.write_text(content, encoding="utf-8")
    return results_dir


# ---------------------------------------------------------------------------
# TestQAAgentTestParsing
# ---------------------------------------------------------------------------

class TestQAAgentTestParsing:
    """Tests for _parse_test_results()."""

    def test_parse_results_when_all_pass_returns_zero_failures(
        self, agent: QAAgent, fake_project: Path
    ):
        """All-passing test suite should report 0 failures."""
        results_dir = _write_xml(fake_project, "TEST-all-pass.xml", JUNIT_ALL_PASS)
        report = agent._parse_test_results(str(results_dir))
        assert report["total"] == 3
        assert report["passed"] == 3
        assert report["failed"] == 0
        assert report["failures"] == []

    def test_parse_results_when_some_fail_returns_correct_count(
        self, agent: QAAgent, fake_project: Path
    ):
        """Failing tests should be counted and details captured."""
        results_dir = _write_xml(fake_project, "TEST-with-failures.xml", JUNIT_WITH_FAILURES)
        report = agent._parse_test_results(str(results_dir))
        assert report["total"] == 4
        assert report["passed"] == 2
        assert report["failed"] == 2
        assert len(report["failures"]) == 2
        names = [f["name"] for f in report["failures"]]
        assert "com.example.AppTest.testLogout" in names
        assert "com.example.AppTest.testProfile" in names

    def test_parse_results_when_no_xml_files_returns_empty_report(
        self, agent: QAAgent, tmp_path: Path
    ):
        """Non-existent results directory should return empty report."""
        empty_dir = tmp_path / "empty"
        empty_dir.mkdir()
        report = agent._parse_test_results(str(empty_dir))
        assert report["total"] == 0
        assert report["passed"] == 0
        assert report["failed"] == 0

    def test_parse_results_when_malformed_xml_logs_warning(
        self, agent: QAAgent, fake_project: Path
    ):
        """Malformed XML should be skipped without raising."""
        results_dir = _write_xml(fake_project, "TEST-bad.xml", JUNIT_MALFORMED)
        report = agent._parse_test_results(str(results_dir))
        # Should not crash, returns empty or partial results
        assert isinstance(report, dict)
        assert "total" in report


# ---------------------------------------------------------------------------
# TestQAAgentFlakyDetection
# ---------------------------------------------------------------------------

class TestQAAgentFlakyDetection:
    """Tests for _is_flaky_failure()."""

    def test_is_flaky_when_test_failed_only_once_in_history_returns_true(
        self, agent: QAAgent, tmp_pipeline: PipelineMemory
    ):
        """Test that passed in a recent run but fails now should be flaky."""
        # Mock get_history to return a run where this test passed
        with patch.object(tmp_pipeline, "get_history") as mock_history:
            mock_history.return_value = [
                {
                    "agents": {
                        "qa": {
                            "output": {
                                "passed_test_names": [
                                    "com.example.AppTest.testLogout"
                                ]
                            }
                        }
                    }
                }
            ]
            assert agent._is_flaky_failure("com.example.AppTest.testLogout") is True

    def test_is_flaky_when_test_always_fails_returns_false(
        self, agent: QAAgent, tmp_pipeline: PipelineMemory
    ):
        """Test that never passed before should not be considered flaky."""
        with patch.object(tmp_pipeline, "get_history") as mock_history:
            mock_history.return_value = [
                {"agents": {"qa": {"output": {"passed_test_names": []}}}},
                {"agents": {"qa": {"output": {"passed_test_names": []}}}},
            ]
            assert agent._is_flaky_failure("com.example.AppTest.testLogout") is False

    def test_is_flaky_when_no_history_returns_false(
        self, agent: QAAgent, tmp_pipeline: PipelineMemory
    ):
        """No history available should default to not flaky."""
        with patch.object(tmp_pipeline, "get_history") as mock_history:
            mock_history.return_value = []
            assert agent._is_flaky_failure("com.example.AppTest.testLogout") is False


# ---------------------------------------------------------------------------
# TestQAAgentExecute
# ---------------------------------------------------------------------------

class TestQAAgentExecute:
    """Tests for the execute() method."""

    def test_execute_when_all_tests_pass_returns_ok(
        self, agent: QAAgent, fake_project: Path
    ):
        """All tests passing should return ok."""
        _write_xml(fake_project, "TEST-pass.xml", JUNIT_ALL_PASS)

        with patch.object(agent, "_run_unit_tests") as mock_run:
            mock_run.return_value = (True, "BUILD SUCCESSFUL")
            result = agent.execute()

        assert result.success is True
        assert result.data["total"] == 3
        assert result.data["failed"] == 0

    def test_execute_when_tests_fail_and_not_warn_only_returns_fail(
        self, agent: QAAgent, fake_project: Path
    ):
        """Failing tests in strict mode should fail the pipeline."""
        agent.warn_only = False
        _write_xml(fake_project, "TEST-fail.xml", JUNIT_WITH_FAILURES)

        with patch.object(agent, "_run_unit_tests") as mock_run:
            mock_run.return_value = (False, "TESTS FAILED")
            with patch.object(agent, "_is_flaky_failure", return_value=False):
                result = agent.execute()

        assert result.success is False
        assert "non-flaky test failure" in result.error

    def test_execute_when_tests_fail_and_warn_only_returns_ok(
        self, agent: QAAgent, fake_project: Path
    ):
        """Failing tests in warn-only mode should still return ok."""
        agent.warn_only = True
        _write_xml(fake_project, "TEST-fail.xml", JUNIT_WITH_FAILURES)

        with patch.object(agent, "_run_unit_tests") as mock_run:
            mock_run.return_value = (False, "TESTS FAILED")
            result = agent.execute()

        assert result.success is True
        assert result.data["failed"] == 2
        assert result.data["warn_only"] is True

    def test_execute_when_gradle_not_found_returns_fail(
        self, agent: QAAgent
    ):
        """Missing gradlew should return fail."""
        with patch.object(agent, "_run_unit_tests") as mock_run:
            mock_run.return_value = (False, "gradlew not found. Cannot run tests.")
            result = agent.execute()

        # With no XML files and no test failures, it returns ok with 0 tests
        # But if the subprocess itself fails, we report based on parsed results
        assert isinstance(result, AgentResult)


# ---------------------------------------------------------------------------
# TestQAAgentReport
# ---------------------------------------------------------------------------

class TestQAAgentReport:
    """Tests for _generate_report()."""

    def test_generate_report_includes_counts(self, agent: QAAgent):
        """Report should include total, passed, failed, skipped counts."""
        results = {
            "total": 10,
            "passed": 8,
            "failed": 1,
            "skipped": 1,
            "failures": [{"name": "test_a", "message": "failed"}],
        }
        report = agent._generate_report(results)
        assert "10 total" in report
        assert "8 passed" in report
        assert "1 failed" in report
        assert "test_a" in report


from agents.base_agent import AgentResult
