package com.lahacks2026.pretriage.triage

import com.lahacks2026.pretriage.data.RedFlag

object EmergencyShortCircuit {

    private data class Pattern(val label: String, val regex: Regex)

    private val patterns: List<Pattern> = listOf(
        Pattern(
            "chest_pain",
            Regex(
                "\\b(crushing|severe|tight|squeezing)\\s+chest\\s+pain\\b" +
                    "|\\bchest\\s+pain\\b.*\\b(arm|jaw|short(ness)?\\s+of\\s+breath)\\b" +
                    "|\\bheart\\s+attack\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Pattern(
            "stroke_fast",
            Regex(
                "\\bface\\b[\\s\\w']{0,20}\\b(droop|drooping|sagging)\\b" +
                    "|\\barm\\b[\\s\\w']{0,15}\\b(weak(ness)?|numb(ness)?)\\b.*\\b(speech|talk|words?)\\b" +
                    "|\\bslurred\\s+speech\\b" +
                    "|\\bsudden\\s+(weakness|numbness)\\s+(on\\s+)?one\\s+side\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Pattern(
            "severe_bleeding",
            Regex(
                "\\b(uncontroll(ed|able)\\s+bleed(ing)?)\\b" +
                    "|\\b(spurt(ing)?|gush(ing)?)\\s+blood\\b" +
                    "|\\bbleeding\\s+won'?t\\s+stop\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Pattern(
            "breathing_difficulty",
            Regex(
                "\\b(can'?t\\s+breathe)\\b" +
                    "|\\b(struggling\\s+to\\s+breathe)\\b" +
                    "|\\b(gasping\\s+for\\s+air)\\b" +
                    "|\\bturning\\s+blue\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Pattern(
            "anaphylaxis",
            Regex(
                "\\bthroat\\b[\\s\\w']{0,10}\\b(closing|swelling|swollen)\\b" +
                    "|\\bface\\b[\\s\\w']{0,10}\\bswelling\\b.*\\b(hives?|rash)\\b" +
                    "|\\banaphyla(xis|ctic)\\b",
                RegexOption.IGNORE_CASE
            )
        )
    )

    fun match(transcript: String): RedFlag? {
        for (p in patterns) {
            val m = p.regex.find(transcript) ?: continue
            return RedFlag(label = p.label, matchedPhrase = m.value)
        }
        return null
    }
}
