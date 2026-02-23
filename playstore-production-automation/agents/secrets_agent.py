"""
Secrets agent for the Android CI/CD AI Agent Pipeline.

Securely decodes and validates all pipeline secrets during Phase 1.
Runs in parallel with the Validator and Version agents. Handles base64
keystore decoding, keytool validation, and Play Store JSON key extraction.

SECURITY: Never logs, prints, or includes secret VALUES in any output.
Only logs existence/absence of secrets.
"""

from __future__ import annotations

import base64
import logging
import os
import stat
import subprocess
from pathlib import Path

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory

logger = logging.getLogger(__name__)

# Required environment variables sourced from GitHub Secrets
REQUIRED_ENV_VARS: list[str] = [
    "KEYSTORE_FILE_B64",
    "KEYSTORE_PASSWORD",
    "KEY_ALIAS",
    "KEY_PASSWORD",
    "PLAY_STORE_JSON_KEY",
]


class SecretsAgent(BaseAgent):
    """
    Pipeline agent that securely decodes and validates all CI/CD secrets.

    Responsibilities:
    - Validate that all required environment variables are set
    - Decode the base64 keystore and write it to the artifacts directory
    - Validate the keystore using keytool
    - Write the Play Store JSON key to the artifacts directory
    - Clean up sensitive files when done
    """

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
    ) -> None:
        """Initialize the SecretsAgent."""
        super().__init__(name="secrets", memory=memory, dry_run=dry_run)

    # ------------------------------------------------------------------ #
    #  Validation
    # ------------------------------------------------------------------ #

    def _resolve_keystore_b64(self) -> str:
        """
        Resolve keystore secret value with backward compatibility.

        Preferred variable is KEYSTORE_FILE_B64. Legacy KEYSTORE_B64 is
        accepted as a fallback and normalized to KEYSTORE_FILE_B64.
        """
        preferred = os.environ.get("KEYSTORE_FILE_B64", "")
        if preferred:
            return preferred

        legacy = os.environ.get("KEYSTORE_B64", "")
        if legacy:
            os.environ["KEYSTORE_FILE_B64"] = legacy
            logger.info("Using legacy secret name KEYSTORE_B64 as KEYSTORE_FILE_B64.")
            return legacy

        return ""

    def _validate_all_env_vars(self) -> tuple[bool, list[str]]:
        """
        Check each of the 5 required env vars is set and non-empty.

        Returns:
            (True, [])  — if all present and non-empty.
            (False, [list of missing names]) — otherwise.
        """
        missing: list[str] = []

        keystore_b64 = self._resolve_keystore_b64()
        if not keystore_b64:
            missing.append("Missing: KEYSTORE_FILE_B64")

        for var in REQUIRED_ENV_VARS:
            if var == "KEYSTORE_FILE_B64":
                continue
            value = os.environ.get(var, "")
            if not value:
                missing.append(f"Missing: {var}")
        if missing:
            logger.warning("Secret validation failed: %s", missing)
            return False, missing
        logger.info("All required secrets are present")
        return True, []

    # ------------------------------------------------------------------ #
    #  Keystore decoding
    # ------------------------------------------------------------------ #

    def _decode_keystore(self) -> str:
        """
        Read KEYSTORE_FILE_B64 from env, base64-decode it, and write to
        ``self.memory.artifacts_dir / "release.jks"``.

        Returns:
            The path to the written keystore file as a string.

        Raises:
            ValueError: If the base64 payload is invalid.
        """
        b64_data = self._resolve_keystore_b64()
        try:
            keystore_bytes = base64.b64decode(b64_data, validate=True)
        except Exception as exc:
            raise ValueError("Invalid base64 in KEYSTORE_FILE_B64") from exc

        keystore_path = self.memory.artifacts_dir / "release.jks"
        keystore_path.write_bytes(keystore_bytes)
        os.chmod(str(keystore_path), 0o600)

        logger.info("Keystore written to %s", keystore_path)
        return str(keystore_path)

    # ------------------------------------------------------------------ #
    #  Keystore validation via keytool
    # ------------------------------------------------------------------ #

    def _validate_keystore(self, keystore_path: str) -> bool:
        """
        Validate the keystore by running ``keytool -list``.

        Uses a two-step approach:
        1. First, verify the keystore can be opened with the store password
           (no alias check — validates the file + password).
        2. Then, optionally verify the alias exists (warning only).

        Args:
            keystore_path: Path to the keystore file.

        Returns:
            True if the keystore is valid and can be opened, False otherwise.
        """
        password = os.environ.get("KEYSTORE_PASSWORD", "")
        alias = os.environ.get("KEY_ALIAS", "")

        # Step 1: Validate keystore file + store password (list all entries)
        try:
            result = subprocess.run(
                [
                    "keytool",
                    "-list",
                    "-keystore",
                    keystore_path,
                    "-storepass",
                    password,
                ],
                capture_output=True,
                text=True,
                timeout=30,
            )
            if result.returncode != 0:
                logger.error(
                    "keytool: cannot open keystore (bad store password or corrupt file). "
                    "stderr: %s",
                    result.stderr.strip() if result.stderr else "no output",
                )
                return False
            logger.info("Keystore file validated successfully (store password OK).")
        except FileNotFoundError:
            logger.warning(
                "keytool not found on PATH — skipping keystore validation."
            )
            return True
        except subprocess.TimeoutExpired:
            logger.error("keytool timed out during keystore validation.")
            return False

        # Step 2: Verify alias exists (warning only, not a hard failure)
        if alias:
            try:
                alias_result = subprocess.run(
                    [
                        "keytool",
                        "-list",
                        "-keystore",
                        keystore_path,
                        "-storepass",
                        password,
                        "-alias",
                        alias,
                    ],
                    capture_output=True,
                    text=True,
                    timeout=30,
                )
                if alias_result.returncode != 0:
                    logger.warning(
                        "keytool: alias '%s' not found in keystore. "
                        "Available entries shown above. This may cause signing failures. "
                        "stderr: %s",
                        alias,
                        alias_result.stderr.strip() if alias_result.stderr else "no output",
                    )
                    # Not a hard failure — keystore itself is valid
                else:
                    logger.info("Keystore alias '%s' verified.", alias)
            except (subprocess.TimeoutExpired, FileNotFoundError):
                logger.warning("Could not verify alias — continuing anyway.")

        return True

    # ------------------------------------------------------------------ #
    #  Play Store JSON key
    # ------------------------------------------------------------------ #

    def _write_play_key(self) -> str:
        """
        Read PLAY_STORE_JSON_KEY from env and write to
        ``self.memory.artifacts_dir / "play_key.json"``.

        Returns:
            The path to the written file as a string.
        """
        json_key = os.environ.get("PLAY_STORE_JSON_KEY", "")
        play_key_path = self.memory.artifacts_dir / "play_key.json"
        play_key_path.write_text(json_key, encoding="utf-8")
        os.chmod(str(play_key_path), 0o600)

        logger.info("Play Store key written to %s", play_key_path)
        return str(play_key_path)

    # ------------------------------------------------------------------ #
    #  Cleanup
    # ------------------------------------------------------------------ #

    def cleanup(self) -> None:
        """
        Delete sensitive files (release.jks and play_key.json) from the
        artifacts directory. Ignores FileNotFoundError so that calling
        cleanup is always safe.
        """
        for filename in ("release.jks", "play_key.json"):
            filepath = self.memory.artifacts_dir / filename
            try:
                os.remove(str(filepath))
                logger.info("Cleaned up %s", filepath)
            except FileNotFoundError:
                pass

    # ------------------------------------------------------------------ #
    #  Main execution
    # ------------------------------------------------------------------ #

    def execute(self) -> AgentResult:
        """
        Execute the secrets agent workflow:

        1. Validate all env vars → fail immediately if any missing
        2. Decode keystore → write to artifacts
        3. Validate keystore with keytool
        4. Write play key → artifacts
        5. Return AgentResult.ok with paths

        Note: cleanup is orchestrated at pipeline end so downstream
        agents (Build/Upload) can consume these files.
        """
        # Step 1: Validate all environment variables
        valid, missing = self._validate_all_env_vars()
        if not valid:
            return AgentResult.fail(
                f"Missing secrets: {', '.join(missing)}"
            )

        # Step 2: Decode keystore
        try:
            keystore_path = self._decode_keystore()
        except ValueError as exc:
            return AgentResult.fail(str(exc))

        # Step 3: Validate keystore
        if not self._validate_keystore(keystore_path):
            self.cleanup()
            return AgentResult.fail(
                "Keystore validation failed: keytool returned non-zero"
            )

        # Step 4: Write Play Store key
        play_key_path = self._write_play_key()

        # Step 5: Success
        return AgentResult.ok(
            {
                "keystore_path": keystore_path,
                "play_key_path": play_key_path,
            }
        )
