package com.lahacks2026.pretriage.triage

import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.ml.MelangeRuntime

class TriageOrchestrator(
    private val runtime: MelangeRuntime,
    private val confidenceFloor: Double = 0.6
) {
    suspend fun triage(req: TriageRequest): Result<TriageDecision> {
        EmergencyShortCircuit.match(req.transcript)?.let { redFlag ->
            val copay = req.plan?.copayFor(SeverityLevel.EMERGENCY)
            return Result.success(
                TriageDecision(
                    severity = SeverityLevel.EMERGENCY,
                    reasoning = "Emergency red flag detected (${redFlag.label}: \"${redFlag.matchedPhrase}\"). Call 911 immediately.",
                    redFlags = listOf(redFlag),
                    recommendedAction = RecommendedAction(
                        provider = "911",
                        copay = copay,
                        intentHint = IntentHint.DIAL_911
                    ),
                    confidence = 1.0
                )
            )
        }

        val result = runtime.triage(req)
        return result.map { decision ->
            if (decision.confidence < confidenceFloor) {
                val downgraded = decision.severity.moreConservative()
                decision.copy(
                    severity = downgraded,
                    reasoning = decision.reasoning +
                        "\n\n(Low confidence ${"%.2f".format(decision.confidence)} — recommending more conservative care: ${downgraded.name}.)"
                )
            } else {
                decision
            }
        }
    }
}
