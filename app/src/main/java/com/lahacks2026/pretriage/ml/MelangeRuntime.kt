package com.lahacks2026.pretriage.ml

import android.graphics.Bitmap
import com.lahacks2026.pretriage.data.ChatMessage
import com.lahacks2026.pretriage.data.ChatTurnMode
import com.lahacks2026.pretriage.data.ChatTurnResponse
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import kotlinx.coroutines.flow.StateFlow

interface MelangeRuntime {
    val isReady: StateFlow<Boolean>

    suspend fun warmUp(onProgress: (Float) -> Unit = {}): Result<Unit>

    suspend fun triage(req: TriageRequest): Result<TriageDecision>

    /**
     * Multi-turn chat. The model picks one of three response kinds per turn:
     * ask a follow-up, request a photo, or emit a final triage decision. PRD §6.3.
     */
    suspend fun nextTurn(
        history: List<ChatMessage>,
        image: Bitmap?,
        plan: InsurancePlan?,
        followupCount: Int,
        mode: ChatTurnMode,
    ): Result<ChatTurnResponse>

    suspend fun transcribe(audioPath: String): Result<String>

    suspend fun extractDocument(imageUri: String): Result<String>
}
