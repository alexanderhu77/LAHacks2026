package com.lahacks2026.pretriage.ml

import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.TriageRequest
import kotlinx.coroutines.flow.StateFlow

interface MelangeRuntime {
    val isReady: StateFlow<Boolean>

    suspend fun warmUp(onProgress: (Float) -> Unit = {}): Result<Unit>

    suspend fun triage(req: TriageRequest): Result<TriageDecision>

    suspend fun transcribe(audioPath: String): Result<String>

    suspend fun extractDocument(imageUri: String): Result<String>
}
