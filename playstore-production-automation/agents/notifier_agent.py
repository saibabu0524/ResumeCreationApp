"""
Notifier Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 5 (parallel with Tester Agent).
Sends Slack notifications and posts GitHub commit statuses.
CRITICAL: This agent NEVER fails the pipeline — always returns ok().
"""

from __future__ import annotations

import logging
import os
from datetime import datetime, timezone
from typing import Any, Optional

import requests

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory, PipelineStatus

logger = logging.getLogger(__name__)


class NotifierAgent(BaseAgent):
    """
    Agent that sends pipeline status notifications.

    Posts Slack messages and GitHub commit statuses. This agent ALWAYS
    returns AgentResult.ok() — notification failures never block the
    pipeline.
    """

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        max_retries: int = 1,  # Low retries — notifications are best-effort
    ) -> None:
        """Initialize the Notifier Agent."""
        super().__init__(
            name="notifier",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )

    def _build_slack_message(self, run: Any) -> dict[str, Any]:
        """
        Build a Slack Block Kit message payload.

        Includes status indicator, version info, duration, track,
        and failure details if applicable.

        Args:
            run: The current PipelineRun instance.

        Returns:
            Slack-compatible payload dict.
        """
        is_success = run.status == PipelineStatus.SUCCESS
        icon = "✅" if is_success else "❌"
        status_text = "SUCCESS" if is_success else "FAILED"

        version_text = f"v{run.version_name} (build {run.version_code})"
        duration = self._calculate_duration(run)

        # Build blocks
        blocks: list[dict[str, Any]] = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": f"{icon} Pipeline {status_text}",
                },
            },
            {
                "type": "section",
                "fields": [
                    {
                        "type": "mrkdwn",
                        "text": f"*Version:*\n{version_text}",
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Duration:*\n{duration}",
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Branch:*\n`{run.branch}`",
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Commit:*\n`{run.commit_sha[:7]}`",
                    },
                ],
            },
        ]

        # Add failure details
        if not is_success and run.errors:
            error_text = "\n".join(f"• {e}" for e in run.errors[-3:])
            blocks.append({
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Errors:*\n{error_text}",
                },
            })

        # Check for failed agents
        failed_agents = []
        for agent_name, agent_data in run.agents.items():
            if isinstance(agent_data, dict):
                if agent_data.get("status") == "FAILED":
                    error = agent_data.get("error", "Unknown error")
                    failed_agents.append(f"• *{agent_name}*: {error[:100]}")

        if failed_agents:
            blocks.append({
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        "*Failed Agents:*\n"
                        + "\n".join(failed_agents)
                    ),
                },
            })

        return {"blocks": blocks}

    def _send_slack(self, payload: dict[str, Any]) -> bool:
        """
        Send a message to Slack via incoming webhook.

        Args:
            payload: Slack Block Kit payload.

        Returns:
            True on success or if webhook not configured. False on failure.
        """
        webhook_url = os.getenv("SLACK_WEBHOOK_URL")
        if not webhook_url:
            logger.info("Slack webhook not configured, skipping notification.")
            return True

        try:
            response = requests.post(
                webhook_url,
                json=payload,
                timeout=10,
            )
            if response.status_code == 200:
                logger.info("Slack notification sent successfully.")
                return True
            else:
                logger.warning(
                    "Slack returned status %d: %s",
                    response.status_code,
                    response.text[:200],
                )
                return False
        except Exception as exc:
            logger.warning("Slack notification failed: %s", exc)
            return False

    def _post_github_commit_status(self, run: Any) -> bool:
        """
        Post a commit status to GitHub.

        Args:
            run: The current PipelineRun instance.

        Returns:
            True on success or if token not configured. False on failure.
        """
        token = os.getenv("GITHUB_TOKEN")
        if not token:
            logger.info("GitHub token not set, skipping commit status.")
            return True

        repo = os.getenv("GITHUB_REPOSITORY", "")
        if not repo:
            logger.warning("GITHUB_REPOSITORY not set, skipping.")
            return True

        is_success = run.status == PipelineStatus.SUCCESS
        state = "success" if is_success else "failure"
        description = (
            f"Pipeline {'succeeded' if is_success else 'failed'}: "
            f"v{run.version_name}"
        )

        url = (
            f"https://api.github.com/repos/{repo}/statuses/{run.commit_sha}"
        )

        try:
            response = requests.post(
                url,
                json={
                    "state": state,
                    "description": description[:140],
                    "context": "ci/android-pipeline",
                },
                headers={
                    "Authorization": f"token {token}",
                    "Accept": "application/vnd.github+json",
                },
                timeout=10,
            )
            if response.status_code in (200, 201):
                logger.info("GitHub commit status posted: %s", state)
                return True
            else:
                logger.warning(
                    "GitHub API returned %d: %s",
                    response.status_code,
                    response.text[:200],
                )
                return False
        except Exception as exc:
            logger.warning("GitHub commit status failed: %s", exc)
            return False

    def _calculate_duration(self, run: Any) -> str:
        """
        Calculate how long the pipeline has been running.

        Args:
            run: The current PipelineRun instance.

        Returns:
            Human-readable duration string, e.g., "19m 34s".
        """
        try:
            started = datetime.fromisoformat(run.started_at)
            now = datetime.now(timezone.utc)
            delta = now - started
            total_seconds = int(delta.total_seconds())

            if total_seconds < 60:
                return f"{total_seconds}s"
            elif total_seconds < 3600:
                minutes = total_seconds // 60
                seconds = total_seconds % 60
                return f"{minutes}m {seconds}s"
            else:
                hours = total_seconds // 3600
                minutes = (total_seconds % 3600) // 60
                return f"{hours}h {minutes}m"

        except (ValueError, AttributeError):
            return "unknown"

    def execute(self) -> AgentResult:
        """
        Send pipeline notifications.

        1. Load pipeline state
        2. Build Slack message
        3. Send Slack notification (ignore failures)
        4. Post GitHub commit status (ignore failures)
        5. ALWAYS return ok()
        """
        run = self.memory.load()
        if run is None:
            logger.warning("No pipeline state for notification.")
            return AgentResult.ok({"notified": False, "reason": "no state"})

        # Build and send
        payload = self._build_slack_message(run)
        slack_ok = self._send_slack(payload)
        github_ok = self._post_github_commit_status(run)

        # ALWAYS return ok — notification failure never blocks pipeline
        return AgentResult.ok({
            "slack_sent": slack_ok,
            "github_status_posted": github_ok,
            "notified": True,
        })
