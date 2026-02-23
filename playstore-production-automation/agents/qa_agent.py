"""
QA Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 3 (parallel with Metadata Agent), after Build succeeds.
Executes the Android test suite, parses JUnit XML results, detects flaky
tests, and blocks or warns based on configuration.
"""

from __future__ import annotations

import logging
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Optional

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory

logger = logging.getLogger(__name__)


class QAAgent(BaseAgent):
    """
    Agent that runs the Android test suite and reports results.

    Parses JUnit XML output, detects flaky tests by comparing with
    historical runs, and either blocks the pipeline or logs warnings
    depending on the QA_WARN_ONLY environment variable.
    """

    TEST_RESULTS_GLOB = "**/build/test-results/**/*.xml"

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        project_root: str = ".",
        max_retries: int = 3,
    ) -> None:
        """Initialize the QA Agent."""
        super().__init__(
            name="qa",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )
        self.project_root = Path(project_root).resolve()
        self.warn_only = os.getenv("QA_WARN_ONLY", "false").lower() == "true"

    def _run_unit_tests(self) -> tuple[bool, str]:
        """
        Execute the Gradle test task.

        Returns:
            Tuple of (success: bool, output: str).
        """
        gradlew = self.project_root / "gradlew"
        cmd = [str(gradlew), "test"] if gradlew.exists() else ["./gradlew", "test"]

        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=600,
                cwd=str(self.project_root),
            )

            if result.returncode == 0:
                logger.info("Unit tests passed.")
                return True, result.stdout
            else:
                output = result.stderr or result.stdout
                logger.warning("Unit tests failed: %s", output[:500])
                return False, output

        except FileNotFoundError:
            msg = "gradlew not found. Cannot run tests."
            logger.error(msg)
            return False, msg

        except subprocess.TimeoutExpired:
            msg = "Test execution timed out after 10 minutes."
            logger.error(msg)
            return False, msg

    def _parse_test_results(self, results_dir: str | None = None) -> dict[str, Any]:
        """
        Parse JUnit XML test result files.

        Args:
            results_dir: Optional explicit directory to search for XML files.
                         Defaults to project_root / TEST_RESULTS_GLOB.

        Returns:
            Dictionary with keys: total, passed, failed, skipped, failures.
        """
        report: dict[str, Any] = {
            "total": 0,
            "passed": 0,
            "failed": 0,
            "skipped": 0,
            "failures": [],
        }

        if results_dir:
            search_dir = Path(results_dir)
            xml_files = list(search_dir.rglob("*.xml"))
        else:
            xml_files = list(self.project_root.glob(self.TEST_RESULTS_GLOB))

        if not xml_files:
            logger.warning("No JUnit XML result files found.")
            return report

        for xml_file in xml_files:
            try:
                tree = ET.parse(xml_file)
                root = tree.getroot()

                # Handle both <testsuite> and <testsuites> roots
                suites = (
                    root.findall("testsuite")
                    if root.tag == "testsuites"
                    else [root]
                )

                for suite in suites:
                    tests = int(suite.get("tests", 0))
                    failures = int(suite.get("failures", 0))
                    errors = int(suite.get("errors", 0))
                    skipped = int(suite.get("skipped", 0))

                    report["total"] += tests
                    report["failed"] += failures + errors
                    report["skipped"] += skipped
                    report["passed"] += tests - failures - errors - skipped

                    # Collect individual failure details
                    for testcase in suite.findall("testcase"):
                        failure = testcase.find("failure")
                        error = testcase.find("error")
                        elem = failure if failure is not None else error
                        if elem is not None:
                            name = (
                                f"{testcase.get('classname', '')}."
                                f"{testcase.get('name', 'unknown')}"
                            )
                            message = elem.get("message", elem.text or "No details")
                            report["failures"].append({
                                "name": name,
                                "message": str(message)[:200],
                            })

            except ET.ParseError:
                logger.warning("Malformed XML in %s, skipping.", xml_file)
            except Exception as exc:
                logger.warning("Error parsing %s: %s", xml_file, exc)

        return report

    def _is_flaky_failure(self, test_name: str) -> bool:
        """
        Determine if a test failure is flaky by checking run history.

        A test is considered flaky if it failed in the current run but
        passed in at least 1 of the last 3 pipeline runs.

        Args:
            test_name: Fully qualified test name.

        Returns:
            True if the test is likely flaky, False otherwise.
        """
        history = self.memory.get_history()
        if not history:
            return False

        recent_runs = history[-3:]
        for run_data in recent_runs:
            qa_state = run_data.get("agents", {}).get("qa", {})
            qa_output = qa_state.get("output", {})
            passed_tests = qa_output.get("passed_test_names", [])
            if test_name in passed_tests:
                return True

        return False

    def _generate_report(self, results: dict[str, Any]) -> str:
        """
        Build a human-readable test report.

        Args:
            results: Parsed test results dictionary.

        Returns:
            Formatted report string.
        """
        lines = [
            f"Tests: {results['total']} total, "
            f"{results['passed']} passed, "
            f"{results['failed']} failed, "
            f"{results['skipped']} skipped",
        ]

        if results["failures"]:
            lines.append("")
            lines.append("Failures:")
            for f in results["failures"]:
                lines.append(f"  ✗ {f['name']}: {f['message']}")

        return "\n".join(lines)

    def execute(self) -> AgentResult:
        """
        Execute the QA pipeline.

        1. Run Gradle test task
        2. Parse JUnit XML results
        3. Check for flaky failures
        4. Generate report
        5. Return ok/fail based on results and warn_only mode
        """
        # Step 1: Run tests
        success, output = self._run_unit_tests()

        # Step 2: Parse results (even if tests 'failed', we still want to parse)
        results = self._parse_test_results()
        report = self._generate_report(results)

        logger.info("QA Report:\n%s", report)

        # Step 3: Check flaky tests
        flaky_tests = []
        if results["failures"]:
            for failure in results["failures"]:
                if self._is_flaky_failure(failure["name"]):
                    flaky_tests.append(failure["name"])

        if flaky_tests:
            report += f"\n\nFlaky tests detected ({len(flaky_tests)}):"
            for name in flaky_tests:
                report += f"\n  ⚠ {name}"

        # Step 4: Determine pass/fail
        data = {
            "report": report,
            "total": results["total"],
            "passed": results["passed"],
            "failed": results["failed"],
            "skipped": results["skipped"],
            "flaky_count": len(flaky_tests),
            "warn_only": self.warn_only,
        }

        if results["failed"] > 0 and not self.warn_only:
            # Real failures in strict mode → block pipeline
            non_flaky = results["failed"] - len(flaky_tests)
            if non_flaky > 0:
                return AgentResult.fail(
                    f"{non_flaky} non-flaky test failure(s).\n{report}"
                )
            # All failures are flaky — allow retry
            logger.warning("All %d failure(s) appear flaky.", len(flaky_tests))

        return AgentResult.ok(data)
