package com.lahacks2026.pretriage.data

/**
 * One bubble in the Nora chat transcript. Persists in [com.lahacks2026.pretriage.ui.AppViewModel]
 * so it survives the round-trip through the camera flow.
 */
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Nora(val text: String) : ChatMessage()
    /** User-side photo bubble. Bitmap lives on AppState.image; this is a pointer for ordering. */
    data class Photo(val ref: String = "image") : ChatMessage()
    /** Nora-side request_photo bubble with primary "Take a photo" / "Skip" CTAs. */
    data class PhotoRequest(val reason: String) : ChatMessage()
}

/**
 * Scenario keys mirror the prototype's CHAT_SCRIPTS in screens.jsx. Picked from the
 * opening intake transcript via lightweight keyword detection — close enough for the
 * scripted demo path while a real model-driven nextTurn() lands later.
 */
enum class ScenarioKey { SELFCARE, TELEHEALTH, URGENT }

/** One step in a scripted dialogue. Mirrors the JSX `kind` field. */
sealed class ChatTurn {
    data class Ask(val text: String) : ChatTurn()
    data class RequestPhoto(val reason: String) : ChatTurn()
    /** Hand off to Triaging — orchestrator finalizes against the chat history. */
    data object Ready : ChatTurn()
}

object ChatScripts {

    private val selfcare = listOf(
        ChatTurn.Ask("I'm sorry you're feeling rough. Any fever, or trouble swallowing?"),
        ChatTurn.Ask("And the ear pain — is it sharp, or more of a dull pressure?"),
        ChatTurn.Ask("Anyone around you sick recently?"),
        ChatTurn.Ready,
    )

    private val telehealth = listOf(
        ChatTurn.Ask("Got it. Is it one eye or both?"),
        ChatTurn.Ask("Any fever, or are they otherwise acting like themselves?"),
        ChatTurn.Ask("Any recent cold symptoms or exposure at school?"),
        ChatTurn.Ready,
    )

    private val urgent = listOf(
        ChatTurn.Ask("Thanks. Roughly how long has it been changing — weeks, months?"),
        ChatTurn.Ask("Has the border or color changed, or is it just the size?"),
        ChatTurn.RequestPhoto("A photo would really help here — I can look at the borders and color contrast."),
        ChatTurn.Ready,
    )

    fun scriptFor(key: ScenarioKey): List<ChatTurn> = when (key) {
        ScenarioKey.SELFCARE -> selfcare
        ScenarioKey.TELEHEALTH -> telehealth
        ScenarioKey.URGENT -> urgent
    }
}

/**
 * Picks the scripted scenario from the opening transcript. Keep keywords aligned with
 * [com.lahacks2026.pretriage.ml.FakeMelangeRuntime] so the chat path matches the eventual
 * triage decision.
 */
object ScenarioPicker {
    private val urgentKeywords = listOf(
        "mole", "rash", "lesion", "bump", "growth", "skin spot", "cut", "wound", "bleeding",
    )
    private val telehealthKeywords = listOf(
        "eye", "pink eye", "red eye", "goopy", "discharge", "fever", "vomit", "diarrhea",
    )

    fun pick(transcript: String): ScenarioKey {
        val t = transcript.lowercase()
        return when {
            urgentKeywords.any { it in t } -> ScenarioKey.URGENT
            telehealthKeywords.any { it in t } -> ScenarioKey.TELEHEALTH
            else -> ScenarioKey.SELFCARE
        }
    }
}

/**
 * Mode flag for the model-driven chat turn.
 *
 * - DIALOGUE: model may emit any of ask_followup / request_photo / ready_to_triage.
 * - FINALIZE: forced (e.g., 7-turn cap reached); model must emit ready_to_triage.
 *   The orchestrator coerces non-conforming output via the timeout-fallback path.
 */
enum class ChatTurnMode { DIALOGUE, FINALIZE }

/**
 * One model turn in the chat. Mirrors PRD §6.3.
 *
 * - [AskFollowup] — Nora asks a focused question. Increments followupCount.
 * - [RequestPhoto] — Nora asks for a photo. Does NOT increment followupCount.
 * - [ReadyToTriage] — model has enough to triage; carries the final [TriageDecision].
 */
sealed class ChatTurnResponse {
    data class AskFollowup(val question: String) : ChatTurnResponse()
    data class RequestPhoto(val reason: String) : ChatTurnResponse()
    data class ReadyToTriage(val decision: TriageDecision) : ChatTurnResponse()
}
