"""
Orchestrator for the Android CI/CD AI Agent Pipeline.

Entry point for the entire system. Coordinates all 9 agents across
5 phases with parallel execution at sync points. Does NOT perform
agent work itself — reads state, decides what to run.
"""

from __future__ import annotations

import argparse
import logging
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Optional

from agents.base_agent import BaseAgent, AgentResult
from agents.memory import PipelineMemory, PipelineStatus

logger = logging.getLogger(__name__)


class Orchestrator:
    """
    Pipeline orchestrator that coordinates all agents.

    Manages the 5-phase pipeline execution, handling parallel agent
    execution within phases and sync points between phases.
    """

    def __init__(self, memory: Optional[PipelineMemory] = None) -> None:
        """Initialize the orchestrator with a memory instance."""
        self.memory = memory or PipelineMemory()

    def init(
        self,
        commit_sha: str,
        branch: str,
        triggered_by: str = "manual",
    ) -> None:
        """
        Initialize a new pipeline run.

        Args:
            commit_sha: Git commit SHA that triggered the run.
            branch: Git branch name.
            triggered_by: Who/what triggered the run.
        """
        run = self.memory.init_run(
            commit_sha=commit_sha,
            branch=branch,
            triggered_by=triggered_by,
        )
        print(run.summary())
        logger.info("Pipeline initialized: %s", run.run_id)

    def _create_agents(self, dry_run: bool = False) -> dict[str, BaseAgent]:
        """
        Instantiate all pipeline agents.

        Args:
            dry_run: If True, agents skip real operations.

        Returns:
            Dict mapping agent names to instances.
        """
        from agents.validator_agent import ValidatorAgent
        from agents.version_agent import VersionAgent
        from agents.secrets_agent import SecretsAgent
        from agents.build_agent import BuildAgent
        from agents.qa_agent import QAAgent
        from agents.metadata_agent import MetadataAgent
        from agents.upload_agent import UploadAgent
        from agents.tester_agent import TesterAgent
        from agents.notifier_agent import NotifierAgent

        project_root = self._resolve_project_root()

        return {
            "validator": ValidatorAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "version": VersionAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "secrets": SecretsAgent(memory=self.memory, dry_run=dry_run),
            "build": BuildAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "qa": QAAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "metadata": MetadataAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "upload": UploadAgent(
                memory=self.memory,
                dry_run=dry_run,
                project_root=project_root,
            ),
            "tester": TesterAgent(
                memory=self.memory,
                dry_run=dry_run,
                config_path=str(
                    Path(project_root) / "config" / "tester-groups.yaml"
                ),
            ),
            "notifier": NotifierAgent(memory=self.memory, dry_run=dry_run),
        }

    def _resolve_project_root(self) -> str:
        """
        Resolve Android project root for CI and local runs.

        Priority:
          1. PROJECT_ROOT env var
          2. GITHUB_WORKSPACE env var
          3. Repository root inferred from this file location
        """
        explicit = os.getenv("PROJECT_ROOT")
        if explicit:
            return str(Path(explicit).resolve())

        github_workspace = os.getenv("GITHUB_WORKSPACE")
        if github_workspace:
            return str(Path(github_workspace).resolve())

        return str(Path(__file__).resolve().parent.parent)

    def _run_phase_parallel(self, agents: list[BaseAgent]) -> bool:
        """
        Run a list of agents concurrently.

        Args:
            agents: List of agent instances to run in parallel.

        Returns:
            True if all agents succeeded.
        """
        if not agents:
            return True

        with ThreadPoolExecutor(max_workers=len(agents)) as executor:
            futures = {
                executor.submit(agent.run): agent.name for agent in agents
            }
            all_passed = True

            for future in as_completed(futures):
                agent_name = futures[future]
                try:
                    result = future.result()
                    if not result.success:
                        logger.error(
                            "Agent '%s' failed: %s", agent_name, result.error
                        )
                        all_passed = False
                    else:
                        logger.info("Agent '%s' completed successfully.", agent_name)
                except Exception as exc:
                    logger.error(
                        "Agent '%s' raised exception: %s", agent_name, exc
                    )
                    all_passed = False

        return all_passed

    def _run_phase_serial(self, agent: BaseAgent) -> bool:
        """
        Run a single agent.

        Args:
            agent: Agent instance to run.

        Returns:
            True if the agent succeeded.
        """
        result = agent.run()
        if result.success:
            logger.info("Agent '%s' completed successfully.", agent.name)
        else:
            logger.error("Agent '%s' failed: %s", agent.name, result.error)
        return result.success

    def _sync_point(self, phase: str) -> None:
        """
        Verify all agents in a phase completed successfully.

        Args:
            phase: Phase identifier (e.g., "P1", "P2").

        Raises:
            RuntimeError: If any agent in the phase failed.
        """
        run = self.memory.load()
        if run is None:
            raise RuntimeError("No pipeline state found at sync point.")

        if not run.phase_is_complete(phase):
            # Identify which agents failed
            from agents.memory import PHASE_AGENTS
            agent_names = PHASE_AGENTS.get(phase, [])
            failed = []
            for name in agent_names:
                agent_state = run.get_agent(name)
                if not agent_state.is_done():
                    failed.append(
                        f"{name}: {agent_state.status} "
                        f"({agent_state.error or 'no error details'})"
                    )

            raise RuntimeError(
                f"Phase {phase} sync point failed. "
                f"Failed agents: {'; '.join(failed)}"
            )

        # Update current phase
        run.current_phase = phase
        self.memory.save(run)
        logger.info("Sync point %s passed.", phase)

    def run(self, dry_run: bool = False) -> bool:
        """
        Execute all pipeline phases in order.

        Args:
            dry_run: If True, agents skip real operations.

        Returns:
            True if the entire pipeline succeeded.
        """
        agents = self._create_agents(dry_run=dry_run)

        try:
            # Phase 1: Validator + Version + Secrets (parallel)
            logger.info("=== Phase 1: Validation, Version, Secrets ===")
            self._run_phase_parallel([
                agents["validator"],
                agents["version"],
                agents["secrets"],
            ])
            self._sync_point("P1")

            # Phase 2: Build (serial)
            logger.info("=== Phase 2: Build ===")
            self._run_phase_serial(agents["build"])
            self._sync_point("P2")

            # Phase 3: QA + Metadata (parallel)
            logger.info("=== Phase 3: QA and Metadata ===")
            self._run_phase_parallel([
                agents["qa"],
                agents["metadata"],
            ])
            self._sync_point("P3")

            # Phase 4: Upload (serial)
            logger.info("=== Phase 4: Upload ===")
            self._run_phase_serial(agents["upload"])
            self._sync_point("P4")

            # Phase 5: Tester + Notifier (parallel, no sync)
            logger.info("=== Phase 5: Tester and Notifier ===")
            self._run_phase_parallel([
                agents["tester"],
                agents["notifier"],
            ])

            # Mark pipeline success
            run = self.memory.load()
            if run:
                run.status = PipelineStatus.SUCCESS
                self.memory.save(run)

            logger.info("🎉 Pipeline completed successfully!")
            return True

        except RuntimeError as exc:
            logger.error("Pipeline failed: %s", exc)
            run = self.memory.load()
            if run:
                run.status = PipelineStatus.FAILED
                run.errors.append(str(exc))
                self.memory.save(run)

            # Still run notifier on failure
            try:
                agents["notifier"].run()
            except Exception:
                pass

            return False
        finally:
            # Ensure sensitive artifacts are always removed at pipeline end.
            try:
                agents["secrets"].cleanup()
            except Exception:
                pass

    def resume(self, from_phase: str) -> bool:
        """
        Resume the pipeline from a specific phase.

        Resets agents from the given phase onward, then runs.

        Args:
            from_phase: Phase to restart from (e.g., "P3").

        Returns:
            True if the resumed pipeline succeeded.

        Raises:
            ValueError: If the phase is not valid.
        """
        from agents.memory import PHASE_AGENTS
        valid_phases = list(PHASE_AGENTS.keys())
        if from_phase not in valid_phases:
            raise ValueError(
                f"Invalid phase '{from_phase}'. "
                f"Valid phases: {', '.join(valid_phases)}"
            )

        result = self.memory.resume_from_phase(from_phase)
        if result is None:
            raise RuntimeError(
                "No pipeline state found. Cannot resume. "
                "Run --init first."
            )

        logger.info("Resuming pipeline from phase %s.", from_phase)
        print(result.summary())
        return self.run()

    def status(self) -> None:
        """Print the current pipeline status."""
        run = self.memory.load()
        if run is None:
            print("Pipeline: NOT STARTED")
            print("No state file found. Run --init to start a new pipeline.")
            return
        print(run.summary())

    def history(self) -> None:
        """Print all historical pipeline runs."""
        runs = self.memory.get_history()
        if not runs:
            print("No pipeline history found.")
            return

        for run_data in runs:
            run_id = run_data.get("run_id", "unknown")
            status = run_data.get("status", "unknown")
            started = run_data.get("started_at", "unknown")
            print(f"  {run_id}  |  {status}  |  {started}")


