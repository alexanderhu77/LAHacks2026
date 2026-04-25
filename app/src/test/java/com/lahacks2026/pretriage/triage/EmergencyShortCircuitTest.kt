package com.lahacks2026.pretriage.triage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EmergencyShortCircuitTest {

    @Test
    fun crushingChestPain_matches() {
        val r = EmergencyShortCircuit.match("I have crushing chest pain for 20 minutes")
        assertNotNull(r)
        assertEquals("chest_pain", r!!.label)
    }

    @Test
    fun chestPainRadiatingToArm_matches() {
        val r = EmergencyShortCircuit.match("chest pain that goes down my left arm")
        assertNotNull(r)
        assertEquals("chest_pain", r!!.label)
    }

    @Test
    fun fastStroke_faceDrooping_matches() {
        val r = EmergencyShortCircuit.match("My dad's face is drooping on one side")
        assertNotNull(r)
        assertEquals("stroke_fast", r!!.label)
    }

    @Test
    fun fastStroke_armWeaknessAndSpeech_matches() {
        val r = EmergencyShortCircuit.match("arm weakness and slurred speech for the last hour")
        assertNotNull(r)
        assertEquals("stroke_fast", r!!.label)
    }

    @Test
    fun cantBreathe_matches() {
        val r = EmergencyShortCircuit.match("I can't breathe and I'm gasping for air")
        assertNotNull(r)
        assertEquals("breathing_difficulty", r!!.label)
    }

    @Test
    fun anaphylaxis_throatClosing_matches() {
        val r = EmergencyShortCircuit.match("ate peanuts and my throat is closing up")
        assertNotNull(r)
        assertEquals("anaphylaxis", r!!.label)
    }

    @Test
    fun severeBleeding_matches() {
        val r = EmergencyShortCircuit.match("the cut is gushing blood and bleeding won't stop")
        assertNotNull(r)
        assertEquals("severe_bleeding", r!!.label)
    }

    @Test
    fun mildHeadache_doesNotMatch() {
        assertNull(EmergencyShortCircuit.match("I have a mild headache and feel tired"))
    }

    @Test
    fun runnyNose_doesNotMatch() {
        assertNull(EmergencyShortCircuit.match("runny nose and a sore throat for 2 days"))
    }

    @Test
    fun rashOnArm_doesNotMatch() {
        assertNull(EmergencyShortCircuit.match("a small itchy rash on my arm"))
    }

    @Test
    fun caseInsensitive() {
        val r = EmergencyShortCircuit.match("CRUSHING CHEST PAIN!")
        assertNotNull(r)
    }
}
