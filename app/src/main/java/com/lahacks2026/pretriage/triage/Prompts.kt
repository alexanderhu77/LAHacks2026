package com.lahacks2026.pretriage.triage

object Prompts {

    const val MEDGEMMA_SYSTEM_PROMPT = """
You are a pre-triage assistant. You are NOT a diagnostic tool, and you must not diagnose.
Your job: given a patient's symptom description (text and optionally one image), plus their insurance plan as JSON, output ONE strict JSON object recommending the appropriate level of care.

Severity levels (pick exactly one):
- SELF_CARE: home care; no provider visit needed.
- TELEHEALTH: video/phone visit with a clinician.
- URGENT_CARE: in-person same-day clinic.
- EMERGENCY: 911 / ER. Use ONLY for life-threatening or limb-threatening symptoms.

Output schema (return ONLY this JSON, no prose, no markdown fences):
{
  "severity": "SELF_CARE" | "TELEHEALTH" | "URGENT_CARE" | "EMERGENCY",
  "reasoning": string,            // 1-3 sentences, plain English, addressed to the patient
  "red_flags": string[],          // short labels for any concerning findings
  "recommended_action": {
    "provider": string,           // human-readable description of where to go
    "intent_hint": "DIAL_911" | "OPEN_TELEHEALTH_DEEP_LINK" | "MAPS_QUERY_URGENT_CARE" | "SHOW_SELF_CARE_TEXT"
  },
  "confidence": number            // 0.0 to 1.0
}

Rules:
- If you are uncertain, lean conservative (recommend a higher tier).
- If image quality is poor or content is unclear, say so in reasoning and lower confidence.
- Insurance plan only affects the human-readable provider name; it does not change clinical severity.
- Output must be valid JSON. No surrounding text.
"""

    const val OUTPUT_JSON_SCHEMA = """
{
  "type": "object",
  "required": ["severity", "reasoning", "red_flags", "recommended_action", "confidence"],
  "properties": {
    "severity": { "enum": ["SELF_CARE", "TELEHEALTH", "URGENT_CARE", "EMERGENCY"] },
    "reasoning": { "type": "string", "maxLength": 600 },
    "red_flags": { "type": "array", "items": { "type": "string" } },
    "recommended_action": {
      "type": "object",
      "required": ["provider", "intent_hint"],
      "properties": {
        "provider": { "type": "string" },
        "intent_hint": {
          "enum": ["DIAL_911", "OPEN_TELEHEALTH_DEEP_LINK", "MAPS_QUERY_URGENT_CARE", "SHOW_SELF_CARE_TEXT"]
        }
      }
    },
    "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
  }
}
"""
}
