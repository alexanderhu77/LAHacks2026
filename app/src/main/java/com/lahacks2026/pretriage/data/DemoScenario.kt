package com.lahacks2026.pretriage.data

data class DemoScenario(
    val title: String,
    val initialSymptom: String,
    val severity: SeverityLevel,
    val reasoning: String,
    val redFlags: List<String> = emptyList(),
    val confidence: Double = 0.9,
    val hasVisual: Boolean = false
)

object DemoScenarios {
    val MoleCheck = DemoScenario(
        title = "Mole Check",
        initialSymptom = "I have a dark mole on my arm that's been growing.",
        severity = SeverityLevel.URGENT_CARE,
        reasoning = "The growth and color profile suggests an atypical nevus. An in-person dermatological screening is recommended within 24-48 hours.",
        redFlags = listOf("Atypical growth pattern", "Potential melanoma precursor"),
        confidence = 0.88,
        hasVisual = true
    )

    val PinkEye = DemoScenario(
        title = "Pink Eye",
        initialSymptom = "My 5 year old's eye is red and goopy.",
        severity = SeverityLevel.TELEHEALTH,
        reasoning = "Symptoms are consistent with viral or bacterial conjunctivitis. Most cases can be managed via a video consultation.",
        redFlags = listOf("Ocular discharge"),
        confidence = 0.94,
        hasVisual = false
    )

    val ChestPain = DemoScenario(
        title = "Chest Pain",
        initialSymptom = "I've had crushing chest pain for 20 minutes radiating to my left arm.",
        severity = SeverityLevel.EMERGENCY,
        reasoning = "RED FLAG: Symptoms indicate a high risk of acute cardiac event. DO NOT DRIVE. Call emergency services now.",
        redFlags = listOf("Crushing chest pain", "Radiation to left arm", "Symptom duration > 15m"),
        confidence = 0.99,
        hasVisual = false
    )
    
    val All = listOf(MoleCheck, PinkEye, ChestPain)
}
