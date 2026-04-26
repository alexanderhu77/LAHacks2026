package com.lahacks2026.pretriage.triage

import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.RedFlag
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import org.json.JSONException
import org.json.JSONObject

object JsonDecisionParser {

    fun parse(rawLlmOutput: String): TriageDecision? {
        val jsonText = extractFirstJsonObject(rawLlmOutput) ?: return null
        return runCatching {
            val obj = JSONObject(jsonText)

            val severity = SeverityLevel.valueOf(obj.getString("severity"))
            val reasoning = obj.getString("reasoning")

            val redFlagsArr = obj.optJSONArray("red_flags")
            val redFlags = buildList {
                if (redFlagsArr != null) {
                    for (i in 0 until redFlagsArr.length()) {
                        val label = redFlagsArr.optString(i).orEmpty()
                        if (label.isNotBlank()) add(RedFlag(label = label, matchedPhrase = label))
                    }
                }
            }

            val actionObj = obj.getJSONObject("recommended_action")
            val intentHint = IntentHint.valueOf(actionObj.getString("intent_hint"))
            val provider = actionObj.getString("provider")

            val confidence = obj.optDouble("confidence", 0.5).coerceIn(0.0, 1.0)

            TriageDecision(
                severity = severity,
                reasoning = reasoning,
                redFlags = redFlags,
                recommendedAction = RecommendedAction(
                    provider = provider,
                    copay = null,
                    intentHint = intentHint
                ),
                confidence = confidence
            )
        }.getOrNull()
    }

    private fun extractFirstJsonObject(s: String): String? {
        val start = s.indexOf('{')
        if (start < 0) return null
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
                '}' -> {
                    depth--
                    if (depth == 0) return s.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
