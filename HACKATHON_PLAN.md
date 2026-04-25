# Pre-Triage Co-Pilot — Hackathon Plan

> Read this once at kickoff. It's the team contract. If anything here goes stale, edit it on `main` — don't let the doc drift from reality.

## Pitch

A pre-triage navigator that decides whether you need a doctor, in under a minute, on your phone — and sends de-identified data to your provider when escalation is warranted.

## Architecture: three principles

1. **Everything that touches patient data runs on-device.** Voice, text, images, document extraction, PHI scrubbing — all via Melange-hosted models on the phone NPU/GPU. The cloud only ever sees de-identified placeholder text.
2. **Every ML call has a deterministic fallback.** If MedGemma fails to load or times out, a `RuleBasedTriageService` returns a conservative routing. If tanaos fails, a `RegexAnonymizerService` does best-effort PHI scrubbing. If Whisper fails, the user types instead. The demo never crashes from a model edge case.
3. **One multimodal model does the reasoning.** No chained classifiers. MedGemma takes the transcript + optional image + insurance-plan JSON + a tuned system prompt and returns one strict JSON object: `{ severity, reasoning, red_flags, recommended_action, confidence }`.

## Screen flow

```
Splash (warmup)
   │
Intake ───── (voice via Whisper or text)
   │
   ├─ optional: Camera (rash / wound / mole) — quality precheck before send
   │
TriageOrchestrator ── one MedGemma call ──> JSON
   │
Result (severity + reasoning + one-tap action)
   │
   └─ on escalation: De-id Doc Upload (photo of labs / prescription)
        ├─ MedGemma extracts structured JSON
        ├─ tanaos scrubs PHI → placeholder tokens
        ├─ token map → Android Keystore (never leaves device)
        ├─ POST de-identified payload to mock telehealth (local Ktor)
        └─ on response: re-identify locally before display
```

## Performance targets (hard, non-negotiable)

On-device doesn't mean free. The phone is the server. Treat it that way.

- **Sub-3-second** text triage from button-tap to first rendered token.
- **Sub-6-second** image triage (text + image MedGemma call).
- **Load every model exactly once at app start**, not per-screen. Hold them in a singleton tied to the application lifecycle.
- **Warm each model with a dummy inference** during the splash screen so the first user-facing run isn't paying cold-start.
- **Cap system prompts**: MedGemma system prompt under 600 tokens; user-facing output capped at 256 tokens (streamed).
- **Resize images to 768px max edge** before passing to MedGemma. Do not send full-resolution camera bitmaps.
- **All inference on `Dispatchers.IO`**, never on the main thread. UI updates via `StateFlow`.
- **Stream tokens to the UI** so perceived latency stays low even when generation is slow.
- **Profile cold-start time and RAM by hour 6, not hour 30.** A Windows dev (phone in hand) owns this. If we're over budget, drop to a smaller MedGemma variant before we keep building features.

## Three on-device models

See [`docs/models.md`](./docs/models.md) for the catalog IDs. Roles:

- **Whisper Tiny** — voice → text on Intake. Audio buffer lives in memory, never written to disk.
- **MedGemma 1.5 4B** — triage reasoning (text + image) **and** document extraction (lab reports / prescriptions → JSON).
- **tanaos-text-anonymizer-v1** — PHI scrub before any de-identified payload leaves the device.

## Service-interface contract (every ML service)

```kotlin
interface TriageService {
    suspend fun triage(request: TriageRequest): Result<TriageDecision>
}
```

- All ML services return `kotlin.Result<T>` — no thrown exceptions across the boundary.
- Each service has **two implementations** behind the same interface: a real one (Melange-backed) and a deterministic fallback (`RuleBased*`, `Regex*`). The orchestrator picks at runtime based on init success.
- Pattern source: SumiSense protocol-boundary architecture. Don't reinvent it.

## Data layer

- Bundled JSON in `app/src/main/assets/`:
  - `insurance_plans/{basic,ppo,hmo}.json` — three mock plans with telehealth vendor deep-link, in-network urgent-care identifier, copay table.
  - `demo_scenarios/{skin_lesion,pink_eye,chest_pain}.json` — pre-built inputs for the pitch demo so we never depend on live voice/camera in front of judges.
