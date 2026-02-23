# Implementation Plan — Android CI/CD AI Parallel Agent Pipeline

> **Purpose:** Step-by-step execution plan for building the full pipeline from zero to production.
> Every milestone has clear tasks, acceptance criteria, and the exact files to create.
> Follow this top to bottom. Do not skip phases.

---

## Table of Contents

1. [Project Summary](#1-project-summary)
2. [Prerequisites & One-Time Manual Steps](#2-prerequisites--one-time-manual-steps)
3. [Milestone Overview](#3-milestone-overview)
4. [Milestone 1 — Project Scaffold & Memory System](#milestone-1--project-scaffold--memory-system)
5. [Milestone 2 — Phase 1 Agents (Parallel)](#milestone-2--phase-1-agents-parallel)
6. [Milestone 3 — Phase 2: Build Agent](#milestone-3--phase-2-build-agent)
7. [Milestone 4 — Phase 3 Agents (Parallel)](#milestone-4--phase-3-agents-parallel)
8. [Milestone 5 — Phase 4: Upload Agent](#milestone-5--phase-4-upload-agent)
9. [Milestone 6 — Phase 5 Agents + Orchestrator](#milestone-6--phase-5-agents--orchestrator)
10. [Milestone 7 — GitHub Actions Workflow](#milestone-7--github-actions-workflow)
11. [Milestone 8 — End-to-End Testing](#milestone-8--end-to-end-testing)
12. [Dependency Map](#dependency-map)
13. [Timeline Summary](#timeline-summary)
14. [Definition of Done](#definition-of-done)

---

## 1. Project Summary

| Item | Detail |
|------|--------|
| **Goal** | Merge to `release` branch → app automatically lands in Google Play Console |
| **Approach** | 9 AI agents, 5 pipeline phases, max parallelism |
| **Language** | Python 3.11+ (agents), YAML (CI/CD), Ruby (Fastlane) |
| **CI Platform** | GitHub Actions |
| **Memory** | File-based state in `.pipeline/state.json` — survives session loss |
| **Total pipeline time** | ~18–22 minutes (vs 40+ manually) |
| **Test requirement** | 100% of functions must have tests before merging |

---

## 2. Prerequisites & One-Time Manual Steps

These steps are done **once by a human** before any automation can work.
They cannot be automated — do them before writing any code.

### 2.1 Google Play Console — Manual First Upload

> ⚠️ **The Google Play API cannot create a brand-new app.** The very first
> build must be uploaded manually through the Play Console web UI.

- [ ] Go to [play.google.com/console](https://play.google.com/console)
- [ ] Create a new app → fill in name, language, app/game, free/paid
- [ ] Upload an initial AAB manually (can be a debug build)
- [ ] Complete the store listing form (basic details only)
- [ ] Save the **Package Name** — you will need it (e.g. `com.yourcompany.app`)

### 2.2 Google Cloud Service Account Setup

- [ ] Go to [console.cloud.google.com](https://console.cloud.google.com)
- [ ] Select or create a project linked to your Play Console account
- [ ] Navigate to **IAM & Admin → Service Accounts**
- [ ] Click **Create Service Account**
  - Name: `android-cicd-agent`
  - Description: `Service account for automated Play Store uploads`
- [ ] After creation, click the service account → **Keys** tab → **Add Key → JSON**
- [ ] Download the JSON key file — save as `play_store_key.json` temporarily
- [ ] Back in Play Console → **Users & Permissions → Invite new users**
  - Email: paste the service account email (ends in `.iam.gserviceaccount.com`)
  - Role: **Release Manager**
  - Apply to your app

### 2.3 Android Keystore

- [ ] If you don't have a keystore, generate one:
  ```bash
  keytool -genkey -v \
    -keystore release.jks \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -alias my-key-alias
  ```
- [ ] **Back up the keystore in at least 3 places.** Losing it means losing your app.
- [ ] Note down: `keystore password`, `key alias`, `key password`

### 2.4 Encode Keystore to Base64

```bash
base64 -i release.jks | tr -d '\n' > keystore_b64.txt
```

### 2.5 GitHub Secrets — Store Everything

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_FILE_B64` | Contents of `keystore_b64.txt` |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | Your key alias |
| `KEY_PASSWORD` | Your key password |
| `PLAY_STORE_JSON_KEY` | Full contents of `play_store_key.json` |
| `ANTHROPIC_API_KEY` | Your Claude API key |
| `SLACK_WEBHOOK_URL` | Slack incoming webhook URL |

Go to **Settings → Variables → Actions → New repository variable**

| Variable Name | Value |
|---------------|-------|
| `ANDROID_PACKAGE_NAME` | `com.yourcompany.yourapp` |

> 🔒 After storing all secrets, delete your local copies of `play_store_key.json`
> and `keystore_b64.txt`. They should never exist on disk outside of CI.

---

## 3. Milestone Overview

```
M1 ──────────────────────────────────────────────── Project Scaffold + Memory System
  └─► M2 ──────────────────────────────────────── Phase 1 Agents (Validator, Version, Secrets)
        └─► M3 ────────────────────────────────── Phase 2 Build Agent
              └─► M4 ──────────────────────────── Phase 3 Agents (QA, Metadata) ─────────────┐
                    └─► M5 ──────────────────────── Phase 4 Upload Agent                     │
                          └─► M6 ────────────────── Phase 5 Agents + Orchestrator            │
                                └─► M7 ──────────── GitHub Actions Workflow                  │
                                      └─► M8 ──────── End-to-End Testing + Launch ◄──────────┘
```

| Milestone | Focus | Estimated Effort | Parallel Work Available? |
|-----------|-------|-----------------|------------------------|
| M1 | Scaffold, memory, base classes | 1–2 days | No — everything depends on this |
| M2 | Validator, Version, Secrets agents | 2–3 days | ✅ Yes — 3 agents in parallel |
| M3 | Build agent + Fastlane setup | 1–2 days | No |
| M4 | QA agent + Metadata agent | 2 days | ✅ Yes — 2 agents in parallel |
| M5 | Upload agent + Play Console integration | 2 days | No |
| M6 | Tester, Notifier, Orchestrator | 2 days | ✅ Yes — Tester + Notifier in parallel |
| M7 | GitHub Actions YAML + secrets wiring | 1 day | No |
| M8 | Integration tests + dry run + live run | 2 days | No |
| **Total** | | **~13–16 days** | |

---

## Milestone 1 — Project Scaffold & Memory System

> **Goal:** A clean, runnable repo with the memory system working and all tests passing.
> Nothing else is built until this is solid — every agent depends on it.

### Tasks

#### 1.1 Create repository structure

```bash
mkdir -p android-cicd-agents/{agents,tests,config,.github/workflows,fastlane}
touch android-cicd-agents/agents/__init__.py
touch android-cicd-agents/tests/__init__.py
```

Create these empty placeholder files so imports work:
```
agents/__init__.py
agents/memory.py          ← implement in 1.2
agents/base_agent.py      ← implement in 1.3
tests/__init__.py
tests/test_memory.py      ← implement in 1.4
tests/test_base_agent.py  ← implement in 1.4
requirements.txt          ← implement in 1.5
.gitignore                ← implement in 1.6
```

#### 1.2 Implement `agents/memory.py`

The full implementation is in `AGENTS.md` section 1. Key classes to build:

| Class | Purpose |
|-------|---------|
| `AgentStatus` | Enum: PENDING, RUNNING, DONE, FAILED, SKIPPED |
| `PipelineStatus` | Enum: NOT_STARTED, IN_PROGRESS, SUCCESS, FAILED, NEEDS_MANUAL |
| `AgentState` | Dataclass for one agent's status, result, timestamps, errors |
| `PipelineRun` | Full pipeline run state — serialized to `state.json` |
| `PipelineMemory` | Reads/writes state, manages locks, archives history |

**Critical methods to implement on `PipelineMemory`:**

```python
init_run(commit_sha, branch, triggered_by, package_name) → PipelineRun
load() → Optional[PipelineRun]
save(run: PipelineRun) → None           # must be atomic (write tmp → rename)
mark_agent_running(agent_name) → PipelineRun
mark_agent_done(agent_name, output) → PipelineRun
mark_agent_failed(agent_name, error) → PipelineRun
set_ai_notes(notes: str) → None
acquire_lock(lock_name) → bool          # for version collision prevention
release_lock(lock_name) → None
resume_from_phase(phase: str) → Optional[PipelineRun]
get_history() → list[dict]
```

#### 1.3 Implement `agents/base_agent.py`

| Class | Purpose |
|-------|---------|
| `AgentResult` | Standard result: `.ok(data)` or `.fail(error)` |
| `BaseAgent` | Abstract base with `run()` lifecycle + `execute()` to implement |

**`BaseAgent.run()` must handle:**
- Check if agent is already DONE (idempotency — safe to re-run)
- Dry-run mode (skip real operations)
- Mark as RUNNING before executing
- Retry loop up to `MAX_RETRIES` (default 3)
- Catch all exceptions, format them cleanly
- Mark DONE or FAILED based on outcome
- Call `self.memory.save()` at each state change

#### 1.4 Write tests — `tests/test_memory.py` and `tests/test_base_agent.py`

Every test must follow the naming pattern: `test_<function>_when_<condition>_<expected>`

**Required tests for `test_memory.py`:**

```
TestPipelineMemoryInit
  test_init_when_called_creates_required_directories
  test_init_run_when_called_creates_state_file
  test_init_run_when_called_sets_all_agents_to_pending

TestPipelineMemoryLoadSave
  test_load_when_no_state_file_returns_none
  test_load_after_save_returns_same_data
  test_save_is_atomic_when_file_exists_overwrites_cleanly
  test_load_when_state_file_corrupted_returns_none

TestAgentStateTransitions
  test_mark_agent_running_when_pending_changes_status
  test_mark_agent_done_when_running_changes_status
  test_mark_agent_failed_when_running_changes_status
  test_mark_agent_failed_when_failed_increments_retry_count
  test_agent_can_retry_when_failed_and_under_max
  test_agent_cannot_retry_when_failed_and_at_max

TestPhaseCompletion
  test_phase_is_complete_when_all_phase_agents_done_returns_true
  test_phase_is_complete_when_one_agent_not_done_returns_false
  test_phase_is_complete_when_agent_failed_returns_false

TestResumeFromPhase
  test_resume_from_phase_when_p3_resets_p3_agents
  test_resume_from_phase_when_resumed_preserves_earlier_phases
  test_resume_from_phase_when_no_state_returns_none

TestRunSummary
  test_summary_when_pipeline_running_contains_status
  test_summary_when_agent_failed_shows_error
  test_summary_when_ai_notes_present_includes_them
```

#### 1.5 Create `requirements.txt`

```
anthropic>=0.34.0
requests>=2.31.0
pyyaml>=6.0.1
pytest>=7.4.0
pytest-cov>=4.1.0
pytest-mock>=3.11.0
python-dotenv>=1.0.0
google-auth>=2.23.0
google-auth-httplib2>=0.1.1
google-api-python-client>=2.100.0
```

#### 1.6 Create `.gitignore`

```gitignore
# Pipeline state — runtime only, never commit
.pipeline/
*.jks
*.keystore
play_store_key.json
keystore_b64.txt
/tmp/play_store_key.json

# Python
__pycache__/
*.pyc
*.pyo
.pytest_cache/
htmlcov/
.coverage

# Env
.env
.env.local

# OS
.DS_Store
Thumbs.db
```

### Milestone 1 Acceptance Criteria

- [ ] `pytest tests/test_memory.py -v` → all tests pass
- [ ] `pytest tests/test_base_agent.py -v` → all tests pass
- [ ] `python -c "from agents.memory import PipelineMemory; m = PipelineMemory(); r = m.init_run('abc', 'release'); print(r.summary())"` runs without error
- [ ] `.pipeline/` directory is created on first run
- [ ] `state.json` is valid JSON after `init_run()`
- [ ] Re-running `init_run()` archives the previous run to `.pipeline/history/`

---

## Milestone 2 — Phase 1 Agents (Parallel)

> **Goal:** Three agents that run simultaneously at pipeline start.
> They validate, version-bump, and fetch secrets in parallel.

> 💡 **These 3 agents can be developed in parallel by 3 different AI sessions.**
> Each session reads from the same `AGENTS.md` and writes to a different file.

### Agent 2A — Validator Agent

**File:** `agents/validator_agent.py`
**Test file:** `tests/test_validator_agent.py`

**Implements these checks (all must run even if one fails — collect all issues):**

| Check | What It Does | Failure Means |
|-------|-------------|---------------|
| `_check_sdk_versions()` | Reads `build.gradle`, verifies `targetSdkVersion ≥ 34` | Build blocked |
| `_check_manifest()` | Scans `AndroidManifest.xml` for dangerous permissions | Build blocked |
| `_run_lint()` | Runs `./gradlew lint` and parses output | Build blocked |
| File existence | Verifies `build.gradle` and `AndroidManifest.xml` exist | Build blocked |

**Required tests:**
```
TestValidatorAgentSdkCheck
  test_check_sdk_versions_when_valid_returns_no_issues
  test_check_sdk_versions_when_target_too_low_returns_issue
  test_check_sdk_versions_when_gradle_missing_returns_issue
  test_check_sdk_versions_when_no_target_sdk_returns_issue

TestValidatorAgentManifestCheck
  test_check_manifest_when_clean_returns_no_issues
  test_check_manifest_when_sms_permission_returns_issue
  test_check_manifest_when_query_all_packages_returns_issue
  test_check_manifest_when_missing_returns_issue
  test_check_manifest_when_multiple_permissions_returns_all_issues

TestValidatorAgentExecute
  test_execute_when_all_checks_pass_returns_ok
  test_execute_when_sdk_too_low_returns_fail
  test_execute_when_lint_fails_returns_fail
```

---

### Agent 2B — Version Agent

**File:** `agents/version_agent.py`
**Test file:** `tests/test_version_agent.py`

**Implements:**

| Method | What It Does |
|--------|-------------|
| `read_version_from_gradle()` | Reads `versionCode` and `versionName` from `build.gradle` |
| `write_version_to_gradle()` | Writes new values back using regex replacement |
| `_bump_patch()` | Increments patch: `1.2.3 → 1.2.4` |
| `execute()` | Acquires lock → reads → increments → writes → saves to state → releases lock |

**Version lock logic** (prevents collision when 2 pipelines run simultaneously):
```python
# Acquire .pipeline/locks/version.lock
# Timeout after 30 seconds
# Release in a finally block — always released even if agent crashes
```

**Writes to `PipelineRun`:**
- `run.version_code = new_code`
- `run.version_name = new_name`

**Required tests:**
```
TestReadVersionFromGradle
  test_read_version_when_valid_gradle_returns_code_and_name
  test_read_version_when_file_missing_raises_file_not_found
  test_read_version_when_no_version_code_raises_value_error
  test_read_version_when_no_version_name_raises_value_error

TestWriteVersionToGradle
  test_write_version_when_called_updates_version_code
  test_write_version_when_called_updates_version_name
  test_write_version_when_called_preserves_other_gradle_content

TestBumpPatch
  test_bump_patch_when_valid_semver_increments_patch
  test_bump_patch_when_patch_is_9_carries_correctly
  test_bump_patch_when_non_semver_returns_unchanged
  test_bump_patch_when_two_part_version_returns_unchanged

TestVersionAgentExecute
  test_execute_when_successful_increments_version_code
  test_execute_when_lock_unavailable_returns_fail
  test_execute_when_gradle_missing_returns_fail
```

---

### Agent 2C — Secrets Agent

**File:** `agents/secrets_agent.py`
**Test file:** `tests/test_secrets_agent.py`

**Implements:**

| Method | What It Does |
|--------|-------------|
| `_decode_keystore()` | Base64-decodes `KEYSTORE_FILE_B64` → writes to `.pipeline/artifacts/release.jks` |
| `_validate_keystore()` | Runs `keytool -list` to verify the keystore is valid and accessible |
| `_write_play_key()` | Writes `PLAY_STORE_JSON_KEY` to `.pipeline/artifacts/play_key.json` |
| `_validate_all_env_vars()` | Checks all 5 required secrets are set and non-empty |
| `cleanup()` | Securely deletes keystore and key files after pipeline completes |
| `execute()` | Validates → decodes → writes → verifies all secrets available |

> ⚠️ **Security rules for this agent:**
> - Never log secret values — only log whether they exist
> - Always call `cleanup()` in a `finally` block
> - Never write secrets outside `.pipeline/artifacts/`
> - Write keystore with `chmod 600` permissions

**Required tests:**
```
TestSecretsAgentValidation
  test_validate_env_vars_when_all_present_returns_true
  test_validate_env_vars_when_keystore_missing_returns_false
  test_validate_env_vars_when_play_key_missing_returns_false
  test_validate_env_vars_when_empty_string_returns_false

TestSecretsAgentDecoding
  test_decode_keystore_when_valid_b64_writes_file
  test_decode_keystore_when_invalid_b64_returns_fail
  test_decode_keystore_when_written_has_correct_permissions

TestSecretsAgentCleanup
  test_cleanup_when_files_exist_removes_them
  test_cleanup_when_files_missing_does_not_raise

TestSecretsAgentExecute
  test_execute_when_all_secrets_available_returns_ok
  test_execute_when_missing_secret_returns_fail_with_name
  test_execute_when_invalid_keystore_returns_fail
```

### Milestone 2 Acceptance Criteria

- [ ] `pytest tests/test_validator_agent.py tests/test_version_agent.py tests/test_secrets_agent.py -v` → all pass
- [ ] Running all 3 agents against a real Android project passes without errors
- [ ] `versionCode` in `build.gradle` is incremented after Version Agent runs
- [ ] `.pipeline/artifacts/release.jks` exists and is valid after Secrets Agent runs
- [ ] Running Version Agent twice does not double-increment (idempotency check)

---

## Milestone 3 — Phase 2: Build Agent

> **Goal:** Take the signed keystore from Phase 1 and produce a signed `.aab` file.
> This is the longest-running step — build caching is critical.

### Tasks

#### 3.1 Set up Fastlane

**File:** `fastlane/Fastfile`

```ruby
# fastlane/Fastfile

default_platform(:android)

platform :android do
  
  desc "Build signed release AAB"
  lane :build_release do
    gradle(
      task: "bundle",
      build_type: "Release",
      properties: {
        "android.injected.signing.store.file"     => ENV["KEYSTORE_PATH"],
        "android.injected.signing.store.password" => ENV["KEYSTORE_PASSWORD"],
        "android.injected.signing.key.alias"      => ENV["KEY_ALIAS"],
        "android.injected.signing.key.password"   => ENV["KEY_PASSWORD"],
      }
    )
  end

end
```

**File:** `fastlane/Appfile`

```ruby
# fastlane/Appfile
package_name(ENV["ANDROID_PACKAGE_NAME"])
json_key_file(ENV["PLAY_STORE_JSON_KEY"])
```

#### 3.2 Implement `agents/build_agent.py`

**File:** `agents/build_agent.py`
**Test file:** `tests/test_build_agent.py`

| Method | What It Does |
|--------|-------------|
| `_resolve_keystore_path()` | Finds the keystore written by Secrets Agent |
| `_run_build()` | Runs `fastlane build_release` with signing env vars injected |
| `_verify_output()` | Confirms `.aab` file exists and is non-empty after build |
| `_get_aab_size_mb()` | Reports AAB size in MB for the pipeline log |
| `execute()` | Resolves keystore → runs build → verifies output → saves aab_path to state |

**Writes to `PipelineRun`:**
- `run.aab_path = str(aab_path)`

**Build output path:**
```python
AAB_OUTPUT_PATH = "app/build/outputs/bundle/release/app-release.aab"
```

**Required tests:**
```
TestBuildAgentSetup
  test_resolve_keystore_when_secrets_agent_ran_returns_path
  test_resolve_keystore_when_file_missing_raises_error

TestBuildAgentOutput
  test_verify_output_when_aab_exists_returns_true
  test_verify_output_when_aab_missing_returns_false
  test_verify_output_when_aab_empty_returns_false
  test_get_aab_size_when_file_exists_returns_float

TestBuildAgentExecute
  test_execute_when_gradle_succeeds_returns_ok_with_path
  test_execute_when_gradle_fails_returns_fail_with_error
  test_execute_when_aab_not_produced_returns_fail
  test_execute_when_keystore_missing_returns_fail
```

#### 3.3 Configure Gradle caching

This is handled in the GitHub Actions workflow (Milestone 7), but document the cache key here:

```yaml
# Cache key: hash of all .gradle and gradle wrapper files
key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

# Paths to cache:
# ~/.gradle/caches
# ~/.gradle/wrapper
```

### Milestone 3 Acceptance Criteria

- [ ] `pytest tests/test_build_agent.py -v` → all pass
- [ ] Running the Build Agent on a real project produces a `.aab` at the expected path
- [ ] Pipeline state has `aab_path` set after Build Agent completes
- [ ] Fastlane lane runs without error: `fastlane build_release`
- [ ] Second run uses Gradle cache (build time should be ~40% faster)

---

## Milestone 4 — Phase 3 Agents (Parallel)

> **Goal:** QA and Metadata agents run simultaneously. Both must pass before upload.

> 💡 **These 2 agents can be developed in parallel.**

### Agent 4A — QA Agent

**File:** `agents/qa_agent.py`
**Test file:** `tests/test_qa_agent.py`

| Method | What It Does |
|--------|-------------|
| `_run_unit_tests()` | Runs `./gradlew test` and captures JUnit XML output |
| `_parse_test_results()` | Reads `app/build/test-results/**/*.xml` and counts pass/fail |
| `_is_flaky_failure(test_name)` | Checks history for this test — retries if failed in only 1 of last 3 runs |
| `_generate_report()` | Builds structured summary: total, passed, failed, skipped |
| `execute()` | Runs tests → parses → checks for flaky → blocks or passes pipeline |

**Flaky detection logic:**
```python
# A test is considered flaky if:
# - It failed in the current run
# - But it passed in at least 1 of the last 3 pipeline runs in history
# Flaky tests: retry the whole test run once before failing
# Real failures: fail immediately
```

**Warn-only mode** (controlled by env var):
```python
QA_WARN_ONLY = os.getenv("QA_WARN_ONLY", "false").lower() == "true"
# If True: test failures are logged as warnings, pipeline continues
# If False (default): test failures block the pipeline
```

**Required tests:**
```
TestQAAgentTestParsing
  test_parse_results_when_all_pass_returns_zero_failures
  test_parse_results_when_some_fail_returns_correct_count
  test_parse_results_when_no_xml_files_returns_empty_report
  test_parse_results_when_malformed_xml_logs_warning

TestQAAgentFlakyDetection
  test_is_flaky_when_test_failed_only_once_in_history_returns_true
  test_is_flaky_when_test_always_fails_returns_false
  test_is_flaky_when_no_history_returns_false

TestQAAgentExecute
  test_execute_when_all_tests_pass_returns_ok
  test_execute_when_tests_fail_and_not_warn_only_returns_fail
  test_execute_when_tests_fail_and_warn_only_returns_ok
  test_execute_when_gradle_not_found_returns_fail
```

---

### Agent 4B — Metadata Agent

**File:** `agents/metadata_agent.py`
**Test file:** `tests/test_metadata_agent.py`

| Method | What It Does |
|--------|-------------|
| `_parse_changelog(content)` | Extracts latest version section from `CHANGELOG.md` |
| `_get_recent_commits(since_sha)` | Runs `git log --oneline -20 --no-merges` |
| `_generate_with_claude(commits)` | Sends commits to Claude API, returns release notes string |
| `_truncate(text)` | Enforces 500 char Play Store limit, breaks at word boundary |
| `_get_release_notes(run)` | Priority chain: CHANGELOG.md → Claude API → default fallback |
| `execute()` | Builds metadata dict → writes `.pipeline/artifacts/metadata.json` |

**Priority chain for release notes:**
```
1. CHANGELOG.md exists → parse latest section → use it
   ↓ (only if no CHANGELOG.md)
2. ANTHROPIC_API_KEY set → call Claude API with git commits → use result
   ↓ (only if API fails or key not set)
3. Default: "Bug fixes and performance improvements."
```

**Output file:** `.pipeline/artifacts/metadata.json`
```json
{
  "package_name": "com.example.app",
  "version_code": 42,
  "version_name": "2.1.0",
  "track": "internal",
  "release_notes": {
    "en-US": "Fixed crash on login screen. Improved performance on older devices."
  }
}
```

**Required tests:**
```
TestParseChangelog
  test_parse_changelog_when_standard_format_returns_latest_section
  test_parse_changelog_when_empty_file_returns_empty_string
  test_parse_changelog_when_no_sections_returns_empty_string
  test_parse_changelog_when_single_section_returns_all_content

TestTruncate
  test_truncate_when_under_limit_returns_unchanged
  test_truncate_when_over_limit_returns_within_limit
  test_truncate_when_over_limit_ends_with_ellipsis
  test_truncate_when_exactly_at_limit_returns_unchanged

TestGetReleaseNotes
  test_get_release_notes_when_changelog_exists_uses_changelog
  test_get_release_notes_when_no_changelog_and_no_api_key_uses_default
  test_get_release_notes_when_claude_api_fails_uses_default
  test_get_release_notes_when_claude_api_succeeds_uses_ai_notes

TestMetadataAgentExecute
  test_execute_when_successful_writes_metadata_json
  test_execute_when_output_dir_missing_creates_it
  test_execute_when_metadata_has_correct_structure
```

### Milestone 4 Acceptance Criteria

- [ ] `pytest tests/test_qa_agent.py tests/test_metadata_agent.py -v` → all pass
- [ ] QA Agent correctly identifies failing tests by name
- [ ] Metadata Agent writes valid `metadata.json` to `.pipeline/artifacts/`
- [ ] Metadata Agent falls back gracefully when `ANTHROPIC_API_KEY` is not set
- [ ] Release notes are always ≤ 500 characters

---

## Milestone 5 — Phase 4: Upload Agent

> **Goal:** Upload the signed AAB to Google Play Console using the metadata
> prepared by the Metadata Agent.

### Tasks

#### 5.1 Implement `agents/upload_agent.py`

**File:** `agents/upload_agent.py`
**Test file:** `tests/test_upload_agent.py`

| Method | What It Does |
|--------|-------------|
| `_load_metadata()` | Reads `.pipeline/artifacts/metadata.json` |
| `_write_json_key()` | Writes Play Store JSON key from env to `/tmp/play_store_key.json` |
| `_write_fastlane_release_notes(notes)` | Writes to `fastlane/metadata/android/en-US/changelogs/default.txt` |
| `_run_fastlane_supply(...)` | Executes `fastlane supply` subprocess |
| `_parse_fastlane_error(output)` | Maps known error strings to actionable messages |
| `check_permissions()` | Verifies API access (run this manually before first release) |
| `execute()` | Validates AAB + metadata exist → writes key → runs supply → cleans up key |

**Error mapping table (implement in `_parse_fastlane_error`):**

| Play Store Error | Human-Readable Output |
|-----------------|----------------------|
| `Version code has already been used` | "Run Version Agent again: `python -m agents.version_agent`" |
| `403` / `Forbidden` | "Grant 'Release manager' role to service account in Play Console" |
| `Package not found` | "First upload must be manual. See README Known Limitations." |
| `401` / `Unauthorized` | "PLAY_STORE_JSON_KEY is invalid or expired. Regenerate it." |
| Anything else | First 500 chars of raw output |

**Extend Fastfile for upload:**

```ruby
# Add to fastlane/Fastfile

desc "Upload signed AAB to Play Store"
lane :upload_to_play_store do |options|
  supply(
    track:            options[:track] || "internal",
    aab:              ENV["SUPPLY_AAB"],
    package_name:     ENV["ANDROID_PACKAGE_NAME"],
    json_key:         ENV["PLAY_STORE_JSON_KEY"],
    skip_upload_apk:  true,
    release_status:   "draft"
  )
end
```

**Required tests:**
```
TestLoadMetadata
  test_load_metadata_when_file_exists_returns_dict
  test_load_metadata_when_file_missing_returns_empty_dict
  test_load_metadata_when_invalid_json_returns_empty_dict

TestParseFastlaneError
  test_parse_error_when_version_code_used_returns_clear_message
  test_parse_error_when_403_returns_permission_message
  test_parse_error_when_package_not_found_returns_manual_upload_message
  test_parse_error_when_401_returns_key_error_message
  test_parse_error_when_unknown_error_returns_truncated_output

TestUploadAgentExecute
  test_execute_when_aab_missing_returns_fail_with_clear_message
  test_execute_when_metadata_missing_returns_fail_with_clear_message
  test_execute_when_json_key_missing_returns_fail_with_clear_message
  test_execute_when_fastlane_succeeds_returns_ok
  test_execute_when_fastlane_fails_parses_error_message
  test_execute_always_deletes_key_file_on_success
  test_execute_always_deletes_key_file_on_failure
```

### Milestone 5 Acceptance Criteria

- [ ] `pytest tests/test_upload_agent.py -v` → all pass
- [ ] JSON key file is always deleted after execution (even on failure)
- [ ] `python -m agents.upload_agent --check-permissions` succeeds with real credentials
- [ ] Uploading an AAB to a test app in Play Console succeeds end-to-end
- [ ] Error messages are human-readable and actionable (not raw API errors)

---

## Milestone 6 — Phase 5 Agents + Orchestrator

> **Goal:** Tester and Notifier run in parallel after upload. Orchestrator ties everything together.

> 💡 **Tester and Notifier can be developed in parallel.**

### Agent 6A — Tester Agent

**File:** `agents/tester_agent.py`
**Test file:** `tests/test_tester_agent.py`

| Method | What It Does |
|--------|-------------|
| `_load_tester_config()` | Reads `config/tester-groups.yaml` |
| `_get_track_config(track)` | Returns tester group config for the active track |
| `_assign_testers(package_name, track, emails)` | Calls Play Console API to assign testers |
| `_should_promote(run, config)` | Checks if auto-promote condition is met |
| `execute()` | Loads config → assigns testers → checks promotion rules |

**`config/tester-groups.yaml` format:**
```yaml
groups:
  internal:
    track: "internal"
    emails:
      - dev@company.com
    auto_promote_to: null
  alpha:
    track: "alpha"
    emails:
      - beta@company.com
    auto_promote_to: null
```

**Required tests:**
```
TestTesterAgentConfig
  test_load_config_when_file_exists_returns_dict
  test_load_config_when_file_missing_returns_empty_config
  test_get_track_config_when_track_exists_returns_config
  test_get_track_config_when_track_missing_returns_none

TestTesterAgentPromotion
  test_should_promote_when_qa_passed_and_rule_exists_returns_true
  test_should_promote_when_qa_failed_returns_false
  test_should_promote_when_no_rule_returns_false
  test_should_promote_when_manual_required_returns_false

TestTesterAgentExecute
  test_execute_when_testers_assigned_returns_ok
  test_execute_when_no_config_for_track_skips_gracefully
  test_execute_when_api_fails_returns_fail
```

---

### Agent 6B — Notifier Agent

**File:** `agents/notifier_agent.py`
**Test file:** `tests/test_notifier_agent.py`

| Method | What It Does |
|--------|-------------|
| `_build_slack_message(run)` | Builds Slack Block Kit payload with status, version, timings |
| `_send_slack(payload)` | POSTs to `SLACK_WEBHOOK_URL` |
| `_post_github_commit_status(run)` | Posts success/failure commit status via GitHub API |
| `_calculate_duration(run)` | Computes total pipeline duration from start to now |
| `execute()` | Builds message → sends Slack → posts GitHub status |

**Slack message must include:**
- ✅ / ❌ Status indicator
- App name + version (e.g. `MyApp v2.1.0 (build 42)`)
- Pipeline duration (e.g. `Completed in 19m 34s`)
- Track deployed to (e.g. `internal`)
- Link to GitHub commit
- On failure: which agent failed + the error message

**Required tests:**
```
TestNotifierAgentMessage
  test_build_slack_message_when_success_includes_checkmark
  test_build_slack_message_when_failure_includes_x_and_error
  test_build_slack_message_includes_version_info
  test_build_slack_message_includes_duration
  test_calculate_duration_when_start_and_end_known_returns_minutes

TestNotifierAgentExecute
  test_execute_when_slack_webhook_not_set_skips_gracefully
  test_execute_when_slack_post_fails_logs_warning_not_error
  test_execute_when_github_token_not_set_skips_github_status
  test_execute_always_returns_ok_even_if_notifications_fail
```

> ⚠️ Notifier Agent must **always return `AgentResult.ok()`** even if Slack is down.
> A notification failure should never block or fail the pipeline.

---

### 6C — Orchestrator

**File:** `agents/orchestrator.py`
**Test file:** `tests/test_orchestrator.py`

The Orchestrator is the entry point — it is called by humans and CI/CD systems.
It does not do agent work itself; it reads state and decides what to run next.

**CLI interface:**

```bash
python -m agents.orchestrator --init --commit abc123 --branch release
python -m agents.orchestrator --run
python -m agents.orchestrator --dry-run
python -m agents.orchestrator --resume --from-phase P3
python -m agents.orchestrator --status
python -m agents.orchestrator --history
```

| Method | What It Does |
|--------|-------------|
| `init(commit_sha, branch)` | Calls `memory.init_run()`, sets initial state |
| `run(dry_run)` | Executes all phases in order, blocking at sync points |
| `resume(from_phase)` | Calls `memory.resume_from_phase()` then continues |
| `status()` | Prints `state.summary()` to stdout |
| `_run_phase_parallel(agents)` | Runs multiple agents concurrently using `ThreadPoolExecutor` |
| `_sync_point(phase)` | Blocks until all agents in phase are DONE, fails if any FAILED |

**Parallelism implementation:**
```python
from concurrent.futures import ThreadPoolExecutor, as_completed

def _run_phase_parallel(self, agents: list[BaseAgent]) -> bool:
    """Run agents concurrently. Returns True if all pass."""
    with ThreadPoolExecutor(max_workers=len(agents)) as executor:
        futures = {executor.submit(agent.run): agent.name for agent in agents}
        all_passed = True
        for future in as_completed(futures):
            result = future.result()
            if not result.success:
                all_passed = False
    return all_passed
```

**Required tests:**
```
TestOrchestratorInit
  test_init_when_called_creates_state_with_correct_commit
  test_init_when_state_exists_archives_old_run

TestOrchestratorStatus
  test_status_when_state_exists_prints_summary
  test_status_when_no_state_prints_not_started

TestOrchestratorSyncPoint
  test_sync_point_when_all_agents_done_returns_true
  test_sync_point_when_one_agent_failed_returns_false
  test_sync_point_when_agent_still_running_waits

TestOrchestratorResume
  test_resume_when_phase_given_resets_agents_from_that_phase
  test_resume_when_invalid_phase_raises_value_error
```

### Milestone 6 Acceptance Criteria

- [ ] `pytest tests/test_tester_agent.py tests/test_notifier_agent.py tests/test_orchestrator.py -v` → all pass
- [ ] Tester Agent correctly reads `config/tester-groups.yaml`
- [ ] Notifier Agent sends a Slack message with correct format
- [ ] Notifier Agent returns `ok()` even when Slack webhook fails
- [ ] Orchestrator dry-run completes without calling any real APIs
- [ ] `python -m agents.orchestrator --status` shows current state correctly

---

## Milestone 7 — GitHub Actions Workflow

> **Goal:** Wire everything together in CI/CD so the pipeline triggers automatically
> on merge to `release`.

### Tasks

#### 7.1 Create `.github/workflows/release.yml`

The full YAML is in `AGENTS.md` section 4. Key structural points:

**Job dependency graph:**
```yaml
# Phase 1 — all 3 run in parallel (no 'needs')
validator:   runs-on: ubuntu-latest
version:     runs-on: ubuntu-latest
secrets-fetch: runs-on: ubuntu-latest

# Phase 2 — waits for all P1
build:
  needs: [validator, version, secrets-fetch]

# Phase 3 — waits for build, runs in parallel
qa:       needs: [build]
metadata: needs: [build]

# Phase 4 — waits for both P3
upload:
  needs: [qa, metadata]

# Phase 5 — both start after upload, run in parallel
tester:   needs: [upload]
notifier:
  needs: [upload]
  if: always()   # Critical: always notify, even if upload failed
```

**Concurrency guard (prevents partial releases):**
```yaml
concurrency:
  group: release-pipeline
  cancel-in-progress: false   # Never cancel mid-run
```

**Gradle caching (speeds up build by ~40%):**
```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      gradle-
```

**State sharing between jobs (using artifacts):**
```yaml
# After any job that modifies state:
- uses: actions/upload-artifact@v4
  with:
    name: pipeline-state
    path: .pipeline/state.json

# At the start of any job that reads state:
- uses: actions/download-artifact@v4
  with:
    name: pipeline-state
```

#### 7.2 Verify all secrets are wired

Every job that needs secrets must declare them explicitly under `env:`.
Check this table before considering Milestone 7 done:

| Job | Secrets Needed |
|-----|---------------|
| `version` | `PLAY_STORE_JSON_KEY`, `GITHUB_TOKEN` |
| `secrets-fetch` | `KEYSTORE_FILE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `PLAY_STORE_JSON_KEY` |
| `build` | `KEYSTORE_FILE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` |
| `metadata` | `ANTHROPIC_API_KEY` |
| `upload` | `PLAY_STORE_JSON_KEY` |
| `tester` | `PLAY_STORE_JSON_KEY` |
| `notifier` | `SLACK_WEBHOOK_URL`, `GITHUB_TOKEN` |

### Milestone 7 Acceptance Criteria

- [ ] Pushing to `release` branch triggers the workflow in GitHub Actions
- [ ] P1 jobs start simultaneously (check GitHub Actions UI — they should all show "In progress" at the same time)
- [ ] P2 job only starts after all P1 jobs have green checkmarks
- [ ] P3 jobs start simultaneously after P2
- [ ] Notifier job runs even when upload fails (`if: always()` is working)
- [ ] Workflow YAML passes `yamllint` with no errors

---

## Milestone 8 — End-to-End Testing

> **Goal:** Prove the full pipeline works from trigger to Play Store, with real credentials,
> on a real (test) app. Fix anything that fails before going live.

### Step 8.1 — Full Test Suite

```bash
# Run all tests with coverage
pytest tests/ -v --cov=agents --cov-report=html --cov-fail-under=90

# Expected: all tests pass, ≥90% coverage
```

### Step 8.2 — Local Dry Run

```bash
# Initialize a fake run
python -m agents.orchestrator --init --commit $(git rev-parse HEAD) --branch release

# Dry run — no real API calls
python -m agents.orchestrator --dry-run

# Verify state looks correct
python -m agents.orchestrator --status
```

Expected output:
```
Run ID: run_20260217_143022
Status: IN_PROGRESS  |  Phase: P5
All agents: ✅ DONE (dry_run mode)
```

### Step 8.3 — Real Run on Test App

Before running on your production app, test on a throwaway app:

1. Create a new test app in Play Console with a dummy package name
2. Run the full pipeline against it
3. Verify the build appears in Play Console under the test app

```bash
# Set env vars for test app
export ANDROID_PACKAGE_NAME=com.yourcompany.testapp
export PIPELINE_STATE_DIR=.pipeline-test

# Real run
python -m agents.orchestrator --run
```

### Step 8.4 — Phase Resume Test

Simulate a failure at Phase 3 and verify resume works:

```bash
# Force Phase 3 to pending
python -m agents.orchestrator --resume --from-phase P3

# Verify state
python -m agents.orchestrator --status
# Expected: P3 agents show PENDING, P1+P2 agents still show DONE

# Re-run from P3
python -m agents.orchestrator --run
```

### Step 8.5 — Trigger via GitHub Actions

1. Make a small commit to your `develop` branch
2. Merge `develop` → `release` via a pull request
3. Watch the GitHub Actions workflow run
4. Verify all jobs complete with green checkmarks
5. Verify the build appears in Google Play Console → Internal Testing

### Milestone 8 Acceptance Criteria

- [ ] `pytest tests/ --cov=agents --cov-fail-under=90` passes
- [ ] Dry run completes with all agents showing DONE
- [ ] Real run uploads a build to Play Console test app
- [ ] Phase resume correctly re-runs only failed phase
- [ ] GitHub Actions workflow completes fully green on a real merge
- [ ] Slack notification received with correct version number and status
- [ ] Pipeline duration is under 25 minutes total

---

## Dependency Map

The following shows exactly which tasks block which. Use this to plan parallel work.

```
[2-Prerequisites]
      │
      ▼
[M1: Scaffold + Memory] ──────────────────────────────────────────────────────┐
      │                                                                        │
      ├──────────────────────┬──────────────────────┐                         │
      ▼                      ▼                      ▼                         │
[M2A: Validator]      [M2B: Version]        [M2C: Secrets]                    │
      │                      │                      │                         │
      └──────────────────────┴──────────────────────┘                         │
                             │                                                 │
                             ▼                                                 │
                    [M3: Build Agent]                                          │
                             │                                                 │
                    ┌────────┴────────┐                                        │
                    ▼                 ▼                                        │
             [M4A: QA]         [M4B: Metadata]                                │
                    │                 │                                        │
                    └────────┬────────┘                                        │
                             │                                                 │
                             ▼                                                 │
                    [M5: Upload Agent]                                         │
                             │                                                 │
                    ┌────────┴────────┐                                        │
                    ▼                 ▼                                        │
             [M6A: Tester]    [M6B: Notifier]                                 │
                    │                 │                                        │
                    └────────┬────────┘                                        │
                             │                                                 │
                             ▼                                                 │
                    [M6C: Orchestrator]                                        │
                             │                                                 │
                             ▼                                                 │
                    [M7: GitHub Actions]                                       │
                             │                                                 │
                             ▼                                                 │
                    [M8: End-to-End Testing] ◄─────────────────────────────────┘
```

---

## Timeline Summary

| Week | Milestone(s) | Focus |
|------|-------------|-------|
| Week 1 | Prerequisites + M1 | Manual setup, project scaffold, memory system |
| Week 1–2 | M2 (A+B+C in parallel) | All 3 Phase 1 agents |
| Week 2 | M3 | Build Agent + Fastlane |
| Week 2–3 | M4 (A+B in parallel) | QA Agent + Metadata Agent |
| Week 3 | M5 | Upload Agent + Play Console integration |
| Week 3–4 | M6 (A+B in parallel, then C) | Tester + Notifier + Orchestrator |
| Week 4 | M7 | GitHub Actions workflow |
| Week 4–5 | M8 | End-to-end testing + production launch |

**Total: ~4–5 weeks** (shorter if M2, M4, M6 sub-tasks are parallelized across devs/agents)

---

## Definition of Done

The project is complete when **all** of the following are true:

- [ ] `pytest tests/ -v` → 100% of tests pass
- [ ] `pytest tests/ --cov=agents --cov-fail-under=90` → ≥90% coverage
- [ ] Merging a real commit to `release` branch triggers the full pipeline automatically
- [ ] The signed AAB appears in Google Play Console under the correct track
- [ ] Tester group is assigned correctly
- [ ] Slack notification is received with the correct version and status
- [ ] A failed pipeline sends a failure notification with the failing agent name and error
- [ ] Resuming a failed pipeline from a specific phase works correctly
- [ ] `python -m agents.orchestrator --status` always shows accurate current state
- [ ] No secrets are ever logged or committed to the repository
- [ ] All agent files have the standard header comment (see `DEVELOPMENT.md`)
- [ ] `CHANGELOG.md` is documented with how to add release notes
- [ ] `README.md` is updated with actual package name and Slack channel

---

*Last updated: February 2026*
*See `README.md` for architecture overview. See `AGENTS.md` for code. See `DEVELOPMENT.md` for standards.*
