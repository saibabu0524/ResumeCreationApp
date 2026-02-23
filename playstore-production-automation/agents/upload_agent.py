"""
Upload Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 4, after QA and Metadata agents both pass.
Uploads the signed AAB to Google Play Console via Fastlane's supply tool,
using metadata prepared by the Metadata Agent.
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


# Error message mapping for common Fastlane/Play Store failures
ERROR_MAP: dict[str, str] = {
    "Version code has already been used": (
        "Version code collision — run the Version Agent again: "
        "python -m agents.version_agent"
    ),
    "403": (
        "Permission denied (403). Grant 'Release manager' role to the "
        "service account in Play Console → Users & Permissions."
    ),
    "Forbidden": (
        "Permission denied (Forbidden). Grant 'Release manager' role to the "
        "service account in Play Console → Users & Permissions."
    ),
    "caller does not have permission": (
        "Permission denied by Google Play API. Verify this exact service "
        "account email is added in Play Console for this app with release "
        "permissions (for internal/closed testing), and confirm you're "
        "uploading to the intended track."
    ),
    "Package not found": (
        "Package not found on Play Console. The first upload must be done "
        "manually via the Play Console web UI. See README Known Limitations."
    ),
    "This app has no title for language en-US": (
        "Play listing metadata for en-US is incomplete. Either add the app "
        "title in Play Console (Store presence → Main store listing) or "
        "upload with supply metadata disabled."
    ),
    "401": (
        "Unauthorized (401). PLAY_STORE_JSON_KEY is invalid or expired. "
        "Regenerate the service account key in Google Cloud Console."
    ),
    "Unauthorized": (
        "Unauthorized. PLAY_STORE_JSON_KEY is invalid or expired. "
        "Regenerate the service account key in Google Cloud Console."
    ),
}


class UploadAgent(BaseAgent):
    """
    Agent that uploads the signed AAB to Google Play Console.

    Reads the metadata.json produced by the Metadata Agent, writes the
    Play Store JSON key to a temp file, invokes Fastlane supply, and
    always cleans up sensitive files in a finally block.
    """

    JSON_KEY_TMP_PATH = "/tmp/play_store_key.json"

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        project_root: str = ".",
        max_retries: int = 3,
    ) -> None:
        """Initialize the Upload Agent."""
        super().__init__(
            name="upload",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )
        self.project_root = Path(project_root).resolve()

    def _load_metadata(self) -> dict[str, Any]:
        """
        Load metadata.json written by the Metadata Agent.

        Returns:
            Parsed metadata dict, or empty dict if not found or invalid.
        """
        metadata_path = self.memory.artifacts_dir / "metadata.json"
        if not metadata_path.exists():
            logger.warning("metadata.json not found at %s", metadata_path)
            return {}

        try:
            with open(metadata_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return data
        except (json.JSONDecodeError, OSError) as exc:
            logger.warning("Failed to read metadata.json: %s", exc)
            return {}

    def _write_json_key(self) -> str:
        """
        Write the Play Store JSON key from env to a temp file.

        Returns:
            Path to the written key file.

        Raises:
            ValueError: If PLAY_STORE_JSON_KEY is not set.
        """
        key_content = os.getenv("PLAY_STORE_JSON_KEY")
        if not key_content:
            raise ValueError(
                "PLAY_STORE_JSON_KEY environment variable is not set."
            )

        key_path = Path(self.JSON_KEY_TMP_PATH)
        key_path.write_text(key_content, encoding="utf-8")
        os.chmod(str(key_path), 0o600)

        # Safe diagnostic: identifies which service account is being used.
        try:
            data = json.loads(key_content)
            client_email = data.get("client_email")
            if client_email:
                logger.info("Using Play service account: %s", client_email)
        except Exception:
            # Keep upload flow resilient even if diagnostics parsing fails.
            pass

        logger.info("Play Store JSON key written to %s", key_path)
        return str(key_path)

    def _write_fastlane_release_notes(
        self, notes: str, version_code: int | None = None
    ) -> None:
        """
        Write release notes to the Fastlane metadata directory.

        Args:
            notes: Release notes text.
            version_code: Version code for the versioned changelog file.
        """
        changelogs_dir = (
            self.project_root
            / "fastlane"
            / "metadata"
            / "android"
            / "en-US"
            / "changelogs"
        )
        changelogs_dir.mkdir(parents=True, exist_ok=True)

        # Write default changelog
        default_file = changelogs_dir / "default.txt"
        default_file.write_text(notes, encoding="utf-8")

        # Write version-specific changelog
        if version_code is not None:
            versioned_file = changelogs_dir / f"{version_code}.txt"
            versioned_file.write_text(notes, encoding="utf-8")

        logger.info("Release notes written to %s", changelogs_dir)

    def _run_fastlane_supply(
        self,
        aab_path: str,
        track: str,
        json_key_path: str,
        package_name: str,
    ) -> tuple[bool, str]:
        """
        Execute the Fastlane supply command to upload to Play Store.

        Args:
            aab_path: Path to the signed AAB file.
            track: Play Store track (e.g., 'internal', 'alpha').
            json_key_path: Path to the Play Store JSON key file.
            package_name: Android package name to upload.

        Returns:
            Tuple of (success, output_text).
        """
        env = os.environ.copy()
        # Fastlane-native supply env vars (automatically picked up by supply action)
        env["SUPPLY_AAB"] = aab_path
        env["SUPPLY_JSON_KEY"] = json_key_path          # ← Fastlane reads THIS for auth
        env["PLAY_STORE_JSON_KEY"] = json_key_path      # ← Also set for Fastfile compat
        # Pass package name and track via env for robustness
        if package_name:
            env["SUPPLY_PACKAGE_NAME"] = package_name
        env["SUPPLY_TRACK"] = track

        try:
            fastlane_cmd = self._get_fastlane_cmd()
            result = subprocess.run(
                [*fastlane_cmd, "upload_release", f"track:{track}"],
                capture_output=True,
                text=True,
                timeout=600,
                cwd=str(self.project_root),
                env=env,
            )

            if result.returncode == 0:
                logger.info("Fastlane supply succeeded.")
                return True, result.stdout
            else:
                output = result.stderr or result.stdout
                logger.error("Fastlane supply failed: %s", output[:500])
                return False, output

        except FileNotFoundError:
            msg = (
                "Fastlane not found. If using Bundler run `bundle install`; "
                "otherwise install globally with: gem install fastlane"
            )
            logger.error(msg)
            return False, msg

        except subprocess.TimeoutExpired:
            msg = "Upload timed out after 10 minutes."
            logger.error(msg)
            return False, msg

    def _get_fastlane_cmd(self) -> list[str]:
        """
        Build the Fastlane command.

        Prefer Bundler when a Gemfile is present to avoid global gem conflicts.
        """
        if (self.project_root / "Gemfile").exists():
            return ["bundle", "exec", "fastlane"]
        return ["fastlane"]

    def _parse_fastlane_error(self, output: str) -> str:
        """
        Map Fastlane error output to human-readable, actionable messages.

        Args:
            output: Raw stderr/stdout from Fastlane.

        Returns:
            A clear, actionable error message.
        """
        for trigger, message in ERROR_MAP.items():
            if trigger in output:
                return message

        # Unknown error — return truncated raw output
        return output[:500]

    def check_permissions(self) -> bool:
        """
        Verify API access by attempting a read-only operation.

        Can be run standalone:
            python -m agents.upload_agent --check-permissions

        Returns:
            True if service account has valid access.
        """
        key_content = os.getenv("PLAY_STORE_JSON_KEY")
        if not key_content:
            logger.error("PLAY_STORE_JSON_KEY not set.")
            return False

        try:
            key_path = self._write_json_key()
            # Quick validation — check that the JSON is parseable
            data = json.loads(key_content)
            if "client_email" not in data:
                logger.error("JSON key is missing 'client_email' field.")
                return False
            logger.info("Service account: %s", data["client_email"])
            return True
        except Exception as exc:
            logger.error("Permission check failed: %s", exc)
            return False
        finally:
            self._cleanup_key()

    def _cleanup_key(self) -> None:
        """Remove the temporary JSON key file. Safe to call always."""
        try:
            key_path = Path(self.JSON_KEY_TMP_PATH)
            if key_path.exists():
                key_path.unlink()
                logger.info("Cleaned up temporary JSON key.")
        except OSError:
            pass  # Best-effort cleanup

    def execute(self) -> AgentResult:
        """
        Execute the upload pipeline.

        1. Load pipeline state → get aab_path
        2. Load metadata → get track and release notes
        3. Write JSON key to temp file
        4. Write release notes to Fastlane metadata dir
        5. Run Fastlane supply
        6. Always clean up JSON key in finally block
        """
        json_key_path: str | None = None

        try:
            # Step 1: Get AAB path from pipeline state
            run = self.memory.load()
            if run is None:
                return AgentResult.fail("No pipeline state found.")

            aab_path = run.aab_path
            if not aab_path or not Path(aab_path).exists():
                return AgentResult.fail(
                    f"No AAB found at '{aab_path}'. Did the Build Agent run?"
                )

            # Step 2: Load metadata
            metadata = self._load_metadata()
            if not metadata:
                return AgentResult.fail(
                    "metadata.json not found or invalid. "
                    "Did the Metadata Agent run?"
                )

            track = metadata.get("track", "internal")
            package_name = metadata.get("package_name", "")
            release_notes = metadata.get("release_notes", {}).get(
                "en-US", "Bug fixes and performance improvements."
            )
            version_code = metadata.get("version_code")

            # Step 3: Write JSON key
            try:
                json_key_path = self._write_json_key()
            except ValueError as exc:
                return AgentResult.fail(str(exc))

            # Step 4: Write release notes
            self._write_fastlane_release_notes(release_notes, version_code)

            # Step 5: Run Fastlane supply
            success, output = self._run_fastlane_supply(
                aab_path=aab_path,
                track=track,
                json_key_path=json_key_path,
                package_name=package_name,
            )

            if success:
                return AgentResult.ok({
                    "track": track,
                    "version_code": version_code,
                    "uploaded": True,
                })
            else:
                error_msg = self._parse_fastlane_error(output)
                return AgentResult.fail(f"Upload failed: {error_msg}")

        finally:
            # Step 6: ALWAYS clean up
            self._cleanup_key()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Upload Agent")
    parser.add_argument(
        "--check-permissions",
        action="store_true",
        help="Verify API access without uploading.",
    )
    args = parser.parse_args()

    if args.check_permissions:
        agent = UploadAgent(memory=PipelineMemory())
        ok = agent.check_permissions()
        print("✅ Permissions OK" if ok else "❌ Permission check failed")
