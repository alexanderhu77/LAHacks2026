package com.lahacks2026.pretriage.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deterministic fallback PHI scrubber. Catches the obvious cases — names, DOBs,
 * MRNs, emails, phone numbers — using a small set of regex patterns. Not a
 * substitute for tanaos in production, but enough to demo the privacy story
 * when the on-device model fails to load.
 */
class RegexAnonymizer : AnonymizerService {

    private data class Pattern(val category: String, val regex: Regex)

    // Order matters: more-specific patterns first so they win at overlap.
    private val patterns: List<Pattern> = listOf(
        // DOB: 1979-03-14, 03/14/1979, March 14 1979
        Pattern("DOB", Regex("""\b(19|20)\d{2}-\d{2}-\d{2}\b""")),
        Pattern("DOB", Regex("""\b\d{1,2}/\d{1,2}/(19|20)\d{2}\b""")),
        // MRN-style identifiers: A-2849-1077, MRN12345
        Pattern("MRN", Regex("""\b[A-Z]?-?\d{4,}-\d{3,}\b""")),
        Pattern("MRN", Regex("""\bMRN[-:\s]?\d{4,}\b""", RegexOption.IGNORE_CASE)),
        // Email + phone
        Pattern("EMAIL", Regex("""\b[\w.+-]+@[\w-]+\.[\w.-]+\b""")),
        Pattern("PHONE", Regex("""\b(?:\+?1[-. ]?)?\(?\d{3}\)?[-. ]?\d{3}[-. ]?\d{4}\b""")),
        // "Dr. James Chen" / "Dr James Chen"
        Pattern("PROVIDER", Regex("""\bDr\.?\s+[A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,2}\b""")),
        // Patient name, two or three capitalized words on a line that follows a labelled key.
        Pattern("PATIENT_NAME", Regex("""(?<=Patient[\s:])\s*[A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,2}""")),
        // Plain two/three-word capitalized name fallback (after our labels miss).
        Pattern("PATIENT_NAME", Regex("""\b[A-Z][a-z]+\s+[A-Z][a-z]+\b""")),
    )

    override suspend fun scrub(text: String, tokenMap: PhiTokenMap): Result<ScrubResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                var out = text
                val firedCategories = mutableSetOf<String>()
                for ((category, regex) in patterns) {
                    out = regex.replace(out) { match ->
                        val original = match.value
                        firedCategories += category
                        tokenMap.put(category, original)
                    }
                }
                ScrubResult(redacted = out, categories = firedCategories.toList())
            }
        }
}
