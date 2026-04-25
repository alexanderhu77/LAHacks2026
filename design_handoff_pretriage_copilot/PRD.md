# Pre-Triage Co-Pilot — End-to-End PRD

> Implementation handoff for Claude Code. This PRD specifies the user-facing flow, screen-by-screen requirements, state contracts, and visual system needed to build the prototype as a real Android (Jetpack Compose) app against the existing repo scaffold (`LAHacks2026/`).

---

## 1. Product summary

**Pre-Triage Co-Pilot** is a native Android app that helps a patient decide, in under a minute, whether they need self-care, telehealth, urgent care, or an emergency room — using on-device multimodal ML (MedGemma 1.5 4B + Whisper Tiny + tanaos anonymizer via Melange).

**Core promise to the user:** voice-first symptom intake, optional camera, on-device only, with a one-tap recommended next step.

**Core promise to the architecture:** every patient datum stays on the phone. The cloud only ever sees de-identified placeholder text.

---

## 2. Personas & primary use

| Persona | Need | Constraint |
|---|---|---|
| Anxious patient | "Should I go to the ER?" | Wants a fast, calm answer in plain language. |
| Parent of sick child | "Is this pink eye?" | Holding a child; needs voice and big tap targets. |
| Older adult / low-vision | "I can't type a long thing" | Voice-first; ≥18pt body type; high-contrast option. |

The app is **English-only**, **Android-only**, and **does not require login**. Sessions die with the activity.

---

## 3. End-to-end flow (high level)

```
Splash (model warmup)
   │
Permissions (mic / camera offer)
   │
Intake ───── voice (Whisper) or text
   │
   ├─ optional: Camera offer ─→ Capture ─→ Quality precheck
   │
Triaging (one MedGemma call)
   │
Result (severity badge + reasoning + 1-tap action)
   │
   └─ on escalation: De-id Doc Upload (labs / prescription)
        ├─ MedGemma extracts JSON
        ├─ tanaos scrubs PHI → placeholder tokens
        ├─ token map → Android Keystore
        ├─ POST de-identified payload to mock telehealth (Ktor)
        └─ on response: re-identify locally before display
```

There is one **emergency short-circuit**: a regex pre-filter on the intake transcript fires before any model call when keywords match (chest pain + radiation, stroke signs, severe bleeding, breathing trouble). On match, skip directly from Intake → Result(emergency) sub-1-second.

---

## 4. Screen-by-screen specification

### 4.1 Splash

**Purpose:** warm three on-device models so the first user-facing inference isn't paying cold-start.

**Layout:**
- Centered: pulsing branded mark ("Nora") + product subtitle "Your pre-triage co-pilot".
- Below: a 4-px progress bar.
- Below progress: a 3-row checklist.
  - Row 1: "Loading nurse model" — `MedGemma 1.5 · 4B`
  - Row 2: "Loading voice model" — `Whisper · tiny`
  - Row 3: "Loading privacy filter" — `tanaos anonymizer`
- Bottom: persistent privacy badge "🔒 On-device · nothing leaves your phone".

**Behavior:**
- Each step runs a real model load + a dummy inference in sequence.
- Rows transition: muted → active → checkmarked (`accent` filled circle).
- When all three complete, advance to Permissions automatically (no tap).
- If any model fails, surface "Continuing in safe mode" toast and proceed; the orchestrator will choose the deterministic fallback for that service.

**State:**
```kotlin
data class SplashState(
    val step: Int,                  // 0..3
    val failures: Set<ModelKey>,    // which models fell back
)
```

**Definition of done:** all three models warm with no main-thread blocking; advances within ≤ 8 s on the demo phone; cold-start metric logged.

---

### 4.2 Permissions

**Purpose:** request mic + camera up front, but always allow text-only fallback.

**Layout:**
- Brand mark (small).
- Display headline: "One quick step before we start".
- Sub: "Voice and camera make Nora more accessible. You can use text instead — your choice, every time."
- Two cards:
  - Microphone — "So you can speak symptoms instead of typing."
  - Camera — "Optional — for rashes, wounds, or moles."
- Privacy badge (full).
- Primary CTA (big): **"Allow & continue"** — triggers `RECORD_AUDIO` then `CAMERA` permission prompts.
- Secondary CTA: **"Use text only"** — proceeds without prompts.

