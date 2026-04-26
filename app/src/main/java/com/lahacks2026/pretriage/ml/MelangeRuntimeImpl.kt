package com.lahacks2026.pretriage.ml

import android.content.Context
import android.util.Log
import com.lahacks2026.pretriage.BuildConfig
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
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
private const val MAX_OUTPUT_TOKENS = 350
private const val PROMPT_PREFILL_TAIL = "REASONING: "

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
        _isReady.value = true
        Log.i(TAG, "warmUp complete")
    }

    override suspend fun triage(req: TriageRequest): Result<TriageDecision> = runCatching {
        ensureModel { }
        val prompt = buildPrompt(req)
        val started = System.currentTimeMillis()

        val raw = runMutex.withLock {
            val m = model ?: error("model null")
            withContext(Dispatchers.IO) {
                m.cleanUp()
                m.run(prompt)
                val sb = StringBuilder(PROMPT_PREFILL_TAIL)
                var count = 0
                while (count < MAX_OUTPUT_TOKENS) {
                    val r = m.waitForNextToken()
                    if (r.generatedTokens == 0) break
                    sb.append(r.token)
                    count++
                    if (count > 50 && sb.contains("RESULT:")) {
                        // Continue generating to ensure the final tag is captured
                    }
                }
                Log.i(TAG, "triage gen: tokens=$count durMs=${System.currentTimeMillis() - started}")
                sb.toString()
            }
        }

        Log.d(TAG, "raw output:\n$raw")
        parseRawResponse(raw)
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
        append("Instructions: Analyze user symptoms and photos to determine a care tier.\n")
        append("Tiers: SELF_CARE, TELEHEALTH, URGENT_CARE, EMERGENCY.\n\n")
        
        append("###\nUser: \"I have a cut.\"\nImage: (User attached photo)\nREASONING: Photo shows depth needing stitches.\nRESULT: URGENT_CARE\n\n")
        
        append("###\nUser: \"")
        val transcript = req.transcript.trim()
        if (transcript.isEmpty()) append("Hi.") 
        else append(transcript)
        append("\"\n")
        
        if (req.image != null) {
            append("Image: (User provided a photo for visual analysis)\n")
        }
        append(PROMPT_PREFILL_TAIL)
    }

    private fun parseRawResponse(raw: String): TriageDecision {
        val upper = raw.uppercase()
        
        // Find the RESULT which should now be AFTER the reasoning
        val severity = when {
            "RESULT: EMERGENCY" in upper -> SeverityLevel.EMERGENCY
            "RESULT: URGENT_CARE" in upper -> SeverityLevel.URGENT_CARE
            "RESULT: TELEHEALTH" in upper -> SeverityLevel.TELEHEALTH
            "RESULT: SELF_CARE" in upper -> SeverityLevel.SELF_CARE
            // Fallback for safety if it missed the tag but has the word
            "EMERGENCY" in upper -> SeverityLevel.EMERGENCY
            "URGENT_CARE" in upper -> SeverityLevel.URGENT_CARE
            "TELEHEALTH" in upper -> SeverityLevel.TELEHEALTH
            else -> SeverityLevel.SELF_CARE
        }

        val reasoning = raw.substringAfter("REASONING:", "")
            .substringBefore("RESULT:")
            .trim()
            .ifBlank { "Routing based on analysis." }

        return TriageDecision(
            severity = severity,
            reasoning = reasoning,
            redFlags = emptyList(),
            recommendedAction = RecommendedAction(
                provider = when(severity) {
                    SeverityLevel.SELF_CARE -> "Home care"
                    SeverityLevel.TELEHEALTH -> "Telehealth visit"
                    SeverityLevel.URGENT_CARE -> "Urgent Care"
                    SeverityLevel.EMERGENCY -> "911 / ER"
                },
                copay = null,
                intentHint = when(severity) {
                    SeverityLevel.SELF_CARE -> IntentHint.SHOW_SELF_CARE_TEXT
                    SeverityLevel.TELEHEALTH -> IntentHint.OPEN_TELEHEALTH_DEEP_LINK
                    SeverityLevel.URGENT_CARE -> IntentHint.MAPS_QUERY_URGENT_CARE
                    SeverityLevel.EMERGENCY -> IntentHint.DIAL_911
                }
            ),
            confidence = if (severity == SeverityLevel.EMERGENCY) 1.0 else 0.85
        )
    }
}
