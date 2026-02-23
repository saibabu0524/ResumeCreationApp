# 🚀 Project Guide — Android CI/CD AI Parallel Agent Pipeline

> A complete guide to what we built, how to configure it, and how it works.

---

## 📋 Table of Contents

- [What We Built](#-what-we-built)
- [How It Works](#-how-it-works)
- [Architecture Overview](#-architecture-overview)
- [All 9 Agents Explained](#-all-9-agents-explained)
- [Project Structure](#-project-structure)
- [Setup & Configuration](#-setup--configuration)
- [Running the Pipeline](#-running-the-pipeline)
- [Running Tests](#-running-tests)
- [GitHub Actions (CI/CD)](#-github-actions-cicd)
- [Troubleshooting](#-troubleshooting)
- [Known Limitations](#-known-limitations)

---

## 🎯 What We Built

A **multi-agent Android CI/CD pipeline** that automates the entire release process — from code validation to Play Store deployment. Instead of running steps sequentially (40+ minutes), this system uses **9 specialized agents** that execute in **5 coordinated phases with parallel execution**, bringing total time down to **~18–22 minutes**.

### What happens when you push to `release` branch:

1. ✅ **Validates** your build (lint, SDK compliance, Play Store policy)
2. 🔢 **Auto-increments** version code & name (no manual bumping)
3. 🔐 **Decodes & validates** signing secrets securely
4. 🔨 **Builds** a signed Android App Bundle (AAB) via Fastlane
5. 🧪 **Runs tests** in parallel with metadata generation
6. ☁️ **Uploads** to Google Play Console automatically
7. 👥 **Assigns** tester groups and sends notifications

### Key Features

| Feature | Description |
|---------|-------------|
| **Parallel Execution** | Agents run concurrently where possible (P1: 3 agents, P3: 2 agents, P5: 2 agents) |
| **Session Recovery** | State persisted to `.pipeline/state.json` — survives crashes and restarts |
| **Resume from Phase** | Failed at Phase 3? Resume from there, don't restart the whole pipeline |
| **Dry-Run Mode** | Test the full pipeline flow without building or uploading anything |
| **Thread-Safe** | RLock-based state management prevents race conditions during parallel execution |
| **Idempotent** | Re-running a completed agent is a no-op — no duplicate work |
| **Retry Logic** | Failed agents automatically retry up to 3 times |
| **Never-Fail Notifier** | The Notifier Agent always returns success — it never blocks the pipeline |

---

## ⚙️ How It Works

The pipeline executes in **5 phases** with sync points between them:

```
Phase 1 (Parallel)     Phase 2 (Serial)     Phase 3 (Parallel)     Phase 4 (Serial)     Phase 5 (Parallel)
┌─────────────┐       ┌───────────┐        ┌──────────────┐       ┌──────────────┐     ┌────────────┐
│ Validator   │       │           │        │  QA Agent    │       │              │     │ Tester     │
│ Version     │──────▶│  Build    │───────▶│              │──────▶│   Upload     │────▶│            │
│ Secrets     │       │  Agent    │        │  Metadata    │       │   Agent      │     │ Notifier   │
└─────────────┘       └───────────┘        └──────────────┘       └──────────────┘     └────────────┘
       │                    │                     │                      │                    │
   SYNC POINT 1        SYNC POINT 2          SYNC POINT 3          SYNC POINT 4         (no sync)
   (all 3 must pass)   (must pass)           (both must pass)      (must pass)
```

### Phase Rules

| Phase | Agents | Parallel? | Sync Point |
|-------|--------|----------|------------|
| **P1** | Validator, Version, Secrets | ✅ All 3 run together | All must pass |
| **P2** | Build | ❌ Sequential (needs P1 outputs) | Must succeed |
| **P3** | QA, Metadata | ✅ Both run together | Both must pass |
| **P4** | Upload | ❌ Sequential (needs AAB + metadata) | Must succeed |
| **P5** | Tester, Notifier | ✅ Both run together | No sync needed |

### State Machine

Each agent goes through these states:
```
PENDING → RUNNING → DONE ✅
                  → FAILED ❌ → (retry up to 3x) → RUNNING → ...
```

Pipeline states: `NOT_STARTED` → `IN_PROGRESS` → `SUCCESS` / `FAILED`

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         ORCHESTRATOR                            │
│   Reads state.json → decides which phase to run → coordinates  │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
   ┌───────────┐      ┌───────────┐      ┌────────────┐
   │ Validator │      │  Version  │      │  Secrets   │    Phase 1
   │   Agent   │      │   Agent   │      │   Agent    │
   └─────┬─────┘      └─────┬─────┘      └──────┬─────┘
         └──────────────────┼────────────────────┘
                            ▼ sync
                     ┌────────────┐
                     │   Build    │                        Phase 2
                     │   Agent    │
                     └──────┬─────┘
                            ▼ sync
              ┌─────────────┼─────────────┐
              ▼                           ▼
        ┌───────────┐              ┌────────────┐
        │ QA Agent  │              │  Metadata  │          Phase 3
        │           │              │   Agent    │
        └─────┬─────┘              └──────┬─────┘
              └───────────┬───────────────┘
                          ▼ sync
                   ┌────────────┐
                   │  Upload    │                          Phase 4
                   │  Agent     │
                   └──────┬─────┘
                          ▼ sync
              ┌───────────┼───────────┐
              ▼                       ▼
        ┌───────────┐          ┌────────────┐
        │  Tester   │          │  Notifier  │              Phase 5
        │  Agent    │          │   Agent    │
        └───────────┘          └────────────┘

    All state reads/writes go through PipelineMemory (thread-safe)
    State file: .pipeline/state.json
```

---

## 🤖 All 9 Agents Explained

### 1. Validator Agent (`agents/validator_agent.py`)
**Purpose:** Pre-flight checks before any build starts.

| Check | What It Does |
|-------|-------------|
| SDK Version | Ensures `targetSdkVersion` meets Play Store minimum (currently 34) |
| Manifest Scan | Detects dangerous permissions (SMS, CALL_LOG, QUERY_ALL_PACKAGES) |
| Lint Check | Runs `./gradlew lint` and checks for errors |
| File Check | Verifies `build.gradle` and `AndroidManifest.xml` exist |

**Fails pipeline if:** Any critical issue is found.

---

### 2. Version Agent (`agents/version_agent.py`)
**Purpose:** Auto-increment version code and name.

- Reads `versionCode` and `versionName` from `build.gradle` (or `.kts`)
- Increments `versionCode` by 1
- Bumps patch version (e.g., `2.1.0` → `2.1.1`)
- Uses file-based locking to prevent race conditions
- Saves version info to pipeline state for downstream agents

**Fails pipeline if:** Can't acquire lock or Gradle file is missing.

---

### 3. Secrets Agent (`agents/secrets_agent.py`)
**Purpose:** Securely decode and validate pipeline secrets.

- Validates all required environment variables are set
- Decodes Base64-encoded keystore to a file
- Validates keystore with `keytool`
- Sets file permissions to `0o600` (owner read/write only)
- **Never logs secret values**
- Cleanup runs in `finally` block (always executes)

**Fails pipeline if:** Any required secret is missing or keystore is invalid.

---

### 4. Build Agent (`agents/build_agent.py`)
**Purpose:** Build the signed Android App Bundle.

- Resolves keystore path from Secrets Agent output
- Invokes `fastlane build_release` with a 20-minute timeout
- Verifies the AAB file exists and reports its size
- Saves AAB path to pipeline state

**Fails pipeline if:** Build fails or AAB not found after build.

---

### 5. QA Agent (`agents/qa_agent.py`)
**Purpose:** Run tests and analyze results.

- Executes `./gradlew test`
- Parses JUnit XML test results
- Detects flaky tests by checking against run history
- Generates a test report in `.pipeline/artifacts/`
- Supports `QA_WARN_ONLY=true` mode (warning instead of failure)

**Fails pipeline if:** Tests fail (unless warn-only mode is enabled).

---

### 6. Metadata Agent (`agents/metadata_agent.py`)
**Purpose:** Generate Play Store release metadata.

Release notes are determined by a **priority chain**:
1. **CHANGELOG.md** — if it exists and has a section for the current version
2. **Claude AI** — generates notes from recent git commits (needs `ANTHROPIC_API_KEY`)
3. **Default fallback** — generic release notes with version info

Also builds `metadata.json` with package name, version, and track info.

**Fails pipeline if:** Cannot generate metadata (extremely rare).

---

### 7. Upload Agent (`agents/upload_agent.py`)
**Purpose:** Upload the AAB to Google Play Console.

- Loads metadata from Metadata Agent
- Writes Play Store JSON key to temp file (always cleaned up in `finally`)
- Writes release notes to Fastlane metadata directory
- Runs `fastlane supply` to upload
- Maps Play Store errors to actionable fix messages:
  - `403` → "Service account needs Play Editor role"
  - `401` → "JSON key is invalid or expired"
  - `Version code already used` → "Run version agent again"

**Fails pipeline if:** Upload fails (with clear error message).

---

### 8. Tester Agent (`agents/tester_agent.py`)
**Purpose:** Assign testers to Play Console tracks.

- Loads group configs from `config/tester-groups.yaml`
- Assigns testers to tracks (internal, alpha, beta, production)
- Checks auto-promotion rules (e.g., promote to production after QA passes)
- Uses Google Play Developer API

**Fails pipeline if:** API call fails (but promotion is optional).

---

### 9. Notifier Agent (`agents/notifier_agent.py`)
**Purpose:** Send deployment notifications. **⚠️ Never fails the pipeline.**

- **Slack**: Builds Block Kit messages with ✅/❌ status, version info, duration
- **GitHub**: Posts commit status on the triggering commit
- Calculates pipeline duration and formats it nicely
- **CRITICAL: Always returns `AgentResult.ok()`** — even if Slack/GitHub calls fail

---

## 📁 Project Structure

```
playstore-production-automation/
│
├── README.md                       ← AI agent entry point
├── GUIDE.md                        ← You are here (human guide)
├── AGENTS.md                       ← Detailed agent implementation specs
├── DEVELOPMENT.md                  ← Code standards & test requirements
├── IMPLEMENTATION_PLAN.md          ← Step-by-step build plan
│
├── agents/                         # All pipeline agents
│   ├── __init__.py
│   ├── memory.py                   # PipelineMemory — state management (thread-safe)
│   ├── base_agent.py               # BaseAgent — abstract class all agents extend
│   ├── orchestrator.py             # Coordinates all 9 agents across 5 phases
│   ├── validator_agent.py          # P1: SDK, manifest, lint checks
│   ├── version_agent.py            # P1: Auto-increment version code/name
│   ├── secrets_agent.py            # P1: Decode & validate signing secrets
│   ├── build_agent.py              # P2: Build signed AAB via Fastlane
│   ├── qa_agent.py                 # P3: Run tests, parse JUnit XML
│   ├── metadata_agent.py           # P3: Generate Play Store release notes
│   ├── upload_agent.py             # P4: Upload AAB to Play Console
│   ├── tester_agent.py             # P5: Assign tester groups
│   └── notifier_agent.py          # P5: Slack + GitHub notifications
│
├── tests/                          # Comprehensive test suite (207 tests)
│   ├── __init__.py
│   ├── test_memory.py              # 22 tests — state persistence & locking
│   ├── test_base_agent.py          # 7 tests — lifecycle, retries, idempotency
│   ├── test_validator_agent.py     # 23 tests — SDK, manifest, lint checks
│   ├── test_version_agent.py       # 23 tests — version parsing, bumping, locking
│   ├── test_secrets_agent.py       # 12 tests — decoding, validation, permissions
│   ├── test_build_agent.py         # 16 tests — keystore, AAB verification
│   ├── test_qa_agent.py            # 12 tests — JUnit parsing, flaky detection
│   ├── test_metadata_agent.py      # 18 tests — CHANGELOG, Claude API, fallback
│   ├── test_upload_agent.py        # 17 tests — upload, error mapping, cleanup
│   ├── test_tester_agent.py        # 11 tests — config loading, promotion rules
│   ├── test_notifier_agent.py      # 12 tests — Slack messages, never-fail rule
│   ├── test_orchestrator.py        # 11 tests — init, sync points, resume
│   └── test_e2e_pipeline.py        # 23 tests — full pipeline dry-run, E2E
│
├── config/
│   └── tester-groups.yaml          # Tester group definitions per track
│
├── fastlane/
│   ├── Fastfile                    # Build & upload lanes
│   └── Appfile                     # Package name & JSON key config
│
├── .github/
│   └── workflows/
│       └── pipeline.yml            # GitHub Actions CI/CD workflow
│
├── .pipeline/                      # Runtime state directory (git-ignored)
│   ├── state.json                  # Current pipeline run state
│   ├── history/                    # Archived past runs
│   ├── locks/                      # File-based locks (version.lock)
│   └── artifacts/                  # Build outputs (AAB, test reports, metadata)
│
├── requirements.txt                # Python dependencies
└── .gitignore                      # Ignores .pipeline/, secrets, caches
```

---

## 🔧 Setup & Configuration

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **Python** | 3.11+ | Agent runtime |
| **Ruby** | 3.0+ | Fastlane |
| **Fastlane** | Latest | Build & upload |
| **JDK** | 17 | Android build |
| **Android SDK** | Latest | Build tools |

### Step 1: Install Dependencies

```bash
# Clone the repo
git clone https://github.com/your-org/playstore-production-automation.git
cd playstore-production-automation

# Create virtual environment
python -m venv venv
source venv/bin/activate

# Install Python dependencies
pip install -r requirements.txt

# Install Fastlane
gem install fastlane
```

### Step 2: Set Environment Variables

Create a `.env` file (git-ignored) or set these in your CI/CD:

```bash
# ===== REQUIRED: Android Signing =====
KEYSTORE_B64=<base64-encoded .jks keystore>
KEYSTORE_PASSWORD=<keystore password>
KEY_ALIAS=<signing key alias>
KEY_PASSWORD=<signing key password>

# ===== REQUIRED: Google Play =====
PLAY_STORE_JSON_KEY=<service account JSON string or file path>
ANDROID_PACKAGE_NAME=com.yourcompany.yourapp

# ===== OPTIONAL: AI Release Notes =====
ANTHROPIC_API_KEY=<Claude API key for AI-generated release notes>

# ===== OPTIONAL: Notifications =====
SLACK_WEBHOOK_URL=<Slack incoming webhook URL>
GITHUB_TOKEN=<GitHub token with repo:status permission>
GITHUB_REPOSITORY=<owner/repo>

# ===== OPTIONAL: QA Mode =====
QA_WARN_ONLY=false   # Set to 'true' to make test failures non-blocking
```

#### How to encode your keystore:
```bash
# Encode your keystore to Base64
base64 -i your-keystore.jks | tr -d '\n' > keystore_b64.txt

# Copy the content and set as KEYSTORE_B64
export KEYSTORE_B64=$(cat keystore_b64.txt)
```

#### How to set up Google Play service account:
1. Go to [Google Play Console](https://play.google.com/console) → **Setup** → **API access**
2. Create or link a Google Cloud project
3. Create a **Service Account** with **Release Manager** role
4. Download the JSON key file
5. Set `PLAY_STORE_JSON_KEY` to the JSON content:
   ```bash
   export PLAY_STORE_JSON_KEY=$(cat service-account.json)
   ```

### Step 3: Configure Tester Groups

Edit `config/tester-groups.yaml`:

```yaml
groups:
  internal:
    track: "internal"
    emails:
      - dev@company.com
      - qa@company.com
    auto_promote_to: null          # No auto-promotion

  alpha:
    track: "alpha"
    emails:
      - beta@company.com
      - external-tester@gmail.com
    auto_promote_to: "production"  # Auto-promote if QA passes
```

### Step 4: Verify Setup

```bash
# Run all tests (should see 207 passed)
pytest tests/ -v

# Dry-run the pipeline (no real build/upload)
python -m agents.orchestrator --init --commit "test" --branch "release"
python -m agents.orchestrator --dry-run
```

---

## 🚀 Running the Pipeline

### CLI Commands

```bash
# Initialize a new pipeline run
python -m agents.orchestrator --init --commit <SHA> --branch <BRANCH>

# Run the full pipeline
python -m agents.orchestrator --run

# Dry-run (test flow without real operations)
python -m agents.orchestrator --dry-run

# Check current status
python -m agents.orchestrator --status

# Resume from a specific phase (e.g., after fixing a test failure)
python -m agents.orchestrator --resume --from-phase P3

# View run history
python -m agents.orchestrator --history
```

### Example: Full Run

```bash
# Step 1: Initialize
$ python -m agents.orchestrator --init --commit "a3f2b19" --branch "release"
Pipeline Run: 55fe0234
Status: IN_PROGRESS
Agents: validator(PENDING) version(PENDING) secrets(PENDING) ...

# Step 2: Run
$ python -m agents.orchestrator --run
=== Phase 1: Validation, Version, Secrets ===
Agent 'validator' completed successfully.
Agent 'version' completed successfully.
Agent 'secrets' completed successfully.
Sync point P1 passed.
=== Phase 2: Build ===
Agent 'build' completed successfully.
Sync point P2 passed.
=== Phase 3: QA and Metadata ===
Agent 'qa' completed successfully.
Agent 'metadata' completed successfully.
Sync point P3 passed.
=== Phase 4: Upload ===
Agent 'upload' completed successfully.
Sync point P4 passed.
=== Phase 5: Tester and Notifier ===
Agent 'tester' completed successfully.
Agent 'notifier' completed successfully.
🎉 Pipeline completed successfully!

# Step 3: Check status
$ python -m agents.orchestrator --status
Pipeline Run: 55fe0234
Status: SUCCESS
All 9 agents: DONE ✅
```

### Example: Resume After Failure

```bash
# QA tests failed at Phase 3
$ python -m agents.orchestrator --status
  qa: FAILED (3 test failures)
  metadata: DONE
  upload: PENDING

# Fix the tests, then resume from P3
$ python -m agents.orchestrator --resume --from-phase P3
# → Re-runs QA + Metadata, then continues to Upload, Tester, Notifier
```

---

## 🧪 Running Tests

```bash
# Run all 207 tests
pytest tests/ -v

# Run with coverage report
pytest tests/ --cov=agents --cov-report=term-missing

# Run a specific agent's tests
pytest tests/test_build_agent.py -v
pytest tests/test_memory.py -v

# Run only E2E integration tests
pytest tests/test_e2e_pipeline.py -v

# Run tests matching a pattern
pytest tests/ -k "test_execute" -v
```

### Current Test Stats

```
207 tests | 82% coverage | 0.6s runtime
```

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `test_memory.py` | 22 | 91% |
| `test_base_agent.py` | 7 | 87% |
| `test_validator_agent.py` | 23 | 88% |
| `test_version_agent.py` | 23 | 93% |
| `test_secrets_agent.py` | 12 | 93% |
| `test_build_agent.py` | 16 | 100% |
| `test_qa_agent.py` | 12 | 78% |
| `test_metadata_agent.py` | 18 | 79% |
| `test_upload_agent.py` | 17 | 65% |
| `test_tester_agent.py` | 11 | 70% |
| `test_notifier_agent.py` | 12 | 81% |
| `test_orchestrator.py` | 11 | 69% |
| `test_e2e_pipeline.py` | 23 | — |

---

## 🔄 GitHub Actions (CI/CD)

The pipeline is configured to run automatically via GitHub Actions.

**Trigger:** Push to `main`, `release`, or `release/*` branches

**File:** `.github/workflows/pipeline.yml`

### What the workflow does:

1. **Checks out** code with 20-commit history (for git log in metadata agent)
2. **Sets up** Python 3.11, JDK 17, Ruby 3.2, Fastlane
3. **Runs** all 207 unit tests as a pre-flight check
4. **Initializes** the pipeline with the commit SHA
5. **Executes** the pipeline (all 5 phases)
6. **Uploads artifacts** — pipeline state, signed AAB, test results

### GitHub Secrets Required

Set these in your repo: **Settings → Secrets and variables → Actions**

| Secret | Required | Description |
|--------|----------|-------------|
| `ANDROID_PACKAGE_NAME` | ✅ | Your app's package name |
| `KEYSTORE_B64` | ✅ | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | ✅ | Keystore password |
| `KEY_ALIAS` | ✅ | Signing key alias |
| `KEY_PASSWORD` | ✅ | Signing key password |
| `PLAY_STORE_JSON_KEY` | ✅ | Google Play service account JSON |
| `ANTHROPIC_API_KEY` | ❌ | Claude API key (for AI release notes) |
| `SLACK_WEBHOOK_URL` | ❌ | Slack webhook (for notifications) |

### Manual Dry-Run via GitHub Actions

You can trigger a dry-run manually:
1. Go to **Actions** → **Android CI/CD Pipeline**
2. Click **Run workflow**
3. Check **"Run in dry-run mode"**
4. Click **Run workflow**

---

## 🔍 Troubleshooting

### Common Errors and Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Version code already used` | Pipeline re-ran without incrementing version | Run `--resume --from-phase P1` to re-increment |
| `403 Forbidden on Play Console` | Service account missing permissions | Play Console → Users → Service Accounts → grant **Release Manager** role |
| `401 Unauthorized` | JSON key is invalid or expired | Regenerate the service account JSON key |
| `Keystore not found` | Secrets Agent failed to decode | Verify `KEYSTORE_B64` is valid: `echo $KEYSTORE_B64 \| base64 -d > test.jks` |
| `state.json not found` | First run or `.pipeline/` deleted | Run `--init` to start fresh |
| `QA test failure` | Unit tests failed | Check `.pipeline/artifacts/test_report.xml`, fix tests, then `--resume --from-phase P3` |
| `Sync point failed` | An agent failed in a parallel phase | Check `--status` to see which agent failed, fix the issue, then `--resume` |

### Debugging Commands

```bash
# View the full pipeline state
cat .pipeline/state.json | python -m json.tool

# Check a specific agent's status
python -m agents.orchestrator --status

# View past runs
python -m agents.orchestrator --history

# Check Play Store API access
python -m agents.upload_agent --check-permissions
```

---

## ⚠️ Known Limitations

1. **First upload must be manual.** Google Play API doesn't support creating a brand-new app. Upload the first AAB manually via the Play Console web UI. After that, all updates are automated.

2. **Version code must always increase.** The Version Agent handles this automatically, but if you manually change the version code, ensure it's higher than what's on the Play Store.

3. **Signing key is permanent.** Once you upload with a keystore, Google ties your app identity to that key forever. Never lose or change your keystore.

4. **Service account needs correct permissions.** The Play Store service account needs **Release Manager** (not just Viewer) permissions to upload.

5. **`ANTHROPIC_API_KEY` is optional.** Without it, the Metadata Agent falls back to CHANGELOG.md or generic release notes — the pipeline won't fail.

---

## 📊 Implementation Summary

| What | Count |
|------|-------|
| **Agents** | 9 specialized agents |
| **Pipeline Phases** | 5 (with parallel execution) |
| **Python Files** | 11 agent modules + orchestrator |
| **Test Files** | 13 test modules |
| **Total Tests** | 207 (all passing ✅) |
| **Code Coverage** | 82% |
| **Lines of Code** | ~3,400 (agents) + ~2,800 (tests) |

### What Was Built (Chronological):

1. **Foundation Layer** — `PipelineMemory` (state management, atomic writes, locking) + `BaseAgent` (lifecycle, retries, idempotency)
2. **Wave 1 Agents** — Validator, Version, Secrets (parallel Phase 1)
3. **Wave 2 Agent** — Build Agent (Fastlane integration, AAB verification)
4. **Wave 3 Agents** — QA (JUnit parser, flaky detection) + Metadata (CHANGELOG → Claude AI → fallback)
5. **Wave 4 Agent** — Upload Agent (Fastlane supply, error mapping, secure key handling)
6. **Wave 5 Agents** — Tester (YAML config, API assignment) + Notifier (Slack + GitHub, never-fail)
7. **Orchestrator** — 5-phase coordinator with `ThreadPoolExecutor`, sync points, resume
8. **CI/CD** — GitHub Actions workflow + E2E integration tests
9. **Bug Fix** — Thread-safety race condition in `PipelineMemory.save()` (unique tmp filenames + RLock)

---

## 📝 License

This project is designed for internal use. Refer to your organization's licensing policy.
