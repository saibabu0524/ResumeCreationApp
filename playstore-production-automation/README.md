# Android CI/CD — AI Parallel Agent Pipeline

> **AI AGENT READING THIS:** Start here. This file is your entry point.
> Read all 3 READMEs before writing any code. Each README has a specific role:
> - `README.md` → Architecture, flow, quick start (YOU ARE HERE)
> - `AGENTS.md` → Every agent's code, memory system, state management
> - `DEVELOPMENT.md` → Code standards, test requirements, file generation rules

---

## What This System Does

When a developer merges code to the `release` branch, this pipeline automatically:

1. Validates the build (lint, SDK compliance, Play Store policy)
2. Increments version codes (no manual bumping ever)
3. Fetches secrets securely from vault
4. Builds a signed Android AAB (App Bundle)
5. Runs test suite in parallel with metadata generation
6. Uploads to Google Play Console (creates app if it doesn't exist)
7. Assigns tester groups and notifies the team

**Total time: ~18–22 minutes** (vs 40+ minutes manually or sequentially)

---

## Critical: How This System Survives Session Loss

> **AI AGENT:** This section is the most important one for you.
> Every agent writes its state to `.pipeline/state.json` before it exits.
> When you start a new session, your FIRST action must always be:
>
> ```python
> from agents.memory import PipelineMemory
> memory = PipelineMemory()
> state = memory.load()
> print(state.summary())  # tells you exactly where the pipeline is
> ```
>
> Never assume a fresh start. Always check state first.

### Memory Architecture

```
.pipeline/
├── state.json          ← Current pipeline run state (read this first)
├── history/
│   ├── run_001.json    ← Completed run archive
│   ├── run_002.json
│   └── ...
├── locks/
│   └── version.lock    ← Prevents version code collisions
└── artifacts/
    ├── build.aab       ← Signed AAB (temp, cleaned after upload)
    ├── test_report.xml ← QA Agent output
    └── metadata.json   ← Store listing data from Metadata Agent
```

### State Format (what AI reads on session resume)

```json
{
  "run_id": "run_20260217_143022",
  "triggered_by": "merge:develop→release",
  "commit_sha": "a3f2b19",
  "started_at": "2026-02-17T14:30:22Z",
  "status": "IN_PROGRESS",
  "current_phase": "P3",
  "agents": {
    "validator":  { "status": "DONE",    "result": "PASS",   "completed_at": "2026-02-17T14:31:05Z" },
    "version":    { "status": "DONE",    "result": "PASS",   "completed_at": "2026-02-17T14:31:12Z" },
    "secrets":    { "status": "DONE",    "result": "PASS",   "completed_at": "2026-02-17T14:31:08Z" },
    "build":      { "status": "DONE",    "result": "PASS",   "completed_at": "2026-02-17T14:43:01Z" },
    "qa":         { "status": "RUNNING", "result": null,     "started_at":   "2026-02-17T14:43:05Z" },
    "metadata":   { "status": "RUNNING", "result": null,     "started_at":   "2026-02-17T14:43:05Z" },
    "upload":     { "status": "PENDING", "result": null },
    "tester":     { "status": "PENDING", "result": null },
    "notifier":   { "status": "PENDING", "result": null }
  },
  "version_info": {
    "version_code": 42,
    "version_name": "2.1.0"
  },
  "errors": [],
  "ai_notes": "Build succeeded in 11m 56s. QA and Metadata running in parallel."
}
```

> **AI AGENT:** The `ai_notes` field is for you. Write human-readable context
> in this field so your next session understands what was happening.
> Example: "Upload failed with error 403 — service account needs Play Editor role.
> Fix: go to Play Console > Users > Service Account > grant Editor permission."

---

## System Architecture

```
GitHub: merge to 'release'
          │
          ▼
  ┌───────────────────┐
  │  Orchestrator     │  ← Reads state.json, resumes or starts fresh
  │  Agent            │
  └─────────┬─────────┘
            │
     ┌──────┴──────────────────────────────────────┐
     │         PHASE 1 (ALL PARALLEL)              │
     ▼              ▼                  ▼           │
┌──────────┐  ┌──────────┐   ┌──────────────┐    │
│Validator │  │ Version  │   │   Secrets    │    │
│  Agent   │  │  Agent   │   │   Agent      │    │
└────┬─────┘  └────┬─────┘   └──────┬───────┘    │
     └──────────────┴────────────────┘            │
                    │ SYNC POINT 1                │
                    ▼                             │
           ┌───────────────┐                     │
           │  Build Agent  │  (single, isolated)  │
           └───────┬───────┘                     │
                   │                             │
          ┌────────┴────────┐                   │
          │  PHASE 3 (PARALLEL)                 │
          ▼                 ▼                   │
    ┌──────────┐    ┌─────────────┐             │
    │ QA Agent │    │  Metadata   │             │
    │          │    │   Agent     │             │
    └────┬─────┘    └──────┬──────┘             │
         └────────┬─────────┘                   │
                  │ SYNC POINT 2                │
                  ▼                             │
         ┌────────────────┐                    │
         │  Upload Agent  │                    │
         └───────┬────────┘                    │
                 │                             │
        ┌────────┴────────┐                   │
        ▼                 ▼                   │
  ┌──────────┐    ┌──────────────┐            │
  │  Tester  │    │   Notifier   │            │
  │  Agent   │    │   Agent      │            │
  └──────────┘    └──────────────┘            │
                                              └─┘
```

---

## Repository Structure

```
android-cicd-agents/
│
├── README.md                    ← You are here (AI entry point)
├── AGENTS.md                    ← All agent code + memory system
├── DEVELOPMENT.md               ← Code standards + test requirements
│
├── agents/
│   ├── __init__.py
│   ├── memory.py                ← PipelineMemory class (CRITICAL)
│   ├── base_agent.py            ← BaseAgent class all agents extend
│   ├── orchestrator.py
│   ├── validator_agent.py
│   ├── version_agent.py
│   ├── secrets_agent.py
│   ├── build_agent.py
│   ├── qa_agent.py
│   ├── metadata_agent.py
│   ├── upload_agent.py
│   ├── tester_agent.py
│   └── notifier_agent.py
│
├── tests/
│   ├── __init__.py
│   ├── test_memory.py           ← Tests for memory/state system
│   ├── test_base_agent.py
│   ├── test_orchestrator.py
│   ├── test_validator_agent.py
│   ├── test_version_agent.py
│   ├── test_secrets_agent.py
│   ├── test_build_agent.py
│   ├── test_qa_agent.py
│   ├── test_metadata_agent.py
│   ├── test_upload_agent.py
│   ├── test_tester_agent.py
│   └── test_notifier_agent.py
│
├── config/
│   ├── pipeline.yaml            ← Pipeline configuration
│   └── tester-groups.yaml       ← Tester group definitions
│
├── .github/
│   └── workflows/
│       └── release.yml          ← GitHub Actions CI/CD workflow
│
├── fastlane/
│   ├── Fastfile                 ← Fastlane lanes
│   └── Appfile                  ← App config
│
├── .pipeline/                   ← Runtime state (git-ignored)
│   ├── state.json               ← Current run state
│   ├── history/
│   ├── locks/
│   └── artifacts/
│
├── requirements.txt
└── .gitignore
```

---

## Quick Start

### Prerequisites
```bash
# Python 3.11+
pip install -r requirements.txt

# Ruby + Fastlane
gem install fastlane

# Android SDK (set ANDROID_HOME)
export ANDROID_HOME=/path/to/android-sdk
```

### First-Time Setup Checklist

> **AI AGENT:** Run this checklist top to bottom. Each step must pass before
> proceeding to the next. Write the result of each step into state.json.

```bash
# 1. Verify secrets are available
python -m agents.secrets_agent --verify-only

# 2. Verify Play Console API access
python -m agents.upload_agent --verify-connection

# 3. Run all tests (must be 100% pass before any pipeline run)
pytest tests/ -v

# 4. Dry-run the full pipeline (no actual upload)
python -m agents.orchestrator --dry-run

# 5. Real run
python -m agents.orchestrator --run
```

### Environment Variables Required

```bash
# Android Signing
KEYSTORE_FILE_B64=<base64-encoded keystore>
KEYSTORE_PASSWORD=<keystore password>
KEY_ALIAS=<key alias>
KEY_PASSWORD=<key password>

# Google Play
PLAY_STORE_JSON_KEY=<path to service account JSON or inline JSON string>
ANDROID_PACKAGE_NAME=com.yourcompany.yourapp

# AI / LLM (for Metadata Agent)
ANTHROPIC_API_KEY=<your Claude API key>

# Notifications
SLACK_WEBHOOK_URL=<slack incoming webhook>
GITHUB_TOKEN=<github token with repo write access>

# Pipeline
PIPELINE_STATE_DIR=.pipeline
PIPELINE_MAX_RETRIES=3
```

---

## Phase Definitions

| Phase | Agents Running | Can Parallelize? | Sync Point After? |
|-------|---------------|-----------------|------------------|
| P1    | Validator, Version, Secrets | ✅ Yes — all 3 at once | ✅ Yes — all 3 must pass |
| P2    | Build | ❌ No — sequential | ✅ Yes — must succeed |
| P3    | QA, Metadata | ✅ Yes — both at once | ✅ Yes — both must pass |
| P4    | Upload | ❌ No — sequential | ✅ Yes — must succeed |
| P5    | Tester, Notifier | ✅ Partial — fire both | ❌ No sync needed |

---

## Known Limitations (AI: read before attempting automation)

1. **App creation is manual for first upload.** Google Play API does not support
   creating a brand-new app programmatically. The first build must be uploaded
   manually via the Play Console web UI. After that, all updates are automated.

2. **Version code must always increase.** If a pipeline fails mid-run and the
   version code was already incremented, the next run must increment again.
   The Version Agent handles this automatically via state.json.

3. **Service account needs Play Editor role.** If upload fails with 403,
   go to Play Console → Users & Permissions → Service Accounts → grant
   "Release manager" permission.

4. **AAB signing key is permanent.** Never change your keystore after first
   upload. Google ties your app identity to the signing key forever.

---

## Troubleshooting Guide (AI: use this to self-diagnose)

```
ERROR: "Upload failed: APK specifies a version code that has already been used"
FIX:   Version Agent did not run, or ran but state was lost.
       Run: python -m agents.version_agent --force-increment

ERROR: "403 Forbidden on Play Console API"  
FIX:   Service account missing permissions.
       Check: python -m agents.upload_agent --check-permissions

ERROR: "Keystore not found at path"
FIX:   Secrets Agent failed to decode/write keystore.
       Check: echo $KEYSTORE_FILE_B64 | base64 -d > /tmp/test.jks && keytool -list -keystore /tmp/test.jks

ERROR: "state.json not found"
FIX:   First run, or .pipeline/ was deleted.
       Run: python -m agents.orchestrator --init

ERROR: "QA Agent: test failure detected"
FIX:   Read .pipeline/artifacts/test_report.xml for failing test names.
       Fix the tests, then re-run: python -m agents.orchestrator --resume --from-phase P3
```

---

## Running Tests

```bash
# All tests
pytest tests/ -v

# Single agent tests
pytest tests/test_memory.py -v
pytest tests/test_validator_agent.py -v

# With coverage
pytest tests/ --cov=agents --cov-report=html

# Test naming convention: test_<what>_<condition>_<expected>
# Example: test_version_agent_when_store_ahead_uses_store_version
```

> **AI AGENT:** Every function you write must have a corresponding test.
> No exceptions. See `DEVELOPMENT.md` for the full test-writing protocol.