- **Nothing persisted to long-term storage** except the Keystore-protected PHI token map.
- Session data lives in memory and dies with the activity.

## PHI / privacy invariants

- The PHI token map (`[PATIENT_NAME_1]` → `"Jane Doe"`) is encrypted via `MasterKey` + `EncryptedSharedPreferences`, keyed by the Keystore.
- The map **never** crosses the network. Re-identification is local-only.
- The mock telehealth endpoint receives only de-identified text. If you find yourself logging the original payload anywhere, delete that log line immediately.
- Regex emergency short-circuit (chest pain / stroke / severe bleeding / breathing trouble) runs **before** any LLM call. It's a safety floor that doesn't depend on the model behaving.

## Team split — backend Macs, frontend Windows

Two roles, two people each. **No vertical ownership within either pair** — both Macs may touch any backend file; both Windows devs may touch any UI screen. Disjoint package sets between the pairs keep cross-pair conflicts rare.

### Backend Macs (2 people, fungible)

Own everything that isn't a Compose screen: `ml/`, `privacy/`, `triage/`, `data/`. Pull tasks off a shared queue (pinned Slack thread) — whoever is free grabs the next one.

**Definition of Done across the backend, by area:**

| Area | Package | DoD |
|---|---|---|
| ML runtime | `ml/` | Singleton `MelangeRuntime` loads MedGemma + Whisper + anonymizer at app start with progress callbacks; warmup with dummy inference on splash; streaming token API; cold-start metric logged. |
| Privacy / de-id | `privacy/` | `Anonymizer` (tanaos) + `RegexAnonymizer` fallback behind one interface; deterministic placeholder tokens; Keystore-backed token map; re-id pass on incoming response payload; mock telehealth Ktor endpoint that echoes payload to prove round-trip. |
| Triage logic | `triage/` | MedGemma system prompt (capped, JSON-output-only); strict schema parser with confidence-floor handling (<0.6 → conservative routing); regex emergency short-circuit; the three demo scenarios scripted and timed; pitch script. |
| Data | `data/` | Bundled insurance plan JSON loader; demo scenario JSON loader; in-memory session models. |

**Pair-program** for the 2-3 hardest stretches: (1) MedGemma prompt + schema convergence, (2) de-id ↔ re-id round-trip, (3) demo-day debugging. Otherwise work solo on different files.

### Frontend Windows (2 people, fungible)

Own the Compose UI: `ui/intake/`, `ui/camera/`, `ui/result/`, splash, de-id doc upload screen. Layout, theming, navigation, button intents, animation, copy. **This is real development work, not just testing.** The inner loop for UI is "edit Compose → install on phone → tap → adjust" — that loop is fastest in the hands of someone holding the phone.

**Definition of Done across the UI:**

| Screen | DoD |
|---|---|
| Splash | Warmup of all three models with progress UI; advances to Intake when all three are warm. |
| Intake | Text field + voice button (Whisper start/stop); RECORD_AUDIO permission flow; transcript handed to `TriageOrchestrator`. |
| Camera | CameraX preview + capture; blur/luminance precheck on captured frame; "retake" flow if quality fails; resized bitmap (768px max edge) handed to orchestrator. |
| Result | Severity badge + reasoning text + red-flags list + one-tap action button wired to intent (911 dialer / telehealth deep link / maps query / self-care text). |
| De-id Doc Upload | Photo capture → MedGemma extraction (via interface) → tanaos scrub → POST → re-id pass → render result. |
| Navigation | Compose Navigation between all screens; back-stack handling. |

**Also own** — phone-coupled tasks that genuinely require hardware: cold-start profiling on the demo device (by hour 6, posted as a number not a guess); demo phone stewardship from hour 24 onward (clean install, pre-cached models, read-only for dev installs after the freeze).

**Also serve as on-device validators** for backend pushes — when a Mac pushes ML/privacy/triage code, a Windows dev pulls latest `main`, builds, and runs the relevant flow within ~20 minutes. No PR review needed; just verify it didn't break the demo path. Until that confirmation, the feature isn't done.

## Risk register

