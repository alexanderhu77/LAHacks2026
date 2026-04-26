package com.lahacks2026.pretriage.ml

import android.content.Context
import android.util.Log
import com.lahacks2026.pretriage.BuildConfig
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.ml.clip.ClipImageEncoder
import com.lahacks2026.pretriage.ml.clip.ClipLabelClassifier
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
// Hard cap on generated tokens. With RESULT-first flip, a complete answer is
// usually ~60 tokens (tier name + one sentence of reasoning). 160 leaves
// headroom for slightly verbose models without burning budget on rambling.
private const val MAX_OUTPUT_TOKENS = 160
// Prefill the assistant turn with the start of the RESULT line, so the
// model's very first generated token must be a tier name. Reasoning comes
// after, capped naturally by token budget. Old format had REASONING first,
// which let the model use the entire token budget on prose and never reach
// the verdict — observed on-device cut-off mid-sentence.
private const val PROMPT_PREFILL_TAIL = "RESULT: "

class MelangeRuntimeImpl(
    private val context: Context
) : MelangeRuntime {

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    @Volatile
    private var model: ZeticMLangeLLMModel? = null

    // Cached CLIP encoder. Construction calls ZeticMLangeModel(...) which hits
    // Zetic's catalog/auth endpoints — doing that per-triage burns quota before
    // the LLM even runs. Build once, reuse across calls.
    @Volatile
    private var clipEncoder: ClipImageEncoder? = null
    private val clipMutex = Mutex()

    // Cached label classifier — loads ~12 categories × ~3 phrasings worth of
    // 512-dim CLIP text embeddings from assets/clip_labels.json. Pure Kotlin,
    // no network or quota hit.
    @Volatile
    private var labelClassifier: ClipLabelClassifier? = null

    // Warmup runs at most once per process. We cache the Result of the first
    // attempt — success or failure — and every subsequent call returns it
    // without touching the Zetic SDK again. This prevents recomposition,
    // navigation, or post-failure UI retries from burning a quota slot per
    // re-entry. Explicit user retry would need to instantiate a fresh runtime.
    @Volatile
    private var warmupResult: Result<Unit>? = null
    private val warmupMutex = Mutex()

    private val initMutex = Mutex()
    private val runMutex = Mutex()

    override suspend fun warmUp(onProgress: (Float) -> Unit): Result<Unit> {
        warmupResult?.let { return it }
        return warmupMutex.withLock {
            warmupResult ?: runCatching {
                if (BuildConfig.MELANGE_TOKEN.isBlank()) {
                    error("MELANGE_TOKEN missing in local.properties")
                }
                ensureModel(onProgress)
                _isReady.value = true
                Log.i(TAG, "warmUp complete")
            }.also { warmupResult = it }
        }
    }

    override suspend fun triage(req: TriageRequest): Result<TriageDecision> = runCatching {
        // Fast-fail if warmup has already failed — don't retry constructor and
        // burn another quota slot. UI gets a clear error to surface.
        warmupResult?.exceptionOrNull()?.let {
            error("triage skipped — warmup failed: ${it.message}")
        }
        ensureModel { }

        // CLIP zero-shot classification: encode the image, score it against
        // precomputed label embeddings (assets/clip_labels.json), and pick the
        // top medical-finding category. The category name is injected into
        // the LLM prompt as a grounded "Visual finding: …" line so Qwen
        // reasons about what's actually in the image rather than hallucinating
        // about whether one exists. The "junk" category catches blurry /
        // off-topic photos and suppresses the visual-finding line entirely.
        // Wrapped in runCatching so a CLIP load failure can't break text
        // triage.
        var visualFinding: String? = null
        req.image?.let { bitmap ->
            runCatching {
                withContext(Dispatchers.IO) {
                    val encoder = ensureClipEncoder()
                    val classifier = ensureLabelClassifier()
                    val emb = encoder.encode(bitmap)
                    val ranked = classifier.classify(emb)
                    val top = ranked.first()
                    Log.i(
                        TAG,
                        "CLIP top-3: " + ranked.take(3).joinToString {
                            "${it.category}=${"%.2f".format(it.score)}"
                        }
                    )
                    visualFinding = when {
                        top.category == ClipLabelClassifier.JUNK_CATEGORY ->
                            "image was unclear or off-topic; visual analysis skipped"
                        top.score < ClipLabelClassifier.MIN_CONFIDENCE ->
                            "image too unclear for confident visual analysis (top guess: ${top.category}, ${"%.2f".format(top.score)})"
                        else ->
                            "${top.category} (visual confidence ${"%.2f".format(top.score)})"
                    }
                }
            }.onFailure { Log.w(TAG, "CLIP classify failed; ignoring", it) }
        }

        val prompt = buildPrompt(req, visualFinding)
        val started = System.currentTimeMillis()

        val raw = runMutex.withLock {
            val m = model ?: error("model null")
            withContext(Dispatchers.IO) {
                m.cleanUp()
                m.run(prompt)
                val sb = StringBuilder(PROMPT_PREFILL_TAIL)
                var count = 0
                // Once REASONING starts, we already have the tier (the
                // load-bearing field). Allow up to reasoningBudget more
                // tokens for the explanation, then stop cleanly at the
                // next sentence boundary or hard cap. Prevents mid-word
                // cut-off without burning the full 160-token budget on
                // verbose models.
                var reasoningStartedAt = -1
                val reasoningBudget = 80
                while (count < MAX_OUTPUT_TOKENS) {
                    val r = m.waitForNextToken()
                    if (r.generatedTokens == 0) break
                    sb.append(r.token)
                    count++

                    // Detect a new few-shot block boundary - model spilled
                    // into pretending to start a new example. Stop.
                    if (sb.endsWith("\n###") || sb.endsWith("\n\n###")) break

                    if (reasoningStartedAt < 0 && sb.contains("REASONING:")) {
                        reasoningStartedAt = count
                    }
                    if (reasoningStartedAt > 0) {
                        val sinceReasoning = count - reasoningStartedAt
                        if (sinceReasoning >= reasoningBudget) break
                        // Soft stop: end on a sentence terminator after at
                        // least 20 reasoning tokens, so we don't truncate
                        // mid-sentence.
                        if (sinceReasoning >= 20 && (sb.endsWith(".\n") || sb.endsWith(". ") || sb.endsWith("."))) break
                    }
                }
                Log.i(TAG, "triage gen: tokens=$count durMs=${System.currentTimeMillis() - started}")
                sb.toString()
            }
        }

        Log.d(TAG, "raw output:\n$raw")
        parseRawResponse(raw, visualFinding)
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

    private fun buildPrompt(req: TriageRequest, visualFinding: String?): String = buildString {
        // /no_think disables Qwen3.5's default chain-of-thought emission.
        // Without it the model spends the entire token budget on a "Thinking
        // Process: ..." block and never reaches RESULT/REASONING.
        append("/no_think\n")
        append("Instructions: Analyze user symptoms to determine a care tier.\n")
        append("Tiers: SELF_CARE, TELEHEALTH, URGENT_CARE, EMERGENCY.\n")
        append("If a 'Visual finding' line is present, treat it as a fact about the user's photo and reason about it.\n")
        append("Do not show your reasoning steps. Write RESULT first (one tier on its own line), then a single short REASONING sentence.\n\n")

        append("###\nUser: \"I cut my hand.\"\nVisual finding: deep cut needing stitches (visual confidence 0.31)\nRESULT: URGENT_CARE\nREASONING: A deep cut needs stitches today.\n\n")

        append("###\nUser: \"")
        val transcript = req.transcript.trim()
        if (transcript.isEmpty()) append("Hi.")
        else append(transcript)
        append("\"\n")

        if (visualFinding != null) {
            append("Visual finding: ")
            append(visualFinding)
            append("\n")
        }
        append(PROMPT_PREFILL_TAIL)
    }

    private suspend fun ensureClipEncoder(): ClipImageEncoder {
        clipEncoder?.let { return it }
        return clipMutex.withLock {
            clipEncoder ?: ClipImageEncoder(context).also { clipEncoder = it }
        }
    }

    private fun ensureLabelClassifier(): ClipLabelClassifier =
        labelClassifier ?: synchronized(this) {
            labelClassifier ?: ClipLabelClassifier(context).also { labelClassifier = it }
        }

    private fun parseRawResponse(raw: String, visualFinding: String?): TriageDecision {
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

        val explicit = raw.substringAfter("REASONING:", "")
            .substringBefore("RESULT:")
            .trim()
        val modelText = explicit.ifBlank {
            // No REASONING: tag emitted (likely token-cap mid-generation).
            // Surface what the model actually said instead of a generic
            // placeholder — strip the prefilled "RESULT: <TIER>" line so the
            // user sees the prose only.
            raw.substringAfter("\n", raw)
                .trim()
                .ifBlank { "Routing based on analysis." }
        }
        val reasoning = if (visualFinding.isNullOrBlank()) modelText
        else "$modelText\n\nVisual finding: $visualFinding"

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
