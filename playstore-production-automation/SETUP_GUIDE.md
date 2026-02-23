# 🛠️ Real-World Setup Guide — Automate Your Android App to Play Store

> **Scenario:** You have a GitHub repo with your Android app, a keystore file,
> and you want to fully automate: Build → Sign → Upload to Play Store.
>
> This guide uses a real-world example with an app called **"MyApp"**.

---

## 📋 What You Need Before Starting

| Item | Example | Where to Get It |
|------|---------|-----------------|
| **GitHub Repo** | `github.com/saibabu/MyApp` | Your existing Android project |
| **Keystore File** | `myapp-release.jks` | You already have this |
| **Keystore Password** | `MyStr0ngP@ss` | You already know this |
| **Key Alias** | `myapp-key` | Set when keystore was created |
| **Key Password** | `K3yP@ssword` | Set when keystore was created |
| **Google Play Console Account** | — | [play.google.com/console](https://play.google.com/console) |
| **App already on Play Store** (at least once) | — | First upload must be manual |

---

## Step 1: Set Up Google Play API Access

> This is the most important step. Without this, you can't upload automatically.

### 1.1 Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. **Create a new project** (or use existing): `MyApp-CICD`
3. Note the **Project ID**

### 1.2 Enable the Google Play Developer API

1. In Google Cloud Console → **APIs & Services** → **Library**
2. Search for **"Google Play Android Developer API"**
3. Click **Enable**

### 1.3 Create a Service Account

1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **Service Account**
3. Name it: `myapp-play-uploader`
4. Role: **No role needed here** (permissions are granted in Play Console)
5. Click **Done**
6. Click on the newly created service account
7. Go to **Keys** tab → **Add Key** → **Create new key** → **JSON**
8. Download the JSON file — **save this securely!**

Example downloaded file (`myapp-service-account.json`):
```json
{
  "type": "service_account",
  "project_id": "myapp-cicd-12345",
  "private_key_id": "abc123...",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----\n",
  "client_email": "myapp-play-uploader@myapp-cicd-12345.iam.gserviceaccount.com",
  "client_id": "123456789",
  ...
}
```

  ### 1.4 Grant Permissions in Play Console

  1. Go to [Google Play Console](https://play.google.com/console)
  2. **Setup** → **API access**
  3. **Link** your Google Cloud project (`MyApp-CICD`)
  4. Find your service account (`myapp-play-uploader@...`)
  5. Click **Grant access**
  6. Set permissions:
    - ✅ **Release to production, exclude devices, and use Play App Signing**
    - ✅ **Release apps to testing tracks**
    - ✅ **Manage testing tracks and edit tester lists**
  7. Click **Invite user** → **Send invite**

  > ⚠️ **Important:** The service account needs **Release Manager** or equivalent
  > permissions. Without this, you'll get `403 Forbidden` errors.

---

## Step 2: Encode Your Keystore to Base64

Your keystore file needs to be stored as a secret in GitHub. Since GitHub secrets
are text-only, we encode the binary `.jks` file to Base64:

```bash
# Replace with your actual keystore path
base64 -i /path/to/myapp-release.jks | tr -d '\n' > keystore_b64.txt

# Verify it worked (should print keystore info)
cat keystore_b64.txt | base64 -d > /tmp/verify.jks
keytool -list -keystore /tmp/verify.jks -storepass "MyStr0ngP@ss"

# Clean up the verification file
rm /tmp/verify.jks
```

You should see output like:
```
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 1 entry

myapp-key, Feb 17, 2026, PrivateKeyEntry,
Certificate fingerprint (SHA-256): AB:CD:EF:12:34:...
```

> ✅ If you see this, your keystore and password are correct.

---

## Step 3: Add the Pipeline to Your Repo

### 3.1 Copy the pipeline files into your Android project

```bash
# Go to your Android app repo
cd /path/to/MyApp

# Copy the pipeline
cp -r /Users/saibabu/Documents/playstore-production-automation/agents/ ./agents/
cp -r /Users/saibabu/Documents/playstore-production-automation/tests/ ./tests/
cp -r /Users/saibabu/Documents/playstore-production-automation/config/ ./config/
cp -r /Users/saibabu/Documents/playstore-production-automation/fastlane/ ./fastlane/
cp -r /Users/saibabu/Documents/playstore-production-automation/.github/ ./.github/
cp /Users/saibabu/Documents/playstore-production-automation/requirements.txt ./
cp /Users/saibabu/Documents/playstore-production-automation/.gitignore ./.gitignore
```

### 3.2 Update `fastlane/Appfile` with your package name

```ruby
# fastlane/Appfile
package_name(ENV["ANDROID_PACKAGE_NAME"] || "com.saibabu.myapp")
json_key_file(ENV["PLAY_STORE_JSON_KEY_PATH"] || "play_store_key.json")
```

### 3.3 Update `config/tester-groups.yaml`

```yaml
groups:
  internal:
    track: "internal"
    emails:
      - saibabu@gmail.com
      - your-qa-team@gmail.com
    auto_promote_to: null

  # Add alpha/beta tracks as needed
  # alpha:
  #   track: "alpha"
  #   emails:
  #     - beta-tester@gmail.com
  #   auto_promote_to: "production"
```

### 3.4 Your project structure should now look like:

```
MyApp/                              ← Your existing Android project
├── app/
│   ├── build.gradle                ← Has versionCode and versionName
│   └── src/
│       └── main/
│           └── AndroidManifest.xml
├── build.gradle
├── gradlew
│
├── agents/                         ← Pipeline agents (just added)
├── tests/                          ← Pipeline tests (just added)
├── config/
│   └── tester-groups.yaml
├── fastlane/
│   ├── Fastfile
│   └── Appfile
├── .github/
│   └── workflows/
│       └── pipeline.yml
├── requirements.txt
└── .gitignore
```

---

## Step 4: Set Up GitHub Secrets

Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add these secrets one by one:

### Required Secrets

| Secret Name | Value | How to Get It |
|-------------|-------|---------------|
| `ANDROID_PACKAGE_NAME` | `com.saibabu.myapp` | Your app's `applicationId` from `build.gradle` |
| `KEYSTORE_B64` | *(paste content of `keystore_b64.txt`)* | From Step 2 |
| `KEYSTORE_PASSWORD` | `MyStr0ngP@ss` | Your keystore password |
| `KEY_ALIAS` | `myapp-key` | Your key alias |
| `KEY_PASSWORD` | `K3yP@ssword` | Your key password |
| `PLAY_STORE_JSON_KEY` | *(paste entire content of `myapp-service-account.json`)* | From Step 1.3 |

### Optional Secrets (for extra features)

| Secret Name | Value | Purpose |
|-------------|-------|---------|
| `ANTHROPIC_API_KEY` | `sk-ant-...` | AI-generated release notes (Claude) |
| `SLACK_WEBHOOK_URL` | `https://hooks.slack.com/services/...` | Slack notifications |

### How to add them:

```
1. Go to: github.com/saibabu/MyApp/settings/secrets/actions
2. Click "New repository secret"
3. Name: KEYSTORE_B64
4. Value: (paste the content from keystore_b64.txt)
5. Click "Add secret"
6. Repeat for each secret
```

---

## Step 5: First-Time Manual Upload (One-time only)

> ⚠️ Google Play API doesn't support creating a brand-new app.
> You must upload the first AAB manually through the Play Console web UI.
> After this one-time step, all future uploads are automated.

### 5.1 Build the first AAB locally

```bash
cd /path/to/MyApp

# Build signed AAB
./gradlew bundleRelease \
  -Pandroid.injected.signing.store.file=/path/to/myapp-release.jks \
  -Pandroid.injected.signing.store.password=MyStr0ngP@ss \
  -Pandroid.injected.signing.key.alias=myapp-key \
  -Pandroid.injected.signing.key.password=K3yP@ssword
```

The AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

### 5.2 Upload to Play Console

1. Go to [Play Console](https://play.google.com/console)
2. Select your app (or create a new one)
3. Go to **Production** → **Create new release**
4. Upload `app-release.aab`
5. Add release notes
6. Click **Review release** → **Start rollout to production**

> ✅ After this first manual upload, the pipeline handles everything automatically.

---

## Step 6: Trigger the Automated Pipeline

Now everything is set up! Here's how to trigger the pipeline:

### Option A: Push to `release` branch (automatic)

```bash
cd /path/to/MyApp

# Create and push to release branch
git checkout -b release
git push origin release

# This automatically triggers the GitHub Actions pipeline!
```

### Option B: Manual trigger from GitHub Actions

1. Go to your repo → **Actions** → **Android CI/CD Pipeline**
2. Click **Run workflow**
3. Select branch: `release`
4. Optionally check **"Run in dry-run mode"** for testing
5. Click **Run workflow**

### Option C: Run locally (for testing)

```bash
cd /path/to/MyApp

# Set up Python environment
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Set environment variables (one-time, or use .env file)
export ANDROID_PACKAGE_NAME="com.saibabu.myapp"
export KEYSTORE_B64="$(cat keystore_b64.txt)"
export KEYSTORE_PASSWORD="MyStr0ngP@ss"
export KEY_ALIAS="myapp-key"
export KEY_PASSWORD="K3yP@ssword"
export PLAY_STORE_JSON_KEY="$(cat myapp-service-account.json)"

# Initialize pipeline
python -m agents.orchestrator --init --commit "$(git rev-parse HEAD)" --branch "release"

# Dry-run first (recommended!)
python -m agents.orchestrator --dry-run

# Real run
python -m agents.orchestrator --run
```

---

## Step 7: What Happens During a Pipeline Run

Here's exactly what happens when the pipeline runs:

```
┌────────────────────────────────────────────────────────────────────┐
│ 🚀 PHASE 1 — Validation (runs 3 agents in parallel, ~1 min)      │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Validator Agent                                                   │
│  ├── Checks targetSdkVersion >= 34                                │
│  ├── Scans AndroidManifest.xml for dangerous permissions          │
│  └── Runs ./gradlew lint                                          │
│                                                                    │
│  Version Agent                                                     │
│  ├── Reads versionCode (42) from build.gradle                     │
│  ├── Increments to 43                                             │
│  ├── Bumps versionName: "2.1.0" → "2.1.1"                        │
│  └── Writes back to build.gradle                                  │
│                                                                    │
│  Secrets Agent                                                     │
│  ├── Decodes KEYSTORE_B64 → /tmp/keystore.jks                    │
│  ├── Validates keystore with keytool                              │
│  └── Verifies PLAY_STORE_JSON_KEY is valid JSON                   │
│                                                                    │
│  ──── SYNC POINT: All 3 must pass ────                            │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ 🔨 PHASE 2 — Build (~12-15 min)                                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Build Agent                                                       │
│  ├── Runs: fastlane build_release                                 │
│  ├── Which runs: ./gradlew bundleRelease (signed with keystore)   │
│  ├── Timeout: 20 minutes                                          │
│  ├── Verifies app/build/outputs/bundle/release/app-release.aab   │
│  └── Saves AAB path to state                                     │
│                                                                    │
│  ──── SYNC POINT: Build must succeed ────                         │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ 🧪 PHASE 3 — QA + Metadata (parallel, ~3-5 min)                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  QA Agent                                                          │
│  ├── Runs: ./gradlew test                                         │
│  ├── Parses JUnit XML results                                     │
│  └── Reports: 45 passed, 0 failed                                 │
│                                                                    │
│  Metadata Agent                                                    │
│  ├── Checks CHANGELOG.md for v2.1.1 section                      │
│  ├── If not found: asks Claude AI to generate notes               │
│  ├── If no API key: uses default "Bug fixes and improvements"     │
│  └── Saves metadata.json with release notes                       │
│                                                                    │
│  ──── SYNC POINT: Both must pass ────                             │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ ☁️  PHASE 4 — Upload (~2-3 min)                                    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Upload Agent                                                      │
│  ├── Writes JSON key to /tmp/play_store_key.json                  │
│  ├── Writes release notes to fastlane/metadata/                   │
│  ├── Runs: fastlane supply                                        │
│  │   └── Uploads app-release.aab to Play Console "internal" track │
│  ├── If error: maps to human-readable fix                         │
│  └── ALWAYS deletes /tmp/play_store_key.json (finally block)      │
│                                                                    │
│  ──── SYNC POINT: Upload must succeed ────                        │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ 📣 PHASE 5 — Notify (parallel, ~10 sec)                           │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Tester Agent                                                      │
│  └── Assigns testers from tester-groups.yaml to internal track    │
│                                                                    │
│  Notifier Agent                                                    │
│  ├── Posts Slack message: "✅ MyApp v2.1.1 (43) deployed!"         │
│  ├── Posts GitHub commit status: success                           │
│  └── NEVER fails (always returns ok)                              │
│                                                                    │
│  ──── NO SYNC POINT (fire and forget) ────                        │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ 🎉 PIPELINE COMPLETE — Total time: ~18-22 minutes                 │
└────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference: Common Commands

```bash
# Check pipeline status
python -m agents.orchestrator --status

# See past runs
python -m agents.orchestrator --history

# Resume after a failure (e.g., fix failing tests, then resume from QA)
python -m agents.orchestrator --resume --from-phase P3

# Run tests only
pytest tests/ -v

# Dry-run (safe — no real build/upload)
python -m agents.orchestrator --dry-run
```

---

## FAQ

**Q: Do I need to upload the first build manually?**
A: Yes, Google Play API doesn't support creating new apps. After the first manual upload, everything is automated.

**Q: What if I don't have a Claude API key?**
A: No problem! The Metadata Agent falls back to CHANGELOG.md or generic release notes. The pipeline won't fail.

**Q: What if I don't have a Slack webhook?**
A: The Notifier Agent gracefully skips Slack notifications. No failure.

**Q: Can I test without actually uploading to Play Store?**
A: Yes! Use `--dry-run` mode. It runs all agents but skips real builds and uploads.

**Q: What if the build fails?**
A: Fix the issue, then run `python -m agents.orchestrator --resume --from-phase P2`. It re-runs from the Build phase without redoing validation.

**Q: How do I change which track to upload to (internal/alpha/beta/production)?**
A: The Metadata Agent determines the track. By default it's `internal`. You can modify this in the agent config or environment.

**Q: Is my keystore safe?**
A: Yes. The keystore is stored as a GitHub Secret (encrypted at rest), decoded to a temp file during build, and always cleaned up in a `finally` block. It's never committed to the repo.
