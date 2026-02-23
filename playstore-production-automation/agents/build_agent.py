"""
Build Agent for the Android CI/CD AI Agent Pipeline.

Runs in Phase 2, after Phase 1 (Validator + Version + Secrets) completes.
Uses the keystore decoded by the Secrets Agent to produce a signed AAB
via Gradle's bundleRelease task.
"""

from __future__ import annotations

import logging
import os
import subprocess
from pathlib import Path

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory

logger = logging.getLogger(__name__)


class BuildAgent(BaseAgent):
    """
    Agent that builds a signed Android App Bundle (AAB).

    Reads the keystore written by the Secrets Agent, invokes Gradle's
    release bundle task, and verifies the AAB output. The path to the
    produced AAB is saved in the pipeline state for downstream agents.
    """

    # Standard output path for a release AAB produced by Gradle
    AAB_OUTPUT_PATH = "app/build/outputs/bundle/prodRelease/app-prod-release.aab"

    def __init__(
        self,
        memory: PipelineMemory,
        dry_run: bool = False,
        project_root: str = ".",
        max_retries: int = 3,
    ) -> None:
        """Initialize the Build Agent."""
        super().__init__(
            name="build",
            memory=memory,
            dry_run=dry_run,
            max_retries=max_retries,
        )
        self.project_root = Path(project_root).resolve()

    def _resolve_keystore_path(self) -> str:
        """
        Locate the keystore written by the Secrets Agent.

        Returns:
            Absolute path to the keystore file.

        Raises:
            FileNotFoundError: If the keystore has not been written by the
                Secrets Agent.
        """
        keystore_path = (self.memory.artifacts_dir / "release.jks").resolve()
        if not keystore_path.exists():
            raise FileNotFoundError(
                f"Keystore not found at {keystore_path}. "
                "Did the Secrets Agent run successfully?"
            )
        return str(keystore_path)

    def _run_build(self, keystore_path: str) -> tuple[bool, str]:
        """
        Execute a signed Gradle bundleRelease build.

        Injects signing properties via -Pandroid.injected.signing.* args.

        Args:
            keystore_path: Absolute path to the signing keystore.

        Returns:
            Tuple of (success, output_text).
        """
        env = os.environ.copy()
        store_password = env.get("KEYSTORE_PASSWORD", "")
        key_alias = env.get("KEY_ALIAS", "")
        key_password = env.get("KEY_PASSWORD", "")
        gradlew = self.project_root / "gradlew"

        try:
            result = subprocess.run(
                [
                    str(gradlew),
                    "bundleProdRelease",
                    "--stacktrace",
                    "--no-daemon",
                    f"-Pandroid.injected.signing.store.file={keystore_path}",
                    f"-Pandroid.injected.signing.store.password={store_password}",
                    f"-Pandroid.injected.signing.key.alias={key_alias}",
                    f"-Pandroid.injected.signing.key.password={key_password}",
                ],
                capture_output=True,
                text=True,
                timeout=1200,  # 20-minute timeout
                cwd=str(self.project_root),
                env=env,
            )

            if result.returncode == 0:
                logger.info("Gradle bundleRelease succeeded.")
                return True, result.stdout
            error_output = self._sanitize_build_output(
                self._tail_output((result.stdout or "") + (result.stderr or ""))
            )
            logger.error("Gradle bundleRelease failed:\n%s", error_output)
            return False, error_output

        except FileNotFoundError:
            msg = f"gradlew not found at {gradlew}. Ensure Gradle wrapper is committed."
            logger.error(msg)
            return False, msg

        except subprocess.TimeoutExpired as exc:
            partial = self._tail_output((exc.stdout or "") + (exc.stderr or ""))
            partial = self._sanitize_build_output(partial)
            msg = f"Build timed out after 20 minutes.\n{partial}".strip()
            logger.error(msg)
            return False, msg

    def _tail_output(self, text: str, max_lines: int = 80, max_chars: int = 8000) -> str:
        """
        Keep only the most relevant end of large command output.
        """
        if not text:
            return ""
        lines = text.strip().splitlines()
        tail = "\n".join(lines[-max_lines:])
        if len(tail) > max_chars:
            tail = tail[-max_chars:]
        return tail

    def _sanitize_build_output(self, text: str) -> str:
        """
        Redact secret values from build output before logging.

        Only non-empty env secret values are redacted.
        """
        redacted = text
        for key in ("KEYSTORE_PASSWORD", "KEY_PASSWORD", "PLAY_STORE_JSON_KEY"):
            value = os.environ.get(key, "")
            if value:
                redacted = redacted.replace(value, "***")
        return redacted

    def _verify_output(self) -> bool:
        """
        Verify the AAB file was produced and is non-empty.

        Returns:
            True if a valid AAB file exists at the expected path.
        """
        aab_path = self.project_root / self.AAB_OUTPUT_PATH
        if not aab_path.exists():
            logger.error("AAB not found at %s", aab_path)
            return False
        if aab_path.stat().st_size == 0:
            logger.error("AAB file is empty at %s", aab_path)
            return False
        return True

    def _get_aab_path(self) -> Path:
        """Return the full path to the expected AAB output."""
        return self.project_root / self.AAB_OUTPUT_PATH

    def _get_aab_size_mb(self) -> float:
        """
        Get the AAB file size in megabytes.

        Returns:
            Size in MB rounded to 2 decimal places.
        """
        aab_path = self._get_aab_path()
        if not aab_path.exists():
            return 0.0
        size_bytes = aab_path.stat().st_size
        return round(size_bytes / (1024 * 1024), 2)

    def execute(self) -> AgentResult:
        """
        Execute the build pipeline.

        1. Resolve keystore path (from Secrets Agent output)
        2. Run Gradle bundleRelease
        3. Verify AAB output exists and is non-empty
        4. Save aab_path to pipeline state
        5. Return result with path and size
        """
        # Step 1: Resolve keystore
        try:
            keystore_path = self._resolve_keystore_path()
        except FileNotFoundError as exc:
            return AgentResult.fail(str(exc))

        logger.info("Using keystore at: %s", keystore_path)

        # Step 2: Run build
        success, output = self._run_build(keystore_path)
        if not success:
            return AgentResult.fail(f"Build failed: {output}")

        # Step 3: Verify output
        if not self._verify_output():
            return AgentResult.fail(
                f"Build completed but AAB not found at "
                f"{self._get_aab_path()}. Check Gradle configuration."
            )

        # Step 4: Save AAB path to state
        aab_path = str(self._get_aab_path())
        size_mb = self._get_aab_size_mb()

        run = self.memory.load()
        if run is not None:
            run.aab_path = aab_path
            self.memory.save(run)

        logger.info("Build succeeded: %s (%.2f MB)", aab_path, size_mb)

        # Step 5: Return result
        return AgentResult.ok({
            "aab_path": aab_path,
            "size_mb": size_mb,
        })
