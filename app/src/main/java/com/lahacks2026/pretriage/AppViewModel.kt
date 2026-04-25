package com.lahacks2026.pretriage

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.InsurancePlanLoader
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.RedFlag
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.ml.RuntimeProvider
import com.lahacks2026.pretriage.triage.EmergencyShortCircuit
import com.lahacks2026.pretriage.triage.TriageOrchestrator
import com.lahacks2026.pretriage.ui.theme.ThemeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ModelKey { MedGemma, Whisper, Anonymizer }

data class WarmupState(
    val step: Int = 0,
    val total: Int = 3,
    val failures: Set<ModelKey> = emptySet(),
    val complete: Boolean = false,
)

data class IntakeState(
    val transcript: String = "",
    val recording: Boolean = false,
    val emergencyShortCircuit: Boolean = false,
)

enum class DeidPhase { Preview, Extracting, Scrubbing, Sending, Done }

data class DeidState(
    val phase: DeidPhase = DeidPhase.Preview,
)

data class AppState(
    val warmup: WarmupState = WarmupState(),
    val intake: IntakeState = IntakeState(),
    val image: Bitmap? = null,
    val decision: TriageDecision? = null,
    val deid: DeidState = DeidState(),
    val theme: ThemeKey = ThemeKey.Warm,
    val largeType: Boolean = false,
    val plan: InsurancePlan? = null,
    val triageInFlight: Boolean = false,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val orchestrator = TriageOrchestrator(RuntimeProvider.runtime)

    init {
        // Bundle insurance plan up front. Hardcoded to PPO for the demo.
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) {
                runCatching { InsurancePlanLoader.load(getApplication(), "ppo") }.getOrNull()
            }
            _state.update { it.copy(plan = plan) }
        }
    }

    fun warmUp() {
        if (_state.value.warmup.complete) return
        viewModelScope.launch {
            val rt = RuntimeProvider.runtime
            val started = System.currentTimeMillis()
            // Real warmup runs progress 0..1; we map onto 3 logical model steps.
            val result = withContext(Dispatchers.IO) {
                rt.warmUp { progress ->
                    val step = (progress * 3f).toInt().coerceIn(0, 3)
                    _state.update { s -> s.copy(warmup = s.warmup.copy(step = step)) }
                }
            }
            // Even with the FakeRuntime returning instantly, give the user something to see.
            // Sequentially advance step counts so the splash row checklist animates.
            for (next in (_state.value.warmup.step + 1)..3) {
                kotlinx.coroutines.delay(650)
                _state.update { s -> s.copy(warmup = s.warmup.copy(step = next)) }
            }
            val elapsed = System.currentTimeMillis() - started
            Log.i("PreTriage", "Cold-start warmup elapsed=${elapsed}ms result=${result.isSuccess}")
            _state.update { s ->
                s.copy(
                    warmup = s.warmup.copy(
                        step = 3,
                        complete = true,
                        failures = if (result.isFailure) setOf(ModelKey.MedGemma) else emptySet(),
                    )
                )
            }
        }
    }

    fun setTranscript(text: String) {
        val emergency = EmergencyShortCircuit.match(text) != null
        _state.update {
            it.copy(intake = it.intake.copy(transcript = text, emergencyShortCircuit = emergency))
        }
    }

    fun setRecording(recording: Boolean) {
        _state.update { it.copy(intake = it.intake.copy(recording = recording)) }
    }

    fun setImage(bitmap: Bitmap?) {
        _state.update { it.copy(image = bitmap) }
    }

    fun setTheme(key: ThemeKey) {
        _state.update { it.copy(theme = key) }
    }

    fun setLargeType(enabled: Boolean) {
        _state.update { it.copy(largeType = enabled) }
    }

    fun runTriage(onComplete: () -> Unit) {
        val s = _state.value
        if (s.triageInFlight) return
        _state.update { it.copy(triageInFlight = true, decision = null) }
        viewModelScope.launch {
            val req = TriageRequest(
                transcript = s.intake.transcript.ifBlank { "(no symptoms described)" },
                imageUri = if (s.image != null) "in-memory" else null,
                plan = s.plan,
            )
            val started = System.currentTimeMillis()
            val outcome = withContext(Dispatchers.IO) { orchestrator.triage(req) }
            val elapsed = System.currentTimeMillis() - started
            Log.i("PreTriage", "Triage decision elapsed=${elapsed}ms success=${outcome.isSuccess}")
            val decision = outcome.getOrElse { fallbackDecision(s) }
            _state.update { it.copy(decision = decision, triageInFlight = false) }
            onComplete()
        }
    }

    private fun fallbackDecision(s: AppState): TriageDecision {
        // Last-resort deterministic fallback if both MedGemma and the rule-based fake fail.
        return TriageDecision(
            severity = SeverityLevel.TELEHEALTH,
            reasoning = "We couldn't reach the on-device model. To stay safe, talk to a clinician via your telehealth plan.",
            redFlags = emptyList<RedFlag>(),
            recommendedAction = RecommendedAction(
                provider = s.plan?.name ?: "Telehealth",
                copay = s.plan?.copayFor(SeverityLevel.TELEHEALTH),
                intentHint = IntentHint.OPEN_TELEHEALTH_DEEP_LINK,
            ),
            confidence = 0.5,
        )
    }

    fun resetSession() {
        _state.update {
            AppState(
                warmup = WarmupState(step = 3, complete = true),
                theme = it.theme,
                largeType = it.largeType,
                plan = it.plan,
            )
        }
    }

    fun setDeidPhase(phase: DeidPhase) {
        _state.update { it.copy(deid = DeidState(phase)) }
    }
}