| Risk | Trigger | Mitigation |
|---|---|---|
| Model download fails or auth-errors mid-demo | Bad token, network blip, catalog change | Pre-download all three models on the demo phone the night before. Cache lives in app sandbox; don't clear data. |
| NPU OOM when loading MedGemma + Whisper + anonymizer | Concurrent load on a 6 GB device | Load sequentially in splash; if heap pressure, free Whisper after transcription before MedGemma run. |
| Cold-start > 6 s | First-token latency on cold MedGemma | Warmup dummy inference in splash; profile by hour 6 (Windows dev with phone); if over budget, drop to a smaller MedGemma variant immediately. |
| Audio permission denied on demo phone | User taps "Don't allow" | Always allow text input as the primary path. Voice is a bonus, not a dependency. |
| Phone thermal-throttles during pitch | Back-to-back inferences on a hot SoC | Backup phone primed and cool; run scenarios from script with 30 s breathing room. |
| Two devs push to `main` and conflict | Direct-to-main workflow without PRs | Disjoint package ownership (backend vs `ui/`) keeps cross-pair conflicts rare. `git pull --rebase` before every change. `assembleDebug` gate before push. 15-min fix-forward-or-revert rule. |
| Backend interface change breaks UI integration | Mac dev rev's a service signature | Heads-up in Slack `#pushes` *before* the breaking push. UI works against `FakeMelangeRuntime` so it stays decoupled from real-model availability. |
| UI great on dev phone, broken on demo phone (different SoC / screen) | Hardware divergence | Run the full demo on the demo phone at hour 24 *before* freezing it; fix on dev phone first then refresh. |
| Mac dev needs to test on a phone | No phone in hand | Hand them a phone for 30 minutes. Don't grind on remote screencaps. |

## Demo script (3 stories, ~75 s each)

1. **Skin lesion check** — text input "I have a dark mole on my arm that's been growing" + photo of a mole. Expected: `urgent_care` or `telehealth` with dermatology deep link, confidence > 0.7.
2. **Pink eye for a child** — voice input "My 5 year old's eye is red and goopy". Expected: `telehealth` with pediatric vendor link, copay shown from insurance plan.
3. **Chest pain emergency** — voice input "I've had crushing chest pain for 20 minutes radiating to my left arm". Expected: regex short-circuit fires **before** the LLM call → `emergency` screen with 911 dialer button. Sub-1-second to render.

After story 3, do the de-id story: photograph a fake lab report, show the placeholder-tokenized payload going out, the response coming back, and the re-identified result on screen.

## Out of scope (don't build this weekend)

- Real authentication, user accounts, real insurance APIs.
- Real telehealth backend — we ship the mock Ktor endpoint.
- Multi-language. English only.
- iOS. Android only.
- Persistent history. Sessions die with the activity.

## Workflow — push to main, no PRs

Everyone pushes directly to `main`. No `feat/*` branches. No PR review. The team is small enough that PR ceremony adds friction without catching real bugs, and we don't want anyone learning GitHub PR mechanics under hackathon pressure.

**The loop, every time you change code:**

1. `git pull --rebase`
2. Make the change.
3. `./gradlew assembleDebug` — confirm it still compiles. **No exceptions.** This is the only gate keeping `main` green.
4. `git push`
5. Post a one-line note in Slack `#pushes`: `pushed: <what changed, one sentence>`.

**On-device validation (Windows devs):**

1. Pull latest `main`.
2. `./gradlew installDebug` (or `adb install -r app/build/outputs/apk/debug/app-debug.apk`).
3. Run the relevant flow.
4. Reply in the same Slack thread: thumbs-up, or `broken: <log snippet>`.

**If you break `main`, you have 15 minutes to fix-forward or revert.** The `#pushes` channel makes breakage immediately visible. If you can't fix in 15, revert and reopen the change locally.

**Standup every 4 hours, 5 minutes max.** Show what's broken, not what's working.

**Commit message format:** `<area>: <verb> <thing>` — e.g., `triage: add emergency short-circuit regex`, `ui/intake: wire voice button to whisper`.

**Conflict avoidance:** Mac and Windows pairs own disjoint packages (backend vs `ui/`), so cross-pair collisions are rare. Within a pair, the task queue prevents two people grabbing the same task; if you both need to touch the same file, pair-program for that stretch instead of racing.
