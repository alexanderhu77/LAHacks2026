package com.lahacks2026.pretriage.ml

import android.content.Context
import android.util.Log
import com.lahacks2026.pretriage.BuildConfig
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.triage.JsonDecisionParser
import com.lahacks2026.pretriage.triage.Prompts
import com.zeticai.mlange.core.model.llm.LLMQuantType
import com.zeticai.mlange.core.model.llm.LLMTarget
import com.zeticai.mlange.core.model.llm.ZeticMLangeLLMModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "MelangeRuntime"
private const val MODEL_ID = "Steve/Qwen3.5-2B"
private const val MAX_OUTPUT_TOKENS = 180

private const val PROMPT_PREFILL_TAIL = "{\"severity\":\""

class MelangeRuntimeImpl(
    private val context: Context
) : MelangeRuntime {

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    @Volatile
    private var model: ZeticMLangeLLMModel? = null

    private val initMutex = Mutex()
    private val runMutex = Mutex()

    override suspend fun warmUp(onProgress: (Float) -> Unit): Result<Unit> = runCatching {
        if (BuildConfig.MELANGE_TOKEN.isBlank()) {
            error("MELANGE_TOKEN missing in local.properties")
        }
        ensureModel(onProgress)
        // Dummy inference to surface NPU/CPU init cost during warmup, not first
        // triage. cleanUp() before AND after — Zetic's LLM runtime retains KV
        // cache / conversation state across .run() calls, so without these the
        // dummy "Hi." would leak into the next triage prompt and the model
        // would respond as if the user actually said hello.
        runMutex.withLock {
            val m = model ?: error("model null after init")
            withContext(Dispatchers.IO) {
                m.cleanUp()
                m.run("Hi.")
                var i = 0
                while (i < 4) {
                    val r = m.waitForNextToken()
                    if (r.generatedTokens == 0) break
                    i++
                }
                m.cleanUp()
            }
        }
        _isReady.value = true
        Log.i(TAG, "warmUp complete")
    }

    override suspend fun triage(req: TriageRequest): Result<TriageDecision> = runCatching {
        ensureModel { /* no progress during triage; warmup already paid for it */ }
        val prompt = buildPrompt(req)
        val started = System.currentTimeMillis()

        val raw = runMutex.withLock {
            val m = model ?: error("model null")
            withContext(Dispatchers.IO) {
                // Reset KV cache / conversation state from any prior run
                // (warmup dummy or previous triage). Without this Zetic's LLM
                // runtime treats every triage as a multi-turn continuation
                // and the model responds to whatever was in the buffer last.
                m.cleanUp()
                m.run(prompt)
                // Prompt ends with the JSON prefill `{"severity":"`. Model has
                // nowhere to write prose — its next token has to be one of the
                // four enum values. Seed the buffer with the same prefill so
                // depth counting + JSON parser see a complete object.
                val sb = StringBuilder(PROMPT_PREFILL_TAIL)
                var count = 0
                while (count < MAX_OUTPUT_TOKENS) {
                    val r = m.waitForNextToken()
                    if (r.generatedTokens == 0) break
                    sb.append(r.token)
                    count++
                    // Greedy early-exit once we observe a balanced top-level JSON close.
                    if (count > 12 && looksLikeCompletedJson(sb)) break
                }
                Log.i(TAG, "triage gen: tokens=$count durMs=${System.currentTimeMillis() - started}")
                sb.toString()
            }
        }

        Log.d(TAG, "raw output:\n$raw")
        JsonDecisionParser.parse(raw) ?: fallbackDecision(raw)
    }

    override suspend fun transcribe(audioPath: String): Result<String> =
        Result.failure(NotImplementedError("Whisper transcription is wired directly in IntakeScreen"))

    override suspend fun extractDocument(imageUri: String): Result<String> =
        Result.failure(NotImplementedError("Document extraction lands when MedGemma is unblocked"))

    private suspend fun ensureModel(onProgress: (Float) -> Unit) {
        if (model != null) return
        initMutex.withLock {
            if (model != null) return
            withContext(Dispatchers.IO) {
                Log.i(TAG, "constructing ZeticMLangeLLMModel($MODEL_ID, LLAMA_CPP, Q4_K_M)")
                val ctorStart = System.currentTimeMillis()
                model = ZeticMLangeLLMModel(
                    context.applicationContext,
                    BuildConfig.MELANGE_TOKEN,
                    MODEL_ID,
                    version = null,
                    target = LLMTarget.LLAMA_CPP,
                    quantType = LLMQuantType.GGUF_QUANT_Q4_K_M,
                    onProgress = { p ->
                        Log.d(TAG, "download progress ${(p * 100).toInt()}%")
                        onProgress(p)
                    },
                    onStatusChanged = { s -> Log.i(TAG, "status: $s") }
                )
                Log.i(TAG, "constructor returned in ${System.currentTimeMillis() - ctorStart}ms")
            }
        }
    }

    private fun buildPrompt(req: TriageRequest): String = buildString {
        // Plain text-completion prompt. We tried Qwen3 chat tags + <think>
        // skip on the previous push - they had no effect because Zetic's
        // runtime treats the prompt as raw text and does not apply a chat
        // template, so the special tokens were just noise to the model.
        //
        // Strategy now: few-shot pattern matching. Three example transcripts
        // paired with their JSON outputs teach the model the output shape.
        // The prompt then ends mid-JSON at `{"severity":"` so the model's
        // next token must be one of the four enum values - prose is
        // structurally impossible. Seeded into the output buffer in triage()
        // so the parser sees a complete object.
        append(Prompts.MEDGEMMA_SYSTEM_PROMPT.trim())
        append("\n\nExamples (output ONLY the JSON object, nothing else):\n\n")

        append("Symptoms: \"My finger is cut and won't stop bleeding.\"\n")
        append("JSON: {\"severity\":\"URGENT_CARE\",\"reasoning\":\"Bleeding that won't stop needs same-day in-person evaluation.\",\"red_flags\":[\"persistent bleeding\"],\"recommended_action\":{\"provider\":\"Urgent care clinic\",\"intent_hint\":\"MAPS_QUERY_URGENT_CARE\"},\"confidence\":0.85}\n\n")

        append("Symptoms: \"Mild sore throat for two days.\"\n")
        append("JSON: {\"severity\":\"SELF_CARE\",\"reasoning\":\"Mild sore throats typically resolve at home with rest and fluids.\",\"red_flags\":[],\"recommended_action\":{\"provider\":\"Self-care guidance\",\"intent_hint\":\"SHOW_SELF_CARE_TEXT\"},\"confidence\":0.80}\n\n")

        append("Symptoms: \"My five-year-old's eye is red and goopy.\"\n")
        append("JSON: {\"severity\":\"TELEHEALTH\",\"reasoning\":\"Pediatric pink eye can be diagnosed and treated by video visit.\",\"red_flags\":[],\"recommended_action\":{\"provider\":\"Pediatric telehealth\",\"intent_hint\":\"OPEN_TELEHEALTH_DEEP_LINK\"},\"confidence\":0.80}\n\n")

        append("Symptoms: \"")
        append(req.transcript.ifBlank { "(none provided)" })
        append("\"\nJSON: ")
        append(PROMPT_PREFILL_TAIL)
    }

    private fun looksLikeCompletedJson(sb: StringBuilder): Boolean {
        val s = sb.toString()
        val firstBrace = s.indexOf('{')
        if (firstBrace < 0) return false
        var depth = 0
        var inString = false
        var escaped = false
        for (i in firstBrace until s.length) {
            val c = s[i]
            if (escaped) { escaped = false; continue }
            if (c == '\\' && inString) { escaped = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return true
                }
            }
        }
        return false
    }

    /**
     * Last-resort decision when the model output cannot be parsed.
     * We refuse to silently invent a severity — return TELEHEALTH with a low
     * confidence note so the orchestrator's confidence floor downgrades it
     * conservatively, and surface the raw output for debugging via reasoning.
     */
    private fun fallbackDecision(raw: String): TriageDecision {
        val snippet = raw.take(160).replace("\n", " ")
        Log.w(TAG, "JSON parse failed; falling back. snippet=$snippet")
        return TriageDecision(
            severity = SeverityLevel.TELEHEALTH,
            reasoning = "I couldn't parse a clean recommendation. Out of caution, please book a telehealth visit so a clinician can review.",
            redFlags = emptyList(),
            recommendedAction = RecommendedAction(
                provider = "Primary-care telehealth",
                copay = null,
                intentHint = IntentHint.OPEN_TELEHEALTH_DEEP_LINK
            ),
            confidence = 0.4
        )
    }
}
