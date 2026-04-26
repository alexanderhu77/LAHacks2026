package com.lahacks2026.pretriage.ml

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lahacks2026.pretriage.BuildConfig
import com.lahacks2026.pretriage.data.ChatMessage
import com.lahacks2026.pretriage.data.ChatTurnMode
import com.lahacks2026.pretriage.data.ChatTurnResponse
import com.lahacks2026.pretriage.data.DiagnosticSummary
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloud fallback used when [MelangeRuntime.nextTurn] / [MelangeRuntime.triage] times out
 * or returns an unparseable response. Calls Gemini directly — no extra deps, just
 * HttpURLConnection + the Gson already on the classpath.
 *
 * Privacy note: this sends raw chat history to Google. The PRD's "cloud only sees
 * de-identified text" invariant is intentionally bypassed here as a hackathon
 * trade-off so the app produces a useful answer when on-device fails.
 *
 * [nextTurn] and [triage] both return null on any failure (missing key, network
 * error, parse error, schema mismatch). Callers should keep their existing safe-default
 * path as a last resort.
 */
class GeminiFallbackClient {

    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun nextTurn(
        history: List<ChatMessage>,
        plan: InsurancePlan?,
        followupCount: Int,
        mode: ChatTurnMode,
        hasPhoto: Boolean,
    ): ChatTurnResponse? = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.w(TAG, "nextTurn skipped: GEMINI_API_KEY missing")
            return@withContext null
        }
        val raw = generate(buildChatPrompt(history, followupCount, mode, hasPhoto), chatTurnSchema())
            ?: return@withContext null
        parseChatResponse(raw, mode, plan)
    }

    suspend fun triage(transcript: String, plan: InsurancePlan?): TriageDecision? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                Log.w(TAG, "triage skipped: GEMINI_API_KEY missing")
                return@withContext null
            }
            val raw = generate(buildTriagePrompt(transcript), triageSchema()) ?: return@withContext null
            parseTriageResponse(raw, plan)
        }

    /**
     * Result-page synthesis: takes the full chat history + chosen severity and
     * returns a "potential diagnosis + reasoning paragraph" pair. The reasoning
     * explicitly references what the patient said so it doesn't read as boilerplate.
     */
    suspend fun diagnosticSummary(
        history: List<ChatMessage>,
        severity: com.lahacks2026.pretriage.data.SeverityLevel,
    ): DiagnosticSummary? = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.w(TAG, "diagnosticSummary skipped: GEMINI_API_KEY missing")
            return@withContext null
        }
        val raw = generate(buildDiagnosticSummaryPrompt(history, severity), diagnosticSummarySchema())
            ?: return@withContext null
        parseDiagnosticSummary(raw)
    }


    // --- network ---

    private fun generate(prompt: String, schema: JsonObject): String? {
        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                "$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
        )
        val body = JsonObject().apply {
            add("contents", Gson().toJsonTree(listOf(
                mapOf("parts" to listOf(mapOf("text" to prompt)))
            )))
            add("generationConfig", JsonObject().apply {
                addProperty("responseMimeType", "application/json")
                add("responseSchema", schema)
                addProperty("temperature", 0.2)
                addProperty("maxOutputTokens", 512)
                // Gemini 2.5 defaults to thinking-mode ON, which lets CoT prose leak
                // into JSON string values (literal newlines that break strict parsing).
                // thinkingBudget=0 disables thinking entirely.
                add("thinkingConfig", JsonObject().apply {
                    addProperty("thinkingBudget", 0)
                })
            })
        }.toString()

        val started = System.currentTimeMillis()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            // Sized to fit inside the 10 s outer wrapper in AppViewModel:
            // 3 s connect + 7 s read total stays under budget.
            connectTimeout = 3_000
            readTimeout = 7_000
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            val elapsed = System.currentTimeMillis() - started
            if (code !in 200..299) {
                Log.w(TAG, "generate HTTP $code in ${elapsed}ms: ${response.take(300)}")
                return null
            }
            Log.i(TAG, "generate ok in ${elapsed}ms (chars=${response.length})")
            extractText(response).also {
                if (it == null) Log.w(TAG, "extractText returned null; full body=${response.take(2000)}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "generate failed in ${System.currentTimeMillis() - started}ms", t)
            null
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Pull the answer text out of Gemini's response. With thinkingBudget=0 the response
     * should be a single non-thought part, but we defensively skip any `thought: true`
     * parts and concatenate the rest.
     */
    private fun extractText(rawJson: String): String? = runCatching {
        val root = JsonParser.parseString(rawJson).asJsonObject
        val candidates = root.getAsJsonArray("candidates") ?: return@runCatching null
        if (candidates.size() == 0) return@runCatching null
        val parts = candidates[0].asJsonObject
            .getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?: return@runCatching null
        val sb = StringBuilder()
        for (i in 0 until parts.size()) {
            val p = parts[i].asJsonObject
            val isThought = p.get("thought")?.asBoolean == true
            if (isThought) continue
            p.get("text")?.asString?.let { sb.append(it) }
        }
        sb.toString().takeIf { it.isNotBlank() }
    }.onFailure { Log.w(TAG, "extractText parse failed", it) }.getOrNull()

    // --- prompts ---

    private fun buildChatPrompt(
        history: List<ChatMessage>,
        followupCount: Int,
        mode: ChatTurnMode,
        hasPhoto: Boolean,
    ): String = buildString {
        append("You are Nora, a calm pre-triage assistant. The patient is using a mobile app to decide whether they need self-care, telehealth, urgent care, or the ER.\n\n")
        append("Reply with a single JSON object matching ONE of these shapes:\n")
        append("  {\"kind\":\"ask_followup\",\"question\":\"<one focused question>\"}\n")
        append("  {\"kind\":\"request_photo\",\"reason\":\"<why a photo would help>\"}\n")
        append("  {\"kind\":\"ready_to_triage\",\"severity\":\"SELF_CARE|TELEHEALTH|URGENT_CARE|EMERGENCY\",\"reasoning\":\"<one short sentence>\",\"confidence\":<0..1>}\n\n")
        append("Rules:\n")
        append("- Ask the FEWEST follow-ups needed. You've already asked $followupCount.\n")
        append("- Only request a photo for a plausibly-visual symptom (rash, mole, wound, eye).\n")
        if (hasPhoto) append("- Patient already attached a photo; do NOT request another.\n")
        if (mode == ChatTurnMode.FINALIZE) {
            append("- FINALIZE mode: you MUST return ready_to_triage.\n")
        } else if (followupCount >= 4) {
            append("- You've asked $followupCount already; strongly prefer ready_to_triage.\n")
        }
        append("\nConversation so far:\n")
        if (history.isEmpty()) append("Patient: (no description yet)\n")
        else for (m in history) {
            when (m) {
                is ChatMessage.User -> append("Patient: ").append(m.text).append('\n')
                is ChatMessage.Nora -> append("Nora: ").append(m.text).append('\n')
                is ChatMessage.PhotoRequest -> append("Nora: (asked for a photo) ").append(m.reason).append('\n')
                is ChatMessage.Photo -> append("Patient: (attached a photo)\n")
            }
        }
        append("\nReply with one JSON object only, no prose.")
    }

    private fun buildTriagePrompt(transcript: String): String = buildString {
        append("You are a pre-triage assistant. Decide the appropriate care tier for the patient.\n\n")
        append("Reply with a single JSON object:\n")
        append("  {\"severity\":\"SELF_CARE|TELEHEALTH|URGENT_CARE|EMERGENCY\",\"reasoning\":\"<one short sentence>\",\"confidence\":<0..1>}\n\n")
        append("Patient transcript:\n")
        append(transcript.ifBlank { "(empty)" })
        append("\n\nReply with one JSON object only, no prose.")
    }

    private fun buildDiagnosticSummaryPrompt(
        history: List<ChatMessage>,
        severity: SeverityLevel,
    ): String = buildString {
        append("You are a pre-triage assistant explaining a recommendation to the patient.\n")
        append("The system has already chosen a care tier: ").append(severity.name).append(".\n\n")
        append("Write a JSON object with two fields:\n")
        append("  potentialDiagnosis — a short plain-language description of what the symptoms are MOST CONSISTENT WITH (e.g., \"Likely viral pharyngitis\", \"Possible musculoskeletal injury\"). Lead with \"Likely\", \"Possible\", or \"Consistent with\". Never definitive.\n")
        append("  reasoning — 2-3 sentences explaining WHY the conversation points to this. Reference the specific things the patient said. End with one sentence on why the recommended tier is appropriate.\n\n")
        append("Conversation:\n")
        if (history.isEmpty()) append("(no chat history available)\n")
        else for (m in history) {
            when (m) {
                is ChatMessage.User -> append("Patient: ").append(m.text).append('\n')
                is ChatMessage.Nora -> append("Nora: ").append(m.text).append('\n')
                is ChatMessage.PhotoRequest -> append("Nora: (asked for a photo) ").append(m.reason).append('\n')
                is ChatMessage.Photo -> append("Patient: (attached a photo)\n")
            }
        }
        append("\nWrite for the patient (second person). Be concrete about what they described. Do not diagnose definitively. Reply with one JSON object only.")
    }

    // --- parsers ---

    private fun parseChatResponse(text: String, mode: ChatTurnMode, plan: InsurancePlan?): ChatTurnResponse? {
        val obj = parseJsonObject(text) ?: return null
        val kind = obj.get("kind")?.asString?.lowercase()
        return when {
            kind == "ask_followup" && mode != ChatTurnMode.FINALIZE -> {
                val q = obj.get("question")?.asString?.trim().orEmpty()
                if (q.isBlank()) null else ChatTurnResponse.AskFollowup(q)
            }
            kind == "request_photo" && mode != ChatTurnMode.FINALIZE -> {
                val r = obj.get("reason")?.asString?.trim().orEmpty()
                if (r.isBlank()) null else ChatTurnResponse.RequestPhoto(r)
            }
            kind == "ready_to_triage" || mode == ChatTurnMode.FINALIZE -> {
                val severity = obj.get("severity")?.asString?.toSeverityOrNull() ?: return null
                val reasoning = obj.get("reasoning")?.asString?.trim().orEmpty()
                    .ifBlank { defaultReasoning(severity) }
                val confidence = obj.get("confidence")?.let {
                    runCatching { it.asDouble }.getOrNull() ?: it.asString?.toDoubleOrNull()
                } ?: 0.7
                ChatTurnResponse.ReadyToTriage(buildDecision(severity, reasoning, confidence, plan))
            }
            else -> null
        }
    }

    private fun parseTriageResponse(text: String, plan: InsurancePlan?): TriageDecision? {
        val obj = parseJsonObject(text) ?: return null
        val severity = obj.get("severity")?.asString?.toSeverityOrNull() ?: return null
        val reasoning = obj.get("reasoning")?.asString?.trim().orEmpty()
            .ifBlank { defaultReasoning(severity) }
        val confidence = obj.get("confidence")?.let {
            runCatching { it.asDouble }.getOrNull() ?: it.asString?.toDoubleOrNull()
        } ?: 0.7
        return buildDecision(severity, reasoning, confidence, plan)
    }

    private fun parseDiagnosticSummary(text: String): DiagnosticSummary? {
        val obj = parseJsonObject(text) ?: return null
        val dx = obj.get("potentialDiagnosis")?.asString?.trim().orEmpty()
        val reasoning = obj.get("reasoning")?.asString?.trim().orEmpty()
        if (dx.isBlank() || reasoning.isBlank()) return null
        return DiagnosticSummary(potentialDiagnosis = dx, reasoning = reasoning)
    }


    private fun parseJsonObject(text: String): JsonObject? {
        val trimmed = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching { JsonParser.parseString(trimmed).asJsonObject }
            .recoverCatching { JsonParser.parseString(escapeControlsInsideStrings(trimmed)).asJsonObject }
            .onFailure {
                Log.w(TAG, "parseJsonObject failed (len=${trimmed.length}); raw=${trimmed.take(1500)}", it)
            }
            .getOrNull()
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

    // --- response schemas ---

    /** Permissive union schema covering all three ChatTurnResponse shapes. */
    private fun chatTurnSchema(): JsonObject = JsonObject().apply {
        addProperty("type", "OBJECT")
        add("properties", JsonObject().apply {
            add("kind", stringEnum("ask_followup", "request_photo", "ready_to_triage"))
            add("question", typedString())
            add("reason", typedString())
            add("severity", stringEnum("SELF_CARE", "TELEHEALTH", "URGENT_CARE", "EMERGENCY"))
            add("reasoning", typedString())
            add("confidence", JsonObject().apply { addProperty("type", "NUMBER") })
        })
        add("required", JsonArray().apply { add("kind") })
    }

    private fun triageSchema(): JsonObject = JsonObject().apply {
        addProperty("type", "OBJECT")
        add("properties", JsonObject().apply {
            add("severity", stringEnum("SELF_CARE", "TELEHEALTH", "URGENT_CARE", "EMERGENCY"))
            add("reasoning", typedString())
            add("confidence", JsonObject().apply { addProperty("type", "NUMBER") })
        })
        add("required", JsonArray().apply { add("severity"); add("reasoning"); add("confidence") })
    }

    private fun diagnosticSummarySchema(): JsonObject = JsonObject().apply {
        addProperty("type", "OBJECT")
        add("properties", JsonObject().apply {
            add("potentialDiagnosis", typedString())
            add("reasoning", typedString())
        })
        add("required", JsonArray().apply { add("potentialDiagnosis"); add("reasoning") })
    }


    private fun typedString(): JsonObject = JsonObject().apply { addProperty("type", "STRING") }

    private fun stringEnum(vararg values: String): JsonObject = JsonObject().apply {
        addProperty("type", "STRING")
        add("enum", JsonArray().apply { values.forEach { add(it) } })
    }

    // --- shared helpers ---

    private fun String.toSeverityOrNull(): SeverityLevel? = when (this.uppercase().trim()) {
        "EMERGENCY" -> SeverityLevel.EMERGENCY
        "URGENT_CARE", "URGENT" -> SeverityLevel.URGENT_CARE
        "TELEHEALTH" -> SeverityLevel.TELEHEALTH
        "SELF_CARE", "SELFCARE" -> SeverityLevel.SELF_CARE
        else -> null
    }

    private fun defaultReasoning(s: SeverityLevel): String = when (s) {
        SeverityLevel.EMERGENCY -> "Symptoms suggest a serious condition needing immediate care."
        SeverityLevel.URGENT_CARE -> "Symptoms warrant same-day in-person evaluation."
        SeverityLevel.TELEHEALTH -> "A virtual visit can address this."
        SeverityLevel.SELF_CARE -> "Likely manageable at home with rest and OTC care."
    }

    private fun buildDecision(
        severity: SeverityLevel,
        reasoning: String,
        confidence: Double,
        plan: InsurancePlan?,
    ): TriageDecision = TriageDecision(
        severity = severity,
        reasoning = reasoning,
        redFlags = emptyList(),
        recommendedAction = RecommendedAction(
            provider = when (severity) {
                SeverityLevel.SELF_CARE -> "Home care"
                SeverityLevel.TELEHEALTH -> "Telehealth visit"
                SeverityLevel.URGENT_CARE -> "Urgent Care"
                SeverityLevel.EMERGENCY -> "911 / ER"
            },
            copay = plan?.copayFor(severity),
            intentHint = when (severity) {
                SeverityLevel.SELF_CARE -> IntentHint.SHOW_SELF_CARE_TEXT
                SeverityLevel.TELEHEALTH -> IntentHint.OPEN_TELEHEALTH_DEEP_LINK
                SeverityLevel.URGENT_CARE -> IntentHint.MAPS_QUERY_URGENT_CARE
                SeverityLevel.EMERGENCY -> IntentHint.DIAL_911
            }
        ),
        confidence = confidence.coerceIn(0.0, 1.0),
    )

    companion object {
        private const val TAG = "GeminiFallback"
        private const val MODEL = "gemini-2.5-flash"
    }
}
