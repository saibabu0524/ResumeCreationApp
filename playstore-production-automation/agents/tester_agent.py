"""
Tester Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 5 (parallel with Notifier Agent), after Upload succeeds.
Assigns tester groups to tracks in Google Play Console based on
configuration in config/tester-groups.yaml.
"""

from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any, Optional

import yaml

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory

logger = logging.getLogger(__name__)


class TesterAgent(BaseAgent):
    """
    Agent that assigns tester groups to Play Console tracks.

    Reads tester group configuration from a YAML file, assigns testers
    via the Google Play Developer API, and checks promotion rules.
    """

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        config_path: str = "config/tester-groups.yaml",
        max_retries: int = 3,
    ) -> None:
        """Initialize the Tester Agent."""
        super().__init__(
            name="tester",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )
        self.config_path = Path(config_path)

    def _load_tester_config(self) -> dict[str, Any]:
        """
        Load tester group configuration from YAML.

        Returns:
            Parsed config dict, or {"groups": {}} if file missing.
        """
        if not self.config_path.exists():
            logger.warning(
                "Tester config not found at %s. Using empty config.",
                self.config_path,
            )
            return {"groups": {}}

        try:
            with open(self.config_path, "r", encoding="utf-8") as f:
                config = yaml.safe_load(f)
            return config if config else {"groups": {}}
        except (yaml.YAMLError, OSError) as exc:
            logger.warning("Error reading tester config: %s", exc)
            return {"groups": {}}

    def _get_track_config(self, track: str) -> Optional[dict[str, Any]]:
        """
        Get tester group configuration for a specific track.

        Args:
            track: The Play Store track (e.g., 'internal', 'alpha').

        Returns:
            The group config dict, or None if no config for this track.
        """
        config = self._load_tester_config()
        groups = config.get("groups", {})

        for group_name, group_config in groups.items():
            if group_config.get("track") == track:
                return group_config

        return None

    def _assign_testers(
        self, package_name: str, track: str, emails: list[str]
    ) -> bool:
        """
        Assign tester emails to a Play Store track.

        In dry-run mode, skips the actual API call.

        Args:
            package_name: Android package name.
            track: Play Store track.
            emails: List of tester email addresses.

        Returns:
            True on success, False on failure.
        """
        if self.dry_run:
            logger.info(
                "[DRY RUN] Would assign %d testers to track '%s'.",
                len(emails),
                track,
            )
            return True

        try:
            from google.oauth2 import service_account
            from googleapiclient.discovery import build

            key_path = os.getenv("PLAY_STORE_JSON_KEY")
            if not key_path:
                logger.error("PLAY_STORE_JSON_KEY not set.")
                return False

            credentials = service_account.Credentials.from_service_account_file(
                key_path,
                scopes=["https://www.googleapis.com/auth/androidpublisher"],
            )
            service = build("androidpublisher", "v3", credentials=credentials)

            # Create an edit
            edit_request = service.edits().insert(
                body={}, packageName=package_name
            )
            edit = edit_request.execute()
            edit_id = edit["id"]

            # Set testers
            service.edits().testers().update(
                packageName=package_name,
                editId=edit_id,
                track=track,
                body={"googleGroups": emails},
            ).execute()

            # Commit the edit
            service.edits().commit(
                packageName=package_name, editId=edit_id
            ).execute()

            logger.info(
                "Assigned %d testers to track '%s'.", len(emails), track
            )
            return True

        except ImportError:
            logger.warning("google-api-python-client not installed.")
            return False
        except Exception as exc:
            logger.error("Failed to assign testers: %s", exc)
            return False

    def _should_promote(
        self, run: Any, config: dict[str, Any]
    ) -> bool:
        """
        Check if the current release should be auto-promoted.

        Auto-promotion requires:
        1. The config has an auto_promote_to target (not null)
        2. The QA agent passed in the current run

        Args:
            run: Current PipelineRun instance.
            config: Track config with potential auto_promote_to field.

        Returns:
            True if auto-promotion is warranted.
        """
        promote_to = config.get("auto_promote_to")
        if not promote_to:
            return False

        # Check if QA passed
        qa_state = run.get_agent("qa")
        if not qa_state.is_done():
            return False

        return True

    def execute(self) -> AgentResult:
        """
        Execute tester assignment.

        1. Load tester config
        2. Get track from pipeline state
        3. Find tester config for the track
        4. Assign testers if config exists
        5. Check promotion rules
        """
        run = self.memory.load()
        if run is None:
            return AgentResult.fail("No pipeline state found.")

        # Default track
        track = "internal"
        metadata_path = self.memory.artifacts_dir / "metadata.json"
        if metadata_path.exists():
            import json
            try:
                with open(metadata_path, "r") as f:
                    metadata = json.load(f)
                track = metadata.get("track", "internal")
            except Exception:
                pass

        package_name = run.package_name or os.getenv(
            "ANDROID_PACKAGE_NAME", ""
        )

        # Get tester config for this track
        track_config = self._get_track_config(track)

        testers_assigned = 0
        if track_config:
            emails = track_config.get("emails", [])
            if emails:
                success = self._assign_testers(package_name, track, emails)
                if not success:
                    return AgentResult.fail(
                        f"Failed to assign testers to track '{track}'."
                    )
                testers_assigned = len(emails)
        else:
            logger.info("No tester config found for track '%s'. Skipping.", track)

        # Check promotion
        should_promote = False
        if track_config:
            should_promote = self._should_promote(run, track_config)

        return AgentResult.ok({
            "track": track,
            "testers_assigned": testers_assigned,
            "should_promote": should_promote,
        })
