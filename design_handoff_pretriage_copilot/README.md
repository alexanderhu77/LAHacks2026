# Handoff: Pre-Triage Co-Pilot

## Overview

A native Android app that helps a patient decide, in under a minute, whether they need self-care, telehealth, urgent care, or emergency care — using on-device multimodal ML (MedGemma 1.5 4B + Whisper Tiny + tanaos anonymizer via Melange).

The user describes their symptoms by voice (or text), optionally adds a photo (rash / wound / mole), and gets a color-coded severity recommendation with a one-tap next step. Every patient datum stays on the device; the cloud only ever sees de-identified placeholder text.

This handoff bundles a high-fidelity interactive design prototype + a full E2E PRD ready for Claude Code implementation.

## About the design files

The HTML/JSX files in this bundle are **design references**, not production code. They were built in React + Babel inline JSX so the user could click through the full flow in a browser, swap themes live, and tap any screen for review.

**Your task is to implement these designs in the existing Android (Jetpack Compose) codebase at `LAHacks2026/`** — using Compose idioms, Material 3 where it makes sense, the team's `ml/` + `privacy/` + `triage/` + `data/` package conventions, and the on-device service-interface pattern described in `PRD.md` § 6. Do **not** port the React components directly; treat them as visual + behavioral specs.

## Fidelity

**High-fidelity.** The prototype specifies final colors, typography, spacing, animation timing, copy, and interaction details. Recreate UI pixel-faithfully using Compose. The 4 color themes (Warm / Calm / Bold / Accessible) and large-type accessibility mode are part of the spec, not optional polish.

## What's in this bundle

| File | What it is |
|---|---|
| `PRD.md` | **The primary spec.** End-to-end product requirements: flow, screen-by-screen specs, state contracts, service interfaces, nav contract, demo scenarios, DoD checklist. Read this first. |
| `Pre-Triage Co-Pilot.html` | Open in a browser. The interactive prototype — splash, permissions, intake, camera, triage, result (4 severities), de-id upload. Use the left rail to jump screens or scenarios; use the Tweaks panel to switch themes. |
| `screens.jsx` | All screen components — exact layout, copy, animations, state machines. The visual source of truth. |
| `theme.jsx` | All 4 color themes as token tables (bg / surface / ink / accent / status colors / fonts / body size). Lift these directly into `Color.kt` / theme objects. |
| `icons.jsx` | Line-icon set (mic, camera, lock, alert, etc.). Replace with Compose Material Icons or your own icon set; the strokes/sizes here are reference. |
| `app.jsx` | Root: scenario routing, navigation state, theme/scenario tweaks wiring. Mirrors the nav contract in PRD § 7. |
| `android-frame.jsx`, `tweaks-panel.jsx` | Prototype chrome. Ignore for implementation. |

## Implementation pointers

### Existing repo
The Android project is already scaffolded at `LAHacks2026/` with the package split documented in `HACKATHON_PLAN.md`:
- `ml/` — Melange runtime singleton + model warmup
- `privacy/` — tanaos anonymizer + regex fallback + Keystore-backed token map
- `triage/` — MedGemma prompt + JSON schema parser + emergency regex short-circuit
- `data/` — bundled insurance plan JSON + demo scenario JSON
- `ui/intake/`, `ui/camera/`, `ui/result/`, splash, de-id doc upload

### Service-interface contract
Every ML service has a real (Melange) implementation **and** a deterministic fallback (`RuleBased*`, `Regex*`, `Stub*`) behind the same interface. The orchestrator picks at runtime based on init success. See `PRD.md` § 6.2.

### Performance targets (hard)
- Sub-3 s text triage, sub-6 s image triage.
- Load all 3 models exactly once at app start.
- Warmup with dummy inference on splash.
- Inference on `Dispatchers.IO`, UI via `StateFlow`.
- Stream tokens to UI.
- Resize images to 768 px max edge before MedGemma.

### Privacy invariants (non-negotiable)
- Token map encrypted via `MasterKey` + `EncryptedSharedPreferences`.
- Token map **never** crosses the network.
- Mock telehealth endpoint receives only de-identified text.
- Emergency regex short-circuit fires **before** any LLM call.

## Demo scenarios that must work end-to-end

| Input | Expected severity |
|---|---|
| "I have a dark mole on my arm that's been growing" + photo | urgent (≥0.7 confidence) |
| Voice: "My five-year-old's eye is red and goopy" | telehealth (≥0.7) |
| Voice: "I've had crushing chest pain for 20 minutes…" | emergency (regex short-circuit, sub-1s) |
| Voice: "I've had a sore throat for three days…" | self-care (≥0.6) |

After scenario 1 or 2, demo the de-id story end-to-end: photograph fake lab → tokenize → POST → response → re-id locally.

## Out of scope

No auth, no real telehealth backend (mock Ktor only), English only, Android only, no persistent history, no push, no background processing.

## Suggested order to read

1. `PRD.md` — read end-to-end.
2. Open `Pre-Triage Co-Pilot.html` in a browser; click through every scenario and toggle every theme.
3. Skim `screens.jsx` for any specific layout / animation detail not pinned in the PRD.
4. Lift tokens from `theme.jsx` into your Compose theme objects.
5. Implement screen-by-screen against the DoD matrix in `PRD.md` § 11.
