# Pre-Triage Co-Pilot

A native Android app that decides whether you need a doctor — self-care, telehealth, urgent care, or emergency — in under a minute, on-device, using Melange-hosted models (MedGemma 1.5 4B + Whisper Tiny + tanaos anonymizer).

## Setup (new teammate, ~15 minutes)

1. **Clone the repo** and open the folder in **Android Studio Hedgehog (2023.1.1) or newer**.
2. **Get a Melange token**: sign up at <https://mlange.zetic.ai>, go to *Settings → Personal Access Token*, generate one.
3. **Create `local.properties`** in the repo root (it is gitignored). Copy from `local.properties.example` and set:
   ```properties
   MELANGE_TOKEN=mlange_xxxxxxxxxxxxxxxx
   ```
   Android Studio will append `sdk.dir=...` automatically on first sync.
4. **Plug in an Android phone** (Android 12 / API 31+) with USB debugging enabled. The Melange runtime needs a real NPU/GPU — emulators won't work for the LLM.
5. **Run the app** (Shift+F10). You'll see a single screen with a *Run Melange Test* button. Tap it. The first run downloads the smoke-test model (`Qwen/Qwen3-4B`) and may take a few minutes; subsequent runs reuse the cached model. You should see a one-sentence answer to "What is the capital of France?".

If the build fails on Gradle sync, let it download Gradle 8.5 and the Android SDK packages, then retry. If the model download fails with an auth error, double-check `MELANGE_TOKEN`.

## Team split

| Person | Owns | Package |
|---|---|---|
| A | Melange wrappers, model lifecycle, warmup, streaming | `ml/` |
| B | Compose screens + ViewModels (intake, camera, result) | `ui/intake/`, `ui/camera/`, `ui/result/` |
| C | tanaos integration, Keystore token map, regex anonymizer fallback | `privacy/` |
| D | System prompts, JSON schema, regex emergency short-circuit, demo polish | `triage/` |

See **[HACKATHON_PLAN.md](./HACKATHON_PLAN.md)** for architecture, performance targets, screen flow, definition-of-done per area, and the demo script.

## Links

- ZETIC Melange docs: <https://docs.zetic.ai>
- ZETIC reference apps (Qwen3Chat, whisper-tiny, TextAnonymizer): <https://github.com/zetic-ai/ZETIC_Melange_apps>
- Model catalog: <https://mlange.zetic.ai>
- Model IDs we'll use: see [`docs/models.md`](./docs/models.md)