**Behavior:**
- If the user denies either permission, the app continues; Intake shows text input as primary, camera step is skipped automatically.
- Never block forward progress on a permission denial.

**Definition of done:** both permission flows wired; denial path verified end-to-end; deep-link back to system settings is **not** required for v1.

---

### 4.3 Intake (voice-first)

**Purpose:** capture the patient's symptom description as a transcript string.

**Layout:**
- Top row: brand mark + "Nora · Pre-triage co-pilot" on the left, compact privacy badge on the right.
- Display headline: "What's going on today?"
- Sub: "Tap and tell me in your own words — when it started, where it hurts, anything else."
- Transcript card (always visible):
  - Empty state: dashed border, italic muted placeholder "Your words will appear here."
  - Active recording: dashed border, "Listening…" + blinking caret.
  - Filled: solid border, transcript text + blinking caret while still recording.
- Waveform: 28 vertical bars, 100ms frame, animate amplitudes only while recording.
- **Giant mic button (116 px round)** centered low.
  - Idle: accent fill, white mic glyph, label below "Tap to speak".
  - Recording: red fill, white square (stop) glyph, animated outer ring pulse, label "Tap to stop".
  - After recording: label "Tap to add more".
- Two secondary actions in a row:
  - **"Type instead"** — opens a full-screen text overlay with a single textarea + Done.
  - **"Continue"** — proceeds to next screen with the transcript.

**Behavior:**
- Tapping mic toggles recording. Tap-to-stop is the contract; do **not** require press-and-hold.
- Whisper streams transcription; the transcript card updates as tokens arrive.
- "Continue" is enabled even with empty transcript (lets demo / accessibility users skip ahead).
- Emergency regex runs on transcript text *every keystroke / token* — if it fires, immediately route to Result(emergency).
- If `RECORD_AUDIO` denied, hide the mic button and auto-open the type overlay.

**State:**
```kotlin
data class IntakeState(
    val transcript: String,
    val recording: Boolean,
    val emergencyShortCircuit: Boolean,
)
```

**Outputs:** `transcript: String` → orchestrator.

---

### 4.4 Camera offer (optional step)

**Purpose:** offer (not require) a photo for visual symptoms.

**Layout:**
- Tag: "OPTIONAL"
- Display: "Want to show me?"
- Sub: "A photo helps me look at rashes, wounds, eyes, or moles. Skip if it doesn't apply."
- Info card with shield icon + "The image stays on your device." Below it:
  - Caption "Helpful for things like"
  - Inline list: "skin or rashes · eyes · wounds · moles"
  - **Not** a grid of pressable tiles.
- Primary CTA (big): **"Take a photo"** → Camera capture.
- Secondary CTA (bordered): **"Skip — no photo needed"** → Triaging.

**Behavior:** always reachable from Intake; only shown for non-emergency routes.

---

### 4.5 Camera capture

**Purpose:** capture a photo and run a blur/luminance precheck before sending to MedGemma.

**Layout:**
- Black full-bleed CameraX preview.
- Top bar (overlay): close (×) on left, compact "🔒 On-device only" pill on right.
- Center: framing reticle (white corners + thin midline) sized to the central 60% region.
- Bottom: shutter row — flash toggle (left), large white shutter (center), camera flip (right).

**Behavior:**
- Shutter: brief white flash overlay → switches to "Checking quality…" overlay (centered spinner).
- Quality precheck (Laplacian variance for blur, mean luminance for darkness):
  - Pass → proceed silently to Triaging with the captured bitmap (resized to 768 px max edge).
  - Fail → show a bottom sheet:
    - Amber alert icon
    - "A bit blurry — retake?"
    - Sub: "Hold steady about 6 inches away. Better light helps."
    - Buttons: **Retake** (returns to framing) / **Use anyway** (proceeds with current bitmap).

**Outputs:** `Bitmap?` resized + downsampled, plus a quality metric struct.

---

### 4.6 Triaging (loading)

**Purpose:** keep the user oriented during the MedGemma call. Should never feel longer than ~3–6 s.

