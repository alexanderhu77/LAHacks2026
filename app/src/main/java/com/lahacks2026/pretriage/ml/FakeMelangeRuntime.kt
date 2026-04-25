package com.lahacks2026.pretriage.ml

import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.RedFlag
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMelangeRuntime(
    private val simulatedLatencyMs: Long = 800L
) : MelangeRuntime {

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override suspend fun warmUp(onProgress: (Float) -> Unit): Result<Unit> {
        onProgress(1f)
        return Result.success(Unit)
    }

    override suspend fun triage(req: TriageRequest): Result<TriageDecision> {
        delay(simulatedLatencyMs)
        val transcript = req.transcript.lowercase()
        val (severity, reasoning, intent, provider) = when {
            "rash" in transcript || "mole" in transcript || "skin" in transcript ->
                Quad(
                    SeverityLevel.TELEHEALTH,
                    "Skin concern described — a video visit with dermatology can confirm whether in-person evaluation is needed.",
                    IntentHint.OPEN_TELEHEALTH_DEEP_LINK,
                    "Dermatology telehealth"
                )
            "eye" in transcript || "pink eye" in transcript ->
                Quad(
                    SeverityLevel.TELEHEALTH,
                    "Eye redness/discharge described — telehealth can prescribe drops if bacterial conjunctivitis.",
                    IntentHint.OPEN_TELEHEALTH_DEEP_LINK,
                    "Pediatric telehealth"
                )
            "fever" in transcript || "vomit" in transcript || "diarrhea" in transcript ->
                Quad(
                    SeverityLevel.TELEHEALTH,
                    "Constitutional symptoms described — a virtual visit can triage and order labs if needed.",
                    IntentHint.OPEN_TELEHEALTH_DEEP_LINK,
                    "Primary-care telehealth"
                )
            "cut" in transcript || "wound" in transcript || "bleeding" in transcript ->
                Quad(
                    SeverityLevel.URGENT_CARE,
                    "Open wound described — urgent care can clean, evaluate, and close if needed.",
                    IntentHint.MAPS_QUERY_URGENT_CARE,
                    "Urgent care clinic"
                )
            else ->
                Quad(
                    SeverityLevel.SELF_CARE,
                    "No specific red flags detected. Self-care guidance and watchful waiting recommended.",
                    IntentHint.SHOW_SELF_CARE_TEXT,
                    "Self-care"
                )
        }

        val copay = req.plan?.copayFor(severity)
        return Result.success(
            TriageDecision(
                severity = severity,
                reasoning = reasoning,
                redFlags = emptyList<RedFlag>(),
                recommendedAction = RecommendedAction(
                    provider = provider,
                    copay = copay,
                    intentHint = intent
                ),
                confidence = 0.78
            )
        )
    }

    override suspend fun transcribe(audioPath: String): Result<String> =
        Result.failure(NotImplementedError("Whisper integration lands in a later push"))

    override suspend fun extractDocument(imageUri: String): Result<String> =
        Result.failure(NotImplementedError("Document extraction lands in a later push"))

    private data class Quad(
        val severity: SeverityLevel,
        val reasoning: String,
        val intent: IntentHint,
        val provider: String
    )
}
