# Models we use

All three run on-device via the Melange SDK. No patient data ever leaves the phone.

| Model | Melange ID (TBD — verify in catalog) | Role in app |
|---|---|---|
| Whisper Tiny | `openai/whisper-tiny` *(verify against catalog at mlange.zetic.ai)* | Voice → text on the Intake screen. Transcript held in memory only. |
| MedGemma 1.5 4B | `google/medgemma-4b-it` *(verify)* | The triage brain. One multimodal call: symptom transcript + optional image + insurance plan JSON → strict JSON routing decision. Also used for document understanding (extract structured JSON from photos of lab reports, prescriptions, prior visit summaries) before de-identification. |
| tanaos-text-anonymizer-v1 | `tanaos/text-anonymizer-v1` *(verify)* | PHI scrubber. Replaces names, DOBs, MRNs, addresses, phone numbers, provider names with deterministic placeholder tokens (`[PATIENT_NAME_1]`, `[DOB_1]`, …) before any document text is sent to the mock telehealth endpoint. The mapping lives in the Android Keystore and never leaves the device. |

## Smoke-test model (first push only)

`Qwen/Qwen3-4B` — same SDK API as MedGemma (`ZeticMLangeLLMModel`), smaller, faster cold start. We use it to prove that the Melange plumbing works end-to-end before swapping to MedGemma in the real triage path.

## TODO before hour 6

- Open each `prepare/` directory in <https://github.com/zetic-ai/ZETIC_Melange_apps> for `whisper-tiny` and `TextAnonymizer` and copy the exact model ID strings into the table above.
- Confirm MedGemma 1.5 4B is published in the Melange catalog under our token's access tier. If not: ask the ZETIC team in the hackathon Slack, or fall back to MedGemma 4B vanilla.
