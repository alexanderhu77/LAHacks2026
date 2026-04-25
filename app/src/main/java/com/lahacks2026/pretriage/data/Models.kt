package com.lahacks2026.pretriage.data

enum class SeverityLevel {
    SELF_CARE,
    TELEHEALTH,
    URGENT_CARE,
    EMERGENCY;

    fun moreConservative(): SeverityLevel = when (this) {
        SELF_CARE -> TELEHEALTH
        TELEHEALTH -> URGENT_CARE
        URGENT_CARE -> EMERGENCY
        EMERGENCY -> EMERGENCY
    }
}

data class TriageRequest(
    val transcript: String,
    val imageUri: String? = null,
    val plan: InsurancePlan? = null
)

data class RedFlag(
    val label: String,
    val matchedPhrase: String
)

data class RecommendedAction(
    val provider: String,
    val copay: Int?,
    val intentHint: IntentHint
)

enum class IntentHint {
    DIAL_911,
    OPEN_TELEHEALTH_DEEP_LINK,
    MAPS_QUERY_URGENT_CARE,
    SHOW_SELF_CARE_TEXT
}

data class TriageDecision(
    val severity: SeverityLevel,
    val reasoning: String,
    val redFlags: List<RedFlag>,
    val recommendedAction: RecommendedAction,
    val confidence: Double
)
