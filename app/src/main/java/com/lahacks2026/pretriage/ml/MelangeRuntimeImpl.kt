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
private const val MAX_OUTPUT_TOKENS = 384

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
        // Dummy inference to surface NPU/CPU init cost during warmup, not first triage.
        runMutex.withLock {
            val m = model ?: error("model null after init")
            withContext(Dispatchers.IO) {
                m.run("Hi.")
                var i = 0
                while (i < 4) {
                    val r = m.waitForNextToken()
                    if (r.generatedTokens == 0) break
                    i++
                }
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
                m.run(prompt)
                val sb = StringBuilder()
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
        append(Prompts.MEDGEMMA_SYSTEM_PROMPT.trim())
        append("\n\nPatient transcript:\n")
        append(req.transcript.ifBlank { "(no transcript)" })
        append("\n\nReturn the JSON object only:\n")
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
