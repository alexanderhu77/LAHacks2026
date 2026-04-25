package com.lahacks2026.pretriage.data

data class DemoScenario(
    val title: String,
    val initialSymptom: String,
    val severity: String,
    val reasoning: String,
    val hasVisual: Boolean = false
)

object DemoScenarios {
    val MoleCheck = DemoScenario(
        title = "Mole Check",
        initialSymptom = "I have a dark mole on my arm that's been growing.",
        severity = "Urgent Care Today",
        reasoning = "The growth and color profile suggests an atypical nevus. An in-person dermatological screening is recommended within 24-48 hours.",
        hasVisual = true
    )

    val PinkEye = DemoScenario(
        title = "Pink Eye",
        initialSymptom = "My 5 year old's eye is red and goopy.",
        severity = "Telehealth Now",
        reasoning = "Symptoms are consistent with viral or bacterial conjunctivitis. Most cases can be managed via a video consultation.",
        hasVisual = false
    )

    val ChestPain = DemoScenario(
        title = "Chest Pain",
        initialSymptom = "I've had crushing chest pain for 20 minutes radiating to my left arm.",
        severity = "EMERGENCY IMMEDIATELY",
        reasoning = "RED FLAG: Symptoms indicate a high risk of acute cardiac event. DO NOT DRIVE. Call emergency services now.",
        hasVisual = false
    )
    
    val All = listOf(MoleCheck, PinkEye, ChestPain)
}
