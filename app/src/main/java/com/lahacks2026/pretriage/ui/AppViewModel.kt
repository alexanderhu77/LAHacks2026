package com.lahacks2026.pretriage.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.InsurancePlanLoader
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
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

data class DeidState(
    val phase: DeidPhase = DeidPhase.Preview,
)

enum class DeidPhase { Preview, Extracting, Scrubbing, Sending, Done }

data class AppState(
    val warmup: WarmupState = WarmupState(),
    val intake: IntakeUiState = IntakeUiState(),
    val image: Bitmap? = null,
    val decision: TriageDecision? = null,
    val deid: DeidState = DeidState(),
    val theme: ThemeKey = ThemeKey.Warm,
    val largeType: Boolean = false,
    val plan: InsurancePlan? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val orchestrator = TriageOrchestrator(RuntimeProvider.runtime)

    private var triageJob: Job? = null

    init {
        // Bundled mock plan; in a real build the user would pick.
        runCatching {
            val plan = InsurancePlanLoader.load(app, "ppo")
            _state.update { it.copy(plan = plan) }
        }
    }

    fun setTheme(key: ThemeKey) = _state.update { it.copy(theme = key) }
    fun setLargeType(on: Boolean) = _state.update { it.copy(largeType = on) }

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

    /** Kick off triage; emits decision into state. Caller observes & navigates. */
    fun runTriage(): Job {
        triageJob?.cancel()
        val job = viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            val req = TriageRequest(
                transcript = s.intake.transcript,
                imageUri = null,
                image = s.image,
                plan = s.plan,
            )
            val result = orchestrator.triage(req)
            val decision = result.getOrNull()
            _state.update { it.copy(decision = decision) }
        }
        triageJob = job
        return job
    }

    fun resetSession() {
        triageJob?.cancel()
        _state.update {
            it.copy(
                intake = IntakeUiState(),
                image = null,
                decision = null,
                deid = DeidState(),
            )
        }
    }

    fun setDeidPhase(phase: DeidPhase) = _state.update { it.copy(deid = DeidState(phase)) }
}
