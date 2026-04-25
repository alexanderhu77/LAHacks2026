package com.lahacks2026.pretriage.triage

import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.RecommendedAction
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import com.lahacks2026.pretriage.ml.MelangeRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TriageOrchestratorTest {

    private fun decision(
        severity: SeverityLevel = SeverityLevel.TELEHEALTH,
        confidence: Double = 0.8
    ) = TriageDecision(
        severity = severity,
        reasoning = "fake",
        redFlags = emptyList(),
        recommendedAction = RecommendedAction(
            provider = "Telehealth",
            copay = null,
            intentHint = IntentHint.OPEN_TELEHEALTH_DEEP_LINK
        ),
        confidence = confidence
    )

    private class ThrowingRuntime : MelangeRuntime {
        override val isReady: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun warmUp(onProgress: (Float) -> Unit) = Result.success(Unit)
        override suspend fun triage(req: TriageRequest): Result<TriageDecision> {
            fail("Runtime should not be called when emergency short-circuit fires")
            error("unreachable")
        }
        override suspend fun transcribe(audioPath: String) = Result.success("")
        override suspend fun extractDocument(imageUri: String) = Result.success("")
    }

    private class FixedRuntime(private val out: TriageDecision) : MelangeRuntime {
        override val isReady: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun warmUp(onProgress: (Float) -> Unit) = Result.success(Unit)
        override suspend fun triage(req: TriageRequest) = Result.success(out)
        override suspend fun transcribe(audioPath: String) = Result.success("")
        override suspend fun extractDocument(imageUri: String) = Result.success("")
    }

    @Test
    fun emergencyShortCircuit_firesBeforeRuntimeCall() = runTest {
        val o = TriageOrchestrator(ThrowingRuntime())
        val r = o.triage(TriageRequest("crushing chest pain for 30 minutes")).getOrThrow()
        assertEquals(SeverityLevel.EMERGENCY, r.severity)
        assertEquals(IntentHint.DIAL_911, r.recommendedAction.intentHint)
        assertEquals(1.0, r.confidence, 0.0001)
        assertEquals(1, r.redFlags.size)
    }

    @Test
    fun belowConfidenceFloor_downgradesSeverity() = runTest {
        val o = TriageOrchestrator(FixedRuntime(decision(SeverityLevel.TELEHEALTH, 0.4)))
        val r = o.triage(TriageRequest("vague tummy ache")).getOrThrow()
        assertEquals(SeverityLevel.URGENT_CARE, r.severity)
        assertTrue(r.reasoning.contains("Low confidence"))
    }

    @Test
    fun atOrAboveConfidenceFloor_passesThrough() = runTest {
        val o = TriageOrchestrator(FixedRuntime(decision(SeverityLevel.TELEHEALTH, 0.6)))
        val r = o.triage(TriageRequest("rash on arm")).getOrThrow()
        assertEquals(SeverityLevel.TELEHEALTH, r.severity)
        assertEquals("fake", r.reasoning)
    }

    @Test
    fun lowConfidenceEmergency_staysEmergency() = runTest {
        // moreConservative on EMERGENCY should be EMERGENCY (no escalation past it)
        val o = TriageOrchestrator(FixedRuntime(decision(SeverityLevel.URGENT_CARE, 0.3)))
        val r = o.triage(TriageRequest("something is wrong")).getOrThrow()
        assertEquals(SeverityLevel.EMERGENCY, r.severity)
    }
}