**Layout:**
- Centered branded mark with a rotating ring.
- Headline: "One moment…"
- Sub: "Running on your phone, not the cloud."
- Progress checklist (rows light up sequentially):
  - "Reading what you said"
  - "Looking at the photo" (only if image present)
  - "Checking insurance options"
  - "Drafting recommendation"

**Behavior:**
- Stream tokens from MedGemma. Each row checks off as a corresponding milestone is observed in the streamed JSON.
- Hard cap: 8 s; on timeout, fall back to `RuleBasedTriageService` and proceed.

---

### 4.7 Result

**Purpose:** deliver a severity-coded recommendation with reasoning and one tappable next step.

**Layout (top-down):**
1. Top row: "← Start over" (left) + compact privacy badge (right).
2. **Severity pill** (color-coded):
   - `EMERGENCY · Call 911 now` — red
   - `URGENT CARE · Go in person today` — amber
   - `TELEHEALTH · Talk to a clinician` — blue
   - `SELF-CARE · Manage at home` — green
3. **Display headline** — one sentence in the model's voice ("Get to an emergency room now.", "Worth a same-day in-person visit.", etc.).
4. **"What I'm seeing" card** — surface, bordered:
   - Brand mark + label (single-line)
   - Reasoning paragraph (16 px, line-height 1.5)
   - Red-flag chips (small, comma-equivalent pill list)
   - Footer line, mono font:
     - Normal: `MedGemma · on-device` left, `confidence 0.78` right
     - Emergency short-circuit: red `Safety rule fired — bypassed model`
5. **Primary action button** (full-width, severity-tinted):
   - Icon tile (white at 22% alpha) + title (18 pt, 600) + "One tap" subtitle, stacked clean
   - Trailing chevron
   - Title varies: "Call 911" / "Find urgent care" / "Start video visit" / "See self-care tips"
   - On tap: dispatch the relevant Android intent (tel:, geo:, custom telehealth deep link, in-app self-care content).
6. **Optional escalate button** (urgent or telehealth only): bordered "Send my recent labs (de-identified)" → De-id Doc Upload.
7. **Footer disclaimer** (12 pt, muted): "Nora is a guide, not a diagnosis. Trust your gut — if something feels worse than this says, seek care."

**Severity → action mapping:**

| Severity | Color | Headline tone | Primary action | Intent |
|---|---|---|---|---|
| emergency | red | Imperative | Call 911 | `tel:911` |
| urgent | amber | Strong recommendation | Find urgent care | maps query: in-network urgent care |
| telehealth | blue | Reassuring | Start video visit | telehealth vendor deep link |
| selfcare | green | Calm | See self-care tips | in-app content |

**Confidence handling:**
- ≥ 0.6 → render as model returned.
- < 0.6 → bump severity one step up (more conservative) and append "I'd rather be cautious here." to the reasoning.

---

### 4.8 De-id document upload

**Purpose:** demonstrate that escalation to a telehealth provider sends only de-identified data.

**Layout:**
- Top bar: back arrow + "Send to provider".
- Display: "Lab report · April 22"
- Sub: "Anything personal gets replaced with placeholders before it leaves your phone."
- **Document preview card** — mono font, line-by-line:
  - `Patient   Maria Hernandez`        → animated swap to `[PATIENT_NAME_1]`
  - `DOB       1979-03-14`             → `[DOB_1]`
  - `MRN       A-2849-1077`            → `[MRN_1]`
  - blank line, then visible lab values (kept):
    - `Hemoglobin A1c · 6.7%`
    - `Fasting glucose · 132 mg/dL`
    - `LDL cholesterol · 148 mg/dL`
  - `Provider  Dr. James Chen`         → `[PROVIDER_1]`
- **Pipeline list** (3 rows, light up sequentially):
  - Reading the document — `medgemma → JSON`
  - Scrubbing personal info — `tanaos → [PATIENT_NAME_1]`
  - Sending to your provider — `POST · de-identified only`
- Primary CTA: **"Send de-identified"** with shield icon (preview state).
- Done state: green confirmation banner + "Done" button → restart.

**Behavior:**
- On Send: extract → scrub → POST to mock Ktor endpoint → echo back → re-identify locally before display.
- Token map persists in `EncryptedSharedPreferences` keyed by Keystore master key, **never** posted.

