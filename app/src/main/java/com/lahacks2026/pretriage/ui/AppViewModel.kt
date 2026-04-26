package com.lahacks2026.pretriage.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lahacks2026.pretriage.data.ChatMessage
import com.lahacks2026.pretriage.data.ChatScripts
import com.lahacks2026.pretriage.data.ChatTurn
import com.lahacks2026.pretriage.data.ChatTurnMode
import com.lahacks2026.pretriage.data.ChatTurnResponse
import com.lahacks2026.pretriage.data.DiagnosticSummary
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.LaCareNetwork
import com.lahacks2026.pretriage.data.InsurancePlanLoader
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.ScenarioKey
import com.lahacks2026.pretriage.data.ScenarioPicker
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.ml.GeminiFallbackClient
import com.lahacks2026.pretriage.ml.RuntimeProvider
import com.lahacks2026.pretriage.triage.TriageOrchestrator
import com.lahacks2026.pretriage.ui.theme.ThemeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class WarmupStep { Triage, Voice, Privacy }

data class WarmupState(
    val completed: Set<WarmupStep> = emptySet(),
    val failed: Set<WarmupStep> = emptySet(),
    val active: WarmupStep? = WarmupStep.Triage,
    /** 0.0..1.0 download progress for the active Triage step's model fetch. */
    val triageProgress: Float = 0f,
) {
    val allDone: Boolean get() = (completed + failed).size == WarmupStep.values().size
}

data class IntakeUiState(
    val transcript: String = "",
    val recording: Boolean = false,
    val emergencyShortCircuit: Boolean = false,
)

/**
 * Multi-turn chat state. Survives the round-trip through CameraOffer/CameraCapture so a
 * Photo bubble can be appended in place. Driven by a per-scenario script for the demo
 * path; the script index advances every time the user sends a message.
 *
 * @property scenario derived from the opening intake transcript by [ScenarioPicker].
 * @property scriptIdx next [ChatTurn] index to render once the typing indicator clears.
 * @property pendingNoraTurn true while Nora is "thinking" — drives the dotted bubble.
 * @property photoInjected guards against re-appending a Photo bubble on recomposition.
 */
data class ChatUiState(
    val scenario: ScenarioKey = ScenarioKey.SELFCARE,
    val messages: List<ChatMessage> = emptyList(),
    val scriptIdx: Int = 0,
    val followupCount: Int = 0,
    val composer: String = "",
    val recording: Boolean = false,
    val pendingNoraTurn: Boolean = false,
    val photoInjected: Boolean = false,
    val readyToTriage: Boolean = false,
)

data class DeidState(
    val phase: DeidPhase = DeidPhase.Preview,
)

enum class DeidPhase { Preview, Extracting, Scrubbing, Sending, Done }

