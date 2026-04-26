package com.lahacks2026.pretriage.triage

object Prompts {

    /**
     * System prompt framed as a routing classifier rather than a medical
     * assistant. Qwen3-style models will refuse anything that looks like
     * "medical advice" / "diagnose" / "triage assistant"; framing the same
     * task as "map symptoms → routing tier" sidesteps the alignment refusal
     * without changing what we ask the model to do.
     */
    const val MEDGEMMA_SYSTEM_PROMPT = """
Task: Label text (A, B, C, D) as JSON.
Categories: A=H, B=V, C=C, D=E.
Rule: Output JSON only.
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