def main() -> None:
    """CLI entry point for the orchestrator."""
    parser = argparse.ArgumentParser(
        description="Android CI/CD Pipeline Orchestrator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""\
Examples:
  python -m agents.orchestrator --init --commit abc123 --branch release
  python -m agents.orchestrator --run
  python -m agents.orchestrator --dry-run
  python -m agents.orchestrator --resume --from-phase P3
  python -m agents.orchestrator --status
  python -m agents.orchestrator --history
""",
    )

    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--init", action="store_true", help="Initialize a new run")
    group.add_argument("--run", action="store_true", help="Execute the pipeline")
    group.add_argument("--dry-run", action="store_true", help="Dry-run (no real ops)")
    group.add_argument("--resume", action="store_true", help="Resume from a phase")
    group.add_argument("--status", action="store_true", help="Show current status")
    group.add_argument("--history", action="store_true", help="Show run history")

    parser.add_argument("--commit", help="Commit SHA (required with --init)")
    parser.add_argument("--branch", default="release", help="Branch name")
    parser.add_argument("--from-phase", help="Phase to resume from (e.g., P3)")

    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    orchestrator = Orchestrator()

    try:
        if args.init:
            if not args.commit:
                parser.error("--init requires --commit")
            orchestrator.init(args.commit, args.branch)

        elif args.run:
            success = orchestrator.run()
            sys.exit(0 if success else 1)

        elif args.dry_run:
            success = orchestrator.run(dry_run=True)
            sys.exit(0 if success else 1)

        elif args.resume:
            if not args.from_phase:
                parser.error("--resume requires --from-phase")
            success = orchestrator.resume(args.from_phase)
            sys.exit(0 if success else 1)

        elif args.status:
            orchestrator.status()

        elif args.history:
            orchestrator.history()

    except Exception as exc:
        logger.error("Orchestrator error: %s", exc)
        orchestrator.status()
        sys.exit(1)


if __name__ == "__main__":
    main()