data class AppState(
    val warmup: WarmupState = WarmupState(),
    val intake: IntakeUiState = IntakeUiState(),
    val chat: ChatUiState = ChatUiState(),
    val image: Bitmap? = null,
    val decision: TriageDecision? = null,
    /** Result-page synthesis (potential diagnosis + reasoning paragraph). Fetched
     *  asynchronously from Gemini when [decision] lands; null while loading or if the
     *  call fails / GEMINI_API_KEY is unset. The UI should fall back gracefully. */
    val diagnosticSummary: DiagnosticSummary? = null,
    val diagnosticSummaryLoading: Boolean = false,
    val deid: DeidState = DeidState(),
    val theme: ThemeKey = ThemeKey.Warm,
    val largeType: Boolean = false,
    val plan: InsurancePlan? = null,
    /** LA Care Medi-Cal network the user picked on the first screen. Persists for
     *  the whole session and gets passed into the find-a-provider WebView at result time. */
    val selectedNetwork: LaCareNetwork? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val orchestrator = TriageOrchestrator(RuntimeProvider.runtime)

    /** Cloud fallback used when the on-device model times out or errors. No-op if GEMINI_API_KEY is blank. */
    private val geminiFallback = GeminiFallbackClient()

    private var triageJob: Job? = null
    private var chatTurnJob: Job? = null

    init {
        // Bundled mock plan; in a real build the user would pick.
        runCatching {
            val plan = InsurancePlanLoader.load(app, "ppo")
            _state.update { it.copy(plan = plan) }
        }
    }

    fun setTheme(key: ThemeKey) = _state.update { it.copy(theme = key) }
    fun setLargeType(on: Boolean) = _state.update { it.copy(largeType = on) }
    fun setNetwork(network: LaCareNetwork) = _state.update { it.copy(selectedNetwork = network) }

    /** Sequentially warm each model with a dummy inference, advancing the splash UI. */
    fun runWarmup() {
        viewModelScope.launch {
            advance(WarmupStep.Triage)
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    RuntimeProvider.runtime.warmUp { p ->
                        _state.update { s -> s.copy(warmup = s.warmup.copy(triageProgress = p)) }
                    }
                }
            }
            // Treat any failure as a fallback signal but don't block.
            res.onFailure { markFailed(WarmupStep.Triage) }
                .onSuccess { it.onFailure { markFailed(WarmupStep.Triage) } }
            markComplete(WarmupStep.Triage)

            advance(WarmupStep.Voice)
            // No real Whisper warmup yet; we still show the step for UX.
            kotlinx.coroutines.delay(420)
            markComplete(WarmupStep.Voice)

            advance(WarmupStep.Privacy)
            kotlinx.coroutines.delay(360)
            markComplete(WarmupStep.Privacy)
        }
    }

    private fun advance(step: WarmupStep) = _state.update {
        it.copy(warmup = it.warmup.copy(active = step))
    }

    private fun markComplete(step: WarmupStep) = _state.update {
        it.copy(warmup = it.warmup.copy(completed = it.warmup.completed + step))
    }

    private fun markFailed(step: WarmupStep) = _state.update {
        it.copy(warmup = it.warmup.copy(failed = it.warmup.failed + step))
    }

    fun setTranscript(text: String) = _state.update {
        it.copy(intake = it.intake.copy(transcript = text))
    }

    fun setRecording(on: Boolean) = _state.update {
        it.copy(intake = it.intake.copy(recording = on))
    }

    fun setImage(bm: Bitmap?) = _state.update { it.copy(image = bm) }

    // ── Chat ──────────────────────────────────────────────────────────────

    /**
     * Seed the chat with the opening intake transcript and pick the script. Idempotent —
     * if the chat already has messages (e.g., user popped back from camera), do nothing.
     */
    fun startChatIfNeeded() {
        val s = _state.value
        if (s.chat.messages.isNotEmpty()) return
        val opening = s.intake.transcript.ifBlank { "I'm not sure where to start." }
        val scenario = ScenarioPicker.pick(opening)
        _state.update {
            it.copy(
                chat = it.chat.copy(
                    scenario = scenario,
                    messages = listOf(ChatMessage.User(opening)),
                    pendingNoraTurn = true,
                ),
            )
        }
    }

    fun setComposer(text: String) = _state.update {
        it.copy(chat = it.chat.copy(composer = text))
    }

    fun setChatRecording(on: Boolean) = _state.update {
        it.copy(chat = it.chat.copy(recording = on))
    }

    /**
     * User pressed send. Appends the composer text as a User bubble, clears the composer,
     * and queues Nora's next turn. Caller is responsible for the emergency short-circuit
     * check — when it fires, navigate to Result instead of calling this.
     */
    fun sendUserMessage(): String? {
        val text = _state.value.chat.composer.trim()
        if (text.isEmpty()) return null
        _state.update {
            it.copy(
                chat = it.chat.copy(
                    messages = it.chat.messages + ChatMessage.User(text),
                    composer = "",
                    pendingNoraTurn = true,
                ),
            )
        }
        return text
    }

    /** User tapped Skip on a photo-request bubble. Treated as a synthetic user turn. */
    fun appendSkippedPhoto() = _state.update {
        it.copy(
            chat = it.chat.copy(
                messages = it.chat.messages + ChatMessage.User("(skipped photo)"),
                pendingNoraTurn = true,
            ),
        )
    }

    /**
     * Camera flow returned with an image. Append a Photo bubble (once) and queue Nora's
     * next turn. Bitmap is mirrored from AppState.image.
     */
    fun appendPhotoBubbleIfNeeded() {
        val s = _state.value
        if (s.chat.photoInjected || s.image == null) return
        _state.update {
            it.copy(
                chat = it.chat.copy(
                    messages = it.chat.messages + ChatMessage.Photo(),
                    photoInjected = true,
                    pendingNoraTurn = true,
                ),
            )
        }
    }

    /**
     * Advance the chat: call the on-device model for the next turn (PRD §6.3) and apply
     * its response — ask_followup → Nora bubble; request_photo → PhotoRequest bubble;
     * ready_to_triage → mark chat ready and stage the embedded decision so runTriage
     * can short-circuit straight to Result.
     *
     * Bounded by [CHAT_TURN_TIMEOUT_MS]. On timeout/error, falls back to Gemini
     * ([GEMINI_FALLBACK_TIMEOUT_MS]); on Gemini failure, surfaces a chat error bubble.
     */
    fun advanceNoraTurn() {
        val s0 = _state.value.chat
        if (!s0.pendingNoraTurn) return
        chatTurnJob?.cancel()
        chatTurnJob = viewModelScope.launch(Dispatchers.IO) {
            val mode = if (s0.followupCount >= 7) ChatTurnMode.FINALIZE else ChatTurnMode.DIALOGUE
            val image = _state.value.image
            val plan = _state.value.plan
            val started = System.currentTimeMillis()
            Log.i("AppViewModel", "advanceNoraTurn: nextTurn (mode=$mode, hasImage=${image != null}, followups=${s0.followupCount})")

            var runtimeError: Throwable? = null
            val response: ChatTurnResponse? = withTimeoutOrNull(CHAT_TURN_TIMEOUT_MS) {
                RuntimeProvider.runtime.nextTurn(
                    history = s0.messages,
                    image = image,
                    plan = plan,
                    followupCount = s0.followupCount,
                    mode = mode,
                ).onFailure { runtimeError = it }.getOrNull()
            }

            if (response != null) {
                applyChatResponse(response)
                return@launch
            }

            val elapsed = System.currentTimeMillis() - started
            val cause = runtimeError?.message?.takeIf { it.isNotBlank() }
                ?: "model timed out after ${elapsed / 1000}s"
            Log.w("AppViewModel", "nextTurn failed (${elapsed}ms cause=$cause); attempting Gemini fallback", runtimeError)

            val fallback = withTimeoutOrNull(GEMINI_FALLBACK_TIMEOUT_MS) {
                geminiFallback.nextTurn(
                    history = s0.messages,
                    plan = plan,
                    followupCount = s0.followupCount,
                    mode = mode,
                    hasPhoto = image != null,
                )
            }
            if (fallback != null) {
                Log.i("AppViewModel", "Gemini fallback succeeded (${fallback.javaClass.simpleName})")
                applyChatResponse(fallback)
            } else {
                Log.w("AppViewModel", "Gemini fallback unavailable; surfacing error bubble")
                appendChatError(cause)
            }
        }
    }

    /** Apply a parsed ChatTurnResponse to chat state. Mirrors the scripted-turn structure. */
    private fun applyChatResponse(resp: ChatTurnResponse) = when (resp) {
        is ChatTurnResponse.AskFollowup -> _state.update {
            it.copy(
                chat = it.chat.copy(
                    messages = it.chat.messages + ChatMessage.Nora(resp.question),
                    scriptIdx = it.chat.scriptIdx + 1,
                    followupCount = it.chat.followupCount + 1,
                    pendingNoraTurn = false,
                ),
            )
        }
        is ChatTurnResponse.RequestPhoto -> _state.update {
            it.copy(
                chat = it.chat.copy(
                    messages = it.chat.messages + ChatMessage.PhotoRequest(resp.reason),
                    scriptIdx = it.chat.scriptIdx + 1,
                    // request_photo does NOT count as a follow-up
                    pendingNoraTurn = false,
                ),
            )
        }
        is ChatTurnResponse.ReadyToTriage -> {
            _state.update {
                it.copy(
                    chat = it.chat.copy(
                        scriptIdx = it.chat.scriptIdx + 1,
                        pendingNoraTurn = false,
                        readyToTriage = true,
                    ),
                    // Model already produced the final decision — runTriage short-circuits.
                    decision = resp.decision,
                    diagnosticSummary = null,
                    diagnosticSummaryLoading = geminiFallback.isConfigured,
                )
            }
            fetchDiagnosticSummary(resp.decision)
        }
    }

    /**
     * Append a clearly-marked error bubble and stop the typing indicator. Deliberately
     * does NOT increment followupCount or set readyToTriage — the chat stalls so the
     * user sees the failure mode and can restart.
     */
    private fun appendChatError(cause: String) = _state.update {
        it.copy(
            chat = it.chat.copy(
                messages = it.chat.messages + ChatMessage.Nora("⚠️ Error: $cause. Tap × to start over."),
                pendingNoraTurn = false,
            ),
        )
    }

    /**
     * Kick off triage; emits decision into state. Caller observes & navigates.
     *
     * Hard-bounded by [TRIAGE_TIMEOUT_MS]: if the orchestrator hasn't returned a decision
     * within the cap (real-model inference can stall on low-RAM devices, or the runtime
     * may hit an unrecoverable error), we emit a deterministic safe fallback so the
     * Triaging screen advances to Result instead of spinning forever.
     */
    fun runTriage(): Job {
        triageJob?.cancel()
        val job = viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            // Chat already produced the final decision via nextTurn(ready_to_triage) —
            // the Triaging screen will see decisionAvailable=true and auto-advance.
            // No need to round-trip the model again.
            if (s.decision != null) {
                Log.i("AppViewModel", "runTriage: decision already provided by chat (severity=${s.decision.severity}); skipping")
                return@launch
            }
            val req = TriageRequest(
                transcript = composeTriageTranscript(s),
                imageUri = null,
                image = s.image,
                plan = s.plan,
            )
            val started = System.currentTimeMillis()
            val decision = withTimeoutOrNull(TRIAGE_TIMEOUT_MS) {
                orchestrator.triage(req).getOrNull()
            } ?: run {
                Log.w(
                    "AppViewModel",
                    "triage timed out after ${System.currentTimeMillis() - started}ms; attempting Gemini fallback",
                )
                val fallback = withTimeoutOrNull(GEMINI_FALLBACK_TIMEOUT_MS) {
                    geminiFallback.triage(transcript = req.transcript, plan = s.plan)
                }
                if (fallback != null) {
                    Log.i("AppViewModel", "Gemini triage fallback succeeded (severity=${fallback.severity})")
                    fallback
                } else {
                    Log.w("AppViewModel", "Gemini triage fallback unavailable; emitting safe default")
                    timeoutFallbackDecision(s.plan)
                }
            }
            _state.update {
                it.copy(
                    decision = decision,
                    diagnosticSummary = null,
                    diagnosticSummaryLoading = geminiFallback.isConfigured,
                )
            }
            fetchDiagnosticSummary(decision)
        }
        triageJob = job
        return job
    }

    /**
     * Fire-and-forget Gemini call that synthesizes a "potential diagnosis +
     * reasoning" pair from the chat history and the chosen severity. Result is
     * pushed into [AppState.diagnosticSummary]; null on failure (the result page
     * falls back to [TriageDecision.reasoning]).
     *
     * No-op when GEMINI_API_KEY is unset (loading flag stays false so the UI
     * doesn't render an indefinite spinner).
     */
    private fun fetchDiagnosticSummary(decision: TriageDecision) {
        if (!geminiFallback.isConfigured) {
            _state.update { it.copy(diagnosticSummaryLoading = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            val summary = withTimeoutOrNull(GEMINI_FALLBACK_TIMEOUT_MS) {
                geminiFallback.diagnosticSummary(
                    history = s.chat.messages,
                    severity = decision.severity,
                )
            }
            Log.i(
                "AppViewModel",
                "diagnosticSummary fetched (severity=${decision.severity}, hit=${summary != null})",
            )
            _state.update {
                it.copy(
                    diagnosticSummary = summary,
                    diagnosticSummaryLoading = false,
                )
            }
        }
    }


    /**
     * Deterministic safe-by-default decision used when the orchestrator times out or
     * fails. Recommends a telehealth visit so a clinician can review, with an honest
     * low confidence so the Result screen surfaces it as cautious rather than confident.
     */
    private fun timeoutFallbackDecision(plan: InsurancePlan?): TriageDecision = TriageDecision(
        severity = SeverityLevel.TELEHEALTH,
        reasoning = "I couldn't finish a recommendation in time. To be safe, please book a " +
            "telehealth visit so a clinician can review what you described.",
        redFlags = emptyList(),
        recommendedAction = RecommendedAction(
            provider = "Primary-care telehealth",
            copay = plan?.copayFor(SeverityLevel.TELEHEALTH),
            intentHint = IntentHint.OPEN_TELEHEALTH_DEEP_LINK,
        ),
        confidence = 0.4,
    )

    /**
     * Roll the multi-turn chat into a single transcript for the existing orchestrator.
     * Newest at the bottom; Nora questions kept inline for context. Photo bubbles are
     * not stringified — the bitmap is passed via state if/when image triage lands.
     */
    private fun composeTriageTranscript(s: AppState): String {
        if (s.chat.messages.isEmpty()) return s.intake.transcript
        return buildString {
            for (m in s.chat.messages) {
                when (m) {
                    is ChatMessage.User -> appendLine("Patient: ${m.text}")
                    is ChatMessage.Nora -> appendLine("Nora: ${m.text}")
                    is ChatMessage.PhotoRequest -> appendLine("Nora: (asked for a photo) ${m.reason}")
                    is ChatMessage.Photo -> appendLine("Patient: (attached a photo)")
                }
            }
        }.trim()
    }

    fun resetSession() {
        triageJob?.cancel()
        chatTurnJob?.cancel()
        _state.update {
            it.copy(
                intake = IntakeUiState(),
                chat = ChatUiState(),
                image = null,
                decision = null,
                diagnosticSummary = null,
                diagnosticSummaryLoading = false,
                // selectedNetwork intentionally preserved — user picked it once,
                // they shouldn't have to re-pick on every restart.
                deid = DeidState(),
            )
        }
    }

    fun setDeidPhase(phase: DeidPhase) = _state.update { it.copy(deid = DeidState(phase)) }

    companion object {
        /**
         * Hard cap on a single triage call from the moment runTriage() launches. Tuned to 60 s
         * so a slow real-model device still has a chance to finish; if it doesn't, the user
         * sees a safe telehealth recommendation rather than an infinite spinner.
         */
        const val TRIAGE_TIMEOUT_MS = 60_000L

        /**
         * Per-chat-turn timeout. Each Nora bubble is a real model call; on a healthy device
         * Qwen 3.5 2B finishes a short JSON object in 2–4 s. Past this, we try Gemini.
         */
        const val CHAT_TURN_TIMEOUT_MS = 12_000L

        /**
         * Hard cap on the Gemini cloud fallback (both chat-turn and final triage). Kept
         * short so a wedged network can't extend the user's total wait by much; on miss
         * we fall through to the safe-default decision / chat error bubble.
         */
        const val GEMINI_FALLBACK_TIMEOUT_MS = 10_000L
    }
}
