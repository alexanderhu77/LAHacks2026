package com.lahacks2026.pretriage.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Stand-in for the telehealth provider. In the real build this is a Ktor server
 * the demo phone hits over loopback; here we simulate the round trip in-process
 * to avoid taking a network dependency in the demo.
 *
 * Privacy contract: this only ever sees [redactedPayload] — the placeholder text
 * after [AnonymizerService] has scrubbed PHI. Re-identification happens locally
 * via [PhiTokenMap.resolve] before the response renders to the user.
 */
class MockTelehealthClient {

    suspend fun send(redactedPayload: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            delay(900) // simulate round-trip latency
            // Echo back a "provider response" that references the same placeholders.
            // The view layer re-identifies before display.
            buildString {
                appendLine("Received de-identified document.")
                appendLine()
                appendLine(redactedPayload)
                appendLine()
                append("Reply: thanks for sending — we'll review and reach out within an hour.")
            }
        }
    }
}
