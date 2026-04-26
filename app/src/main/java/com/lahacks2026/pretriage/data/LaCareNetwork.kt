package com.lahacks2026.pretriage.data

/**
 * One of LA Care's Medi-Cal network options shown on the [com.lahacks2026.pretriage.ui.network.PickNetworkScreen]
 * and forwarded into the find-a-provider WebView search at result time.
 *
 * @property id stable key used in URLs / JS bridge messages.
 * @property displayName user-facing name on the picker card.
 * @property tagline one-line subtitle on the card.
 * @property dropdownValue the literal string the LA Care provider-search dropdown
 *   uses for this network. Adjust if the page's option text drifts.
 */
data class LaCareNetwork(
    val id: String,
    val displayName: String,
    val tagline: String,
    val dropdownValue: String,
)

/**
 * Hardcoded list of LA Care Medi-Cal plan partners. Public knowledge — these are
 * LA Care's standard network options. Order matches the one shown on the LA Care
 * provider-search dropdown so the WebView injection can match by visible text.
 */
object LaCareNetworks {
    val all: List<LaCareNetwork> = listOf(
        LaCareNetwork(
            id = "lacare_direct",
            displayName = "L.A. Care Direct Network",
            tagline = "L.A. Care's own provider network",
            dropdownValue = "L.A. Care Direct Network",
        ),
        LaCareNetwork(
            id = "anthem_blue_cross",
            displayName = "Anthem Blue Cross Partnership Plan",
            tagline = "Anthem-managed Medi-Cal plan",
            dropdownValue = "Anthem Blue Cross",
        ),
        LaCareNetwork(
            id = "blue_shield_promise",
            displayName = "Blue Shield of California Promise",
            tagline = "Blue Shield Medi-Cal partner",
            dropdownValue = "Blue Shield Promise",
        ),
        LaCareNetwork(
            id = "kaiser_permanente",
            displayName = "Kaiser Permanente",
            tagline = "Integrated care, Kaiser facilities",
            dropdownValue = "Kaiser Permanente",
        ),
        LaCareNetwork(
            id = "health_net",
            displayName = "Health Net",
            tagline = "Centene-managed Medi-Cal plan",
            dropdownValue = "Health Net",
        ),
    )

    fun byId(id: String?): LaCareNetwork? = all.firstOrNull { it.id == id }
}