---

## 5. Visual system

### 5.1 Themes (4 user-selectable, default Warm)

| Token | Warm (default) | Calm clinical | Bold dark | Accessible |
|---|---|---|---|---|
| bg | `#f5efe2` | `#eef3f6` | `#0e1311` | `#ffffff` |
| surface | `#fbf6ec` | `#ffffff` | `#171c1a` | `#ffffff` |
| ink | `#2b2a26` | `#0f1e2a` | `#f1efe7` | `#000000` |
| accent | `#5b7a63` (sage) | `#2f6fb0` (blue) | `#a3d9b1` (mint) | `#005a9e` (deep blue) |
| statusGreen | `#5b7a63` | `#2e7a55` | `#7fc794` | `#0a6b35` |
| statusAmber | `#c98a3a` | `#b27a2a` | `#e0a85a` | `#8a4b00` |
| statusRed | `#b3493a` | `#a83b32` | `#e07565` | `#9b1a1a` |
| statusBlue | `#4a6f8a` | `#2f6fb0` | `#7fb1d9` | `#003e6e` |

### 5.2 Type

- **Body:** Inter 17 sp default; **20 sp** in Accessible theme.
- **Display:** Fraunces 24–30 sp (Warm + Bold); Inter 24–30 sp (Calm + Accessible).
- **Mono:** JetBrains Mono 11–12 sp for telemetry-style footers.

### 5.3 Spacing & shape

- 8-pt grid. Corner radii: 10 (chips) / 14 (buttons) / 16–18 (cards) / 22 (panels).
- Hit targets ≥ 44 pt; the mic button is 116 pt.
- Status pills use a 16% accent tint background + 33% accent border, on accent-tinted ink.

### 5.4 Trust signals

- Persistent "🔒 On-device · nothing leaves your phone" badge:
  - Splash: full version, bottom.
  - Intake / Result: compact, top right.
  - Camera: compact, top right.

---

## 6. Architecture & contracts

### 6.1 Three on-device models (Melange)

| Model | Role |
|---|---|
| Whisper Tiny | Voice → text on Intake. Audio buffer in memory only. |
| MedGemma 1.5 4B | Triage reasoning (text + image) **and** document JSON extraction. |
| tanaos-text-anonymizer-v1 | PHI scrub before any de-identified payload leaves the device. |

Catalog IDs verified at hour-6 against the Melange catalog (see `docs/models.md` in the repo).

### 6.2 Service interfaces

```kotlin
interface TriageService {
    suspend fun triage(request: TriageRequest): Result<TriageDecision>
}

interface TranscriptionService {
    fun stream(audio: AudioStream): Flow<TranscriptChunk>
}

interface AnonymizerService {
    suspend fun scrub(text: String): Result<ScrubResult>
}

interface DocumentExtractor {
    suspend fun extract(image: Bitmap): Result<DocumentJson>
}
```

Every interface has **two implementations**: a real Melange-backed one and a deterministic fallback (`RuleBased*`, `Regex*`, `Stub*`). The orchestrator picks at runtime based on init success.

### 6.3 Triage request / response

```kotlin
data class TriageRequest(
    val transcript: String,
    val image: Bitmap?,        // 768px max edge
    val plan: InsurancePlan,   // bundled JSON
)

data class TriageDecision(
    val severity: Severity,    // emergency | urgent | telehealth | selfcare
    val reasoning: String,
    val redFlags: List<String>,
    val recommendedAction: RecommendedAction,
    val confidence: Float,
    val emergencyShortCircuit: Boolean,
)
```

### 6.4 Performance targets (hard)

- Sub-3 s text triage (button-tap → first rendered token).
- Sub-6 s image triage.
- All inference on `Dispatchers.IO`. UI via `StateFlow`.
- Stream tokens to UI for perceived latency.
- Image resize to 768 px max edge before MedGemma.
- System prompt cap 600 tokens; output cap 256 tokens.

### 6.5 Privacy invariants

- PHI token map encrypted via `MasterKey` + `EncryptedSharedPreferences`.
- Token map **never** crosses the network. Re-identification is local-only.
- Mock telehealth endpoint receives only de-identified text.
- Emergency regex short-circuit runs **before** any LLM call.

