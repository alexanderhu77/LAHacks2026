package com.lahacks2026.pretriage.privacy

/**
 * Replaces PHI in [text] with deterministic placeholder tokens.
 * The mapping (placeholder -> original) is held in [PhiTokenMap], **never** transmitted.
 *
 * Two implementations exist:
 *  - `TanaosAnonymizer` (Melange-backed, lands with the model integration push)
 *  - `RegexAnonymizer` (deterministic fallback used in the demo and when model load fails)
 */
interface AnonymizerService {
    suspend fun scrub(text: String, tokenMap: PhiTokenMap): Result<ScrubResult>
}

data class ScrubResult(
    /** [text] with PHI replaced by `[CATEGORY_N]` placeholders. */
    val redacted: String,
    /** Categories that fired this run, useful for the de-id pipeline UI. */
    val categories: List<String>,
)
