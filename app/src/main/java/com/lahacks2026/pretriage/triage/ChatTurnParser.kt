package com.lahacks2026.pretriage.triage

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lahacks2026.pretriage.data.ChatTurnResponse
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision

/**
 * Parses the raw token stream from [com.lahacks2026.pretriage.ml.MelangeRuntime.nextTurn]
 * into one of the three [ChatTurnResponse] kinds. Robust to:
 *  - leading prose / `Next:` markers / code fences before the JSON
 *  - trailing junk tokens after the closing brace
 *  - literal newlines inside string values (escapes them and retries)
 */
object ChatTurnParser {
    private const val TAG = "ChatTurnParser"

    fun parse(raw: String, plan: InsurancePlan?): ChatTurnResponse? {
        val obj = extractJsonObject(raw) ?: return null
        val kind = obj.get("kind")?.asString?.lowercase()?.trim()
        return when (kind) {
            "ask_followup" -> {
                val q = obj.get("question")?.asString?.trim().orEmpty()
                if (q.isBlank()) null else ChatTurnResponse.AskFollowup(q)
            }
            "request_photo" -> {
                val r = obj.get("reason")?.asString?.trim().orEmpty()
                if (r.isBlank()) null else ChatTurnResponse.RequestPhoto(r)
            }
            "ready_to_triage" -> parseDecision(obj, plan)?.let { ChatTurnResponse.ReadyToTriage(it) }
            else -> null
        }
    }

    private fun parseDecision(obj: JsonObject, plan: InsurancePlan?): TriageDecision? {
        val severity = obj.get("severity")?.asString?.toSeverityOrNull() ?: return null
        val reasoning = obj.get("reasoning")?.asString?.trim().orEmpty()
            .ifBlank { defaultReasoning(severity) }
        val confidence = obj.get("confidence")?.let {
            runCatching { it.asDouble }.getOrNull() ?: it.asString?.toDoubleOrNull()
        } ?: 0.7
        val redFlags = emptyList<com.lahacks2026.pretriage.data.RedFlag>()

        // Recommended action: try to honor model-supplied provider/intent_hint, else default.
        val action = obj.getAsJsonObject("recommended_action")
        val intentHint = action?.get("intent_hint")?.asString?.toIntentOrNull()
            ?: defaultIntent(severity)
        val provider = action?.get("provider")?.asString?.trim().orEmpty()
            .ifBlank { defaultProvider(severity) }
        val copay = plan?.copayFor(severity)

        return TriageDecision(
            severity = severity,
            reasoning = reasoning,
            redFlags = redFlags,
            recommendedAction = RecommendedAction(provider, copay, intentHint),
            confidence = confidence.coerceIn(0.0, 1.0),
        )
    }

    /**
     * Locate the first balanced top-level JSON object in [raw] and parse it.
     * If strict parse fails (typically from literal newlines in string values),
     * sanitize controls inside strings and retry.
     */
    private fun extractJsonObject(raw: String): JsonObject? {
        val start = raw.indexOf('{').takeIf { it >= 0 } ?: return null
        val end = findBalancedEnd(raw, start)?.takeIf { it > start } ?: return null
        val slice = raw.substring(start, end + 1)
        return runCatching { JsonParser.parseString(slice).asJsonObject }
            .recoverCatching { JsonParser.parseString(escapeControlsInsideStrings(slice)).asJsonObject }
            .onFailure { Log.w(TAG, "extractJsonObject failed; slice=${slice.take(400)}", it) }
            .getOrNull()
    }

    private fun findBalancedEnd(s: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until s.length) {
            val c = s[i]
            if (escaped) { escaped = false; continue }
            if (c == '\\' && inString) { escaped = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
        }
        return null
    }

    private fun escapeControlsInsideStrings(s: String): String {
        val sb = StringBuilder(s.length + 16)
        var inString = false
        var escaped = false
        for (c in s) {
            if (inString) {
                if (escaped) { sb.append(c); escaped = false }
                else when (c) {
                    '\\' -> { sb.append(c); escaped = true }
                    '"' -> { sb.append(c); inString = false }
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> sb.append(c)
                }
            } else {
                sb.append(c)
                if (c == '"') inString = true
            }
        }
        return sb.toString()
    }

    private fun String.toSeverityOrNull(): SeverityLevel? = when (this.uppercase().trim()) {
        "EMERGENCY" -> SeverityLevel.EMERGENCY
        "URGENT_CARE", "URGENT" -> SeverityLevel.URGENT_CARE
        "TELEHEALTH" -> SeverityLevel.TELEHEALTH
        "SELF_CARE", "SELFCARE" -> SeverityLevel.SELF_CARE
        else -> null
    }

    private fun String.toIntentOrNull(): IntentHint? = when (this.uppercase().trim()) {
        "DIAL_911" -> IntentHint.DIAL_911
        "OPEN_TELEHEALTH_DEEP_LINK" -> IntentHint.OPEN_TELEHEALTH_DEEP_LINK
        "MAPS_QUERY_URGENT_CARE" -> IntentHint.MAPS_QUERY_URGENT_CARE
        "SHOW_SELF_CARE_TEXT" -> IntentHint.SHOW_SELF_CARE_TEXT
        else -> null
    }

    private fun defaultProvider(s: SeverityLevel): String = when (s) {
        SeverityLevel.EMERGENCY -> "911 / ER"
        SeverityLevel.URGENT_CARE -> "Urgent care"
        SeverityLevel.TELEHEALTH -> "Telehealth visit"
        SeverityLevel.SELF_CARE -> "Home care"
    }

    private fun defaultIntent(s: SeverityLevel): IntentHint = when (s) {
        SeverityLevel.EMERGENCY -> IntentHint.DIAL_911
        SeverityLevel.URGENT_CARE -> IntentHint.MAPS_QUERY_URGENT_CARE
        SeverityLevel.TELEHEALTH -> IntentHint.OPEN_TELEHEALTH_DEEP_LINK
        SeverityLevel.SELF_CARE -> IntentHint.SHOW_SELF_CARE_TEXT
    }

    private fun defaultReasoning(s: SeverityLevel): String = when (s) {
        SeverityLevel.EMERGENCY -> "Symptoms suggest a serious condition needing immediate care."
        SeverityLevel.URGENT_CARE -> "Symptoms warrant same-day in-person evaluation."
        SeverityLevel.TELEHEALTH -> "A virtual visit can address this."
        SeverityLevel.SELF_CARE -> "Likely manageable at home with rest and OTC care."
    }
}