---

## 7. Navigation contract (Compose Navigation)

```
NavGraph:
  splash        -> permissions  (auto on warmup complete)
  permissions   -> intake       (always, regardless of grant/deny)
  intake        -> cameraOffer  (if !emergency && imagey-symptom-keywords)
                -> triaging     (otherwise; or emergency short-circuit)
  cameraOffer   -> cameraCapture | triaging
  cameraCapture -> triaging     (on accept) | cameraOffer (on cancel)
  triaging      -> result       (on decision)
  result        -> deid         (on "Send labs")
                -> splash       (on "Start over", with state reset)
  deid          -> result       (back) | splash (Done)
```

Back-stack rules:
- "Start over" pops the entire stack and re-enters at splash.
- The Result screen is the deep-link target if the OS resumes the app.

---

## 8. State management

- One `AppViewModel` with `StateFlow<AppState>`:
  ```kotlin
  data class AppState(
      val warmup: WarmupState,
      val intake: IntakeState,
      val image: Bitmap?,
      val decision: TriageDecision?,
      val deid: DeidState,
      val theme: ThemeKey,         // warm | calm | bold | accessible
      val largeType: Boolean,
  )
  ```
- Per-screen `LaunchedEffect` blocks for transitions (auto-advance from Splash, polling regex on Intake transcript, etc.).
- Theme persisted to `DataStore`. Nothing else persisted.

---

## 9. Demo scenarios (must work end-to-end on the demo phone)

| # | Input | Expected severity | Expected confidence | Notes |
|---|---|---|---|---|
| 1 | "I have a dark mole on my arm that's been growing" + photo of mole | urgent | ≥ 0.7 | Dermatology deep link |
| 2 | Voice: "My five-year-old's eye is red and goopy" | telehealth | ≥ 0.7 | Pediatric vendor; copay shown |
| 3 | Voice: "I've had crushing chest pain for 20 minutes radiating to my left arm" | emergency | — | Regex short-circuit; sub-1-second to render |
| 4 | Voice: "I've had a sore throat for three days and now my ear is hurting too." | selfcare | ≥ 0.6 | Self-care tips |

After scenario 1 or 2, demonstrate the de-id story: photograph a fake lab report → show placeholder-tokenized payload → response → re-identified result.

---

## 10. Out of scope (do not build)

- Real authentication / user accounts.
- Real telehealth backend (ship the mock Ktor endpoint).
- Multi-language. English only.
- iOS.
- Persistent symptom history.
- Push notifications.
- Background processing (everything is foreground / activity-bound).

---

## 11. Definition of done (per area)

| Area | DoD |
|---|---|
| ML runtime | Singleton loads MedGemma + Whisper + anonymizer at app start; warmup with dummy inference; streaming token API; cold-start logged. |
| Privacy | tanaos + regex fallback behind one interface; deterministic tokens; Keystore-backed map; re-id pass; mock Ktor endpoint that round-trips. |
| Triage | System prompt under 600 tokens; strict JSON schema parser; confidence-floor handling; regex short-circuit; 4 demo scenarios scripted and timed. |
| UI — Splash | Warmup with progress UI; advances when all three warm. |
| UI — Permissions | Mic + camera flow; denial path proceeds without crashing. |
| UI — Intake | Voice + text; transcript streams; emergency regex on every change; type overlay. |
| UI — Camera | CameraX preview + capture; blur/luminance precheck; retake flow; 768 px resize. |
| UI — Triaging | Streamed checklist; 8 s timeout to fallback. |
| UI — Result | Severity pill + headline + reasoning card + 1-tap action wired to intent; escalate path on urgent/telehealth. |
| UI — De-id | Photo → extract → scrub → POST → re-id → render; encrypted token map. |
| Navigation | Compose Navigation between all screens; correct back-stack. |
| Themes | 4 themes selectable in a Settings sheet; persisted via DataStore; A11y bumps body to 20 sp. |

---

## 12. Reference design

The interactive design prototype that this PRD describes is in `Pre-Triage Co-Pilot.html` at the project root (React + Babel). It shows every screen, animation, and copy decision. Treat it as the source of truth for visuals and copy; this PRD is the source of truth for behavior, contracts, and acceptance criteria.
